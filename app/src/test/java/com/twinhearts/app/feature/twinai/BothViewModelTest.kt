package com.twinhearts.app.feature.twinai

import androidx.lifecycle.SavedStateHandle
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.twinai.data.TwinAiRepository
import com.twinhearts.app.feature.twinai.domain.model.AiIdentity
import com.twinhearts.app.feature.twinai.domain.model.BothDiscussion
import com.twinhearts.app.feature.twinai.domain.model.BothPeople
import com.twinhearts.app.feature.twinai.domain.model.BothRound
import com.twinhearts.app.feature.twinai.domain.model.BothSession
import com.twinhearts.app.feature.twinai.domain.model.RoundStatus
import com.twinhearts.app.feature.twinai.ui.BothHistory
import com.twinhearts.app.feature.twinai.ui.BothViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BothViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    @get:Rule val mainDispatcher = MainDispatcherRule(dispatcher)

    private val repository = mockk<TwinAiRepository>(relaxed = true)
    private val who = AiIdentity("c1", "user1", "Vamsi", "Likitha", "")
    private val session = BothSession("s1", "Trip", 3, null)

    private fun round(
        number: Int = 3,
        status: RoundStatus = RoundStatus.Pending,
        you: Boolean = false,
        partner: Boolean = false,
    ) = BothRound("r1", number, status, you, partner, if (you) "mine" else null, null)

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle(), current: BothRound = round()): BothViewModel {
        coEvery { repository.identity() } returns who
        coEvery { repository.people(who) } returns BothPeople("Vamsi", "Likitha", "https://x/me.png", null)
        coEvery { repository.discussion(who, "s1") } returns AppResult.Success(BothDiscussion(session, listOf(current)))
        return BothViewModel(repository, saved)
    }

    @Test
    fun signedOutHistoryShowsAnErrorInsteadOfLoadingForever() {
        coEvery { repository.identity() } returns null
        val vm = BothViewModel(repository, SavedStateHandle())
        vm.loadHistory()
        assertTrue(vm.state.value.history is BothHistory.Error)
    }

    @Test
    fun namesAndPhotosAreLoadedWhenADiscussionOpens() {
        val vm = viewModel()
        vm.open("s1")
        assertEquals("https://x/me.png", vm.state.value.myAvatar)
        assertEquals("Likitha", vm.state.value.partnerName)
    }

    @Test
    fun theOpenDiscussionIsRememberedAfterTheAppIsKilled() {
        val saved = SavedStateHandle(mapOf("open_session" to "s1"))
        val vm = viewModel(saved)
        vm.setVisible(true)
        assertEquals("s1", vm.state.value.open?.session?.id)
        vm.close()
        assertEquals(null, saved.get<String>("open_session"))
    }

    @Test
    fun submittingSecondShowsAnalyzingNotWaiting() {
        val vm = viewModel(current = round(partner = true))
        vm.open("s1")
        coEvery { repository.submit(who, "r1", "hello") } returns AppResult.Success(RoundStatus.Analyzing)
        vm.onDraftChange("hello")
        vm.submit()
        val now = vm.state.value.open?.current
        assertTrue(now?.youSubmitted == true && now.partnerSubmitted)
        assertEquals(RoundStatus.Analyzing, now?.status)
    }

    @Test
    fun recoveringFromAConflictKeepsTheRoundNumber() {
        val vm = viewModel(current = round(number = 3, you = true))
        vm.open("s1")
        coEvery { repository.edit(who, "r1", "new") } returns AppResult.Failure(AppError.Server(409, "locked"))
        // The server's round answer carries no round number, so it maps to 1.
        coEvery { repository.round(who, "r1") } returns AppResult.Success(round(number = 1, status = RoundStatus.Analyzing, you = true, partner = true))
        vm.beginEdit()
        vm.onDraftChange("new")
        vm.saveEdit()
        assertEquals(3, vm.state.value.open?.current?.number)
        assertEquals(RoundStatus.Analyzing, vm.state.value.open?.current?.status)
    }

    @Test
    fun remindButtonComesBackAfterTwoSeconds() {
        val vm = viewModel(current = round(you = true))
        vm.open("s1")
        coEvery { repository.nudge(who, "r1") } returns AppResult.Success(true)
        vm.nudge()
        assertTrue(vm.state.value.reminded)
        dispatcher.scheduler.advanceTimeBy(2_100)
        dispatcher.scheduler.runCurrent()
        assertFalse(vm.state.value.reminded)
        coVerify(exactly = 1) { repository.nudge(who, "r1") }
    }
}
