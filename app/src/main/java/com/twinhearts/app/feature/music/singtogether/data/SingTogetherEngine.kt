package com.twinhearts.app.feature.music.singtogether.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.music.data.MusicRepository
import com.twinhearts.app.feature.music.data.remote.MusicApi
import com.twinhearts.app.feature.music.domain.model.MusicIdentity
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.player.MusicPlayerController
import com.twinhearts.app.feature.music.singtogether.domain.DuetLine
import com.twinhearts.app.feature.music.singtogether.domain.DuetLrcParser
import com.twinhearts.app.feature.music.singtogether.domain.FloatingReaction
import com.twinhearts.app.feature.music.singtogether.domain.LyricsLoad
import com.twinhearts.app.feature.music.singtogether.domain.SingNotice
import com.twinhearts.app.feature.music.singtogether.domain.SingPhase
import com.twinhearts.app.feature.music.singtogether.domain.SyncClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
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
import kotlin.math.abs

/** Everything the Sing Together screens draw, in one object. */
data class SingState(
    val phase: SingPhase = SingPhase.Idle,
    val isHost: Boolean = false,
    val songId: String? = null,
    val songTitle: String = "",
    val songArtist: String = "",
    val partnerName: String = "",
    /** The partner's picture is flowing. */
    val connected: Boolean = false,
    /** The host went quiet for a few seconds ("Reconnecting…"). */
    val stale: Boolean = false,
    val lyrics: LyricsLoad = LyricsLoad.Loading,
    val lines: List<DuetLine> = emptyList(),
    val micOff: Boolean = false,
    val cameraOff: Boolean = false,
    val speakerOff: Boolean = false,
    val hasCamera: Boolean = false,
    val localVideo: VideoTrack? = null,
    val remoteVideo: VideoTrack? = null,
    val egl: EglBase.Context? = null,
    val reactions: List<FloatingReaction> = emptyList(),
    val notice: SingNotice? = null,
) {
    val inRoom: Boolean get() = phase == SingPhase.Connecting || phase == SingPhase.InRoom
}

/**
 * Why this file exists: the brain of Sing Together. It follows the web app's couple-karaoke.js:
 *  - invite / accept / decline / cancel between the two phones,
 *  - a video + voice connection (WebRTC) set up through the shared-state pipe ([SingChannel]),
 *  - the HOST's song, play/pause, seek and track changes mirrored on the GUEST's phone,
 *  - timed duet lyrics, floating reactions, mic / camera / speaker switches.
 *
 * The song is played by the one shared [MusicPlayerController]; this class only tells it what to do.
 * The guest always gets a one-song queue, so its player can never move on to another song by itself
 * (that was the web app's "different song on each device" bug).
 */
