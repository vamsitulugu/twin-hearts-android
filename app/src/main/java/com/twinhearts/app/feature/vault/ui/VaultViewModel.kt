package com.twinhearts.app.feature.vault.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.vault.data.VaultRepository
import com.twinhearts.app.feature.vault.domain.VaultLogic
import com.twinhearts.app.feature.vault.domain.model.VaultItem
import com.twinhearts.app.feature.vault.domain.model.VaultKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which screen of the vault is showing (the web `vaultMode` plus "unlocked"). */
enum class VaultStage {
    /** Asking the server whether a PIN was chosen yet. */
    Checking,
    /** Normal unlock. */
    Enter,
    /** Choose a PIN (first time only). */
    Setup,
    /** Type the new PIN again. */
    Confirm,
    Open,
}

/** Why a PIN or reset attempt did not work, in words the screen can show. */
sealed interface VaultProblem {
    data object WrongPin : VaultProblem
    data class Rule(@StringRes val textRes: Int) : VaultProblem
    data class Server(val error: AppError) : VaultProblem
}

/** One-off result shown once as a toast. */
sealed interface VaultMessage {
    data class Done(@StringRes val textRes: Int) : VaultMessage
    data class Uploaded(val added: Int, val failed: Int, val kind: VaultKind) : VaultMessage
    data class Failed(val error: AppError) : VaultMessage
}

data class VaultUiState(
    val stage: VaultStage = VaultStage.Checking,
    /** The digits typed so far (at most four). They only live in memory. */
    val pin: String = "",
    val firstPin: String = "",
    val busy: Boolean = false,
    val problem: VaultProblem? = null,
    val resetOpen: Boolean = false,
    val resetProblem: VaultProblem? = null,
    val items: List<VaultItem> = emptyList(),
    val itemsLoading: Boolean = false,
    val itemsError: AppError? = null,
    val uploading: Boolean = false,
    val message: VaultMessage? = null,
    /** Goes up by one on each unlock; the screen plays the petals, as the web does. */
    val petals: Int = 0,
)

/**
 * The Secret Vault: PIN lock (the server is the only judge, there is no offline unlock), then the
 * private photos and videos. Leaving the page drops everything, so it opens locked next time.
 */
