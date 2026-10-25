package com.twinhearts.app.feature.auth.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.data.AuthRepository
import com.twinhearts.app.feature.auth.domain.model.Gender
import com.twinhearts.app.feature.auth.domain.model.SignUpRequest
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

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val gender: Gender? = null,
    val phone: String = "",
    val pin: String = "",
    val pinConfirm: String = "",
    val isLoading: Boolean = false,
    val message: AuthMessage? = null,
)

/** Logic of the Sign Up screen. Checks run in the same order as the web form. */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    // Passwords and PINs are never saved; the plain details survive process death.
    private val _state = MutableStateFlow(
        SignUpUiState(
            email = savedState[KEY_EMAIL] ?: "",
            name = savedState[KEY_NAME] ?: "",
            phone = savedState[KEY_PHONE] ?: "",
            gender = savedState.get<String>(KEY_GENDER)?.let { saved -> Gender.entries.firstOrNull { it.name == saved } },
        ),
    )
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events: Flow<AuthEvent> = _events.receiveAsFlow()

    fun onEmailChange(value: String) {
        savedState[KEY_EMAIL] = value
        _state.update { it.copy(email = value) }
    }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value) }

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

    /** PIN boxes accept digits only, at most 4. */
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
            val request = SignUpRequest(
                email = current.email.trim(),
                password = current.password,
                name = current.name.trim(),
                phone = current.phone.trim(),
                vaultPin = current.pin,
                gender = gender,
            )
            when (val result = repository.signUp(request)) {
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

    private fun validate(s: SignUpUiState): AuthMessage? {
        if (s.email.isBlank() || s.password.isEmpty() || s.name.isBlank() || s.phone.isBlank()) {
            return AuthMessage.Resource(R.string.auth_err_fill_all)
        }
        if (AuthValidators.isPasswordTooShort(s.password)) return AuthMessage.Resource(R.string.auth_err_password_short)
        if (s.gender == null) return AuthMessage.Resource(R.string.auth_err_gender)
        AuthValidators.vaultPinProblem(s.pin)?.let { return AuthMessage.Resource(it) }
        if (s.pin != s.pinConfirm) return AuthMessage.Resource(R.string.auth_err_pin_mismatch)
        return null
    }

    private companion object {
        const val KEY_EMAIL = "sign_up_email"
        const val KEY_NAME = "sign_up_name"
        const val KEY_PHONE = "sign_up_phone"
        const val KEY_GENDER = "sign_up_gender"
    }
}
