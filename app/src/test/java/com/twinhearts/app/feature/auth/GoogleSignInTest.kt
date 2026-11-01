package com.twinhearts.app.feature.auth

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.data.AuthRepository
import com.twinhearts.app.feature.auth.data.google.GoogleTokenResult
import com.twinhearts.app.feature.auth.domain.model.Gender
import com.twinhearts.app.feature.auth.domain.model.GoogleAuthOutcome
import com.twinhearts.app.feature.auth.domain.model.GoogleCompleteRequest
import com.twinhearts.app.feature.auth.ui.AuthEvent
import com.twinhearts.app.feature.auth.ui.AuthMessage
import com.twinhearts.app.feature.auth.ui.CompleteProfileViewModel
import com.twinhearts.app.feature.auth.ui.SignInViewModel
import com.twinhearts.app.feature.auth.ui.SignUpViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class GoogleSignInTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val repository = mockk<AuthRepository>()

    private fun profileViewModel(token: String = "tok.sig") = CompleteProfileViewModel(
        repository,
        SavedStateHandle(mapOf("signupToken" to token, "email" to "me@gmail.com", "name" to "Vamsi G")),
    )

    @Test
    fun signIn_existingAccount_goesStraightIn() = runTest {
        coEvery { repository.googleAuth("idtoken") } returns AppResult.Success(GoogleAuthOutcome.SignedIn(testSession()))
        val vm = SignInViewModel(repository, SavedStateHandle())
        vm.onGoogleResult(GoogleTokenResult.Token("idtoken"))
        vm.events.test { assertEquals(AuthEvent.SignedIn, awaitItem()) }
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun signIn_newEmail_opensAlmostThereScreen() = runTest {
        coEvery { repository.googleAuth(any()) } returns
            AppResult.Success(GoogleAuthOutcome.NeedsProfile("tok.sig", "me@gmail.com", "Vamsi G"))
        val vm = SignInViewModel(repository, SavedStateHandle())
        vm.onGoogleResult(GoogleTokenResult.Token("idtoken"))
        vm.events.test { assertEquals(AuthEvent.NeedsProfile("tok.sig", "me@gmail.com", "Vamsi G"), awaitItem()) }
    }

    @Test
    fun signUp_existingAccount_goesHome_notToPairing() = runTest {
        coEvery { repository.googleAuth(any()) } returns AppResult.Success(GoogleAuthOutcome.SignedIn(testSession()))
        val vm = SignUpViewModel(repository, SavedStateHandle())
        vm.onGoogleResult(GoogleTokenResult.Token("idtoken"))
        vm.events.test { assertEquals(AuthEvent.SignedInExisting, awaitItem()) }
    }

    @Test
    fun cancelledGoogleSheet_showsNothing_andSkipsServer() {
        val vm = SignInViewModel(repository, SavedStateHandle())
        vm.onGoogleResult(GoogleTokenResult.Cancelled)
        assertEquals(null, vm.state.value.message)
        coVerify(exactly = 0) { repository.googleAuth(any()) }
    }

    @Test
    fun noGoogleAccountOnPhone_showsHelpfulMessage() {
        val vm = SignInViewModel(repository, SavedStateHandle())
        vm.onGoogleResult(GoogleTokenResult.NoAccount)
        assertEquals(AuthMessage.Resource(R.string.auth_err_google_no_account), vm.state.value.message)
    }

    @Test
    fun serverRejectingToken_showsServerMessage() {
        val error = AppError.Unauthorized("Google sign-in failed. Please try again.")
        coEvery { repository.googleAuth(any()) } returns AppResult.Failure(error)
        val vm = SignInViewModel(repository, SavedStateHandle())
        vm.onGoogleResult(GoogleTokenResult.Token("bad"))
        assertEquals(AuthMessage.Failure(error), vm.state.value.message)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun completeProfile_prefillsGoogleNameAndEmail() {
        val state = profileViewModel().state.value
        assertEquals("me@gmail.com", state.email)
        assertEquals("Vamsi G", state.name)
    }

    @Test
    fun completeProfile_asksForEverythingTheSignUpFormAsks() {
        val vm = profileViewModel()
        vm.submit()
        assertEquals(AuthMessage.Resource(R.string.auth_err_fill_all), vm.state.value.message)

        vm.onPhoneChange("1234567890"); vm.onPasswordChange("abc")
        vm.submit()
        assertEquals(AuthMessage.Resource(R.string.auth_err_password_short), vm.state.value.message)

        vm.onPasswordChange("secret1")
        vm.submit()
        assertEquals(AuthMessage.Resource(R.string.auth_err_gender), vm.state.value.message)

        vm.onGenderChange(Gender.Female); vm.onPinChange("1234"); vm.onPinConfirmChange("1234")
        vm.submit()
        assertEquals(AuthMessage.Resource(R.string.auth_err_pin_sequence), vm.state.value.message)

        vm.onPinChange("2580"); vm.onPinConfirmChange("2581")
        vm.submit()
        assertEquals(AuthMessage.Resource(R.string.auth_err_pin_mismatch), vm.state.value.message)
        coVerify(exactly = 0) { repository.completeGoogleSignUp(any()) }
    }

    @Test
    fun completeProfile_success_sendsTokenAndDetails_thenSignsIn() = runTest {
        val sent = slot<GoogleCompleteRequest>()
        coEvery { repository.completeGoogleSignUp(capture(sent)) } returns AppResult.Success(testSession())
        val vm = profileViewModel()
        vm.onNameChange(" Vamsi ")
        vm.onGenderChange(Gender.Male)
        vm.onPhoneChange(" 1234567890 ")
        vm.onPasswordChange("secret1")
        vm.onPinChange("2580"); vm.onPinConfirmChange("2580")
        vm.submit()
        vm.events.test { assertEquals(AuthEvent.SignedIn, awaitItem()) }
        assertEquals("tok.sig", sent.captured.signupToken)
        assertEquals("Vamsi", sent.captured.name)
        assertEquals("1234567890", sent.captured.phone)
        assertEquals(Gender.Male, sent.captured.gender)
    }

    @Test
    fun completeProfile_missingToken_isCaughtBeforeServer() {
        val vm = profileViewModel(token = "")
        vm.onPhoneChange("1234567890"); vm.onPasswordChange("secret1"); vm.onGenderChange(Gender.Female)
        vm.onPinChange("2580"); vm.onPinConfirmChange("2580")
        vm.submit()
        assertEquals(AuthMessage.Resource(R.string.auth_err_google_failed), vm.state.value.message)
        coVerify(exactly = 0) { repository.completeGoogleSignUp(any()) }
    }
}
