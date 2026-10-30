package com.twinhearts.app.feature.movie.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.movie.data.MovieRepository
import com.twinhearts.app.feature.movie.data.local.MovieLocalStore
import com.twinhearts.app.feature.movie.data.local.SavedLocalMovie
import com.twinhearts.app.feature.movie.data.upload.CloudUploader
import com.twinhearts.app.feature.movie.data.upload.UploadRequest
import com.twinhearts.app.feature.movie.data.upload.UploadState
import com.twinhearts.app.feature.movie.domain.model.ChatMessage
import com.twinhearts.app.feature.movie.domain.model.CloudLibrary
import com.twinhearts.app.feature.movie.domain.model.HistoryItem
import com.twinhearts.app.feature.movie.domain.model.MovieIdentity
import com.twinhearts.app.feature.movie.domain.model.MovieReaction
import com.twinhearts.app.feature.movie.domain.model.RoomStatus
import com.twinhearts.app.feature.movie.domain.model.SharedMovie
import com.twinhearts.app.feature.movie.domain.model.WatchRoom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/** Which full-screen part of Watch Together is showing. */
/** [NeedFile]: the movie has started but this phone has no copy of the file yet (asks the person to pick it). */
enum class WatchStage { Setup, Countdown, NeedFile, Watching }

data class WatchUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val who: MovieIdentity? = null,
    val room: WatchRoom? = null,
    val stage: WatchStage = WatchStage.Setup,
    val countdown: Int = 0,
    val busy: Boolean = false,
    /** What the video player opens: a phone file (Direct mode) or a signed cloud link. */
    val streamUrl: String? = null,
    val chat: List<ChatMessage> = emptyList(),
    val cloud: UiState<CloudLibrary> = UiState.Loading,
    val history: UiState<List<HistoryItem>> = UiState.Loading,
    /** Emoji floating over the movie right now (this phone's and the partner's). */
    val reactions: List<MovieReaction> = emptyList(),
    val keepScreenOn: Boolean = true,
) {
    /** The partner asked to start and this phone has not answered yet. */
    val inviteVisible: Boolean
        get() = room?.status == RoomStatus.StartRequested && !room.startRequestedByMe
}

/**
 * Why this file exists: it is the "room engine". It polls the shared room (the old server has no
 * websocket, R-66), keeps both phones on the same clock, runs the countdown and sends play / pause
 * / seek actions. The player composable only shows what this class decides.
 */
