package com.twinhearts.app.feature.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.music.data.MusicRepository
import com.twinhearts.app.feature.music.data.VoiceRecorder
import com.twinhearts.app.feature.music.domain.LibraryQueries
import com.twinhearts.app.feature.music.domain.model.MusicIdentity
import com.twinhearts.app.feature.music.domain.model.Recording
import com.twinhearts.app.feature.music.domain.model.RecordingFilter
import com.twinhearts.app.feature.music.domain.model.RecordingKind
import com.twinhearts.app.feature.music.domain.model.RecordingMedia
import com.twinhearts.app.feature.music.domain.model.RecordingOwner
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.domain.model.SongVisibility
import com.twinhearts.app.feature.music.player.MusicPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RecordingsUiState(
    val who: MusicIdentity? = null,
    val all: UiState<List<Recording>> = UiState.Loading,
    val filter: RecordingFilter = RecordingFilter.Audio,
    val owner: RecordingOwner = RecordingOwner.Everyone,
    val saving: Boolean = false,
    val playingId: String? = null,
) {
    /** The rows to draw after the two filters. */
    val visible: List<Recording>
        get() {
            val role = who?.myRole ?: return emptyList()
            val list = (all as? UiState.Content)?.data ?: return emptyList()
            return LibraryQueries.filterRecordings(list, filter, owner, role)
        }
}

/**
 * Why this file exists: runs the "My Recordings" tab: the list with its filters, favourites,
 * rename, delete, and saving a new voice note, video or karaoke take.
 */
@HiltViewModel
class RecordingsViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val controller: MusicPlayerController,
    private val recorder: VoiceRecorder,
) : ViewModel() {

    private val _state = MutableStateFlow(RecordingsUiState())
    val state: StateFlow<RecordingsUiState> = _state.asStateFlow()

    private val eventChannel = Channel<MusicEvent>(Channel.BUFFERED)
    val events: Flow<MusicEvent> = eventChannel.receiveAsFlow()

    private var who: MusicIdentity? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (who == null) who = repository.identity()
            val identity = who
            if (identity == null) {
                _state.update { it.copy(all = UiState.Error(AppError.Unauthorized())) }
                return@launch
            }
            _state.update { it.copy(who = identity) }
            when (val r = repository.recordings(identity)) {
                is AppResult.Success -> _state.update { it.copy(all = if (r.data.isEmpty()) UiState.Empty else UiState.Content(r.data)) }
                is AppResult.Failure -> {
                    val hasRows = _state.value.all is UiState.Content
                    if (hasRows) send(MusicEvent.Failure(r.error)) else _state.update { it.copy(all = UiState.Error(r.error)) }
                }
            }
        }
    }

    // ── the microphone ──

    /** Starts a voice note or karaoke take. False when the microphone could not be opened. */
    fun startTake(): Boolean = recorder.start()

    /** Ends the take. Null when it was too short to keep. */
    fun stopTake(): Pair<File, Int>? = recorder.stop()

    fun cancelTake() = recorder.cancel()

    override fun onCleared() {
        recorder.cancel()
        super.onCleared()
    }

    fun setFilter(filter: RecordingFilter) = _state.update { it.copy(filter = filter) }

    fun setOwner(owner: RecordingOwner) = _state.update { it.copy(owner = owner) }

    /** Plays an audio recording through the shared player (it replaces the music queue, as on the web). */
    fun playAudio(recording: Recording) {
        val pseudo = Song(
            id = MusicPlayerController.RECORDING_PREFIX + recording.id,
            title = recording.title, artist = recording.userRole, album = "", genre = "", year = "",
            audioUrl = recording.mediaUrl, coverUrl = null, durationSec = recording.durationSec,
            visibility = SongVisibility.Both, uploadedBy = recording.userRole, createdAt = recording.createdAt,
            liked = false, partnerLiked = false, myPlayCount = 0, myLastPlayed = null, hasLyrics = false, lyricsSource = null,
        )
        controller.play(listOf(pseudo), pseudo.id)
        _state.update { it.copy(playingId = recording.id) }
    }

    fun pauseMusicForVideo() {
        if (controller.player.isPlaying) controller.togglePlay()
    }

    fun toggleFavorite(recording: Recording) {
        val identity = who ?: return
        val next = !recording.favorite
        change(recording.id) { it.copy(favorite = next) }
        viewModelScope.launch {
            if (repository.setRecordingFavorite(identity, recording.id, next) is AppResult.Failure) {
                change(recording.id) { it.copy(favorite = !next) }
                send(MusicEvent.Message(R.string.music_msg_favorite_failed))
            }
        }
    }

    fun rename(recording: Recording, title: String) {
        val identity = who ?: return
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            when (val r = repository.renameRecording(identity, recording.id, clean)) {
                is AppResult.Success -> change(recording.id) { it.copy(title = clean) }
                is AppResult.Failure -> send(MusicEvent.Failure(r.error))
            }
        }
    }

    fun delete(recording: Recording) {
        val identity = who ?: return
        viewModelScope.launch {
            when (val r = repository.deleteRecording(identity, recording.id)) {
                is AppResult.Success -> _state.update { s ->
                    val rows = ((s.all as? UiState.Content)?.data ?: emptyList()).filterNot { it.id == recording.id }
                    s.copy(all = if (rows.isEmpty()) UiState.Empty else UiState.Content(rows))
                }
                is AppResult.Failure -> send(MusicEvent.Failure(r.error))
            }
        }
    }

    /** Uploads a finished recording. The temporary file is deleted afterwards, success or not. */
    fun save(file: File, media: RecordingMedia, title: String, durationSec: Int, kind: RecordingKind, songId: String?) {
        val identity = who ?: return
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val r = repository.saveRecording(identity, file, media, title.trim().ifEmpty { "Recording" }, durationSec, kind, songId)
            file.delete()
            _state.update { it.copy(saving = false) }
            when (r) {
                is AppResult.Success -> {
                    send(MusicEvent.Message(R.string.music_msg_recording_saved))
                    refresh()
                }
                is AppResult.Failure -> send(MusicEvent.Failure(r.error))
            }
        }
    }

    private fun change(id: String, transform: (Recording) -> Recording) {
        _state.update { s ->
            val rows = (s.all as? UiState.Content)?.data ?: return@update s
            s.copy(all = UiState.Content(rows.map { if (it.id == id) transform(it) else it }))
        }
    }

    private fun send(event: MusicEvent) {
        eventChannel.trySend(event)
    }
}
