package com.twinhearts.app.feature.pairing

import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.auth.MainDispatcherRule
import com.twinhearts.app.feature.pairing.data.PairingRepository
import com.twinhearts.app.feature.pairing.domain.model.IncomingRequest
import com.twinhearts.app.feature.pairing.domain.model.PairingStatus
import com.twinhearts.app.feature.pairing.ui.PairingMessage
import com.twinhearts.app.feature.pairing.ui.PairingViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PairingViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<PairingRepository>()

    private fun viewModel(status: PairingStatus = PairingStatus.NotConnected): PairingViewModel {
        coEvery { repository.status() } returns AppResult.Success(status)
        coEvery { repository.pendingRequests() } returns AppResult.Success(emptyList())
        return PairingViewModel(repository)
    }

    @Test
    fun loadsStatusAndRequests() {
        val request = IncomingRequest("r1", "Asha", "123", null, null)
        coEvery { repository.status() } returns AppResult.Success(PairingStatus.NotConnected)
        coEvery { repository.pendingRequests() } returns AppResult.Success(listOf(request))
        val vm = PairingViewModel(repository)
        assertEquals(UiState.Content(PairingStatus.NotConnected), vm.state.value.status)
        assertEquals(listOf(request), vm.state.value.incoming)
    }

    @Test
    fun firstLoadFailure_showsErrorState() {
        coEvery { repository.status() } returns AppResult.Failure(AppError.Network)
        val vm = PairingViewModel(repository)
        assertEquals(UiState.Error(AppError.Network), vm.state.value.status)
    }

    @Test
    fun emptyPhone_isRejectedBeforeServer() {
        val vm = viewModel()
        vm.sendRequest()
        assertEquals(PairingMessage.Problem(R.string.pairing_err_enter_phone), vm.state.value.message)
        coVerify(exactly = 0) { repository.sendRequest(any()) }
    }

    @Test
    fun sendRequest_success_showsInviteSentMessage() {
        coEvery { repository.sendRequest("9876543210") } returns AppResult.Success(Unit)
        val vm = viewModel()
        vm.onPhoneChange(" 9876543210 ")
        vm.sendRequest()
        assertEquals(PairingMessage.Info(R.string.pairing_info_sent), vm.state.value.message)
        assertEquals(false, vm.state.value.isWorking)
    }

    @Test
    fun sendRequest_serverError_showsServerMessage() {
        val error = AppError.Server(404, "No Twin Hearts account found with this phone number.")
        coEvery { repository.sendRequest(any()) } returns AppResult.Failure(error)
        val vm = viewModel()
        vm.onPhoneChange("1111111111")
        vm.sendRequest()
        assertEquals(PairingMessage.Failure(error), vm.state.value.message)
    }

    @Test
    fun accept_success_showsConnectedMessage() {
        coEvery { repository.accept("r1") } returns AppResult.Success("Asha")
        val vm = viewModel()
        vm.accept("r1")
        assertEquals(PairingMessage.Info(R.string.pairing_info_connected, "Asha"), vm.state.value.message)
        assertTrue(vm.state.value.status is UiState.Content)
    }
}
