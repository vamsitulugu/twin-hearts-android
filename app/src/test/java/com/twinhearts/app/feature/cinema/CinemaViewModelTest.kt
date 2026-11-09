package com.twinhearts.app.feature.cinema

import androidx.lifecycle.SavedStateHandle
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.cinema.data.CinemaRepository
import com.twinhearts.app.feature.cinema.domain.model.CinemaIdentity
import com.twinhearts.app.feature.cinema.domain.model.CinemaInvite
import com.twinhearts.app.feature.cinema.ui.CinemaViewModel
import com.twinhearts.app.feature.cinema.ui.invite.CinemaInviteViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CinemaViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<CinemaRepository>()
    private val me = CinemaIdentity(coupleId = "c1", role = "user1", gender = "female")
    private val link = "https://twinhearts.vercel.app/cinema/#s=abc"   // what the server sends
    private val local = "https://appassets.androidplatform.net/cinema/index.html#s=abc" // what the app opens

    private fun host() = CinemaViewModel(SavedStateHandle(), repository)
    private fun guest() = CinemaViewModel(SavedStateHandle(mapOf("sessionId" to "s1")), repository)

    @Test
    fun host_opensASession_andOpensTheBundledTheatreWithTheBackLink() {
        coEvery { repository.identity() } returns me
        coEvery { repository.open(me) } returns AppResult.Success(link)
        val state = host().state.value
        assertTrue(state is UiState.Content)
        assertTrue((state as UiState.Content).data.startsWith("$local&b="))
    }

    @Test
    fun partner_acceptsTheInvite_andLoadsItsLink() {
        coEvery { repository.identity() } returns me
        coEvery { repository.accept(me, "s1") } returns AppResult.Success(link)
        assertTrue(guest().state.value is UiState.Content)
        coVerify(exactly = 0) { repository.open(any()) }
    }

    @Test
    fun notSignedInOrNotPaired_showsAnError() {
        coEvery { repository.identity() } returns null
        assertTrue(host().state.value is UiState.Error)
    }

    @Test
    fun serverError_isShownWithRetry_andRetryLoadsAgain() {
        coEvery { repository.identity() } returns me
        coEvery { repository.accept(me, "s1") } returns AppResult.Failure(AppError.Server(410, "This invite expired"))
        val vm = guest()
        assertEquals(UiState.Error(AppError.Server(410, "This invite expired")), vm.state.value)
        coEvery { repository.accept(me, "s1") } returns AppResult.Success(link)
        vm.load()
        assertTrue(vm.state.value is UiState.Content)
    }

    @Test
    fun linkWithoutRoomSettings_isNeverLoaded() {
        coEvery { repository.identity() } returns me
        coEvery { repository.open(me) } returns AppResult.Success("https://twinhearts.vercel.app/cinema/")
        assertEquals(UiState.Error(AppError.Unknown), host().state.value)
    }
}

class CinemaInviteViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<CinemaRepository>()
    private val me = CinemaIdentity(coupleId = "c1", role = "user2", gender = null)
    private val invite = CinemaInvite(id = "s1", hostName = "Asha", movie = null)

    @Test
    fun check_showsAWaitingInvite() = runTest {
        coEvery { repository.identity() } returns me
        coEvery { repository.pendingInvite(me) } returns AppResult.Success(invite)
        val vm = CinemaInviteViewModel(repository)
        vm.check()
        assertEquals(invite, vm.invite.value)
    }

    @Test
    fun check_withNoInviteOrOffline_showsNothing() = runTest {
        coEvery { repository.identity() } returns me
        coEvery { repository.pendingInvite(me) } returns AppResult.Failure(AppError.Network)
        val vm = CinemaInviteViewModel(repository)
        vm.check()
        assertNull(vm.invite.value)
    }

    @Test
    fun accept_returnsTheSessionId_andTheSameInviteIsNotShownAgain() = runTest {
        coEvery { repository.identity() } returns me
        coEvery { repository.pendingInvite(me) } returns AppResult.Success(invite)
        val vm = CinemaInviteViewModel(repository)
        vm.check()
        assertEquals("s1", vm.accept())
        assertNull(vm.invite.value)
        vm.check()
        assertNull(vm.invite.value)
    }

    @Test
    fun notNow_tellsTheHost_andClosesTheCard() = runTest {
        coEvery { repository.identity() } returns me
        coEvery { repository.pendingInvite(me) } returns AppResult.Success(invite)
        coEvery { repository.decline(me, "s1") } returns AppResult.Success(Unit)
        val vm = CinemaInviteViewModel(repository)
        vm.check()
        vm.notNow()
        assertNull(vm.invite.value)
        coVerify(exactly = 1) { repository.decline(me, "s1") }
    }
}
