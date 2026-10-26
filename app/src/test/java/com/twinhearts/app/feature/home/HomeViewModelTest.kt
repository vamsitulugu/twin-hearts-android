package com.twinhearts.app.feature.home

import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.auth.testSession
import com.twinhearts.app.feature.home.data.HomeRepository
import com.twinhearts.app.feature.home.domain.model.LoveCardState
import com.twinhearts.app.feature.home.domain.model.LoveKind
import com.twinhearts.app.feature.home.ui.HomeMessage
import com.twinhearts.app.feature.home.ui.HomeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<HomeRepository>()
    private val sessionStore = mockk<SessionStore>()

    private fun viewModel(session: Session): HomeViewModel {
        every { sessionStore.session } returns flowOf(session)
        coEvery { repository.loveCard() } returns AppResult.Success(LoveCardState())
        return HomeViewModel(repository, sessionStore)
    }

    @Test
    fun touch_success_showsSentMessage() {
        coEvery { repository.send(LoveKind.Touch) } returns AppResult.Success(Unit)
        val vm = viewModel(testSession().copy(paired = true, partnerName = "Asha"))
        vm.send(LoveKind.Touch)
        assertEquals(HomeMessage.Info(R.string.home_info_touch_sent, "Asha"), vm.state.value.message)
        assertFalse(vm.state.value.isSending)
    }

    @Test
    fun notPaired_sendsNothing() {
        val vm = viewModel(testSession().copy(paired = false))
        vm.send(LoveKind.Hug)
        coVerify(exactly = 0) { repository.send(any()) }
    }

    @Test
    fun serverError_isShown() {
        val error = AppError.Network
        coEvery { repository.send(LoveKind.MissYou) } returns AppResult.Failure(error)
        val vm = viewModel(testSession().copy(paired = true))
        vm.send(LoveKind.MissYou)
        assertEquals(HomeMessage.Failure(error), vm.state.value.message)
    }

    @Test
    fun acceptHug_showsHuggedMessage_andLaterHidesIt() {
        coEvery { repository.acceptHug() } returns AppResult.Success(Unit)
        coEvery { repository.loveCard() } returns AppResult.Success(LoveCardState(pendingIncomingHugId = 7L))
        every { sessionStore.session } returns flowOf(testSession().copy(paired = true, partnerName = "Asha"))
        val vm = HomeViewModel(repository, sessionStore)
        vm.dismissHug()
        assertEquals(7L, vm.state.value.dismissedHugId)
        vm.acceptHug()
        assertEquals(HomeMessage.Info(R.string.home_info_hug_accepted, "Asha"), vm.state.value.message)
    }
}