@Singleton
class SingTogetherEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: SingApi,
    private val repository: MusicRepository,
    private val musicApi: MusicApi,
    private val controller: MusicPlayerController,
) : SingRtc.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(SingState())
    val state: StateFlow<SingState> = _state.asStateFlow()

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

    // Everything below is touched only on the main thread (the scope) or under [negotiation].
    private var watchers = 0
    private var who: MusicIdentity? = null
    private var channel: SingChannel? = null
    private var listenJob: Job? = null
    private var inviteJob: Job? = null
    private var syncJob: Job? = null
    private var staleJob: Job? = null
    private var offerJob: Job? = null
    private var readyJob: Job? = null
    private var connectingJob: Job? = null
    private var songWatchJob: Job? = null
    private var session: SingRtc? = null
    private val negotiation = Mutex()

    private var roomSongs: List<Song> = emptyList()
    private var roomSongId: String? = null
    private var loadingSongId: String? = null
    private var joinedAt = 0L
    private var lastHostMsgAt = 0L
    private var lastHostSync: HostSync? = null
    private val clock = SyncClock()
    private var lyricsToken = 0
    private var iceJob: Deferred<List<PeerConnection.IceServer>>? = null
    private var lyricsRaw: String? = null

    private var peerReady = false
    private var answered = false
    private var offerSdp: String? = null
    private var offerId = 0L
    private var lastOfferIdSeen = 0L
    private var lastAnswerSdp: String? = null
    private var pendingOffer: JsonObject? = null
    private val iceQueue = mutableListOf<IceCandidate>()
    private var reactionCounter = 0L

    private var savedAudioMode: Int? = null
    private var savedSpeaker = false

    private data class HostSync(val timeMs: Long, val playing: Boolean)

    // ── listening for invites ──

    /** The Music page is open: start listening for the partner's invite. Pair with [unwatch]. */
    fun watch() {
        watchers++
        if (watchers == 1) scope.launch { startListening() }
    }

    fun unwatch() {
        watchers = (watchers - 1).coerceAtLeast(0)
        if (watchers > 0) return
        when (_state.value.phase) {
            SingPhase.Connecting, SingPhase.InRoom -> leave()
            SingPhase.Inviting -> cancelInvite()
            else -> Unit
        }
        stopListening()
    }

    private suspend fun startListening() {
        // Let the Music page load first; listening for invites can wait a moment.
        delay(LISTEN_DELAY_MS)
        val me = repository.identity() ?: return
        if (watchers == 0) return
        who = me
        val ch = SingChannel(api, me.coupleId, me.myRole, scope)
        channel = ch
        // Invites older than a minute and a half are leftovers, not something to ring for.
        ch.setMinTimestamp(System.currentTimeMillis() - STALE_INVITE_MS)
        listenJob?.cancel()
        listenJob = scope.launch { ch.messages.collect { onMessage(it) } }
        ch.startPolling(IDLE_POLL_MS)
    }

    private fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        channel?.stopPolling()
        channel = null
        who = null
    }

    // ── invite flow ──

    /** I want to sing [song] with my partner. [songs] is the list the host's next / previous buttons use. */
    fun invite(song: Song, songs: List<Song>) {
        val ch = channel
        val me = who
        if (ch == null || me == null) {
            notice(SingNotice.CouldNotConnect)
            return
        }
        if (_state.value.phase != SingPhase.Idle) return
        roomSongs = songs.filter { it.audioUrl.isNotBlank() }.ifEmpty { listOf(song) }
        // From now on only answers that come after this moment count (not an old "accepted").
        ch.setMinTimestamp(System.currentTimeMillis() - CLOCK_TOLERANCE_MS)
        ch.startPolling(ROOM_POLL_MS)
        _state.update {
            SingState(
                phase = SingPhase.Inviting,
                isHost = true,
                songId = song.id,
                songTitle = song.title,
                songArtist = song.artist,
                partnerName = me.partnerName,
            )
        }
        scope.launch { ch.send("invite") { put("songId", song.id); put("songTitle", song.title) } }
        inviteJob?.cancel()
        inviteJob = scope.launch {
            delay(INVITE_WAIT_MS)
            if (_state.value.phase == SingPhase.Inviting) {
                cancelInvite()
                notice(SingNotice.NoAnswer)
            }
        }
    }

    fun cancelInvite() {
        if (_state.value.phase != SingPhase.Inviting) return
        inviteJob?.cancel()
        val ch = channel
        scope.launch { ch?.send("invite_cancel") }
        backToIdle()
    }

    fun accept() {
        val s = _state.value
        val id = s.songId
        if (s.phase != SingPhase.Invited || id == null) return
        val ch = channel ?: return
        scope.launch {
            ch.send("invite_accepted")
            join(id, host = false)
        }
    }

    fun decline() {
        if (_state.value.phase != SingPhase.Invited) return
        val ch = channel
        scope.launch { ch?.send("invite_declined") }
        backToIdle()
    }

    private fun backToIdle() {
        channel?.setMinTimestamp(System.currentTimeMillis() - STALE_INVITE_MS)
        channel?.startPolling(IDLE_POLL_MS)
        _state.update { SingState(notice = it.notice) }
    }

    // ── incoming messages ──

    private suspend fun onMessage(msg: JsonObject) {
        val type = msg.text("type") ?: return
        val phase = _state.value.phase
        when (type) {
            "invite" -> if (phase == SingPhase.Idle) onInvite(msg)
            "invite_cancel" -> if (phase == SingPhase.Invited) backToIdle()
            "invite_accepted" -> if (phase == SingPhase.Inviting) {
                inviteJob?.cancel()
                val id = _state.value.songId ?: return
                join(id, host = true)
            }
            "invite_declined" -> if (phase == SingPhase.Inviting) {
                inviteJob?.cancel()
                backToIdle()
                notice(SingNotice.Declined)
            }
            else -> if (_state.value.inRoom) onRoomMessage(type, msg)
        }
    }

    private fun onInvite(msg: JsonObject) {
        val id = msg.text("songId") ?: return
        val me = who ?: return
        _state.update {
            SingState(
                phase = SingPhase.Invited,
                isHost = false,
                songId = id,
                songTitle = msg.text("songTitle").orEmpty(),
                partnerName = me.partnerName,
            )
        }
    }

    private suspend fun onRoomMessage(type: String, msg: JsonObject) {
        val s = _state.value
        when (type) {
            "ready" -> if (s.isHost) {
                peerReady = true
                if (session != null) hostOffer(restart = false)
            }
            "offer" -> if (!s.isHost) applyOffer(msg)
            "answer" -> if (s.isHost) applyAnswer(msg)
            "ice" -> onRemoteIce(msg)
            "sync" -> if (!s.isHost) {
                lastHostMsgAt = System.currentTimeMillis()
                _state.update { it.copy(stale = false) }
                onHostSync(msg)
            }
            "control" -> if (!s.isHost) {
                lastHostMsgAt = System.currentTimeMillis()
                _state.update { it.copy(stale = false) }
                onHostControl(msg)
            }
            "request_sync" -> if (s.isHost) {
                broadcastSyncTick()
                val id = roomSongId
                val raw = lyricsRaw
                if (id != null && !raw.isNullOrBlank() && s.lines.isNotEmpty()) shareLyrics(id, raw)
            }
            "lyrics" -> onSharedLyrics(msg)
            "reaction" -> msg.text("emoji")?.let { showReaction(it) }
            // A "leave" from an earlier room can still sit in the list; 5 s covers the phones' clock gap.
            "leave" -> if ((msg.long("ts") ?: 0L) >= joinedAt - CLOCK_TOLERANCE_MS) {
                leaveRoom(tellPartner = false, notice = SingNotice.PartnerLeft)
            }
        }
    }

    // ── joining the room ──

    private suspend fun join(songId: String, host: Boolean) {
        val ch = channel ?: return
        val me = who ?: return
        resetRoomMemory()
        joinedAt = System.currentTimeMillis()
        lastHostMsgAt = joinedAt
        ch.setMinTimestamp(joinedAt - ROOM_STALE_MS) // drop offers / ICE / leave from earlier rooms
        ch.startPolling(ROOM_POLL_MS)
        _state.update {
            SingState(
                phase = SingPhase.Connecting,
                isHost = host,
                songId = songId,
                songTitle = it.songTitle,
                songArtist = it.songArtist,
                partnerName = me.partnerName,
            )
        }
        enterCallAudio()
        // Relay servers are asked for while the song and library load, not one after the other.
        iceJob = scope.async { iceServers() }

        if (!host) {
            val library = (repository.library(me) as? AppResult.Success)?.data?.songs.orEmpty()
            roomSongs = library.filter { it.audioUrl.isNotBlank() }
        }
        val song = roomSongs.firstOrNull { it.id == songId }
        if (song == null) {
            notice(SingNotice.SongUnavailable)
            leaveRoom(tellPartner = true)
            return
        }

        roomSongId = songId
        publishSong(song)
        if (host) {
            controller.play(roomSongs, songId)
            syncJob = scope.launch {
                while (isActive) {
                    delay(SYNC_EVERY_MS)
                    broadcastSyncTick()
                }
            }
        } else {
            loadGuestSong(songId, startAtMs = 0L, autoplay = false)
            scope.launch { ch.send("request_sync") }
            staleJob = scope.launch {
                while (isActive) {
                    delay(STALE_CHECK_MS)
                    if (System.currentTimeMillis() - lastHostMsgAt > STALE_AFTER_MS) {
                        _state.update { it.copy(stale = true) }
                        ch.send("request_sync")
                    }
                }
            }
        }
        watchSongChanges(host)
        loadLyrics(song)
        // The overlay must not stay forever if the connection is slow; the lyrics and the music still work.
        connectingJob = scope.launch {
            delay(CONNECTING_MAX_MS)
            if (_state.value.phase == SingPhase.Connecting) _state.update { it.copy(phase = SingPhase.InRoom) }
        }
        setupPeer(host)
    }

    private fun publishSong(song: Song) {
        _state.update { it.copy(songId = song.id, songTitle = song.title, songArtist = song.artist) }
    }

    /**
     * Every song change on this phone funnels through here (web: the AudioService 'play' hook).
     * HOST: whatever plays now is the truth, so tell the guest. GUEST: if the song is not the one the
     * host asked for, snap back.
     */
    private fun watchSongChanges(host: Boolean) {
        songWatchJob?.cancel()
        songWatchJob = scope.launch {
            // The state can lag a moment behind the player, so the real item is read from the player.
            controller.state.map { it.current?.id }.distinctUntilChanged().collect {
                if (!_state.value.inRoom) return@collect
                val actual = controller.player.currentMediaItem?.mediaId ?: return@collect
                if (host) {
                    if (actual == roomSongId) return@collect
                    val song = roomSongs.firstOrNull { it.id == actual } ?: controller.state.value.current ?: return@collect
                    roomSongId = actual
                    publishSong(song)
                    loadLyrics(song)
                    broadcastSyncTick()
                    channel?.send("control") { put("action", "track"); put("songId", actual) }
                } else {
                    val wanted = roomSongId ?: return@collect
                    if (actual != wanted) {
                        val sync = lastHostSync
                        loadGuestSong(wanted, sync?.timeMs ?: 0L, sync?.playing ?: true)
                    }
                }
            }
        }
    }

    // ── playback sync ──

    private fun broadcastSyncTick() {
        val ch = channel ?: return
        if (!_state.value.isHost || !_state.value.inRoom) return
        val song = controller.state.value.current ?: return
        val p = controller.player
        val seconds = p.currentPosition.coerceAtLeast(0L) / 1000.0
        val playing = p.playWhenReady && p.playbackState != Player.STATE_ENDED
        scope.launch {
            ch.send("sync") {
                put("songId", song.id)
                put("currentTime", seconds)
                put("playing", playing)
            }
        }
    }

    /** A control message, then a fresh snapshot right behind it: if one is lost the other still lands. */
    private fun broadcastControl(action: String, extra: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit = {}) {
        val ch = channel ?: return
        scope.launch {
            ch.send("control") { put("action", action); extra() }
            delay(SNAPSHOT_AFTER_CONTROL_MS)
            broadcastSyncTick()
        }
    }

    private suspend fun onHostSync(msg: JsonObject) {
        val songId = msg.text("songId") ?: return
        val timeMs = ((msg.number("currentTime") ?: 0.0) * 1000).toLong()
        val playing = msg.flag("playing") ?: false
        lastHostSync = HostSync(timeMs, playing)
        if (roomSongId != songId) {
            // Every tick used to start another parallel load of the same song; load each one once.
            if (loadingSongId != songId) loadGuestSong(songId, timeMs, playing)
            return
        }
        val travel = if (playing) clock.travelSeconds(msg.long("ts") ?: System.currentTimeMillis(), System.currentTimeMillis()) else 0.0
        val expectedMs = timeMs + (travel * 1000).toLong()
        val p = controller.player
        if (p.playbackState != Player.STATE_IDLE && abs(p.currentPosition - expectedMs) > DRIFT_MS) p.seekTo(expectedMs)
        if (playing && !p.playWhenReady) p.play()
        if (!playing && p.playWhenReady) p.pause()
    }

    private suspend fun onHostControl(msg: JsonObject) {
        val p = controller.player
        when (msg.text("action")) {
            "play" -> p.play()
            "pause" -> p.pause()
            "seek" -> p.seekTo(((msg.number("time") ?: 0.0) * 1000).toLong())
            "track" -> msg.text("songId")?.let { loadGuestSong(it, 0L, true) }
        }
    }

    private suspend fun loadGuestSong(songId: String, startAtMs: Long, autoplay: Boolean) {
        if (loadingSongId == songId) return
        loadingSongId = songId
        try {
            var song = roomSongs.firstOrNull { it.id == songId }
            if (song == null) {
                // The host may have picked a song added a moment ago: look in the library once more.
                val me = who
                if (me != null) {
                    val fresh = (repository.library(me) as? AppResult.Success)?.data?.songs.orEmpty()
                    roomSongs = fresh.filter { it.audioUrl.isNotBlank() }
                    song = roomSongs.firstOrNull { it.id == songId }
                }
            }
            if (song == null) {
                notice(SingNotice.SongUnavailable)
                return
            }
            // Set BEFORE playing, so the song watcher sees "already the right one".
            roomSongId = songId
            // One song only: this phone can never move on to another track by itself.
            controller.play(listOf(song), songId)
            controller.player.seekTo(startAtMs)
            if (!autoplay) controller.player.pause()
            publishSong(song)
            loadLyrics(song)
        } finally {
            loadingSongId = null
        }
    }

    // ── lyrics ──

    private fun loadLyrics(song: Song) {
        val token = ++lyricsToken
        _state.update { it.copy(lyrics = LyricsLoad.Loading, lines = emptyList()) }
        scope.launch {
            val me = who ?: return@launch
            var raw = readLyricsText(me, song)
            if (raw.isNullOrBlank()) {
                // Same as the web room: ask the server to look the lyrics up, then read them again.
                repository.findLyrics(me, song)
                raw = readLyricsText(me, song)
            }
            // The partner's phone may still be saving them: look again a few times before giving up.
            var tries = 0
            while (raw.isNullOrBlank() && tries < LYRICS_RETRIES && token == lyricsToken) {
                delay(LYRICS_RETRY_MS)
                raw = readLyricsText(me, song)
                tries++
            }
            if (token != lyricsToken) return@launch // another song started while this one was loading
            lyricsRaw = raw
            // The host also hands the text to the guest, so both screens always show the same words.
            if (_state.value.isHost && !raw.isNullOrBlank()) shareLyrics(song.id, raw)
            val lines = DuetLrcParser.parse(raw)
            _state.update { it.copy(lines = lines, lyrics = if (lines.isEmpty()) LyricsLoad.Missing else LyricsLoad.Ready) }
        }
    }

    private fun shareLyrics(songId: String, raw: String) {
        val ch = channel ?: return
        scope.launch { ch.send("lyrics") { put("songId", songId); put("lrc", raw) } }
    }

    private fun onSharedLyrics(msg: JsonObject) {
        if (_state.value.isHost || msg.text("songId") != roomSongId) return
        val raw = msg.text("lrc") ?: return
        val lines = DuetLrcParser.parse(raw)
        if (lines.isEmpty()) return
        lyricsRaw = raw
        _state.update { it.copy(lines = lines, lyrics = LyricsLoad.Ready) }
    }

    /** Raw lyrics text. It is read here (not through the library) because the duet speaker tags must survive. */
    private suspend fun readLyricsText(me: MusicIdentity, song: Song): String? {
        if (song.hasLyrics) {
            val full = (safeApiCall { musicApi.song(song.id, me.coupleId, me.myRole) } as? AppResult.Success)?.data?.lyrics
            if (!full.isNullOrBlank()) return full
        }
        val status = (safeApiCall { musicApi.lyricsStatus(song.id) } as? AppResult.Success)?.data
        return if (status?.state == "cached") status.lrc else null
    }

    // ── video connection ──

    private suspend fun setupPeer(host: Boolean) {
        val hasCamera = granted(Manifest.permission.CAMERA)
        val hasMic = granted(Manifest.permission.RECORD_AUDIO)
        val rtc = try {
            SingRtc(context, factory, egl, iceJob?.await() ?: iceServers(), hasCamera, hasMic, this)
        } catch (e: IllegalStateException) {
            notice(SingNotice.CouldNotConnect)
            leaveRoom(tellPartner = true)
            return
        }
        session = rtc
        _state.update { it.copy(egl = egl.eglBaseContext, localVideo = rtc.localVideo, hasCamera = rtc.hasCamera) }
        if (!hasCamera || !hasMic) notice(SingNotice.NoCameraOrMic)

        // An offer that arrived while the connection was still being built is used now.
        pendingOffer?.let { pendingOffer = null; applyOffer(it) }

        val ch = channel ?: return
        if (host) {
            // Offer only once the guest says it is ready; keep re-sending until answered, so a
            // message lost on the way can't stall the video.
            if (peerReady) hostOffer(restart = false)
            offerJob = scope.launch {
                while (isActive) {
                    delay(OFFER_RETRY_MS)
                    if (session == null) break
                    if (answered || _state.value.connected) continue
                    if (peerReady) hostOffer(restart = false)
                }
            }
        } else {
            ch.send("ready")
            readyJob = scope.launch {
                while (isActive) {
                    delay(READY_RETRY_MS)
                    val live = session ?: break
                    if (live.hasRemoteDescription) break
                    ch.send("ready")
                }
            }
        }
    }

    private suspend fun hostOffer(restart: Boolean) {
        negotiation.withLock {
            val rtc = session ?: return@withLock
            val ch = channel ?: return@withLock
            if (!_state.value.isHost) return@withLock
            if (restart || offerSdp == null) {
                val offer = rtc.createOffer(restart)
                rtc.setLocal(offer)
                offerSdp = offer.description
                offerId = System.currentTimeMillis()
                answered = false
            }
            val sdp = offerSdp ?: return@withLock
            val id = offerId
            ch.send("offer") {
                put("sdp", buildJsonObject { put("type", "offer"); put("sdp", sdp) })
                put("oid", id)
            }
        }
    }

    private suspend fun applyOffer(msg: JsonObject) {
        negotiation.withLock {
            val rtc = session
            if (rtc == null) {
                pendingOffer = msg
                return@withLock
            }
            val ch = channel ?: return@withLock
            val id = msg.long("oid") ?: 0L
            if (lastOfferIdSeen == id && rtc.hasRemoteDescription) {
                // The same offer again: our answer was probably lost, send it once more.
                lastAnswerSdp?.let { sdp -> sendAnswer(ch, sdp, id) }
                return@withLock
            }
            val sdp = (msg["sdp"] as? JsonObject)?.text("sdp") ?: return@withLock
            lastOfferIdSeen = id
            rtc.setRemote(SessionDescription(SessionDescription.Type.OFFER, sdp))
            flushIce(rtc)
            val answer = rtc.createAnswer()
            rtc.setLocal(answer)
            lastAnswerSdp = answer.description
            sendAnswer(ch, answer.description, id)
            readyJob?.cancel()
        }
    }

    private suspend fun sendAnswer(ch: SingChannel, sdp: String, oid: Long) {
        ch.send("answer") {
            put("sdp", buildJsonObject { put("type", "answer"); put("sdp", sdp) })
            put("oid", oid)
        }
    }

    private suspend fun applyAnswer(msg: JsonObject) {
        negotiation.withLock {
            val rtc = session ?: return@withLock
            if (!rtc.waitingForAnswer) return@withLock // a duplicate answer
            val id = msg.long("oid")
            if (id != null && offerId != 0L && id != offerId) return@withLock // an answer to an older offer
            val sdp = (msg["sdp"] as? JsonObject)?.text("sdp") ?: return@withLock
            rtc.setRemote(SessionDescription(SessionDescription.Type.ANSWER, sdp))
            answered = true
            flushIce(rtc)
        }
    }

    private fun onRemoteIce(msg: JsonObject) {
        val c = msg["candidate"] as? JsonObject ?: return
        val sdp = c.text("candidate") ?: return
        val candidate = IceCandidate(c.text("sdpMid"), c.text("sdpMLineIndex")?.toIntOrNull() ?: 0, sdp)
        val rtc = session
        // Candidates that arrive before the remote description would be thrown away: keep them.
        if (rtc != null && rtc.hasRemoteDescription) rtc.addRemoteCandidate(candidate) else iceQueue += candidate
    }

    private fun flushIce(rtc: SingRtc) {
        val queued = iceQueue.toList()
        iceQueue.clear()
        queued.forEach { rtc.addRemoteCandidate(it) }
    }

    private suspend fun iceServers(): List<PeerConnection.IceServer> {
        val root = (safeApiCall { api.turnCreds() } as? AppResult.Success)?.data
        val list = (root as? JsonObject)?.get("iceServers") as? JsonArray
        val parsed = list.orEmpty().mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val urls = when (val u = obj["urls"]) {
                is JsonArray -> u.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                is JsonPrimitive -> listOfNotNull(u.contentOrNull)
                else -> emptyList()
            }
            if (urls.isEmpty()) {
                null
            } else {
                PeerConnection.IceServer.builder(urls).apply {
                    obj.text("username")?.let { setUsername(it) }
                    obj.text("credential")?.let { setPassword(it) }
                }.createIceServer()
            }
        }
        // Same fallback as the web app: a public STUN server still works on friendly networks.
        return parsed.ifEmpty { listOf(PeerConnection.IceServer.builder(FALLBACK_STUN).createIceServer()) }
    }

    // SingRtc.Listener: called from WebRTC threads, so each one hops to the main scope.

    override fun onLocalCandidate(candidate: IceCandidate) {
        val ch = channel ?: return
        scope.launch {
            ch.send("ice") {
                put(
                    "candidate",
                    buildJsonObject {
                        put("candidate", candidate.sdp)
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                    },
                )
            }
        }
    }

    override fun onIceState(state: PeerConnection.IceConnectionState) {
        scope.launch {
            if (!_state.value.inRoom) return@launch
            when (state) {
                PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> {
                    connectingJob?.cancel()
                    offerJob?.cancel()
                    _state.update { it.copy(connected = true, phase = SingPhase.InRoom) }
                }
                PeerConnection.IceConnectionState.DISCONNECTED -> session?.restartIce()
                PeerConnection.IceConnectionState.FAILED -> {
                    if (_state.value.isHost) runCatching { hostOffer(restart = true) } else channel?.send("ready")
                }
                else -> Unit
            }
        }
    }

    override fun onRemoteVideo(track: VideoTrack) {
        scope.launch { _state.update { it.copy(remoteVideo = track) } }
    }

    // ── room controls ──

    private fun onlyHost(): Boolean {
        if (_state.value.isHost) return true
        notice(SingNotice.OnlyHostControls)
        return false
    }

    fun togglePlay() {
        if (!onlyHost()) return
        val p = controller.player
        if (p.playWhenReady && p.playbackState != Player.STATE_ENDED) {
            p.pause()
            broadcastControl("pause")
        } else {
            p.play()
            broadcastControl("play")
        }
    }

    // The song watcher tells the guest about the new song, so next / previous only move the host's queue.
    fun next() {
        if (onlyHost()) controller.next()
    }

    fun previous() {
        if (onlyHost()) controller.previous()
    }

    fun seekTo(positionMs: Long) {
        if (!onlyHost()) return
        controller.seekTo(positionMs)
        broadcastControl("seek") { put("time", positionMs.coerceAtLeast(0L) / 1000.0) }
    }

    fun toggleMic() {
        val off = !_state.value.micOff
        session?.setMicEnabled(!off)
        _state.update { it.copy(micOff = off) }
    }

    fun toggleCamera() {
        val off = !_state.value.cameraOff
        session?.setCameraEnabled(!off)
        _state.update { it.copy(cameraOff = off) }
    }

    fun toggleSpeaker() {
        val off = !_state.value.speakerOff
        session?.setSpeakerEnabled(!off)
        _state.update { it.copy(speakerOff = off) }
    }

    fun switchCamera() {
        val rtc = session ?: return
        rtc.switchCamera { ok -> if (!ok) scope.launch { notice(SingNotice.SwitchCameraFailed) } }
    }

    fun sendReaction(emoji: String) {
        showReaction(emoji)
        val ch = channel ?: return
        scope.launch { ch.send("reaction") { put("emoji", emoji) } }
    }

    private fun showReaction(emoji: String) {
        val id = ++reactionCounter
        // The horizontal spot is random, like the web app's floating hearts.
        val reaction = FloatingReaction(id, emoji, REACTION_MIN_X + (Math.random().toFloat() * REACTION_SPREAD_X))
        _state.update { it.copy(reactions = it.reactions + reaction) }
        scope.launch {
            delay(REACTION_LIFE_MS)
            _state.update { s -> s.copy(reactions = s.reactions.filterNot { it.id == id }) }
        }
    }

    fun leave() = leaveRoom(tellPartner = true)

    fun consumeNotice() = _state.update { it.copy(notice = null) }

    private fun notice(value: SingNotice) = _state.update { it.copy(notice = value) }

    // ── leaving ──

    private fun leaveRoom(tellPartner: Boolean, notice: SingNotice? = null) {
        val before = _state.value
        if (!before.inRoom) return
        val ch = channel
        syncJob?.cancel()
        staleJob?.cancel()
        offerJob?.cancel()
        readyJob?.cancel()
        connectingJob?.cancel()
        songWatchJob?.cancel()
        val closing = session
        session = null
        lyricsToken++
        controller.player.pause()
        leaveCallAudio()
        // The pictures leave the screen first; the camera is freed a moment later so no view is still drawing a freed track.
        _state.update { SingState(notice = notice ?: it.notice) }
        resetRoomMemory()
        scope.launch {
            if (tellPartner) ch?.send("leave")
            delay(RELEASE_DELAY_MS)
            closing?.close()
        }
        if (ch != null) {
            ch.setMinTimestamp(System.currentTimeMillis() - STALE_INVITE_MS)
            ch.startPolling(IDLE_POLL_MS)
        }
    }

    private fun resetRoomMemory() {
        peerReady = false
        answered = false
        offerSdp = null
        offerId = 0L
        lastOfferIdSeen = 0L
        lastAnswerSdp = null
        pendingOffer = null
        iceQueue.clear()
        roomSongId = null
        loadingSongId = null
        lastHostSync = null
        lyricsRaw = null
        iceJob = null
        clock.reset()
    }

    // ── phone audio ──

    /** Voice-call mode with the loudspeaker: the partner is heard without holding the phone to an ear. */
    @Suppress("DEPRECATION")
    private fun enterCallAudio() {
        if (savedAudioMode == null) {
            savedAudioMode = audio.mode
            savedSpeaker = audio.isSpeakerphoneOn
        }
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        audio.isSpeakerphoneOn = true
    }

    @Suppress("DEPRECATION")
    private fun leaveCallAudio() {
        val mode = savedAudioMode ?: return
        audio.isSpeakerphoneOn = savedSpeaker
        audio.mode = mode
        savedAudioMode = null
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    // ── tiny JSON readers ──

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }

    private fun JsonObject.number(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

    private fun JsonObject.flag(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

    private companion object {
        const val IDLE_POLL_MS = 4_000L
        const val LISTEN_DELAY_MS = 2_500L
        const val LYRICS_RETRIES = 5
        const val LYRICS_RETRY_MS = 3_000L
        const val ROOM_POLL_MS = 700L
        const val SYNC_EVERY_MS = 1_500L
        const val SNAPSHOT_AFTER_CONTROL_MS = 120L
        const val DRIFT_MS = 300L
        const val STALE_AFTER_MS = 4_000L
        const val STALE_CHECK_MS = 1_500L
        const val OFFER_RETRY_MS = 4_000L
        const val READY_RETRY_MS = 3_000L
        const val CONNECTING_MAX_MS = 6_000L
        const val INVITE_WAIT_MS = 90_000L
        const val STALE_INVITE_MS = 90_000L
        const val ROOM_STALE_MS = 30_000L
        const val CLOCK_TOLERANCE_MS = 5_000L
        const val RELEASE_DELAY_MS = 300L
        const val REACTION_LIFE_MS = 3_200L
        const val REACTION_MIN_X = 0.10f
        const val REACTION_SPREAD_X = 0.75f
        const val FALLBACK_STUN = "stun:stun.l.google.com:19302"
    }
}
