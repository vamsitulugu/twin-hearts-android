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
)

/** Loads the couple's events, keeps the month/day being looked at, and adds or deletes events. */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.events()) {
                is AppResult.Success -> _state.update { it.copy(loading = false, events = result.data) }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
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

    fun messageShown() = _state.update { it.copy(message = null) }
}
