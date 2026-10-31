package com.twinhearts.app.feature.search.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.search.data.SearchRepository
import com.twinhearts.app.feature.search.domain.model.ContentStatus
import com.twinhearts.app.feature.search.domain.model.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Everything the Search screen draws. */
data class SearchUiState(
    val query: String = "",
    /** Pages and features: answered on the phone, as each letter is typed. */
    val local: List<SearchResult> = emptyList(),
    /** Messages and page content: answered by the server a moment after typing stops. */
    val content: List<SearchResult> = emptyList(),
    val status: ContentStatus = ContentStatus.Idle,
    val recents: List<String> = emptyList(),
)

/**
 * Why this file exists: runs the search. Pages and features update on every key press; the slower
 * lookups (messages, page content) wait until typing pauses, and an older answer can never
 * replace a newer one. The typed text survives rotation and process death.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var contentJob: Job? = null

    init {
        viewModelScope.launch { repository.warmUp() }
        viewModelScope.launch {
            repository.recents.collect { list -> _state.update { it.copy(recents = list) } }
        }
        val restored = saved.get<String>(KEY_QUERY).orEmpty()
        if (restored.isNotBlank()) onQueryChange(restored)
    }

    fun onQueryChange(raw: String) {
        saved[KEY_QUERY] = raw
        contentJob?.cancel()
        val query = raw.trim()
        if (query.isEmpty()) {
            _state.update { it.copy(query = raw, local = emptyList(), content = emptyList(), status = ContentStatus.Idle) }
            return
        }
        _state.update {
            it.copy(
                query = raw,
                local = repository.catalogResults(query),
                // Keep the old rows until the new answer arrives, so the list does not flash empty.
                status = if (query.length >= MIN_CONTENT_LENGTH) ContentStatus.Loading else ContentStatus.Idle,
                content = if (query.length >= MIN_CONTENT_LENGTH) it.content else emptyList(),
            )
        }
        if (query.length < MIN_CONTENT_LENGTH) return
        contentJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            when (val result = repository.contentResults(query)) {
                is AppResult.Success ->
                    _state.update { it.copy(content = result.data, status = ContentStatus.Done) }
                is AppResult.Failure ->
                    _state.update { it.copy(content = emptyList(), status = ContentStatus.Failed) }
            }
        }
    }

    /** A tap on a suggestion or a recent search. */
    fun useQuery(query: String) = onQueryChange(query)

    /** The person opened a result: remember what they typed. */
    fun onResultOpened() {
        val query = _state.value.query
        viewModelScope.launch { repository.remember(query) }
    }

    fun forgetRecent(query: String) {
        viewModelScope.launch { repository.forget(query) }
    }

    fun clearRecents() {
        viewModelScope.launch { repository.clearRecents() }
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val DEBOUNCE_MS = 350L
        const val MIN_CONTENT_LENGTH = 2
    }
}
