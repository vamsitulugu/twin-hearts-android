package com.twinhearts.app.feature.bucket.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.bucket.data.BucketRepository
import com.twinhearts.app.feature.bucket.domain.model.BucketDream
import com.twinhearts.app.feature.bucket.domain.model.NewDream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-off result shown once as a toast (R-42: not part of the long-lived state). */
sealed interface BucketMessage {
    data object Added : BucketMessage
    data class Failed(val error: AppError) : BucketMessage
}

data class BucketUiState(
    val loading: Boolean = true,
    /** Set only when the first load failed; later save errors come as a [BucketMessage]. */
    val error: AppError? = null,
    val dreams: List<BucketDream> = emptyList(),
    val saving: Boolean = false,
    /** Ids picked in selection mode (long-press, like the web). Empty = normal mode. */
    val selected: Set<String> = emptySet(),
    val message: BucketMessage? = null,
) {
    val selecting: Boolean get() = selected.isNotEmpty()
}

/** Loads the couple's dreams, ticks them off, adds and deletes (one or many). */
@HiltViewModel
class BucketViewModel @Inject constructor(
    private val repository: BucketRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BucketUiState())
    val state: StateFlow<BucketUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.dreams()) {
                is AppResult.Success -> _state.update { it.copy(loading = false, dreams = result.data) }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    fun addDream(dream: NewDream) = save(BucketMessage.Added) { repository.addDream(dream) }

    fun toggleDone(id: String) = save(null) { repository.toggleDone(id) }

    fun deleteOne(id: String) = save(null) { repository.deleteDreams(setOf(id)) }

    fun deleteSelected() {
        val ids = _state.value.selected
        if (ids.isEmpty()) return
        save(null) { repository.deleteDreams(ids) }
    }

    fun startSelecting(id: String) = _state.update { it.copy(selected = it.selected + id) }

    fun toggleSelected(id: String) = _state.update {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    }

    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }

    fun messageShown() = _state.update { it.copy(message = null) }

    private fun save(success: BucketMessage?, call: suspend () -> AppResult<List<BucketDream>>) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val result = call()) {
                is AppResult.Success -> _state.update {
                    it.copy(saving = false, dreams = result.data, selected = emptySet(), message = success)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(saving = false, message = BucketMessage.Failed(result.error))
                }
            }
        }
    }
}
