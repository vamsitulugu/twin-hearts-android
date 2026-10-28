package com.twinhearts.app.feature.calls.data

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.calls.data.webrtc.RtcSession
import com.twinhearts.app.feature.calls.domain.CallIdentity
import com.twinhearts.app.feature.calls.domain.CallPhase
import com.twinhearts.app.feature.calls.domain.EndReason
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the call screen needs to draw, in one object. */
data class CallState(
    val phase: CallPhase = CallPhase.Idle,
    val video: Boolean = false,
    val partnerName: String = "",
    val muted: Boolean = false,
    val speaker: Boolean = false,
    val cameraOff: Boolean = false,
    val seconds: Int = 0,
    val endReason: EndReason? = null,
    val localVideo: VideoTrack? = null,
    val remoteVideo: VideoTrack? = null,
    val egl: EglBase.Context? = null,
) {
    /** True from the first ring until the call is over. */
    val inCall: Boolean get() = phase != CallPhase.Idle && phase != CallPhase.Ended
}

/**
 * Why this file exists: the brain of Calls. One app-wide object (so an incoming call can open the
 * call screen from anywhere) that
 *  - listens for my partner's signals (offer, answer, ice, end, decline) on the old server,
 *  - starts, answers, declines and ends calls,
 *  - keeps the timer, mute, speaker and camera switches.
 *
 * Signalling is plain polling of `/api/call/signal`, exactly like the web app. A call only lives
 * while the app is open: closing the app ends listening (push-to-ring needs Notifications, 2.5).
 */
