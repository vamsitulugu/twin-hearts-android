package com.twinhearts.app.feature.auth.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.data.AuthRepository
import com.twinhearts.app.feature.auth.domain.model.Gender
import com.twinhearts.app.feature.auth.domain.model.GoogleCompleteRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompleteProfileUiState(
    /** The Google email. Shown, not editable: it is what the server verified. */
    val email: String = "",
    val name: String = "",
    val gender: Gender? = null,
    val phone: String = "",
    val password: String = "",
    val pin: String = "",
    val pinConfirm: String = "",
    val isLoading: Boolean = false,
    val message: AuthMessage? = null,
)

/**
 * The "Almost there" screen for a NEW Google account: it asks for what the normal Sign Up form asks
 * (name, boy/girl, phone, password, vault PIN) and creates the account. Checks run in the same order as Sign Up.
 * The route arguments (`signupToken`, `email`, `name`) arrive in the [SavedStateHandle] under those names.
 */
@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val signupToken: String = savedState[ARG_TOKEN] ?: ""

    // Password and PINs are never saved; the plain details survive process death.
    private val _state = MutableStateFlow(
        CompleteProfileUiState(
            email = savedState[ARG_EMAIL] ?: "",
            name = savedState[KEY_NAME] ?: savedState[ARG_NAME] ?: "",
            phone = savedState[KEY_PHONE] ?: "",
            gender = savedState.get<String>(KEY_GENDER)?.let { saved -> Gender.entries.firstOrNull { it.name == saved } },
        ),
    )
    val state: StateFlow<CompleteProfileUiState> = _state.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events: Flow<AuthEvent> = _events.receiveAsFlow()

    fun onNameChange(value: String) {
        savedState[KEY_NAME] = value
        _state.update { it.copy(name = value) }
    }

    fun onGenderChange(value: Gender) {
        savedState[KEY_GENDER] = value.name
        _state.update { it.copy(gender = value) }
    }

    fun onPhoneChange(value: String) {
        savedState[KEY_PHONE] = value
        _state.update { it.copy(phone = value) }
    }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value) }

    fun onPinChange(value: String) =
        _state.update { it.copy(pin = value.filter(Char::isDigit).take(AuthValidators.PIN_LENGTH)) }

    fun onPinConfirmChange(value: String) =
        _state.update { it.copy(pinConfirm = value.filter(Char::isDigit).take(AuthValidators.PIN_LENGTH)) }

    fun submit() {
        val current = _state.value
        if (current.isLoading) return
        validate(current)?.let { problem ->
            _state.update { it.copy(message = problem) }
            return
        }
        val gender = current.gender ?: return
        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val request = GoogleCompleteRequest(
                signupToken = signupToken,
                email = current.email,
                name = current.name.trim(),
                phone = current.phone.trim(),
                password = current.password,
                vaultPin = current.pin,
                gender = gender,
            )
            when (val result = repository.completeGoogleSignUp(request)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.send(AuthEvent.SignedIn)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, message = AuthMessage.Failure(result.error))
                }
            }
        }
    }

    private fun validate(s: CompleteProfileUiState): AuthMessage? {
        if (signupToken.isBlank()) return AuthMessage.Resource(R.string.auth_err_google_failed)
        if (s.name.isBlank() || s.phone.isBlank() || s.password.isEmpty()) {
            return AuthMessage.Resource(R.string.auth_err_fill_all)
        }
        if (AuthValidators.isPasswordTooShort(s.password)) return AuthMessage.Resource(R.string.auth_err_password_short)
        if (s.gender == null) return AuthMessage.Resource(R.string.auth_err_gender)
        AuthValidators.vaultPinProblem(s.pin)?.let { return AuthMessage.Resource(it) }
        if (s.pin != s.pinConfirm) return AuthMessage.Resource(R.string.auth_err_pin_mismatch)
        return null
    }

    private companion object {
        // Same names as the Routes.CompleteProfile properties (type-safe navigation stores them under these).
        const val ARG_TOKEN = "signupToken"
        const val ARG_EMAIL = "email"
        const val ARG_NAME = "name"
        const val KEY_NAME = "google_profile_name"
        const val KEY_PHONE = "google_profile_phone"
        const val KEY_GENDER = "google_profile_gender"
    }
}
