package com.twinhearts.app.feature.money.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.money.data.MoneyRepository
import com.twinhearts.app.feature.money.domain.model.MoneyData
import com.twinhearts.app.feature.money.domain.model.NewTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** One-off result shown once as a toast (R-42: not part of the long-lived state). */
sealed interface MoneyMessage {
    data object Added : MoneyMessage
    data object Deleted : MoneyMessage
    data object GoalSaved : MoneyMessage
    data class Failed(val error: AppError) : MoneyMessage
}

data class MoneyUiState(
    val loading: Boolean = true,
    /** Set only when the first load failed; later save errors come as a [MoneyMessage]. */
    val error: AppError? = null,
    val data: MoneyData? = null,
    val saving: Boolean = false,
    val message: MoneyMessage? = null,
    /** True while the page shows the saved copy because there is no connection. */
    val offline: Boolean = false,
)

/** Loads the couple's transactions and adds / deletes them, or saves my monthly goal. */
@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val repository: MoneyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MoneyUiState())
    val state: StateFlow<MoneyUiState> = _state.asStateFlow()

    /** Every write waits for the one before it: each is read-then-save, so two at once would lose data. */
    private val writeLock = Mutex()

    init {
        viewModelScope.launch {
            // Paint the saved copy straight away, then fetch the fresh data.
            repository.cachedData()?.let { saved ->
                _state.update { if (it.data == null) it.copy(loading = false, data = saved) else it }
            }
            fetch(silent = false)
        }
    }

    /** The refresh button and the Retry button. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            fetch(silent = false)
        }
    }

    /** Called every ~30 s while the page is open: picks up the partner's changes without a spinner. */
    fun refresh() {
        if (_state.value.saving || writeLock.isLocked) return
        viewModelScope.launch { fetch(silent = true) }
    }

    private suspend fun fetch(silent: Boolean) {
        val result = repository.load()
        // A save that started while this was loading has newer data; do not overwrite it.
        if ((_state.value.saving || writeLock.isLocked) && result is AppResult.Success) return
        when (result) {
            is AppResult.Success -> _state.update { it.copy(loading = false, error = null, offline = false, data = result.data) }
            is AppResult.Failure -> _state.update {
                when {
                    it.data != null && result.error == AppError.Network -> it.copy(loading = false, offline = true)
                    silent -> it
                    it.data != null -> it.copy(loading = false, message = MoneyMessage.Failed(result.error))
                    else -> it.copy(loading = false, error = result.error)
                }
            }
        }
    }

    fun addTransaction(tx: NewTransaction) = save(MoneyMessage.Added) { repository.addTransaction(tx) }

    fun deleteTransaction(id: String) = save(MoneyMessage.Deleted) { repository.deleteTransaction(id) }

    /**
     * Saves my goal quietly (the field saves itself as I type, so no toast and no spinner).
     * Saves queue behind each other instead of being dropped, so the last number typed always wins.
     */
    fun saveGoal(goal: Double) {
        viewModelScope.launch {
            writeLock.withLock {
                when (val result = repository.saveGoal(goal)) {
                    is AppResult.Success -> _state.update { it.copy(data = it.data?.copy(myGoal = result.data.myGoal) ?: result.data) }
                    is AppResult.Failure -> _state.update { it.copy(message = MoneyMessage.Failed(result.error)) }
                }
            }
        }
    }

    private fun save(success: MoneyMessage, call: suspend () -> AppResult<MoneyData>) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            writeLock.withLock {
                when (val result = call()) {
                    is AppResult.Success -> _state.update {
                        it.copy(saving = false, data = result.data, message = success)
                    }
                    is AppResult.Failure -> _state.update {
                        it.copy(saving = false, message = MoneyMessage.Failed(result.error))
                    }
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}
