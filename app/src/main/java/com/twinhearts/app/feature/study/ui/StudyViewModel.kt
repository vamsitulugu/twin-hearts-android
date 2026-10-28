package com.twinhearts.app.feature.study.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.study.data.StudyRepository
import com.twinhearts.app.feature.study.domain.model.NewSlot
import com.twinhearts.app.feature.study.domain.model.NewSubject
import com.twinhearts.app.feature.study.domain.model.StudyData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** One-off result shown once as a toast (R-42: not part of the long-lived state). */
sealed interface StudyMessage {
    data object SubjectAdded : StudyMessage
    data object SlotAdded : StudyMessage
    data object Deleted : StudyMessage
    data class Failed(val error: AppError) : StudyMessage
}

data class StudyUiState(
    val loading: Boolean = true,
    /** Set only when the first load failed; later save errors come as a [StudyMessage]. */
    val error: AppError? = null,
    val data: StudyData? = null,
    val saving: Boolean = false,
    val message: StudyMessage? = null,
)

/** Loads the couple's study data and changes my subjects and timetable. */
@HiltViewModel
class StudyViewModel @Inject constructor(
    private val repository: StudyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    // Every change reads the saved state first, so changes must run one after another:
    // quick taps on "+1" are queued instead of dropped or racing each other.
    private val writeLock = Mutex()
    private var pending = 0

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.load()) {
                is AppResult.Success -> _state.update { it.copy(loading = false, data = result.data) }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    fun addSubject(subject: NewSubject) = change(StudyMessage.SubjectAdded) { repository.addSubject(subject) }

    fun changeProgress(id: String, delta: Int) = change(null) { repository.changeProgress(id, delta) }

    fun deleteSubject(id: String) = change(StudyMessage.Deleted) { repository.deleteSubject(id) }

    fun addSlot(slot: NewSlot) = change(StudyMessage.SlotAdded) { repository.addSlot(slot) }

    fun toggleSlot(id: String) = change(null) { repository.toggleSlot(id) }

    fun deleteSlot(id: String) = change(StudyMessage.Deleted) { repository.deleteSlot(id) }

    private fun change(success: StudyMessage?, call: suspend () -> AppResult<StudyData>) {
        pending++
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = writeLock.withLock { call() }
            pending--
            _state.update { current ->
                when (result) {
                    is AppResult.Success -> current.copy(
                        data = result.data,
                        saving = pending > 0,
                        message = success ?: current.message,
                    )
                    is AppResult.Failure -> current.copy(
                        saving = pending > 0,
                        message = StudyMessage.Failed(result.error),
                    )
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}
