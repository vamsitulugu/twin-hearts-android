package com.twinhearts.app.feature.surprises

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.surprises.data.SurprisesRepository
import com.twinhearts.app.feature.surprises.domain.model.Capsule
import com.twinhearts.app.feature.surprises.domain.model.Surprise
import com.twinhearts.app.feature.surprises.domain.model.SurpriseKind
import com.twinhearts.app.feature.surprises.domain.model.SurprisesData
import com.twinhearts.app.feature.surprises.ui.SurprisesMessage
import com.twinhearts.app.feature.surprises.ui.SurprisesTab
import com.twinhearts.app.feature.surprises.ui.SurprisesViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class SurprisesViewModelTest {
    @get:Rule val mainDispatcher = SurprisesMainDispatcherRule()

    private val repository = mockk<SurprisesRepository>(relaxed = true)
    private val today = LocalDate.now()

    private val oneSurprise = Surprise("1", "🎁", "Gift", "", "", null, today.plusDays(2), mine = true)
    private val lockedCapsule = Capsule("2", "Later", today.plusDays(5), opened = false)
    private val openCapsule = Capsule("3", "Now", today, opened = false)
    private val data = SurprisesData(listOf(oneSurprise), listOf(lockedCapsule, openCapsule))

    private fun viewModel(loaded: AppResult<SurprisesData> = AppResult.Success(data)): SurprisesViewModel {
        coEvery { repository.load() } returns loaded
        return SurprisesViewModel(repository)
    }

    @Test fun `loads the page on start`() {
        val vm = viewModel()
        assertFalse(vm.state.value.loading)
        assertEquals(data, vm.state.value.data)
        assertNull(vm.state.value.loadError)
    }

    @Test fun `a failed first load shows the error and no lists`() {
        val vm = viewModel(AppResult.Failure(AppError.Network))
        assertEquals(AppError.Network, vm.state.value.loadError)
        assertFalse(vm.state.value.loading)
    }

    @Test fun `a failed refresh keeps the old lists and only shows a message`() {
        val vm = viewModel()
        coEvery { repository.load() } returns AppResult.Failure(AppError.Network)
        vm.refresh()
        assertEquals(data, vm.state.value.data)
        assertNull(vm.state.value.loadError)
        assertEquals(SurprisesMessage.Failed(AppError.Network), vm.state.value.message)
    }

    @Test fun `tabs switch`() {
        val vm = viewModel()
        vm.selectTab(SurprisesTab.Capsule)
        assertEquals(SurprisesTab.Capsule, vm.state.value.tab)
    }

    @Test fun `a surprise needs a title and a date`() {
        val vm = viewModel()
        vm.addSurprise(SurpriseKind.Gift, "   ", "", null, today.plusDays(1))
        assertEquals(SurprisesMessage.NeedTitleAndDate, vm.state.value.message)
        vm.messageShown()
        vm.addSurprise(SurpriseKind.Gift, "Gift", "", null, null)
        assertEquals(SurprisesMessage.NeedTitleAndDate, vm.state.value.message)
        coVerify(exactly = 0) { repository.addSurprise(any()) }
    }

    @Test fun `a saved surprise replaces the list and says it is hidden`() {
        val vm = viewModel()
        val updated = SurprisesData(listOf(oneSurprise, oneSurprise.copy(id = "9")), data.capsules)
        coEvery { repository.addSurprise(any()) } returns AppResult.Success(updated)
        vm.addSurprise(SurpriseKind.Letter, " Hello ", " hi ", null, today.plusDays(1))
        assertEquals(updated, vm.state.value.data)
        assertEquals(SurprisesMessage.Hidden, vm.state.value.message)
        assertFalse(vm.state.value.saving)
        coVerify { repository.addSurprise(match { it.title == "Hello" && it.message == "hi" && it.kind == SurpriseKind.Letter }) }
    }

    @Test fun `a failed save keeps the lists and reports the error`() {
        val vm = viewModel()
        coEvery { repository.addSurprise(any()) } returns AppResult.Failure(AppError.Server(500, "Oops"))
        vm.addSurprise(SurpriseKind.Gift, "Gift", "", null, today.plusDays(1))
        assertEquals(data, vm.state.value.data)
        assertEquals(SurprisesMessage.Failed(AppError.Server(500, "Oops")), vm.state.value.message)
        assertFalse(vm.state.value.saving)
    }

    @Test fun `a capsule needs a message and a date`() {
        val vm = viewModel()
        vm.addCapsule("", today.plusDays(1))
        assertEquals(SurprisesMessage.NeedMessageAndDate, vm.state.value.message)
        vm.messageShown()
        vm.addCapsule("Hello", null)
        assertEquals(SurprisesMessage.NeedMessageAndDate, vm.state.value.message)
        coVerify(exactly = 0) { repository.addCapsule(any()) }
    }

    @Test fun `a sealed capsule says so`() {
        val vm = viewModel()
        coEvery { repository.addCapsule(any()) } returns AppResult.Success(data)
        vm.addCapsule("Hello", today.plusDays(10))
        assertEquals(SurprisesMessage.Sealed, vm.state.value.message)
    }

    @Test fun `a locked capsule cannot be opened`() {
        val vm = viewModel()
        vm.openCapsule(lockedCapsule.id, lockedCapsule.unlock)
        assertEquals(SurprisesMessage.StillLocked(lockedCapsule.unlock), vm.state.value.message)
        coVerify(exactly = 0) { repository.openCapsule(any()) }
    }

    @Test fun `an unlocked capsule is opened and no toast is shown`() {
        val vm = viewModel()
        val opened = data.copy(capsules = listOf(lockedCapsule, openCapsule.copy(opened = true)))
        coEvery { repository.openCapsule("3") } returns AppResult.Success(opened)
        vm.openCapsule(openCapsule.id, openCapsule.unlock)
        assertTrue(vm.state.value.data.capsules.last().opened)
        assertNull(vm.state.value.message)
    }

    @Test fun `deleting reports it`() {
        val vm = viewModel()
        coEvery { repository.deleteSurprise("1") } returns AppResult.Success(SurprisesData(emptyList(), data.capsules))
        vm.deleteSurprise("1")
        assertTrue(vm.state.value.data.surprises.isEmpty())
        assertEquals(SurprisesMessage.SurpriseDeleted, vm.state.value.message)
    }

    @Test fun `a message is cleared once shown`() {
        val vm = viewModel()
        vm.addCapsule("", null)
        vm.messageShown()
        assertNull(vm.state.value.message)
    }
}
