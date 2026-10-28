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
)

/** Loads the couple's transactions and adds / deletes them, or saves my monthly goal. */
@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val repository: MoneyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MoneyUiState())
    val state: StateFlow<MoneyUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.load()) {
                is AppResult.Success -> _state.update { it.copy(loading = false, data = result.data) }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    fun addTransaction(tx: NewTransaction) = save(MoneyMessage.Added) { repository.addTransaction(tx) }

    fun deleteTransaction(id: String) = save(MoneyMessage.Deleted) { repository.deleteTransaction(id) }

    fun saveGoal(goal: Double) = save(MoneyMessage.GoalSaved) { repository.saveGoal(goal) }

    private fun save(success: MoneyMessage, call: suspend () -> AppResult<MoneyData>) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
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

    fun messageShown() = _state.update { it.copy(message = null) }
}