@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: VaultRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VaultUiState())
    val state: StateFlow<VaultUiState> = _state.asStateFlow()

    init {
        refreshMode()
    }

    /** Asks whether a PIN exists. If the server cannot be reached the normal lock shows and it answers on submit. */
    private fun refreshMode() {
        viewModelScope.launch {
            val next = when (val r = repository.needsSetup()) {
                is AppResult.Success -> if (r.data) VaultStage.Setup else VaultStage.Enter
                is AppResult.Failure -> VaultStage.Enter
            }
            _state.update { if (it.stage == VaultStage.Open) it else it.copy(stage = next, pin = "", firstPin = "") }
        }
    }

    // ---- PIN pad ----
    fun pinKey(digit: Char) = _state.update {
        if (it.busy || it.problem != null || it.pin.length >= VaultLogic.PIN_LENGTH) it else it.copy(pin = it.pin + digit)
    }

    fun pinDelete() = _state.update {
        if (it.busy || it.problem != null) it else it.copy(pin = it.pin.dropLast(1))
    }

    fun submitPin() {
        val s = _state.value
        if (s.busy || s.problem != null) return
        val pin = s.pin
        when (s.stage) {
            VaultStage.Enter -> {
                if (pin.length != VaultLogic.PIN_LENGTH) return
                viewModelScope.launch { verify(pin) }
            }
            VaultStage.Setup -> {
                val rule = VaultLogic.weakPinMessage(pin)
                if (rule != null) return fail(VaultProblem.Rule(rule))
                _state.update { it.copy(stage = VaultStage.Confirm, firstPin = pin, pin = "") }
            }
            VaultStage.Confirm -> {
                if (pin.length != VaultLogic.PIN_LENGTH) return
                if (pin != s.firstPin) {
                    _state.update { it.copy(stage = VaultStage.Setup, firstPin = "", pin = "", message = VaultMessage.Done(R.string.vault_pin_mismatch)) }
                    return
                }
                viewModelScope.launch { choose(pin) }
            }
            VaultStage.Checking, VaultStage.Open -> Unit
        }
    }

    private suspend fun verify(pin: String) {
        _state.update { it.copy(busy = true) }
        when (val r = repository.verifyPin(pin)) {
            is AppResult.Success -> unlocked(R.string.vault_unlocked)
            is AppResult.Failure -> {
                val e = r.error
                when {
                    // 409: no real PIN was ever chosen, so the person must choose one first.
                    e is AppError.Server && e.code == CONFLICT -> _state.update {
                        it.copy(busy = false, stage = VaultStage.Setup, pin = "", message = VaultMessage.Done(R.string.vault_choose_first))
                    }
                    e is AppError.Unauthorized -> fail(VaultProblem.WrongPin)
                    else -> fail(VaultProblem.Server(e))
                }
            }
        }
    }

    private suspend fun choose(pin: String) {
        _state.update { it.copy(busy = true) }
        when (val r = repository.setPin(pin)) {
            is AppResult.Success -> unlocked(R.string.vault_pin_set)
            is AppResult.Failure -> {
                val e = r.error
                if (e is AppError.Server && e.code == FORBIDDEN) {
                    // A PIN already exists (set on the other phone meanwhile).
                    _state.update {
                        it.copy(busy = false, stage = VaultStage.Enter, pin = "", firstPin = "", message = VaultMessage.Done(R.string.vault_pin_already_set))
                    }
                } else {
                    _state.update { it.copy(busy = false, stage = VaultStage.Setup, firstPin = "", pin = "") }
                    fail(VaultProblem.Server(e))
                }
            }
        }
    }

    private fun unlocked(@StringRes toast: Int) {
        _state.update {
            it.copy(busy = false, stage = VaultStage.Open, pin = "", firstPin = "", problem = null, petals = it.petals + 1, message = VaultMessage.Done(toast))
        }
        loadItems()
    }

    /** Shows the problem in red for a moment, then clears the dots so the person can try again. */
    private fun fail(problem: VaultProblem) {
        _state.update { it.copy(busy = false, problem = problem) }
        viewModelScope.launch {
            delay(if (problem is VaultProblem.WrongPin) SHORT_ERROR_MS else LONG_ERROR_MS)
            _state.update { if (it.problem == problem) it.copy(problem = null, pin = "") else it }
        }
    }

    /** The Sync button: reload the list when open, otherwise ask the server again whether a PIN exists. */
    fun refresh() {
        if (_state.value.stage == VaultStage.Open) loadItems() else refreshMode()
    }

    fun lock() {
        _state.update { VaultUiState(petals = it.petals) }
        refreshMode()
    }

    // ---- forgot PIN ----
    fun openReset() = _state.update { it.copy(resetOpen = true, resetProblem = null) }

    fun closeReset() = _state.update { it.copy(resetOpen = false, resetProblem = null) }

    fun resetPin(password: String, newPin: String, newPin2: String) {
        if (_state.value.busy) return
        if (password.isBlank()) return setResetProblem(VaultProblem.Rule(R.string.vault_reset_need_password))
        VaultLogic.weakPinMessage(newPin)?.let { return setResetProblem(VaultProblem.Rule(it)) }
        if (newPin != newPin2) return setResetProblem(VaultProblem.Rule(R.string.vault_pin_mismatch))
        viewModelScope.launch {
            _state.update { it.copy(busy = true, resetProblem = null) }
            when (val r = repository.resetPin(password, newPin)) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        busy = false, resetOpen = false, stage = VaultStage.Enter, pin = "", firstPin = "",
                        message = VaultMessage.Done(R.string.vault_reset_done),
                    )
                }
                is AppResult.Failure -> _state.update { it.copy(busy = false, resetProblem = VaultProblem.Server(r.error)) }
            }
        }
    }

    private fun setResetProblem(problem: VaultProblem) = _state.update { it.copy(resetProblem = problem) }

    // ---- items ----
    fun loadItems() {
        viewModelScope.launch {
            _state.update { it.copy(itemsLoading = true, itemsError = null) }
            when (val r = repository.items()) {
                is AppResult.Success -> _state.update { it.copy(itemsLoading = false, items = r.data) }
                is AppResult.Failure -> _state.update { it.copy(itemsLoading = false, itemsError = r.error) }
            }
        }
    }

    fun add(uris: List<Uri>, kind: VaultKind) {
        if (uris.isEmpty() || _state.value.uploading) return
        viewModelScope.launch {
            _state.update { it.copy(uploading = true) }
            when (val r = repository.add(uris, kind)) {
                is AppResult.Success -> _state.update {
                    it.copy(uploading = false, items = r.data.items, message = VaultMessage.Uploaded(r.data.added, r.data.failed, kind))
                }
                is AppResult.Failure -> _state.update { it.copy(uploading = false, message = VaultMessage.Failed(r.error)) }
            }
        }
    }

    fun delete(id: String) {
        if (_state.value.uploading) return
        viewModelScope.launch {
            _state.update { it.copy(uploading = true) }
            when (val r = repository.delete(id)) {
                is AppResult.Success -> _state.update {
                    it.copy(uploading = false, items = r.data, message = VaultMessage.Done(R.string.vault_deleted))
                }
                is AppResult.Failure -> _state.update { it.copy(uploading = false, message = VaultMessage.Failed(r.error)) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    private companion object {
        const val CONFLICT = 409
        const val FORBIDDEN = 403
        const val SHORT_ERROR_MS = 1_500L
        const val LONG_ERROR_MS = 3_500L
    }
}