@HiltViewModel
class WatchViewModel @Inject constructor(
    private val repo: MovieRepository,
    private val uploader: CloudUploader,
    private val store: MovieLocalStore,
) : ViewModel() {

    private val _state = MutableStateFlow(WatchUiState())
    val state: StateFlow<WatchUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    /** The server's own friendly sentence (for example "A movie session is already in progress."), shown as is. */
    private val _notes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val notes: SharedFlow<String> = _notes.asSharedFlow()

    /** Progress of a cloud upload. It keeps running when this screen is closed. */
    val upload: StateFlow<UploadState> = uploader.state

    private var clockOffsetMs = 0L
    private var lastSeq = 0L
    private var lastChatStamp: String? = null
    private var localUri: String? = null
    private var savedMovie: SavedLocalMovie? = null
    private var sentFirstState = false
    private var reactionsSupported = true
    private var lastReactionAt = 0L

    init {
        viewModelScope.launch { store.keepScreenOn.collect { on -> _state.update { it.copy(keepScreenOn = on) } } }
        viewModelScope.launch {
            uploader.state.collect { s ->
                if (s is UploadState.Done) {
                    _messages.tryEmit(R.string.movie_toast_uploaded)
                    uploader.dismiss()
                    loadCloud()
                }
            }
        }
        viewModelScope.launch {
            val who = repo.identity()
            if (who == null) {
                _state.update { it.copy(loading = false, error = AppError.Unauthorized()) }
                return@launch
            }
            _state.update { it.copy(who = who) }
            savedMovie = store.savedLocalMovie(who.coupleId)
            clockOffsetMs = repo.clockOffsetMs()
            refreshRoom(first = true)
        }
    }

    fun serverNowMs(): Long = System.currentTimeMillis() + clockOffsetMs

    /** Runs while the screen is visible. Cancelled automatically when the app goes to the background. */
    suspend fun runLoop() {
        var tick = 0
        while (true) {
            updateStage()
            if (tick % POLL_EVERY == 0) refreshRoom(first = false)
            if (tick % CHAT_EVERY == 0 && _state.value.stage == WatchStage.Watching) refreshChat()
            if (tick % REACTION_EVERY == 0 && _state.value.stage == WatchStage.Watching) refreshReactions()
            trimReactions()
            tick++
            delay(TICK_MS)
        }
    }

    fun retry() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch { refreshRoom(first = true) }
    }

    // ── Room reading ──

    private suspend fun refreshRoom(first: Boolean) {
        val who = _state.value.who ?: return
        when (val r = repo.room(who)) {
            is AppResult.Success -> applyRoom(r.data)
            is AppResult.Failure -> if (first) _state.update { it.copy(loading = false, error = r.error) }
        }
    }

    private fun applyRoom(room: WatchRoom?) {
        if (room != null) lastSeq = maxOf(lastSeq, room.actionSeq)
        // After an app restart the server still has this phone's movie name, but the file link is gone from memory.
        // Take it back from the phone's storage when it is the same movie.
        val saved = savedMovie
        if (room != null && localUri == null && room.sharedMovieId == null && saved != null && saved.title == room.myTitle) {
            localUri = saved.uri
        }
        _state.update { it.copy(room = room, loading = false, error = null) }
        updateStage()
    }

    /** Works out Setup / Countdown / Watching from the room and the shared clock. */
    private fun updateStage() {
        val s = _state.value
        val room = s.room
        val start = room?.scheduledStartMs
        val running = room != null && (room.status == RoomStatus.Countdown || room.status == RoomStatus.Watching)
        if (room == null || !running || start == null) {
            if (s.stage != WatchStage.Setup) {
                lastReactionAt = 0L
                _state.update { it.copy(stage = WatchStage.Setup, streamUrl = null, chat = emptyList(), reactions = emptyList()) }
            }
            return
        }
        val msLeft = start - serverNowMs()
        if (msLeft > 0) {
            val sec = ((msLeft + 999) / 1000).toInt().coerceAtMost(3)
            _state.update { it.copy(stage = WatchStage.Countdown, countdown = sec) }
            return
        }
        if (s.stage == WatchStage.Watching) return
        if (room.sharedMovieId == null && localUri == null) {
            // This phone has no copy of the file (never picked, or the app was restarted). Before, the countdown
            // froze here and the movie never opened. Now it asks for the file and carries on as soon as it is picked.
            if (s.stage != WatchStage.NeedFile) _state.update { it.copy(stage = WatchStage.NeedFile) }
            return
        }
        _state.update { it.copy(stage = WatchStage.Watching, streamUrl = localUri) }
        if (room.sharedMovieId != null) loadCloudUrl(room.sharedMovieId)
        // Only the very first start sends "play from 0". A phone that joins later must not pull the partner back to 0.
        if (room.status == RoomStatus.Countdown && !sentFirstState && !room.playing && room.positionSec == 0.0) {
            sentFirstState = true
            sendAction(playing = true, positionSec = 0.0)
        }
    }

    private fun loadCloudUrl(movieId: String) {
        val who = _state.value.who ?: return
        viewModelScope.launch {
            when (val r = repo.playbackUrl(who, movieId)) {
                is AppResult.Success -> _state.update { it.copy(streamUrl = r.data) }
                is AppResult.Failure -> _messages.tryEmit(R.string.movie_toast_cloud_unavailable)
            }
        }
    }

    // ── Lobby actions ──

    fun pickLocalMovie(uri: String, title: String, durationSec: Int) {
        localUri = uri
        rememberLocalMovie(SavedLocalMovie(uri, title, durationSec))
        runAction(R.string.movie_toast_save_failed) { who -> repo.pickMovie(who, title, durationSec) }
    }

    fun pickSharedMovie(movie: SharedMovie) {
        localUri = null
        forgetLocalMovie()
        runAction(R.string.movie_toast_save_failed) { who -> repo.selectShared(who, movie.id) }
    }

    /**
     * The movie already started and this phone only needs its own copy of the file. It does NOT tell the server
     * anything (picking through [pickLocalMovie] would reset "ready" and stop the movie for both of you).
     */
    fun attachLocalMovie(uri: String, title: String, durationSec: Int) {
        val expected = _state.value.room?.myDurationSec
        if (expected != null && abs(expected - durationSec) > DURATION_TOLERANCE_SEC) {
            _messages.tryEmit(R.string.movie_need_file_wrong)
            return
        }
        localUri = uri
        rememberLocalMovie(SavedLocalMovie(uri, title, durationSec))
        updateStage()
    }

    private fun rememberLocalMovie(movie: SavedLocalMovie) {
        savedMovie = movie
        val who = _state.value.who ?: return
        viewModelScope.launch { store.saveLocalMovie(who.coupleId, movie) }
    }

    private fun forgetLocalMovie() {
        savedMovie = null
        val who = _state.value.who ?: return
        viewModelScope.launch { store.clearLocalMovie(who.coupleId) }
    }

    fun setReady(ready: Boolean) = runAction(R.string.movie_toast_ready_failed) { who -> repo.setReady(who, ready) }

    fun requestStart() = runAction(R.string.movie_toast_start_failed) { who ->
        sentFirstState = false
        repo.requestStart(who)
    }

    fun cancelStart() = runAction(R.string.movie_toast_cancel_failed) { who -> repo.cancelStart(who) }

    fun acceptInvite() {
        sentFirstState = false
        runAction(R.string.movie_toast_accept_failed) { who -> repo.acceptStart(who) }
    }

    fun cancelMovie() {
        val who = _state.value.who ?: return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (repo.reset(who)) {
                is AppResult.Success -> {
                    localUri = null
                    forgetLocalMovie()
                    _state.update { it.copy(room = null, stage = WatchStage.Setup, streamUrl = null) }
                    _messages.tryEmit(R.string.movie_toast_cancelled)
                }
                is AppResult.Failure -> _messages.tryEmit(R.string.movie_toast_cancel_failed)
            }
            _state.update { it.copy(busy = false) }
        }
    }

    private fun runAction(@StringRes failure: Int, call: suspend (MovieIdentity) -> AppResult<WatchRoom>) {
        val who = _state.value.who ?: return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val r = call(who)) {
                is AppResult.Success -> applyRoom(r.data)
                is AppResult.Failure -> tell(r.error, failure)
            }
            _state.update { it.copy(busy = false) }
        }
    }

    // ── Playing together ──

    /** Tell the server (and so the partner) that this phone played, paused or jumped. */
    fun sendAction(playing: Boolean, positionSec: Double) {
        val who = _state.value.who ?: return
        lastSeq = maxOf(lastSeq + 1, System.currentTimeMillis())
        val seq = lastSeq
        viewModelScope.launch {
            val r = repo.sendState(who, playing, positionSec, seq)
            if (r is AppResult.Success) applyRoom(r.data)
        }
    }

    fun endMovie(positionSec: Double) {
        val who = _state.value.who ?: return
        val room = _state.value.room
        viewModelScope.launch {
            val r = repo.end(who, room?.myTitle, room?.myDurationSec?.toInt(), positionSec, room?.scheduledStartRaw)
            if (r is AppResult.Success) {
                localUri = null
                forgetLocalMovie()
                applyRoom(r.data)
            } else {
                _messages.tryEmit(R.string.movie_toast_cancel_failed)
            }
        }
    }

    // ── Movie chat ──

    private suspend fun refreshChat() {
        val s = _state.value
        val who = s.who ?: return
        val key = s.room?.scheduledStartRaw ?: return
        val r = repo.chat(who, key, lastChatStamp)
        if (r is AppResult.Success && r.data.isNotEmpty()) {
            lastChatStamp = r.data.last().createdAt
            _state.update { it.copy(chat = (it.chat + r.data).distinctBy { m -> m.id }) }
        }
    }

    fun sendChat(text: String, positionSec: Double) {
        val s = _state.value
        val who = s.who ?: return
        val key = s.room?.scheduledStartRaw ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            when (repo.sendChat(who, key, s.room?.myTitle, text.trim(), positionSec)) {
                is AppResult.Success -> refreshChat()
                is AppResult.Failure -> _messages.tryEmit(R.string.movie_toast_message_failed)
            }
        }
    }

    // ── Cloud list and history ──

    fun loadCloud() {
        val who = _state.value.who ?: return
        _state.update { it.copy(cloud = UiState.Loading) }
        viewModelScope.launch {
            val ui = when (val r = repo.library(who)) {
                is AppResult.Success -> if (r.data.movies.isEmpty()) UiState.Empty else UiState.Content(r.data)
                is AppResult.Failure -> UiState.Error(r.error)
            }
            _state.update { it.copy(cloud = ui) }
        }
    }

    fun loadHistory() {
        val who = _state.value.who ?: return
        _state.update { it.copy(history = UiState.Loading) }
        viewModelScope.launch {
            val ui = when (val r = repo.history(who)) {
                is AppResult.Success -> if (r.data.isEmpty()) UiState.Empty else UiState.Content(r.data)
                is AppResult.Failure -> UiState.Error(r.error)
            }
            _state.update { it.copy(history = ui) }
        }
    }

    fun deleteHistory(id: String) {
        val who = _state.value.who ?: return
        viewModelScope.launch {
            when (repo.deleteHistory(who, id)) {
                is AppResult.Success -> loadHistory()
                is AppResult.Failure -> _messages.tryEmit(R.string.movie_toast_delete_failed)
            }
        }
    }

    fun clearHistory() {
        val who = _state.value.who ?: return
        viewModelScope.launch {
            when (repo.clearHistory(who)) {
                is AppResult.Success -> loadHistory()
                is AppResult.Failure -> _messages.tryEmit(R.string.movie_toast_clear_failed)
            }
        }
    }

    /** Shows the server's sentence when it sent one, otherwise the app's own fallback text. */
    private fun tell(error: AppError, @StringRes fallback: Int) {
        val text = (error as? AppError.Server)?.message
        if (text != null) _notes.tryEmit(text) else _messages.tryEmit(fallback)
    }

    // ── Cloud upload and delete ──

    fun uploadMovie(video: LocalVideo) {
        uploader.start(UploadRequest(video.uri, video.title, video.sizeBytes, video.mime, video.durationSec.toDouble()))
    }

    fun cancelUpload() {
        uploader.cancel()
        _messages.tryEmit(R.string.movie_toast_upload_cancelled)
    }

    fun dismissUpload() = uploader.dismiss()

    fun deleteShared(movie: SharedMovie) {
        val who = _state.value.who ?: return
        viewModelScope.launch {
            when (val r = repo.deleteShared(who, movie.id)) {
                is AppResult.Success -> {
                    _messages.tryEmit(R.string.movie_toast_movie_deleted)
                    refreshRoom(first = false)
                    loadCloud()
                }
                is AppResult.Failure -> tell(r.error, R.string.movie_toast_delete_failed)
            }
        }
    }

    // ── Player extras ──

    fun setKeepScreenOn(on: Boolean) {
        viewModelScope.launch { store.setKeepScreenOn(on) }
    }

    /** Shows the emoji at once on this phone, then tells the partner (best effort: it is not saved anywhere). */
    fun sendReaction(emoji: String) {
        val who = _state.value.who ?: return
        val mine = MovieReaction(id = System.nanoTime(), emoji = emoji, fromMe = true)
        _state.update { it.copy(reactions = (it.reactions + mine).takeLast(MAX_REACTIONS)) }
        if (!reactionsSupported) return
        viewModelScope.launch { repo.sendReaction(who, emoji) }
    }

    private suspend fun refreshReactions() {
        if (!reactionsSupported) return
        val who = _state.value.who ?: return
        if (lastReactionAt == 0L) lastReactionAt = serverNowMs()
        when (val r = repo.reactions(who, lastReactionAt)) {
            is AppResult.Success -> {
                if (r.data.isEmpty()) return
                lastReactionAt = maxOf(lastReactionAt, r.data.maxOf { it.id })
                val theirs = r.data.filterNot { it.fromMe }
                if (theirs.isNotEmpty()) _state.update { it.copy(reactions = (it.reactions + theirs).takeLast(MAX_REACTIONS)) }
            }
            // An older server has no reaction route yet. Stop asking; own reactions still show on this phone.
            is AppResult.Failure -> {
                val e = r.error
                if (e is AppError.Server && e.code == NOT_FOUND) reactionsSupported = false
            }
        }
    }

    private fun trimReactions() {
        if (_state.value.reactions.isEmpty()) return
        val now = System.currentTimeMillis()
        _state.update { s -> s.copy(reactions = s.reactions.filter { now - it.shownAtMs < REACTION_LIFE_MS }) }
    }

    private companion object {
        const val REACTION_EVERY = 4   // 1 s
        const val REACTION_LIFE_MS = 3000L
        const val MAX_REACTIONS = 8
        const val NOT_FOUND = 404
        const val DURATION_TOLERANCE_SEC = 3   // same rule the server uses to call two files "the same movie"
        const val TICK_MS = 250L
        const val POLL_EVERY = 6   // 1.5 s
        const val CHAT_EVERY = 8   // 2 s
    }
}
