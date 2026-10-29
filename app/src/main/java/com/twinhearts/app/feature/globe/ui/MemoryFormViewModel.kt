package com.twinhearts.app.feature.globe.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.globe.data.GlobeRepository
import com.twinhearts.app.feature.globe.domain.draftForCity
import com.twinhearts.app.feature.globe.domain.draftOf
import com.twinhearts.app.feature.globe.domain.model.CityGroup
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.Memory
import com.twinhearts.app.feature.globe.domain.model.MemoryDraft
import com.twinhearts.app.feature.globe.domain.model.PlaceHit
import com.twinhearts.app.feature.globe.domain.model.StagedMedia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class MemoryFormState(
    val token: Long = -1L,
    val editingId: String? = null,
    val draft: MemoryDraft = MemoryDraft(),
    val results: List<PlaceHit> = emptyList(),
    val staged: List<StagedMedia> = emptyList(),
    val saving: Boolean = false,
) {
    val isEditing: Boolean get() = editingId != null
    val uploading: Boolean get() = staged.any { it.uploading }
}

sealed interface MemoryFormEvent {
    data class Message(@StringRes val res: Int, val arg: Any? = null) : MemoryFormEvent

    /** Saved: the page should close the form and reload. */
    data object Saved : MemoryFormEvent
}

/**
 * Why this file exists: the "Pin a Memory" / "Edit Memory" form. It searches places while you type,
 * fills the weather, uploads photos / videos / voice notes as soon as they are picked, and saves.
 */
@HiltViewModel
class MemoryFormViewModel @Inject constructor(
    private val repository: GlobeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MemoryFormState())
    val state: StateFlow<MemoryFormState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MemoryFormEvent>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<MemoryFormEvent> = _events.asSharedFlow()

    private var searchJob: Job? = null
    private var localId = 0L

    private fun say(@StringRes res: Int, arg: Any? = null) {
        _events.tryEmit(MemoryFormEvent.Message(res, arg))
    }

    /** Starts a fresh form. Calling it again with the same [token] does nothing (rotation, recomposition). */
    fun start(token: Long, editing: Memory?, city: CityGroup?) {
        if (_state.value.token == token) return
        searchJob?.cancel()
        val draft = when {
            editing != null -> draftOf(editing)
            city != null -> draftForCity(city)
            else -> MemoryDraft()
        }
        _state.value = MemoryFormState(token = token, editingId = editing?.id, draft = draft)
    }

    fun edit(change: (MemoryDraft) -> MemoryDraft) = _state.update { it.copy(draft = change(it.draft)) }

    // ── place search ─────────────────────────────────────────────────────────────────────────

    /** Typing in the location box: wait a moment, then ask the search service (needs 3 letters). */
    fun onLocationText(text: String) {
        // Like the web form, the picked place stays until another one is picked.
        _state.update { it.copy(draft = it.draft.copy(locationText = text)) }
        searchJob?.cancel()
        if (text.trim().length < MIN_SEARCH_LETTERS) {
            _state.update { it.copy(results = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DELAY_MS)
            when (val r = repository.searchPlaces(text.trim())) {
                is AppResult.Success -> _state.update { it.copy(results = r.data) }
                is AppResult.Failure -> _state.update { it.copy(results = emptyList()) }
            }
        }
    }

    fun pickPlace(hit: PlaceHit) {
        _state.update {
            it.copy(
                results = emptyList(),
                draft = it.draft.copy(
                    locationText = hit.city + if (hit.country.isNotBlank()) ", ${hit.country}" else "",
                    city = hit.city,
                    country = hit.country,
                    state = hit.state,
                    lat = hit.lat,
                    lng = hit.lng,
                ),
            )
        }
        viewModelScope.launch {
            val r = repository.weatherAt(hit.lat, hit.lng) as? AppResult.Success ?: return@launch
            val weather = r.data ?: return@launch
            _state.update { s ->
                s.copy(draft = s.draft.copy(weather = weather.option ?: s.draft.weather, temperature = weather.temperatureC.toString()))
            }
            say(R.string.globe_toast_autofilled)
        }
    }

    // ── attachments ──────────────────────────────────────────────────────────────────────────

    /** Photos or a video picked: each one uploads at once and shows a preview. */
    fun stagePicked(uris: List<String>, kind: MediaKind) {
        uris.forEach { uri ->
            val id = ++localId
            _state.update { it.copy(staged = it.staged + StagedMedia(id, kind, null, "", true)) }
            viewModelScope.launch {
                val r = repository.uploadPicked(uri, kind)
                val url = (r as? AppResult.Success)?.data
                if (url == null) {
                    say(R.string.globe_toast_upload_failed_name, uri.substringAfterLast('/'))
                    _state.update { s -> s.copy(staged = s.staged.filterNot { it.localId == id }) }
                } else {
                    _state.update { s -> s.copy(staged = s.staged.map { if (it.localId == id) it.copy(url = url, uploading = false) else it }) }
                }
            }
        }
    }

    fun stageVoice(file: File, seconds: Int) {
        val id = ++localId
        val duration = "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
        _state.update { it.copy(staged = it.staged + StagedMedia(id, MediaKind.Voice, null, duration, true)) }
        viewModelScope.launch {
            val r = repository.uploadVoice(file)
            file.delete()
            val url = (r as? AppResult.Success)?.data
            if (url == null) {
                say(R.string.globe_toast_voice_failed)
                _state.update { s -> s.copy(staged = s.staged.filterNot { it.localId == id }) }
            } else {
                say(R.string.globe_toast_voice_recorded)
                _state.update { s -> s.copy(staged = s.staged.map { if (it.localId == id) it.copy(url = url, uploading = false) else it }) }
            }
        }
    }

    // ── save ─────────────────────────────────────────────────────────────────────────────────

    fun save() {
        val s = _state.value
        if (s.saving || s.uploading) return
        if (!s.draft.hasLocation) {
            say(R.string.globe_toast_need_location)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val r = repository.saveMemory(s.editingId, s.draft, s.staged)
            _state.update { it.copy(saving = false) }
            when (r) {
                is AppResult.Success -> {
                    say(if (s.isEditing) R.string.globe_toast_updated else R.string.globe_toast_pinned)
                    if (r.data.failedMedia > 0) say(R.string.globe_toast_saved_with_failures, r.data.failedMedia)
                    _events.tryEmit(MemoryFormEvent.Saved)
                }
                is AppResult.Failure -> say(R.string.globe_toast_error)
            }
        }
    }

    private companion object {
        const val EVENT_BUFFER = 8
        const val MIN_SEARCH_LETTERS = 3
        const val SEARCH_DELAY_MS = 400L
        const val SECONDS_PER_MINUTE = 60
    }
}
