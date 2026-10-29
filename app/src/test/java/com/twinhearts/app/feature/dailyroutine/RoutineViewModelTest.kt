package com.twinhearts.app.feature.dailyroutine

import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.dailyroutine.data.RoutineIdentity
import com.twinhearts.app.feature.dailyroutine.data.RoutineRepository
import com.twinhearts.app.feature.dailyroutine.domain.model.Reminder
import com.twinhearts.app.feature.dailyroutine.domain.model.Repeat
import com.twinhearts.app.feature.dailyroutine.domain.model.Routine
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineDraft
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineStatus
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineType
import com.twinhearts.app.feature.dailyroutine.domain.model.Visibility
import com.twinhearts.app.feature.dailyroutine.ui.FormError
import com.twinhearts.app.feature.dailyroutine.ui.RoutineMessage
import com.twinhearts.app.feature.dailyroutine.ui.RoutineTab
import com.twinhearts.app.feature.dailyroutine.ui.RoutineViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class RoutineViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<RoutineRepository>()

    // Sunday 25 October 2026, 10:30 UTC
    private val clock = Clock.fixed(Instant.parse("2026-10-25T10:30:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 25)
    private val me = RoutineIdentity("user1", "Vamsi", "Likitha")

    private fun routine(id: String, start: String, end: String, role: String = "user1", visibility: Visibility = Visibility.Private) = Routine(
        id = id, role = role, title = "T$id", description = "", type = RoutineType.Routine, location = "", notes = "",
        startTime = start, endTime = end, date = today, repeat = Repeat.None, repeatDays = emptyList(),
        visibility = visibility, reminder = Reminder.None, statusByDate = emptyMap(), timeByDate = emptyMap(),
    )

    private fun draft(title: String = "Gym", start: String = "07:00", end: String = "08:00") = RoutineDraft(
        title = title, startTime = start, endTime = end, type = RoutineType.Workout, description = "",
        repeat = Repeat.None, reminder = Reminder.None, visibility = Visibility.Private, location = "", notes = "",
    )

    private fun viewModel(routines: List<Routine> = emptyList(), load: AppResult<List<Routine>> = AppResult.Success(routines)): RoutineViewModel {
        coEvery { repository.identity() } returns me
        coEvery { repository.load() } returns load
        return RoutineViewModel(repository, clock)
    }

    @Test
    fun load_success_showsTodaysRoutines() {
        val vm = viewModel(listOf(routine("a", "09:00", "10:00"), routine("b", "11:00", "12:00", role = "user2", visibility = Visibility.Shared)))
        assertFalse(vm.state.value.isLoading)
        assertEquals(today, vm.state.value.date)
        assertEquals(listOf("a"), vm.state.value.occurrences.map { it.routine.id })
        vm.selectTab(RoutineTab.Partner)
        assertEquals(listOf("b"), vm.state.value.occurrences.map { it.routine.id })
    }

    @Test
    fun load_failure_setsError() {
        val vm = viewModel(load = AppResult.Failure(AppError.Network))
        assertEquals(AppError.Network, vm.state.value.error)
    }

    @Test
    fun dates_shiftAndPick() {
        val vm = viewModel()
        vm.shiftDate(1)
        assertEquals(today.plusDays(1), vm.state.value.date)
        assertFalse(vm.state.value.isToday)
        vm.pickDate(today)
        assertTrue(vm.state.value.isToday)
    }

    @Test
    fun save_emptyName_showsProblemInSheet() {
        val vm = viewModel()
        vm.openAdd()
        vm.save(draft(title = " "))
        assertEquals(FormError.Text(R.string.dr_err_name), vm.state.value.formError)
        coVerify(exactly = 0) { repository.create(any(), any()) }
    }

    @Test
    fun save_missingTimes_andSameTimes() {
        val vm = viewModel()
        vm.openAdd()
        vm.save(draft(start = "", end = ""))
        assertEquals(FormError.Text(R.string.dr_err_times), vm.state.value.formError)
        vm.save(draft(start = "07:00", end = "07:00"))
        assertEquals(FormError.Text(R.string.dr_err_same_time), vm.state.value.formError)
    }

    @Test
    fun save_overlap_isRejected_butEditingItselfIsFine() {
        val vm = viewModel(listOf(routine("a", "07:00", "08:00")))
        vm.openAdd()
        vm.save(draft(start = "07:30", end = "08:30"))
        assertEquals(FormError.Text(R.string.dr_err_conflict), vm.state.value.formError)
        coVerify(exactly = 0) { repository.create(any(), any()) }

        coEvery { repository.update("a", any(), any()) } returns AppResult.Success(Unit)
        vm.openEdit("a")
        vm.save(draft(start = "07:15", end = "08:15"))
        coVerify { repository.update("a", any(), today) }
    }

    @Test
    fun save_success_closesSheet_toasts_andReloads() = runTest {
        coEvery { repository.create(any(), any()) } returns AppResult.Success(Unit)
        val vm = viewModel()
        vm.openAdd("07:00", "08:00")
        assertEquals("07:00", vm.state.value.form?.startTime)
        vm.save(draft())
        coVerify { repository.create(any(), today) }
        assertNull(vm.state.value.form)
        assertEquals(RoutineMessage.Info(R.string.dr_saved), vm.events.first())
        coVerify(atLeast = 2) { repository.load() }
    }

    @Test
    fun save_serverError_isShownInSheet_andSheetStaysOpen() {
        coEvery { repository.create(any(), any()) } returns AppResult.Failure(AppError.Server(400, "Start time is invalid"))
        val vm = viewModel()
        vm.openAdd()
        vm.save(draft())
        assertEquals(FormError.Failure(AppError.Server(400, "Start time is invalid")), vm.state.value.formError)
        assertTrue(vm.state.value.form != null)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun detail_markComplete_sendsStatusForThatDay() = runTest {
        coEvery { repository.setStatus(any(), any(), any()) } returns AppResult.Success(Unit)
        val vm = viewModel(listOf(routine("a", "09:00", "10:00")))
        vm.openDetail("a", today)
        vm.markCompleted()
        coVerify { repository.setStatus("a", today, RoutineStatus.Completed) }
        assertNull(vm.state.value.detail)
        assertEquals(RoutineMessage.Info(R.string.dr_marked_complete), vm.events.first())
    }

    @Test
    fun detail_skip_duplicate() {
        coEvery { repository.setStatus(any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery { repository.duplicate(any()) } returns AppResult.Success(Unit)
        val vm = viewModel(listOf(routine("a", "09:00", "10:00")))
        vm.openDetail("a", today)
        vm.skip()
        coVerify { repository.setStatus("a", today, RoutineStatus.Skipped) }
        vm.openDetail("a", today)
        vm.duplicate()
        coVerify { repository.duplicate("a") }
    }

    @Test
    fun delete_asksBeforeDeleting() {
        coEvery { repository.delete("a") } returns AppResult.Success(Unit)
        val vm = viewModel(listOf(routine("a", "09:00", "10:00")))
        vm.openDetail("a", today)
        vm.askDelete()
        assertTrue(vm.state.value.confirmDelete)
        vm.cancelDelete()
        coVerify(exactly = 0) { repository.delete(any()) }
        vm.askDelete()
        vm.confirmDelete()
        coVerify { repository.delete("a") }
        assertNull(vm.state.value.detail)
        assertFalse(vm.state.value.confirmDelete)
    }

    @Test
    fun detail_failure_keepsSheetAndToasts() = runTest {
        coEvery { repository.setStatus(any(), any(), any()) } returns AppResult.Failure(AppError.Network)
        val vm = viewModel(listOf(routine("a", "09:00", "10:00")))
        vm.openDetail("a", today)
        vm.markCompleted()
        assertTrue(vm.state.value.detail != null)
        assertEquals(RoutineMessage.Failure(AppError.Network), vm.events.first())
    }
}
