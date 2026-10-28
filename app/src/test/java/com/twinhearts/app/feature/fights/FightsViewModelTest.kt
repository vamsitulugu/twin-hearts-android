package com.twinhearts.app.feature.fights

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.TwinPromptHandoff
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.fights.data.FightsRepository
import com.twinhearts.app.feature.fights.domain.model.Fight
import com.twinhearts.app.feature.fights.domain.model.FightDraft
import com.twinhearts.app.feature.fights.domain.model.FightForm
import com.twinhearts.app.feature.fights.domain.model.FightIdentity
import com.twinhearts.app.feature.fights.ui.FightView
import com.twinhearts.app.feature.fights.ui.FightsLoad
import com.twinhearts.app.feature.fights.ui.FightsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FightsViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<FightsRepository>(relaxed = true)
    private val handoff = TwinPromptHandoff()
    private val who = FightIdentity("c1", "user1", "Vamsi", "Likitha")
    private val one = Fight(1L, "Plan changes", reason = "Late again", date = "2026-10-20")

    private fun viewModel(list: List<Fight> = listOf(one), draft: FightDraft? = null): FightsViewModel {
        coEvery { repository.identity() } returns who
        coEvery { repository.fights() } returns AppResult.Success(list)
        every { repository.loadDraft() } returns draft
        return FightsViewModel(repository, handoff)
    }

    @Test
    fun loadsTheFights() {
        val vm = viewModel()
        assertEquals(FightsLoad.Ready(listOf(one)), vm.state.value.load)
        assertEquals("Likitha", vm.state.value.partnerName)
    }

    @Test
    fun failedLoadShowsTheError() {
        coEvery { repository.identity() } returns who
        coEvery { repository.fights() } returns AppResult.Failure(AppError.Network)
        every { repository.loadDraft() } returns null
        val vm = FightsViewModel(repository, handoff)
        assertTrue(vm.state.value.load is FightsLoad.Error)
    }

    @Test
    fun nextStepNeedsATitle() {
        val vm = viewModel()
        vm.openLog()
        vm.next()
        assertEquals(1, vm.state.value.step)
        assertTrue(vm.state.value.titleMissing)
        vm.onForm { it.copy(title = "Money talk") }
        vm.next()
        assertEquals(2, vm.state.value.step)
        assertFalse(vm.state.value.titleMissing)
    }

    @Test
    fun backStepsOutOfTheWizardBeforeLeaving() {
        val vm = viewModel()
        vm.openLog()
        vm.onForm { it.copy(title = "x") }
        vm.goStep(3)
        assertTrue(vm.handleBack())
        assertEquals(2, vm.state.value.step)
        assertTrue(vm.handleBack())
        assertEquals(1, vm.state.value.step)
        assertTrue(vm.handleBack())
        assertEquals(FightView.Dashboard, vm.state.value.view)
        assertFalse(vm.handleBack())
    }

    @Test
    fun savingAddsTheFightAndResetsTheWizard() {
        val saved = Fight(2L, "Money talk", date = "2026-10-28")
        coEvery { repository.add(any()) } returns AppResult.Success(listOf(saved, one))
        val vm = viewModel()
        vm.openLog()
        vm.onForm { it.copy(title = "Money talk") }
        vm.goStep(3)
        vm.next()
        assertEquals(FightView.Dashboard, vm.state.value.view)
        assertEquals(FightForm(), vm.state.value.form)
        assertEquals(FightsLoad.Ready(listOf(saved, one)), vm.state.value.load)
        verify { repository.clearDraft() }
    }

    @Test
    fun failedSaveKeepsWhatWasTyped() {
        coEvery { repository.add(any()) } returns AppResult.Failure(AppError.Network)
        val vm = viewModel()
        vm.openLog()
        vm.onForm { it.copy(title = "Money talk") }
        vm.goStep(3)
        vm.next()
        assertEquals(FightView.Log, vm.state.value.view)
        assertEquals("Money talk", vm.state.value.form.title)
    }

    @Test
    fun draftIsRestoredOnce() {
        val draft = FightDraft(FightForm(title = "Draft title"), 2)
        val vm = viewModel(draft = draft)
        vm.openLog()
        assertEquals("Draft title", vm.state.value.form.title)
        assertEquals(2, vm.state.value.step)
    }

    @Test
    fun askAiHandsTheSentenceToTwin() {
        val vm = viewModel()
        vm.openDetail(one)
        vm.askAi()
        assertNull(vm.state.value.detail)
        assertTrue(handoff.take()!!.startsWith("We had a fight about: Plan changes."))
    }

    @Test
    fun resolvingUpdatesTheList() {
        val done = one.copy(status = com.twinhearts.app.feature.fights.domain.model.FightStatus.Resolved, resolvedDate = "2026-10-28")
        coEvery { repository.resolve(1L) } returns AppResult.Success(listOf(done))
        val vm = viewModel()
        vm.openDetail(one)
        vm.resolveDetail()
        assertNull(vm.state.value.detail)
        assertEquals(FightsLoad.Ready(listOf(done)), vm.state.value.load)
    }
}
