package com.twinhearts.app.feature.twinai

import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.twinai.data.TwinAiRepository
import com.twinhearts.app.feature.twinai.data.realtime.BothRealtime
import com.twinhearts.app.feature.twinai.domain.model.AiIdentity
import com.twinhearts.app.feature.twinai.domain.model.BothDiscussion
import com.twinhearts.app.feature.twinai.domain.model.BothRound
import com.twinhearts.app.feature.twinai.domain.model.BothSession
import com.twinhearts.app.feature.twinai.domain.model.RoundStatus
import com.twinhearts.app.feature.twinai.ui.BothHistory
import com.twinhearts.app.feature.twinai.ui.BothViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BothViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<TwinAiRepository>(relaxed = true)
    private val realtime = mockk<BothRealtime>(relaxed = true)
    private val who = AiIdentity("c1", "user1", "Vamsi", "Likitha", "")
    private val session = BothSession("s1", "Where to eat", 1, null)
    private val round = BothRound("r1", 1, RoundStatus.Pending, false, false, null, null)

    private fun viewModel(lastId: String?): BothViewModel {
        coEvery { repository.identity() } returns who
        every { repository.lastDiscussionId() } returns lastId
        coEvery { repository.bothSessions(who) } returns AppResult.Success(listOf(session))
        coEvery { repository.discussion(who, "s1") } returns AppResult.Success(BothDiscussion(session, listOf(round)))
        return BothViewModel(repository, realtime)
    }

    @Test
    fun showsHistoryWhenNothingWasOpenBefore() {
        val vm = viewModel(lastId = null)
        vm.setVisible(true)
        assertTrue(vm.state.value.history is BothHistory.Content)
        assertNull(vm.state.value.open)
        assertEquals("Likitha", vm.state.value.partnerName)
    }

    @Test
    fun reopensTheLastDiscussion() {
        val vm = viewModel(lastId = "s1")
        vm.setVisible(true)
        assertNotNull(vm.state.value.open)
        assertEquals("s1", vm.state.value.open?.session?.id)
    }

    @Test
    fun closingForgetsTheLastDiscussion() {
        val vm = viewModel(lastId = "s1")
        vm.setVisible(true)
        vm.close()
        verify { repository.saveLastDiscussionId(null) }
        assertNull(vm.state.value.open)
    }

    @Test
    fun hidingTheTabStopsLiveUpdates() {
        val vm = viewModel(lastId = null)
        vm.setVisible(true)
        vm.setVisible(false)
        verify { realtime.close() }
    }
}
