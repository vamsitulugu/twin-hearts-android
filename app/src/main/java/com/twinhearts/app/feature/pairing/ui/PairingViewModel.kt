package com.twinhearts.app.feature.pairing.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.feature.pairing.data.PairingRepository
import com.twinhearts.app.feature.pairing.domain.model.IncomingRequest
import com.twinhearts.app.feature.pairing.domain.model.PairingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Message box at the top of the Pairing screen: green for good news, red for problems. */
sealed interface PairingMessage {
    data class Info(@StringRes val id: Int, val arg: String? = null) : PairingMessage
    data class Problem(@StringRes val id: Int) : PairingMessage
    data class Failure(val error: AppError) : PairingMessage
}

@Composable
fun PairingMessage.asText(): String = when (this) {
    is PairingMessage.Info -> if (arg == null) stringResource(id) else stringResource(id, arg)
    is PairingMessage.Problem -> stringResource(id)
    is PairingMessage.Failure -> error.asText()
}

data class PairingUiState(
    val status: UiState<PairingStatus> = UiState.Loading,
    val incoming: List<IncomingRequest> = emptyList(),
    val phone: String = "",
    /** True while send / accept / decline / cancel is running. */
    val isWorking: Boolean = false,
    val message: PairingMessage? = null,
)

/**
 * Logic of the Pairing screen. The screen asks for [refresh] every 5 seconds while it is visible
 * (the web does the same with its sync loop), so a partner's accept shows up without a restart.
 */
@HiltViewModel
class PairingViewModel @Inject constructor(
    private val repository: PairingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PairingUiState())
    val state: StateFlow<PairingUiState> = _state.asStateFlow()

    init {
        refresh(initial = true)
    }

    fun retry() = refresh(initial = true)

    /** [initial] shows the loading skeleton; later refreshes are silent and keep the screen steady. */
    fun refresh(initial: Boolean = false) {
        viewModelScope.launch {
            if (initial) _state.update { it.copy(status = UiState.Loading) }
            when (val result = repository.status()) {
                is AppResult.Success -> {
                    val incoming = if (result.data is PairingStatus.Connected) {
                        emptyList()
                    } else {
                        (repository.pendingRequests() as? AppResult.Success)?.data ?: _state.value.incoming
                    }
                    _state.update { it.copy(status = UiState.Content(result.data), incoming = incoming) }
                }
                is AppResult.Failure -> if (_state.value.status !is UiState.Content) {
                    _state.update { it.copy(status = UiState.Error(result.error)) }
                }
            }
        }
    }

    fun onPhoneChange(value: String) = _state.update { it.copy(phone = value) }

    fun sendRequest() {
        val phone = _state.value.phone.trim()
        if (phone.isEmpty()) {
            _state.update { it.copy(message = PairingMessage.Problem(R.string.pairing_err_enter_phone)) }
            return
        }
        perform(onSuccess = { PairingMessage.Info(R.string.pairing_info_sent) }) { repository.sendRequest(phone) }
    }

    fun accept(requestId: String) = perform(
        onSuccess = { name -> PairingMessage.Info(R.string.pairing_info_connected, name as? String) },
    ) { repository.accept(requestId) }

    fun decline(requestId: String) =
        perform(onSuccess = { PairingMessage.Info(R.string.pairing_info_declined) }) { repository.reject(requestId) }

    fun cancel(requestId: String) = perform(onSuccess = { null }) { repository.cancel(requestId) }

    private fun perform(
        onSuccess: (Any?) -> PairingMessage?,
        call: suspend () -> AppResult<Any?>,
    ) {
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            when (val result = call()) {
                is AppResult.Success -> {
                    _state.update { it.copy(isWorking = false, message = onSuccess(result.data)) }
                    refresh()
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isWorking = false, message = PairingMessage.Failure(result.error))
                }
            }
        }
    }
}
