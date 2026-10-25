package com.twinhearts.app.feature.auth.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ForgotStep { VerifyIdentity, NewPassword }

data class ForgotPasswordUiState(
    val step: ForgotStep = ForgotStep.VerifyIdentity,
    val email: String = "",
    val phone: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    /** True after the password was changed: the form is hidden and a green message shows. */
    val isDone: Boolean = false,
    val message: AuthMessage? = null,
)

/**
 * Two-step reset, like the web: (1) email + phone must match, (2) choose a new password.
 * The one-time reset token is kept only in memory, never saved anywhere.
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private var resetToken: String? = null

    private val _state = MutableStateFlow(
        ForgotPasswordUiState(email = savedState[KEY_EMAIL] ?: "", phone = savedState[KEY_PHONE] ?: ""),
    )
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events: Flow<AuthEvent> = _events.receiveAsFlow()

    fun onEmailChange(value: String) {
        savedState[KEY_EMAIL] = value
        _state.update { it.copy(email = value) }
    }

    fun onPhoneChange(value: String) {
        savedState[KEY_PHONE] = value
        _state.update { it.copy(phone = value) }
    }

    fun onNewPasswordChange(value: String) = _state.update { it.copy(newPassword = value) }

    fun onConfirmPasswordChange(value: String) = _state.update { it.copy(confirmPassword = value) }

    fun verifyIdentity() {
        val current = _state.value
        if (current.isLoading) return
        val email = current.email.trim()
        val phone = current.phone.trim()
        if (email.isEmpty() || phone.isEmpty()) {
            _state.update { it.copy(message = AuthMessage.Resource(R.string.auth_err_forgot_both)) }
            return
        }
        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            when (val result = repository.verifyResetIdentity(email, phone)) {
                is AppResult.Success -> {
                    resetToken = result.data
                    _state.update { it.copy(isLoading = false, step = ForgotStep.NewPassword) }
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, message = AuthMessage.Failure(result.error))
                }
            }
        }
    }

    fun updatePassword() {
        val current = _state.value
        if (current.isLoading) return
        val token = resetToken
        val problem = when {
            token == null -> R.string.auth_err_session_expired
            current.newPassword.isEmpty() || current.confirmPassword.isEmpty() -> R.string.auth_err_new_both
            AuthValidators.isPasswordTooShort(current.newPassword) -> R.string.auth_err_password_short
            current.newPassword != current.confirmPassword -> R.string.auth_err_pass_mismatch
            else -> null
        }
        if (problem != null || token == null) {
            _state.update { it.copy(message = AuthMessage.Resource(problem ?: R.string.auth_err_session_expired)) }
            return
        }
        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            when (val result = repository.resetPassword(token, current.newPassword)) {
                is AppResult.Success -> {
                    resetToken = null // single use: forget it right away
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isDone = true,
                            newPassword = "",
                            confirmPassword = "",
                            message = AuthMessage.Success(R.string.auth_password_updated),
                        )
                    }
                    delay(SUCCESS_PAUSE_MS) // let the user read the green message, then go back to Sign In
                    _events.send(AuthEvent.PasswordUpdated)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, message = AuthMessage.Failure(result.error))
                }
            }
        }
    }

    private companion object {
        const val KEY_EMAIL = "forgot_email"
        const val KEY_PHONE = "forgot_phone"
        const val SUCCESS_PAUSE_MS = 1600L
    }
}
