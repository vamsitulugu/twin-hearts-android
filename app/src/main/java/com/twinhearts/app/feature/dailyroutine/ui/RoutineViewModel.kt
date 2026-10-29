package com.twinhearts.app.feature.dailyroutine.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.feature.dailyroutine.data.RoutineIdentity
import com.twinhearts.app.feature.dailyroutine.data.RoutineRepository
import com.twinhearts.app.feature.dailyroutine.domain.buildRows
import com.twinhearts.app.feature.dailyroutine.domain.hasConflict
import com.twinhearts.app.feature.dailyroutine.domain.model.DayRow
import com.twinhearts.app.feature.dailyroutine.domain.model.DaySummary
import com.twinhearts.app.feature.dailyroutine.domain.model.Occurrence
import com.twinhearts.app.feature.dailyroutine.domain.model.Routine
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineDraft
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineStatus
import com.twinhearts.app.feature.dailyroutine.domain.occurrencesForDate
import com.twinhearts.app.feature.dailyroutine.domain.summarize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

enum class RoutineTab { Mine, Partner }

/** Opens the Add / Edit sheet. [editId] is null for a new routine; the times prefill a free-time gap. */
data class FormTarget(val editId: String? = null, val startTime: String = "", val endTime: String = "")

/** The routine (and day) whose detail sheet is open. */
data class DetailTarget(val id: String, val date: LocalDate)

/** An error line inside the open sheet. */
sealed interface FormError {
    data class Text(@StringRes val id: Int) : FormError
    data class Failure(val error: AppError) : FormError
}

@Composable
fun FormError.asText(): String = when (this) {
    is FormError.Text -> stringResource(id)
    is FormError.Failure -> error.asText()
}

/** A short message (toast) for the person. */
sealed interface RoutineMessage {
    data class Info(@StringRes val id: Int) : RoutineMessage
    data class Failure(val error: AppError) : RoutineMessage
}

@Composable
fun RoutineMessage.asText(): String = when (this) {
    is RoutineMessage.Info -> stringResource(id)
    is RoutineMessage.Failure -> error.asText()
}

data class RoutineUiState(
    val isLoading: Boolean = true,
    /** Set only when the first load failed and there is nothing to show. */
    val error: AppError? = null,
    val routines: List<Routine> = emptyList(),
    val identity: RoutineIdentity? = null,
    val tab: RoutineTab = RoutineTab.Mine,
    val date: LocalDate = LocalDate.now(),
    val now: LocalDateTime = LocalDateTime.now(),
    val form: FormTarget? = null,
    val formError: FormError? = null,
    val detail: DetailTarget? = null,
    val confirmDelete: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isToday: Boolean get() = date == now.toLocalDate()

    /** The activities of the open tab on the open day. Partner routines show only when shared. */
    val occurrences: List<Occurrence>
        get() {
            val me = identity ?: return emptyList()
            val partner = tab == RoutineTab.Partner
            return occurrencesForDate(routines, if (partner) me.partnerRole else me.role, partner, date, now)
        }

    val summary: DaySummary get() = summarize(occurrences, isToday, now)

    val rows: List<DayRow> get() = buildRows(occurrences, isToday, now)

    fun routine(id: String): Routine? = routines.firstOrNull { it.id == id }
}

/**
 * Logic of the Daily Routine screen. It keeps the routines of both people, works out the day that
 * is open and sends changes to the server; all schedule rules live in domain/RoutineLogic.kt.
 */
