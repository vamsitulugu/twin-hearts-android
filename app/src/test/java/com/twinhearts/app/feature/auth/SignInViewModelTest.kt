package com.twinhearts.app.feature.auth

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.data.AuthRepository
import com.twinhearts.app.feature.auth.ui.AuthEvent
import com.twinhearts.app.feature.auth.ui.AuthMessage
import com.twinhearts.app.feature.auth.ui.SignInViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<AuthRepository>()
    private fun viewModel() = SignInViewModel(repository, SavedStateHandle())

    @Test
    fun emptyFields_showMessage_andSkipServer() {
        val vm = viewModel()
        vm.submit()
        assertEquals(AuthMessage.Resource(R.string.auth_err_enter_credentials), vm.state.value.message)
        coVerify(exactly = 0) { repository.signIn(any(), any()) }
    }

    @Test
    fun success_sendsSignedInEvent() = runTest {
        coEvery { repository.signIn("a@b.com", "secret1") } returns AppResult.Success(testSession())
        val vm = viewModel()
        vm.onEmailChange(" a@b.com ")
        vm.onPasswordChange("secret1")
        vm.submit()
        vm.events.test { assertEquals(AuthEvent.SignedIn, awaitItem()) }
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun wrongPassword_showsServerMessage() {
        val error = AppError.Unauthorized("Incorrect password.")
        coEvery { repository.signIn(any(), any()) } returns AppResult.Failure(error)
        val vm = viewModel()
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("nope12")
        vm.submit()
        assertEquals(AuthMessage.Failure(error), vm.state.value.message)
        assertFalse(vm.state.value.isLoading)
    }
}
