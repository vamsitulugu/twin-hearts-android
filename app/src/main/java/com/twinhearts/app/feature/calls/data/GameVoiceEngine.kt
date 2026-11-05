package com.twinhearts.app.feature.calls.data

import android.content.Context
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.sound.TwinSound
import com.twinhearts.app.feature.calls.data.webrtc.RtcSession
import com.twinhearts.app.feature.calls.domain.CallIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
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
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class GameVoicePhase { Idle, Ringing, Connecting, Active }

/** One-off messages the screen shows once (then calls [GameVoiceEngine.clearNotice]). */
enum class GameVoiceNotice { Ended, Declined, NoAnswer, Lost, Failed, NeedsMic, Busy, NotPaired }

data class GameVoiceState(
    val phase: GameVoicePhase = GameVoicePhase.Idle,
    val partnerName: String = "",
    val muted: Boolean = false,
    /** Speaker is ON by default: people look at the board while they talk (same as the web app). */
    val speaker: Boolean = true,
    val notice: GameVoiceNotice? = null,
) {
    /** True while the call is being set up or running (the pill is on screen). */
    val running: Boolean get() = phase == GameVoicePhase.Connecting || phase == GameVoicePhase.Active
}

/**
 * Why this file exists: the in-game voice call (web: `GameVoice` in game-comms.js). It is a small,
 * audio-only call that lives inside one game: no call page, only mute, speaker and end.
 *
 * It speaks the web app's signals exactly: the same `/api/call/signal` rows, tagged
 * `gameCtx = "v:<game>"` plus a `callId`, so a web partner and an Android partner can talk to each
 * other. [CallEngine] ignores `gameCtx` signals on purpose, so the two never collide.
 *
 * Known limit: [CallService] belongs to [CallEngine], so there is no notification keeping the
 * microphone alive when the phone is locked. The call is meant to be used while the game is on screen.
 */
