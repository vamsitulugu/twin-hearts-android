package com.twinhearts.app.feature.home

import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.auth.testSession
import com.twinhearts.app.feature.home.data.HomeRepository
import com.twinhearts.app.feature.home.domain.model.CardPhoto
import com.twinhearts.app.feature.home.domain.model.CardPhotoSlot
import com.twinhearts.app.feature.home.domain.model.LoveCardState
import com.twinhearts.app.feature.home.domain.model.HomeOverview
import com.twinhearts.app.feature.home.domain.model.LoveKind
import com.twinhearts.app.feature.home.domain.model.MoodKind
import java.time.LocalDate
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
        coEvery { repository.overview() } returns AppResult.Success(HomeOverview())
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
        coEvery { repository.overview() } returns AppResult.Success(HomeOverview(loveCard = LoveCardState(pendingIncomingHugId = 7L)))
        every { sessionStore.session } returns flowOf(testSession().copy(paired = true, partnerName = "Asha"))
        val vm = HomeViewModel(repository, sessionStore)
        vm.dismissHug()
        assertEquals(7L, vm.state.value.dismissedHugId)
        vm.acceptHug()
        assertEquals(HomeMessage.Info(R.string.home_info_hug_accepted, "Asha"), vm.state.value.message)
    }

    @Test
    fun setMood_updatesCardAtOnce_andSaves() {
        coEvery { repository.setMood(any(), any()) } returns AppResult.Success(Unit)
        val vm = viewModel(testSession().copy(paired = true))
        val day = LocalDate.of(2026, 10, 26)
        vm.setMood(day, MoodKind.Happy)
        coVerify { repository.setMood(day, MoodKind.Happy) }
        assertEquals(HomeMessage.Info(R.string.home_info_mood_saved, MoodKind.Happy.emoji), vm.state.value.message)
    }

    @Test
    fun setMood_failure_showsError() {
        val error = AppError.Network
        coEvery { repository.setMood(any(), any()) } returns AppResult.Failure(error)
        val vm = viewModel(testSession().copy(paired = true))
        vm.setMood(LocalDate.of(2026, 10, 26), null)
        assertEquals(HomeMessage.Failure(error), vm.state.value.message)
    }

    @Test
    fun useDefaultCardPhoto_success_showsMessage() {
        coEvery { repository.resetCardPhoto(CardPhotoSlot.Hug) } returns AppResult.Success(Unit)
        val vm = viewModel(testSession().copy(paired = true))
        vm.useDefaultCardPhoto(CardPhotoSlot.Hug)
        assertEquals(HomeMessage.Info(R.string.home_photo_back_to_default), vm.state.value.message)
    }

    @Test
    fun useDefaultCardPhoto_failure_showsError() {
        val error = AppError.Network
        coEvery { repository.resetCardPhoto(any()) } returns AppResult.Failure(error)
        val vm = viewModel(testSession().copy(paired = true))
        vm.useDefaultCardPhoto(CardPhotoSlot.Idle)
        assertEquals(HomeMessage.Failure(error), vm.state.value.message)
    }

    @Test
    fun overview_cardPhotos_reachTheState() {
        val photos = mapOf(CardPhotoSlot.Touch to CardPhoto("https://x/t.jpg", "c1/t.jpg"))
        every { sessionStore.session } returns flowOf(testSession().copy(paired = true))
        coEvery { repository.overview() } returns AppResult.Success(HomeOverview(cardPhotos = photos))
        val vm = HomeViewModel(repository, sessionStore)
        assertEquals(photos, vm.state.value.cardPhotos)
    }
}
