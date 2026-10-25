package com.twinhearts.app.feature.auth

import androidx.lifecycle.SavedStateHandle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.data.AuthRepository
import com.twinhearts.app.feature.auth.domain.model.Gender
import com.twinhearts.app.feature.auth.domain.model.SignUpRequest
import com.twinhearts.app.feature.auth.ui.AuthMessage
import com.twinhearts.app.feature.auth.ui.SignUpViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SignUpViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<AuthRepository>()

    private fun filledViewModel(pin: String = "2580", confirm: String = "2580") = SignUpViewModel(repository, SavedStateHandle()).apply {
        onEmailChange(" me@mail.com ")
        onPasswordChange("secret1")
        onNameChange(" Vamsi ")
        onGenderChange(Gender.Male)
        onPhoneChange(" 1234567890 ")
        onPinChange(pin)
        onPinConfirmChange(confirm)
    }

    @Test
    fun missingGender_isCaughtBeforeServer() {
        val vm = SignUpViewModel(repository, SavedStateHandle())
        vm.onEmailChange("me@mail.com"); vm.onPasswordChange("secret1"); vm.onNameChange("V"); vm.onPhoneChange("1234567890")
        vm.submit()
        assertEquals(AuthMessage.Resource(R.string.auth_err_gender), vm.state.value.message)
    }

    @Test
    fun weakPin_isRejected() {
        val vm = filledViewModel(pin = "1234", confirm = "1234")
        vm.submit()
        assertEquals(AuthMessage.Resource(R.string.auth_err_pin_sequence), vm.state.value.message)
        coVerify(exactly = 0) { repository.signUp(any()) }
    }

    @Test
    fun pinsMustMatch() {
        val vm = filledViewModel(confirm = "2581")
        vm.submit()
        assertEquals(AuthMessage.Resource(R.string.auth_err_pin_mismatch), vm.state.value.message)
    }

    @Test
    fun pinBoxes_keepOnlyFourDigits() {
        val vm = SignUpViewModel(repository, SavedStateHandle())
        vm.onPinChange("12a345")
        assertEquals("1234", vm.state.value.pin)
    }

    @Test
    fun validForm_sendsTrimmedRequest() {
        val request = slot<SignUpRequest>()
        coEvery { repository.signUp(capture(request)) } returns AppResult.Success(testSession())
        filledViewModel().submit()
        assertEquals("me@mail.com", request.captured.email)
        assertEquals("Vamsi", request.captured.name)
        assertEquals("1234567890", request.captured.phone)
        assertEquals(Gender.Male, request.captured.gender)
    }
}