@Singleton
class GameVoiceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CallRepository,
    private val calls: CallEngine,
) : RtcSession.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lock = Mutex()
    /** Signals leave one at a time, in order, so an offer always lands before its ice candidates. */
    private val sendLock = Mutex()
    private val _state = MutableStateFlow(GameVoiceState())
    val state: StateFlow<GameVoiceState> = _state.asStateFlow()

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    // Audio only: no video codecs are needed, so the factory is built without them.
    private val factory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext).createInitializationOptions(),
        )
        PeerConnectionFactory.builder().createPeerConnectionFactory()
    }
    // RtcSession wants one; a voice call never opens a camera, so it is never used for pictures.
    private val egl: EglBase by lazy { EglBase.create() }

    // Touched only on the main thread or under [lock].
    private var who: CallIdentity? = null
    private var game: String? = null
    private var cursor = 0L
    private var cursorReady = false
    private var session: RtcSession? = null
    private var callId: String? = null
    private var iAmCaller = false
    private var offer: JsonObject? = null
    private val iceBuffer = HashMap<String, MutableList<IceCandidate>>()
    private var foreground = 0
    private var pollJob: Job? = null
    private var ringJob: Job? = null
    private var connectJob: Job? = null
    private var dropJob: Job? = null

    // ───────────── lifecycle (called by Games) ─────────────

    /**
     * Runs while a game is on screen and the app is visible (the caller cancels it when it is not).
     * Idle: looks for my partner's call every few seconds. During a call: faster.
     */
    suspend fun watch(gameKey: String) {
        if (game != gameKey) {
            game = gameKey
            cursorReady = false
        }
        foreground++
        startPolling()
        try {
            awaitCancellation()
        } finally {
            foreground--
        }
    }

    /** The game was closed: the call ends for both people and any ringing stops (like the web `onExit`). */
    fun leave() {
        if (_state.value.running) stop() else dropRing()
        game = null
        cursorReady = false
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (who == null && game != null) {
                who = repository.identity()
                if (who == null) delay(IDLE_POLL_MS)
            }
            while (who != null && game != null && (foreground > 0 || _state.value.running)) {
                if (!cursorReady) initCursor()
                if (cursorReady) pollOnce()
                delay(if (_state.value.running) BUSY_POLL_MS else IDLE_POLL_MS)
            }
        }
    }

    /** Starts from "now": a signal sent before the game opened never rings the phone. */
    private suspend fun initCursor() {
        val me = who ?: return
        var after = cursor
        repeat(MAX_CURSOR_PAGES) {
            val r = repository.signals(me, after) as? AppResult.Success ?: return
            after = maxOf(after, r.data.maxOfOrNull { it.id } ?: after)
            // A short page (or none) means there is nothing older left to skip.
            if (r.data.size < PAGE_SIZE) {
                cursor = after
                cursorReady = true
                return
            }
        }
        cursor = after
        cursorReady = true
    }

    private suspend fun pollOnce() {
        val me = who ?: return
        val r = repository.signals(me, cursor) as? AppResult.Success ?: return
        for (signal in r.data.sortedBy { it.id }) {
            if (signal.id <= cursor) continue
            cursor = signal.id
            val ctx = signal.payload.text("gameCtx")
            if (ctx != "v:$game") continue
            try {
                handle(signal.payload)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // One bad signal must not stop the others (the web app does the same).
            }
        }
    }

    private suspend fun handle(m: JsonObject) {
        val id = m.text("callId") ?: return
        when (m.text("type")) {
            "offer" -> onOffer(id, m)
            "ice" -> onIce(id, m)
            "answer" -> onAnswer(id, m)
            "end" -> onEnd(id)
            "decline" -> onDecline(id)
        }
    }

    // ───────────── incoming signals ─────────────

    private suspend fun onOffer(id: String, m: JsonObject) {
        val st = _state.value
        when {
            st.phase == GameVoicePhase.Idle -> {
                // A normal call is running: the game call would fight it for the microphone.
                if (calls.state.value.inCall) {
                    send(buildJsonObject { put("type", "decline") }, id)
                    return
                }
                ring(m)
            }
            st.phase == GameVoicePhase.Ringing -> if (offer?.text("callId") != id) offer = m // partner called again
            st.phase == GameVoicePhase.Connecting && iAmCaller && who?.myRole == "user2" && session?.waitingForAnswer == true -> {
                // Both of us tapped call at the same moment: user2 gives way to user1's call.
                dropOwnCallSilently()
                acceptOffer(m)
            }
        }
    }

    private fun onIce(id: String, m: JsonObject) {
        val c = m["candidate"] as? JsonObject ?: return
        val sdp = c.text("candidate") ?: return
        val candidate = IceCandidate(c.text("sdpMid"), c.text("sdpMLineIndex")?.toIntOrNull() ?: 0, sdp)
        val s = session
        if (id == callId && s != null && s.hasRemoteDescription) {
            s.addRemoteCandidate(candidate)
        } else {
            val list = iceBuffer.getOrPut(id) { mutableListOf() }
            if (list.size < MAX_BUFFERED_ICE) list += candidate
        }
    }

    private suspend fun onAnswer(id: String, m: JsonObject) = lock.withLock {
        val s = session ?: return@withLock
        if (id != callId || !iAmCaller || !s.waitingForAnswer) return@withLock
        val sdp = (m["sdp"] as? JsonObject)?.text("sdp") ?: return@withLock
        runCatching {
            s.setRemote(SessionDescription(SessionDescription.Type.ANSWER, sdp))
            drainIce(id, s)
        }
    }

    private fun onEnd(id: String) {
        val st = _state.value
        if (st.phase == GameVoicePhase.Ringing && offer?.text("callId") == id) {
            dropRing()
        } else if (st.running && id == callId) {
            reset(GameVoiceNotice.Ended)
        }
    }

    private fun onDecline(id: String) {
        if (iAmCaller && _state.value.running && id == callId) reset(GameVoiceNotice.Declined)
    }

    // ───────────── actions ─────────────

    /** Calls my partner. The screen has already asked for the microphone permission. */
    fun start() {
        scope.launch {
            lock.withLock {
                if (_state.value.phase != GameVoicePhase.Idle || game == null) return@withLock
                if (calls.state.value.inCall) {
                    notice(GameVoiceNotice.Busy)
                    return@withLock
                }
                val me = who ?: repository.identity()?.also { who = it }
                if (me == null) {
                    notice(GameVoiceNotice.NotPaired)
                    return@withLock
                }
                val id = newCallId()
                callId = id
                iAmCaller = true
                _state.update { GameVoiceState(phase = GameVoicePhase.Connecting, partnerName = me.partnerName) }
                try {
                    enterAudio(speaker = true)
                    val s = RtcSession(context, factory, egl, repository.iceServers(), false, this@GameVoiceEngine)
                    if (callId != id) { s.close(); return@withLock } // ended while connecting
                    session = s
                    val description = s.createOffer()
                    // Queued before setLocal, so the offer is always sent before the first ice candidate.
                    send(
                        buildJsonObject {
                            put("type", "offer")
                            put("sdp", buildJsonObject { put("type", "offer"); put("sdp", description.description) })
                        },
                        id,
                    )
                    s.setLocal(description)
                    armConnectTimeout(id, GameVoiceNotice.NoAnswer)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (callId == id) reset(GameVoiceNotice.Failed, tellPartner = true)
                }
            }
        }
    }

    /** Answers the ringing call. The screen has already asked for the microphone permission. */
    fun accept() {
        val m = offer ?: return
        scope.launch { acceptOffer(m) }
    }

    fun decline() {
        val m = offer ?: return
        val id = m.text("callId")
        dropRing()
        if (id != null) scope.launch { send(buildJsonObject { put("type", "decline") }, id) }
    }

    /** The person said no to the microphone. */
    fun permissionDenied() {
        val m = offer
        if (_state.value.phase == GameVoicePhase.Ringing && m != null) decline()
        notice(GameVoiceNotice.NeedsMic)
    }

    /** Ends the call for both people. */
    fun stop() = reset(null, tellPartner = true)

    fun toggleMute() {
        val muted = !_state.value.muted
        if (muted) TwinSound.switchOff() else TwinSound.switchOn()
        session?.setMicEnabled(!muted)
        _state.update { it.copy(muted = muted) }
    }

    /** Real routing: speaker on = loudspeaker, off = earpiece (the web page can only change the volume). */
    fun toggleSpeaker() {
        val on = !_state.value.speaker
        if (on) TwinSound.switchOn() else TwinSound.switchOff()
        setSpeaker(on)
        _state.update { it.copy(speaker = on) }
    }

    fun clearNotice() = _state.update { it.copy(notice = null) }

    // ───────────── internals ─────────────

    private suspend fun acceptOffer(m: JsonObject) = lock.withLock {
        val id = m.text("callId") ?: return@withLock
        val sdp = (m["sdp"] as? JsonObject)?.text("sdp") ?: return@withLock
        val me = who ?: return@withLock
        ringJob?.cancel()
        offer = null
        callId = id
        iAmCaller = false
        _state.update { GameVoiceState(phase = GameVoicePhase.Connecting, partnerName = me.partnerName) }
        try {
            enterAudio(speaker = true)
            val s = RtcSession(context, factory, egl, repository.iceServers(), false, this@GameVoiceEngine)
            if (callId != id) { s.close(); return@withLock }
            session = s
            s.setRemote(SessionDescription(SessionDescription.Type.OFFER, sdp))
            drainIce(id, s)
            val answer = s.createAnswer()
            send(
                buildJsonObject {
                    put("type", "answer")
                    put("sdp", buildJsonObject { put("type", "answer"); put("sdp", answer.description) })
                },
                id,
            )
            s.setLocal(answer)
            armConnectTimeout(id, GameVoiceNotice.Failed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (callId == id) reset(GameVoiceNotice.Failed, tellPartner = true)
        }
    }

    /** Frees my own unanswered call without telling the partner (their call is the one that continues). */
    private fun dropOwnCallSilently() {
        connectJob?.cancel()
        session?.close()
        session = null
        callId = null
    }

    private fun drainIce(id: String, s: RtcSession) {
        iceBuffer.remove(id)?.forEach { s.addRemoteCandidate(it) }
    }

    private fun ring(m: JsonObject) {
        offer = m
        _state.update { GameVoiceState(phase = GameVoicePhase.Ringing, partnerName = who?.partnerName.orEmpty()) }
        ringJob?.cancel()
        ringJob = scope.launch {
            delay(RING_MS)
            if (_state.value.phase == GameVoicePhase.Ringing) dropRing()
        }
        TwinSound.tap()
        runCatching {
            (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
                ?.vibrate(VibrationEffect.createWaveform(RING_VIBRATION, -1))
        }
    }

    private fun dropRing() {
        ringJob?.cancel()
        offer = null
        if (_state.value.phase == GameVoicePhase.Ringing) _state.update { GameVoiceState(speaker = it.speaker) }
    }

    private fun armConnectTimeout(id: String, what: GameVoiceNotice) {
        connectJob?.cancel()
        connectJob = scope.launch {
            delay(CONNECT_MS)
            if (callId == id && _state.value.phase == GameVoicePhase.Connecting) reset(what, tellPartner = true)
        }
    }

    /** The one way a call ends: tell the partner, free the microphone, show a short note. */
    private fun reset(note: GameVoiceNotice?, tellPartner: Boolean = false) {
        val wasRunning = _state.value.running
        val id = callId
        connectJob?.cancel()
        dropJob?.cancel()
        val closing = session
        session = null
        callId = null
        iAmCaller = false
        iceBuffer.clear()
        runCatching { closing?.close() }
        if (wasRunning) {
            leaveAudio()
            if (_state.value.phase == GameVoicePhase.Active) TwinSound.callEnded()
        }
        if (wasRunning && tellPartner && id != null) {
            scope.launch { send(buildJsonObject { put("type", "end") }, id) }
        }
        _state.update { if (it.phase == GameVoicePhase.Ringing) it.copy(notice = note) else GameVoiceState(notice = note) }
    }

    private fun notice(note: GameVoiceNotice) = _state.update { it.copy(notice = note) }

    /** Posts one signal with the game tag and call id the web app expects. */
    private fun send(body: JsonObject, id: String) {
        val me = who ?: return
        val payload = buildJsonObject {
            body.forEach { (k, v) -> put(k, v) }
            put("gameCtx", "v:$game")
            put("callId", id)
            put("ts", System.currentTimeMillis())
        }
        scope.launch {
            sendLock.withLock {
                repeat(SEND_TRIES) { attempt ->
                    if (repository.send(me, payload) is AppResult.Success) return@withLock
                    if (attempt < SEND_TRIES - 1) delay(SEND_RETRY_MS)
                }
            }
        }
    }

    // ───────────── RtcSession.Listener (WebRTC threads) ─────────────

    override fun onLocalCandidate(candidate: IceCandidate) {
        val id = callId ?: return
        scope.launch {
            send(
                buildJsonObject {
                    put("type", "ice")
                    put(
                        "candidate",
                        buildJsonObject {
                            put("candidate", candidate.sdp)
                            put("sdpMid", candidate.sdpMid)
                            put("sdpMLineIndex", candidate.sdpMLineIndex)
                        },
                    )
                },
                id,
            )
        }
    }

    override fun onIceState(state: PeerConnection.IceConnectionState) {
        scope.launch {
            when (state) {
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED -> {
                    dropJob?.cancel()
                    connectJob?.cancel()
                    if (_state.value.phase == GameVoicePhase.Connecting) {
                        TwinSound.callConnected()
                        _state.update { it.copy(phase = GameVoicePhase.Active) }
                    }
                }
                // A short network blip: give it a few seconds to come back (the web app waits 10 s).
                PeerConnection.IceConnectionState.DISCONNECTED -> if (_state.value.running) {
                    dropJob?.cancel()
                    dropJob = scope.launch {
                        delay(DROP_GRACE_MS)
                        if (_state.value.running) reset(GameVoiceNotice.Lost, tellPartner = true)
                    }
                }
                PeerConnection.IceConnectionState.FAILED -> if (_state.value.running) reset(GameVoiceNotice.Lost, tellPartner = true)
                else -> Unit
            }
        }
    }

    override fun onRemoteVideo(track: VideoTrack) = Unit // voice only

    // ───────────── audio routing ─────────────

    private fun enterAudio(speaker: Boolean) {
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        setSpeaker(speaker)
    }

    private fun leaveAudio() {
        setSpeaker(false)
        audio.mode = AudioManager.MODE_NORMAL
    }

    private fun setSpeaker(on: Boolean) {
        @Suppress("DEPRECATION")
        audio.isSpeakerphoneOn = on
    }

    private fun newCallId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull

    private companion object {
        const val IDLE_POLL_MS = 3_000L
        const val BUSY_POLL_MS = 1_500L
        const val CONNECT_MS = 40_000L
        const val RING_MS = 45_000L
        const val DROP_GRACE_MS = 10_000L
        const val PAGE_SIZE = 50
        const val MAX_CURSOR_PAGES = 40
        const val MAX_BUFFERED_ICE = 80
        const val SEND_TRIES = 3
        const val SEND_RETRY_MS = 500L
        val RING_VIBRATION = longArrayOf(0, 120, 60, 120)
    }
}
