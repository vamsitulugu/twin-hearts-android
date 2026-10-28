package com.twinhearts.app.feature.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.calendar.data.CalendarRepository
import com.twinhearts.app.feature.calendar.domain.model.CalendarEvent
import com.twinhearts.app.feature.calendar.domain.model.NewEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** One-off result shown once as a toast (R-42: not part of the long-lived state). */
sealed interface CalendarMessage {
    data object Added : CalendarMessage
    data object Deleted : CalendarMessage
    data class Failed(val error: AppError) : CalendarMessage
}

data class CalendarUiState(
    val loading: Boolean = true,
    /** Set only when the first load failed; later save errors come as a [CalendarMessage]. */
    val error: AppError? = null,
    val events: List<CalendarEvent> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate? = LocalDate.now(),
    val saving: Boolean = false,
    val message: CalendarMessage? = null,
    /** True while the list on screen is the saved copy because there is no connection. */
    val offline: Boolean = false,
    /** Ids picked with a long press; non-empty means the page is in "select to delete" mode. */
    val selection: Set<String> = emptySet(),
    /** Goes up by one each time an event is added; the screen plays the petals on every change. */
    val petals: Int = 0,
)

/** Loads the couple's events, keeps the month/day being looked at, and adds or deletes events. */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Paint the saved copy straight away, then fetch the fresh list.
            repository.cachedEvents()?.let { saved ->
                _state.update { if (it.events.isEmpty()) it.copy(loading = false, events = saved) else it }
            }
            fetch(silent = false)
        }
    }

    /** The refresh button and the Retry button. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            fetch(silent = false)
        }
    }

    /** Called every ~30 s while the page is open: picks up the partner's changes without a spinner. */
    fun refresh() {
        if (_state.value.saving) return
        viewModelScope.launch { fetch(silent = true) }
    }

    private suspend fun fetch(silent: Boolean) {
        val result = repository.events()
        // A save that started while this was loading has newer data; do not overwrite it.
        if (_state.value.saving && result is AppResult.Success) return
        when (result) {
            is AppResult.Success -> _state.update {
                it.copy(
                    loading = false,
                    error = null,
                    offline = false,
                    events = result.data,
                    selection = it.selection.intersect(result.data.map { e -> e.id }.toSet()),
                )
            }
            is AppResult.Failure -> _state.update {
                when {
                    it.events.isNotEmpty() && result.error == AppError.Network -> it.copy(loading = false, offline = true)
                    silent -> it
                    it.events.isNotEmpty() -> it.copy(loading = false, message = CalendarMessage.Failed(result.error))
                    else -> it.copy(loading = false, error = result.error)
                }
            }
        }
    }

    fun previousMonth() = _state.update { it.copy(month = it.month.minusMonths(1), selected = null) }

    fun nextMonth() = _state.update { it.copy(month = it.month.plusMonths(1), selected = null) }

    fun goToToday() = _state.update {
        val today = LocalDate.now()
        it.copy(month = YearMonth.from(today), selected = today)
    }

    /** Tapping the selected day again clears the selection. */
    fun selectDay(date: LocalDate) = _state.update {
        it.copy(selected = if (it.selected == date) null else date)
    }

    fun addEvent(event: NewEvent) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val result = repository.addEvent(event)) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        saving = false,
                        events = result.data,
                        month = YearMonth.from(event.date),
                        selected = event.date,
                        petals = it.petals + 1,
                        message = CalendarMessage.Added,
                    )
                }
                is AppResult.Failure -> _state.update {
                    it.copy(saving = false, message = CalendarMessage.Failed(result.error))
                }
            }
        }
    }

    fun deleteEvent(id: String) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val result = repository.deleteEvent(id)) {
                is AppResult.Success -> _state.update {
                    it.copy(saving = false, events = result.data, message = CalendarMessage.Deleted)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(saving = false, message = CalendarMessage.Failed(result.error))
                }
            }
        }
    }

    /** Long press on a row starts selecting; while selecting, a tap adds or removes the row. */
    fun toggleSelect(id: String) = _state.update {
        it.copy(selection = if (id in it.selection) it.selection - id else it.selection + id)
    }

    fun clearSelection() = _state.update { it.copy(selection = emptySet()) }

    fun deleteSelected() {
        val ids = _state.value.selection
        if (ids.isEmpty() || _state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val result = repository.deleteEvents(ids)) {
                is AppResult.Success -> _state.update {
                    it.copy(saving = false, events = result.data, selection = emptySet(), message = CalendarMessage.Deleted)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(saving = false, message = CalendarMessage.Failed(result.error))
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}
