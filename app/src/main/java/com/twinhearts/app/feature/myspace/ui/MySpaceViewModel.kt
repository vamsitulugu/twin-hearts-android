package com.twinhearts.app.feature.myspace.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.myspace.data.MySpaceRepository
import com.twinhearts.app.feature.myspace.domain.model.MySpaceData
import com.twinhearts.app.feature.myspace.domain.model.NewHabit
import com.twinhearts.app.feature.myspace.domain.model.NewJournal
import com.twinhearts.app.feature.myspace.domain.model.NewNote
import com.twinhearts.app.feature.myspace.domain.model.NewReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** The five tabs of the web My Space page. */
enum class MySpaceTab(@StringRes val labelRes: Int) {
    Journal(R.string.myspace_tab_journal),
    Habits(R.string.myspace_tab_habits),
    Notes(R.string.myspace_tab_notes),
    Wellness(R.string.myspace_tab_wellness),
    Reminders(R.string.myspace_tab_reminders),
}

/** One-off result shown once as a toast (not part of the long-lived state). */
sealed interface MySpaceMessage {
    data class Done(@StringRes val textRes: Int, val clearsForm: Boolean = true) : MySpaceMessage
    data class Failed(val error: AppError) : MySpaceMessage
}

data class MySpaceUiState(
    val loading: Boolean = true,
    /** Set only when the first load failed; later save errors come as a [MySpaceMessage]. */
    val error: AppError? = null,
    val data: MySpaceData? = null,
    val tab: MySpaceTab = MySpaceTab.Journal,
    val saving: Boolean = false,
    val selectMode: Boolean = false,
    val selected: Set<String> = emptySet(),
    val message: MySpaceMessage? = null,
    /** Goes up by one each time something is added or ticked; the screen plays the petals on every change. */
    val petals: Int = 0,
)

/** Loads the couple's My Space data and applies every add, tick, save and delete. */
@HiltViewModel
class MySpaceViewModel @Inject constructor(
    private val repository: MySpaceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MySpaceUiState())
    val state: StateFlow<MySpaceUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.load()) {
                is AppResult.Success -> _state.update { it.copy(loading = false, data = r.data) }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    /** Silent refresh (every ~30 s while open): no spinner, no error, and never over an unsaved change. */
    fun refresh() {
        val s = _state.value
        if (s.saving || s.loading) return
        viewModelScope.launch {
            val r = repository.load()
            if (r is AppResult.Success && !_state.value.saving) _state.update { it.copy(data = r.data) }
        }
    }

    fun selectTab(tab: MySpaceTab) =
        _state.update { it.copy(tab = tab, selectMode = false, selected = emptySet()) }

    // ---- select mode (long-press, then tick more, then delete) ----
    fun startSelecting(id: String) = _state.update { it.copy(selectMode = true, selected = it.selected + id) }

    fun toggleSelected(id: String) = _state.update {
        val next = if (id in it.selected) it.selected - id else it.selected + id
        // Un-ticking the last one leaves select mode, as the web does.
        it.copy(selected = next, selectMode = next.isNotEmpty())
    }

    fun exitSelecting() = _state.update { it.copy(selectMode = false, selected = emptySet()) }

    /** Deletes these items of the tab being shown (one from its trash button, or all the ticked ones). */
    fun delete(ids: Set<String>) {
        if (ids.isEmpty()) return
        when (_state.value.tab) {
            MySpaceTab.Journal -> commit(R.string.myspace_deleted, false) { repository.deleteJournal(ids) }
            MySpaceTab.Habits -> commit(R.string.myspace_deleted, false) { repository.deleteHabits(ids) }
            MySpaceTab.Notes -> commit(R.string.myspace_deleted, false) { repository.deleteNotes(ids) }
            MySpaceTab.Reminders -> commit(R.string.myspace_deleted, false) { repository.deleteReminders(ids) }
            MySpaceTab.Wellness -> Unit
        }
    }

    // ---- adds and changes ----
    fun addJournal(entry: NewJournal) = commit(R.string.myspace_journal_saved, petals = true) { repository.addJournal(entry) }

    fun addHabit(habit: NewHabit) = commit(R.string.myspace_habit_added, petals = true) { repository.addHabit(habit) }

    fun toggleHabit(id: String) {
        val today = LocalDate.now()
        // Petals only when the tick turns the habit on, as on the web.
        val becomesDone = _state.value.data?.habits?.firstOrNull { it.id == id }?.let { it.doneDates.none { d -> d == today.toString() } } == true
        commit(null, petals = becomesDone) { repository.toggleHabit(id, today) }
    }

    fun addNote(note: NewNote) = commit(R.string.myspace_note_added, petals = true) { repository.addNote(note) }

    fun addReminder(reminder: NewReminder) = commit(R.string.myspace_reminder_added, petals = true) { repository.addReminder(reminder) }

    fun saveWellness(water: Int, sleep: Double, exercise: Int) =
        commit(R.string.myspace_wellness_saved, false) { repository.saveWellness(LocalDate.now(), water, sleep, exercise) }

    fun messageShown() = _state.update { it.copy(message = null) }

    /** Runs one change; on success the page shows the saved data and a toast, on failure only the toast. */
    private fun commit(
        @StringRes done: Int?,
        clearsForm: Boolean = true,
        petals: Boolean = false,
        block: suspend () -> AppResult<MySpaceData>,
    ) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val r = block()) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        saving = false,
                        data = r.data,
                        selectMode = false,
                        selected = emptySet(),
                        petals = if (petals) it.petals + 1 else it.petals,
                        message = done?.let { res -> MySpaceMessage.Done(res, clearsForm) },
                    )
                }
                is AppResult.Failure -> _state.update {
                    it.copy(saving = false, message = MySpaceMessage.Failed(r.error))
                }
            }
        }
    }
}
