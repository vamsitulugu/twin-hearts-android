package com.twinhearts.app.feature.auth.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.auth.data.AuthRepository
import com.twinhearts.app.feature.auth.data.google.GoogleTokenResult
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

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val message: AuthMessage? = null,
)

/** Logic of the Sign In screen. The password is never saved; the email survives process death. */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState(email = savedState[KEY_EMAIL] ?: ""))
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events: Flow<AuthEvent> = _events.receiveAsFlow()

    fun onEmailChange(value: String) {
        savedState[KEY_EMAIL] = value
        _state.update { it.copy(email = value) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value) }
    }

    fun submit() {
        val current = _state.value
        if (current.isLoading) return
        val email = current.email.trim()
        if (email.isEmpty() || current.password.isEmpty()) {
            _state.update { it.copy(message = AuthMessage.Resource(R.string.auth_err_enter_credentials)) }
            return
        }
        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            when (val result = repository.signIn(email, current.password)) {
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

    /**
     * Continue with Google. An existing account goes straight in (nothing more to type);
     * a new email is sent to the "Almost there" screen for the remaining sign-up details.
     */
    fun onGoogleResult(result: GoogleTokenResult) {
        if (_state.value.isLoading) return
        if (result !is GoogleTokenResult.Token) {
            result.problemMessage()?.let { problem -> _state.update { it.copy(message = problem) } }
            return
        }
        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            when (val step = repository.googleStep(result.idToken, existingAccountEvent = AuthEvent.SignedIn)) {
                is GoogleStep.Go -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.send(step.event)
                }
                is GoogleStep.Problem -> _state.update { it.copy(isLoading = false, message = step.message) }
            }
        }
    }

    private companion object {
        const val KEY_EMAIL = "sign_in_email"
    }
}
