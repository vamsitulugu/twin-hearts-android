package com.twinhearts.app.feature.profile.highlights.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.profile.highlights.data.HighlightsRepository
import com.twinhearts.app.feature.profile.highlights.domain.ClipProblem
import com.twinhearts.app.feature.profile.highlights.domain.HighlightCategory
import com.twinhearts.app.feature.profile.highlights.domain.HighlightsMath
import com.twinhearts.app.feature.profile.highlights.domain.UploadTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Brain of the Highlights row: loads the circles, checks what the person picked, sends the clips one by
 * one (the first one creates the highlight, the rest join it) and runs rename and delete.
 */
@HiltViewModel
class HighlightsViewModel @Inject constructor(
    private val repository: HighlightsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HighlightsUiState())
    val state: StateFlow<HighlightsUiState> = _state.asStateFlow()

    private var uploadJob: Job? = null

    init {
        refresh()
    }

    /** Loads (or reloads) the circles. A failed reload keeps what is already on screen. */
    fun refresh() {
        viewModelScope.launch {
            when (val result = repository.load()) {
                is AppResult.Success -> _state.update { it.copy(phase = HighlightsPhase.Ready(result.data)) }
                is AppResult.Failure -> _state.update {
                    if (it.phase is HighlightsPhase.Ready) it else it.copy(phase = HighlightsPhase.Failed(result.error))
                }
            }
        }
    }

    fun retry() {
        _state.update { it.copy(phase = HighlightsPhase.Loading) }
        refresh()
    }

    fun setFilter(category: HighlightCategory?) = _state.update { it.copy(filter = category) }

    fun messageShown() = _state.update { it.copy(message = null) }

    // ── choosing clips ───────────────────────────────────────────────────────────────────────────

    /** [uris] come from the gallery or the camera. Files that are not photos/videos or too big are skipped. */
    fun onClipsPicked(uris: List<String>, preselectId: String?) {
        viewModelScope.launch {
            val described = uris.take(HighlightsMath.MAX_PICK).mapNotNull { repository.describe(it) }
            val problems = described.mapNotNull { HighlightsMath.problemOf(it.mime, it.sizeBytes) }
            val usable = described.filter { HighlightsMath.problemOf(it.mime, it.sizeBytes) == null }
            if (usable.isEmpty()) {
                val message = if (ClipProblem.TooBig in problems) HighlightsMessage.TooBig else HighlightsMessage.NotMedia
                _state.update { it.copy(message = message) }
                return@launch
            }
            val skipped = usable.size < uris.size
            _state.update {
                it.copy(
                    pending = PendingUpload(usable, preselectId),
                    uploadError = null,
                    message = if (skipped) HighlightsMessage.Skipped else it.message,
                )
            }
        }
    }

    /** Closes the add sheet. If a send is running it stops; clips already added stay. */
    fun cancelPending() {
        uploadJob?.cancel()
        uploadJob = null
        val hadProgress = _state.value.progress != null
        _state.update { it.copy(pending = null, progress = null, uploadError = null) }
        if (hadProgress) refresh()
    }

    // ── sending ──────────────────────────────────────────────────────────────────────────────────

    fun startUpload(target: UploadTarget) {
        val pending = _state.value.pending ?: return
        if (uploadJob?.isActive == true) return
        uploadJob = viewModelScope.launch {
            var remaining = pending.clips
            var current = target
            val total = remaining.size
            var done = 0
            _state.update { it.copy(progress = UploadProgress(0, total, 0f), uploadError = null) }
            while (remaining.isNotEmpty()) {
                val clip = remaining.first()
                val sent = repository.uploadClip(clip, current) { fraction ->
                    _state.update { s -> s.copy(progress = UploadProgress(done, total, fraction)) }
                }
                when (sent) {
                    is AppResult.Success -> {
                        // The rest of the clips join the highlight that was just created.
                        current = UploadTarget.Existing(sent.data)
                        remaining = remaining.drop(1)
                        done++
                        _state.update { it.copy(progress = UploadProgress(done, total, 0f)) }
                    }
                    is AppResult.Failure -> {
                        _state.update {
                            it.copy(
                                progress = null,
                                uploadError = sent.error,
                                pending = pending.copy(clips = remaining, lockedHighlightId = (current as? UploadTarget.Existing)?.highlightId),
                            )
                        }
                        if (done > 0) refresh()
                        return@launch
                    }
                }
            }
            _state.update { it.copy(pending = null, progress = null, message = HighlightsMessage.Saved) }
            refresh()
        }
    }

    // ── edit and delete ──────────────────────────────────────────────────────────────────────────

    /** [onDone] runs after a successful save, so the screen can close its sheet. */
    fun rename(highlightId: String, title: String, category: HighlightCategory, onDone: () -> Unit) =
        run(HighlightsMessage.EditSaved, onDone) { repository.update(highlightId, title, category) }

    fun deleteHighlight(highlightId: String, onDone: () -> Unit) =
        run(HighlightsMessage.Deleted, onDone) { repository.deleteHighlight(highlightId) }

    /** [onDone] gets true when that was the last clip and the whole highlight is gone. */
    fun deleteClip(clipId: String, onDone: (highlightGone: Boolean) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (val result = repository.deleteClip(clipId)) {
                is AppResult.Success -> {
                    _state.update { it.copy(busy = false, message = HighlightsMessage.ClipDeleted) }
                    refresh()
                    onDone(result.data)
                }
                is AppResult.Failure -> _state.update { it.copy(busy = false, message = HighlightsMessage.Failed(result.error)) }
            }
        }
    }

    private fun run(success: HighlightsMessage, onDone: () -> Unit, call: suspend () -> AppResult<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (val result = call()) {
                is AppResult.Success -> {
                    _state.update { it.copy(busy = false, message = success) }
                    refresh()
                    onDone()
                }
                is AppResult.Failure -> _state.update { it.copy(busy = false, message = HighlightsMessage.Failed(result.error)) }
            }
        }
    }
}