@Singleton
class CallEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CallRepository,
) : RtcSession.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lock = Mutex()
    private val _state = MutableStateFlow(CallState())
    val state: StateFlow<CallState> = _state.asStateFlow()

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val egl: EglBase by lazy { EglBase.create() }
    private val factory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext).createInitializationOptions(),
        )
        PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
            .createPeerConnectionFactory()
    }

    // Everything below is touched only on the main thread (scope) or under [lock].
    private var who: CallIdentity? = null
    private var session: RtcSession? = null
    private var pendingOfferSdp: String? = null
    private val remoteIceQueue = mutableListOf<IceCandidate>()
    private val localIceQueue = mutableListOf<IceCandidate>()
    private var localIceOpen = false
    private var lastSignalId = 0L
    private var cursorReady = false
    private var iAmCaller = false
    private var logged = false
    private var ringJob: Job? = null
    private var timerJob: Job? = null
    private var ringtone: Ringtone? = null

    // ───────────── listening ─────────────

    /**
     * Runs while the app is on screen (the caller cancels it when the app goes to the background).
     * Idle: one look every few seconds. During a call: faster, so the connection forms quickly.
     */
    suspend fun watch() {
        while (who == null) {
            who = repository.identity()
            if (who == null) delay(IDLE_POLL_MS)
        }
        while (true) {
            if (!cursorReady) initCursor()
            if (cursorReady) pollOnce()
            delay(if (_state.value.inCall) BUSY_POLL_MS else IDLE_POLL_MS)
        }
    }

    /** Skips every old signal, so a long-past offer or "decline" never rings the phone. */
    private suspend fun initCursor() {
        val me = who ?: return
        val r = repository.signals(me, 0L)
        if (r is AppResult.Success) {
            lastSignalId = r.data.maxOfOrNull { it.id } ?: 0L
            cursorReady = true
        }
    }

    private suspend fun pollOnce() {
        val me = who ?: return
        val r = repository.signals(me, lastSignalId) as? AppResult.Success ?: return
        for (signal in r.data.sortedBy { it.id }) {
            if (signal.id <= lastSignalId) continue
            lastSignalId = signal.id
            try {
                handle(signal.payload)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // One bad signal must not stop the others (the web app does the same).
            }
        }
    }

    private suspend fun handle(payload: JsonObject) {
        if (payload.truthy("gameCtx")) return // in-game face-to-face video, not a call
        if (payload.text("context") == "watch_together") return // Watch Together has its own screen
        when (payload.text("type")) {
            "offer" -> onOffer(payload)
            "answer" -> onAnswer(payload)
            "ice" -> onRemoteIce(payload)
            "end" -> onRemoteEnd()
            "decline" -> onRemoteDecline()
            else -> Unit // video-upgrade-* is not ported yet
        }
    }

    private fun onOffer(payload: JsonObject) {
        val phase = _state.value.phase
        if (phase != CallPhase.Idle && phase != CallPhase.Ended) return // already busy
        val sentAt = payload.text("ts")?.toLongOrNull() ?: 0L
        if (sentAt > 0 && System.currentTimeMillis() - sentAt > STALE_OFFER_MS) return
        val sdp = (payload["sdp"] as? JsonObject)?.text("sdp") ?: return
        pendingOfferSdp = sdp
        iAmCaller = false
        logged = false
        _state.value = CallState(
            phase = CallPhase.Incoming,
            video = payload.text("callType") == "video",
            partnerName = who?.partnerName.orEmpty(),
            egl = egl.eglBaseContext,
        )
        startRingtone()
        ringJob?.cancel()
        ringJob = scope.launch {
            delay(INCOMING_RING_MS)
            if (_state.value.phase == CallPhase.Incoming) finish(EndReason.Missed, tellPartner = false)
        }
    }

    private suspend fun onAnswer(payload: JsonObject) = lock.withLock {
        val s = session ?: return@withLock
        if (!s.waitingForAnswer) return@withLock // duplicate or late answer
        val sdp = (payload["sdp"] as? JsonObject)?.text("sdp") ?: return@withLock
        s.setRemote(SessionDescription(SessionDescription.Type.ANSWER, sdp))
        drainRemoteIce(s)
        ringJob?.cancel()
        _state.update { it.copy(phase = CallPhase.Connecting) }
    }

    private fun onRemoteIce(payload: JsonObject) {
        val c = payload["candidate"] as? JsonObject ?: return
        val sdp = c.text("candidate") ?: return
        val candidate = IceCandidate(c.text("sdpMid"), c.text("sdpMLineIndex")?.toIntOrNull() ?: 0, sdp)
        val s = session
        if (s != null && s.hasRemoteDescription) s.addRemoteCandidate(candidate) else remoteIceQueue += candidate
    }

    private fun onRemoteEnd() {
        when (_state.value.phase) {
            CallPhase.Incoming -> finish(EndReason.Missed, tellPartner = false)
            CallPhase.Idle, CallPhase.Ended -> Unit
            else -> finish(EndReason.Ended, tellPartner = false)
        }
    }

    private fun onRemoteDecline() {
        if (_state.value.inCall) finish(EndReason.Declined, tellPartner = false)
    }

    // ───────────── actions ─────────────

    /** Starts a call. The screen must have asked for the microphone (and camera) permission first. */
    fun startCall(video: Boolean) {
        scope.launch {
            lock.withLock {
                if (_state.value.inCall) return@withLock
                val me = who ?: repository.identity()?.also { who = it }
                if (me == null) {
                    _state.value = CallState(phase = CallPhase.Ended, endReason = EndReason.Failed)
                    return@withLock
                }
                resetForNewCall()
                iAmCaller = true
                _state.value = CallState(
                    phase = CallPhase.Calling,
                    video = video,
                    partnerName = me.partnerName,
                    speaker = video,
                    egl = egl.eglBaseContext,
                )
                try {
                    enterCallAudio(speaker = video)
                    val s = RtcSession(context, factory, egl, repository.iceServers(), video, this@CallEngine)
                    if (!_state.value.inCall) { s.close(); return@withLock } // ended while connecting
                    session = s
                    _state.update { it.copy(localVideo = s.localVideo) }
                    val offer = s.createOffer()
                    s.setLocal(offer)
                    if (session !== s) return@withLock // hung up while we were setting up: finish() frees it
                    send(
                        buildJsonObject {
                            put("type", "offer")
                            put("sdp", buildJsonObject { put("type", "offer"); put("sdp", offer.description) })
                            put("callType", if (video) "video" else "voice")
                            put("ts", System.currentTimeMillis())
                        },
                    )
                    openLocalIce()
                    repository.notifyPartner(me, video)
                    ringJob?.cancel()
                    ringJob = scope.launch {
                        delay(OUTGOING_RING_MS)
                        if (_state.value.phase == CallPhase.Calling) finish(EndReason.NoAnswer, tellPartner = true)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    fail()
                }
            }
        }
    }

    /** Answers the ringing call. Needs the same permissions as [startCall]. */
    fun accept() {
        scope.launch {
            lock.withLock {
                val offerSdp = pendingOfferSdp
                if (_state.value.phase != CallPhase.Incoming || offerSdp == null) return@withLock
                stopRingtone()
                ringJob?.cancel()
                val video = _state.value.video
                try {
                    enterCallAudio(speaker = video)
                    val s = RtcSession(context, factory, egl, repository.iceServers(), video, this@CallEngine)
                    if (!_state.value.inCall) { s.close(); return@withLock } // ended while connecting
                    session = s
                    _state.update { it.copy(phase = CallPhase.Connecting, speaker = video, localVideo = s.localVideo) }
                    s.setRemote(SessionDescription(SessionDescription.Type.OFFER, offerSdp))
                    drainRemoteIce(s)
                    val answer = s.createAnswer()
                    s.setLocal(answer)
                    if (session !== s) return@withLock // hung up while we were setting up: finish() frees it
                    send(
                        buildJsonObject {
                            put("type", "answer")
                            put("sdp", buildJsonObject { put("type", "answer"); put("sdp", answer.description) })
                        },
                    )
                    openLocalIce()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    fail()
                }
            }
        }
    }

    /** The person said no to the microphone or camera: show that, instead of a call that cannot work. */
    fun permissionDenied() {
        _state.value = CallState(phase = CallPhase.Ended, endReason = EndReason.PermissionDenied, partnerName = who?.partnerName.orEmpty())
    }

    fun decline() {
        if (_state.value.phase == CallPhase.Incoming) finish(EndReason.Declined, tellPartner = true)
    }

    /** Hangs up (also cancels a call that is still ringing). */
    fun hangUp() {
        val phase = _state.value.phase
        if (phase == CallPhase.Idle || phase == CallPhase.Ended) return
        finish(if (phase == CallPhase.Calling) EndReason.NoAnswer else EndReason.Ended, tellPartner = true)
    }

    /** The screen calls this after it has shown the end of the call and closed itself. */
    fun acknowledgeEnded() {
        if (_state.value.phase == CallPhase.Ended) _state.value = CallState()
    }

    fun toggleMute() {
        val muted = !_state.value.muted
        session?.setMicEnabled(!muted)
        _state.update { it.copy(muted = muted) }
    }

    fun toggleSpeaker() {
        val on = !_state.value.speaker
        @Suppress("DEPRECATION")
        audio.isSpeakerphoneOn = on
        _state.update { it.copy(speaker = on) }
    }

    fun toggleCamera() {
        val off = !_state.value.cameraOff
        session?.setCameraEnabled(!off)
        _state.update { it.copy(cameraOff = off) }
    }

    fun switchCamera() {
        session?.switchCamera()
    }

    // ───────────── RtcSession.Listener (WebRTC threads) ─────────────

    override fun onLocalCandidate(candidate: IceCandidate) {
        scope.launch {
            if (localIceOpen) sendCandidate(candidate) else localIceQueue += candidate
        }
    }

    override fun onIceState(state: PeerConnection.IceConnectionState) {
        scope.launch {
            when (state) {
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED -> onConnected()
                PeerConnection.IceConnectionState.FAILED -> if (_state.value.inCall) finish(EndReason.Failed, tellPartner = true)
                else -> Unit
            }
        }
    }

    override fun onRemoteVideo(track: VideoTrack) {
        scope.launch { _state.update { it.copy(remoteVideo = track) } }
    }

    // ───────────── internals ─────────────

    private fun onConnected() {
        if (_state.value.phase == CallPhase.Connected || !_state.value.inCall) return
        ringJob?.cancel()
        _state.update { it.copy(phase = CallPhase.Connected, seconds = 0) }
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1_000)
                _state.update { it.copy(seconds = it.seconds + 1) }
            }
        }
    }

    private fun drainRemoteIce(s: RtcSession) {
        remoteIceQueue.forEach { s.addRemoteCandidate(it) }
        remoteIceQueue.clear()
    }

    /** My own ice candidates are held back until the offer/answer is on the server, so they never arrive first. */
    private suspend fun openLocalIce() {
        localIceOpen = true
        val held = localIceQueue.toList()
        localIceQueue.clear()
        held.forEach { sendCandidate(it) }
    }

    private suspend fun sendCandidate(c: IceCandidate) {
        send(
            buildJsonObject {
                put("type", "ice")
                put(
                    "candidate",
                    buildJsonObject {
                        put("candidate", c.sdp)
                        put("sdpMid", c.sdpMid)
                        put("sdpMLineIndex", c.sdpMLineIndex)
                    },
                )
            },
        )
    }

    /** Posts one signal; tries again a couple of times because the free server may answer "busy". */
    private suspend fun send(payload: JsonObject) {
        val me = who ?: return
        repeat(SEND_TRIES) { attempt ->
            if (repository.send(me, payload) is AppResult.Success) return
            if (attempt < SEND_TRIES - 1) delay(SEND_RETRY_MS)
        }
    }

    private fun fail() = finish(EndReason.Failed, tellPartner = true)

    /** The one way a call ends: tell the partner, write the chat line, free everything, show the end. */
    private fun finish(reason: EndReason, tellPartner: Boolean) {
        val before = _state.value
        if (before.phase == CallPhase.Idle || before.phase == CallPhase.Ended) return
        val closing = session
        session = null
        ringJob?.cancel()
        timerJob?.cancel()
        stopRingtone()
        leaveCallAudio()
        val me = who
        val wasCaller = iAmCaller
        val shouldLog = wasCaller && !logged && me != null
        logged = true
        _state.value = before.copy(
            phase = CallPhase.Ended,
            endReason = reason,
            localVideo = null,
            remoteVideo = null,
        )
        resetCallMemory()
        scope.launch {
            if (me != null && tellPartner) {
                val type = when (reason) {
                    EndReason.Declined -> "decline"
                    else -> "end"
                }
                send(buildJsonObject { put("type", type) })
            }
            if (me != null && shouldLog) {
                val status = when {
                    reason == EndReason.Declined -> "declined"
                    before.phase == CallPhase.Connected || reason == EndReason.Ended && before.seconds > 0 -> "ended"
                    else -> "missed"
                }
                repository.log(me, before.video, status, before.seconds)
            }
            // The screen needs a moment to let go of the video before WebRTC frees it.
            delay(RELEASE_DELAY_MS)
            closing?.close()
        }
    }

    private fun resetForNewCall() {
        resetCallMemory()
        session?.close()
        session = null
    }

    private fun resetCallMemory() {
        pendingOfferSdp = null
        remoteIceQueue.clear()
        localIceQueue.clear()
        localIceOpen = false
    }

    private fun enterCallAudio(speaker: Boolean) {
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        audio.isSpeakerphoneOn = speaker
    }

    private fun leaveCallAudio() {
        @Suppress("DEPRECATION")
        audio.isSpeakerphoneOn = false
        audio.mode = AudioManager.MODE_NORMAL
    }

    private fun startRingtone() {
        stopRingtone()
        ringtone = runCatching {
            RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                ?.also {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.isLooping = true
                    it.play()
                }
        }.getOrNull()
    }

    private fun stopRingtone() {
        runCatching { ringtone?.stop() }
        ringtone = null
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull

    private fun JsonObject.truthy(key: String): Boolean {
        val p = this[key] as? JsonPrimitive ?: return false
        return p !is JsonNull && p.contentOrNull?.lowercase() !in setOf("false", "0", "")
    }

    private companion object {
        const val IDLE_POLL_MS = 4_000L
        const val BUSY_POLL_MS = 2_000L
        const val STALE_OFFER_MS = 45_000L
        const val INCOMING_RING_MS = 45_000L
        const val OUTGOING_RING_MS = 30_000L
        const val SEND_TRIES = 3
        const val SEND_RETRY_MS = 500L
        const val RELEASE_DELAY_MS = 600L
    }
}