@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val repository: RoutineRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RoutineUiState(date = LocalDate.now(clock), now = LocalDateTime.now(clock)),
    )
    val state: StateFlow<RoutineUiState> = _state.asStateFlow()

    private val _events = Channel<RoutineMessage>(Channel.BUFFERED)
    val events: Flow<RoutineMessage> = _events.receiveAsFlow()

    init {
        refresh()
    }

    /** Loads both people's routines. A failed refresh never wipes what is already shown. */
    fun refresh() {
        viewModelScope.launch {
            val hadData = _state.value.identity != null
            if (!hadData) _state.update { it.copy(isLoading = true, error = null) }
            val identity = repository.identity()
            when (val result = repository.load()) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, error = null, routines = result.data, identity = identity ?: it.identity, now = LocalDateTime.now(clock))
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(isLoading = false, identity = identity ?: it.identity, error = if (hadData) it.error else result.error) }
                    if (hadData) send(RoutineMessage.Failure(result.error))
                }
            }
        }
    }

    /** Called once a minute so "NOW" and "In Progress" follow the clock. */
    fun tick() = _state.update { it.copy(now = LocalDateTime.now(clock)) }

    fun selectTab(tab: RoutineTab) = _state.update { it.copy(tab = tab) }

    fun shiftDate(days: Long) = _state.update { it.copy(date = it.date.plusDays(days)) }

    fun pickDate(date: LocalDate) = _state.update { it.copy(date = date) }

    // ---- Add / Edit sheet ---------------------------------------------------------------------

    fun openAdd(startTime: String = "", endTime: String = "") =
        _state.update { it.copy(form = FormTarget(null, startTime, endTime), formError = null, detail = null) }

    fun openEdit(id: String) {
        if (_state.value.routine(id) == null) return
        _state.update { it.copy(form = FormTarget(editId = id), formError = null, detail = null) }
    }

    fun closeForm() = _state.update { it.copy(form = null, formError = null) }

    /** Checks the form like the web does, then saves. Problems show inside the sheet. */
    fun save(draft: RoutineDraft) {
        val state = _state.value
        val target = state.form ?: return
        val me = state.identity ?: return
        if (state.isSaving) return
        val problem = when {
            draft.title.isBlank() -> R.string.dr_err_name
            draft.startTime.isBlank() || draft.endTime.isBlank() -> R.string.dr_err_times
            draft.startTime == draft.endTime -> R.string.dr_err_same_time
            // The web only checks my own day, and leaves out the routine being edited.
            hasConflict(
                occurrencesForDate(state.routines, me.role, false, state.date, state.now).filter { it.routine.id != target.editId },
                draft.startTime, draft.endTime,
            ) -> R.string.dr_err_conflict
            else -> null
        }
        if (problem != null) {
            _state.update { it.copy(formError = FormError.Text(problem)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, formError = null) }
            val result = if (target.editId == null) {
                repository.create(draft, state.date)
            } else {
                repository.update(target.editId, draft, state.date)
            }
            when (result) {
                is AppResult.Success -> {
                    _state.update { it.copy(isSaving = false, form = null) }
                    send(RoutineMessage.Info(R.string.dr_saved))
                    refresh()
                }
                is AppResult.Failure -> _state.update { it.copy(isSaving = false, formError = FormError.Failure(result.error)) }
            }
        }
    }

    // ---- Detail sheet -------------------------------------------------------------------------

    fun openDetail(id: String, date: LocalDate) = _state.update { it.copy(detail = DetailTarget(id, date)) }

    fun closeDetail() = _state.update { it.copy(detail = null, confirmDelete = false) }

    fun markCompleted() = setStatus(RoutineStatus.Completed, R.string.dr_marked_complete)

    fun skip() = setStatus(RoutineStatus.Skipped, R.string.dr_skipped_toast)

    private fun setStatus(status: RoutineStatus, @StringRes done: Int) {
        val target = _state.value.detail ?: return
        change(done) { repository.setStatus(target.id, target.date, status) }
    }

    fun duplicate() {
        val target = _state.value.detail ?: return
        change(R.string.dr_duplicated) { repository.duplicate(target.id) }
    }

    fun askDelete() = _state.update { it.copy(confirmDelete = true) }

    fun cancelDelete() = _state.update { it.copy(confirmDelete = false) }

    fun confirmDelete() {
        val target = _state.value.detail ?: return
        change(R.string.dr_deleted) { repository.delete(target.id) }
    }

    /** Runs one change from the detail sheet, then closes it and reloads (what the web does). */
    private fun change(@StringRes done: Int, block: suspend () -> AppResult<Unit>) {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, confirmDelete = false) }
            when (val result = block()) {
                is AppResult.Success -> {
                    _state.update { it.copy(isSaving = false, detail = null) }
                    send(RoutineMessage.Info(done))
                    refresh()
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(isSaving = false) }
                    send(RoutineMessage.Failure(result.error))
                }
            }
        }
    }

    private fun send(message: RoutineMessage) {
        _events.trySend(message)
    }
}
