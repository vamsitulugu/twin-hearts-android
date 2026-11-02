package com.twinhearts.app.feature.rewards.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.rewards.data.RewardsRepository
import com.twinhearts.app.feature.rewards.domain.CoinTx
import com.twinhearts.app.feature.rewards.domain.RewardsSummary
import com.twinhearts.app.feature.rewards.domain.ScratchResult
import com.twinhearts.app.feature.rewards.domain.SpinResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RewardsUiState(
    val summary: RewardsSummary? = null,
    val history: List<CoinTx>? = null,
    val loading: Boolean = true,
    val claiming: Boolean = false,
    val spinning: Boolean = false,
    val scratching: Boolean = false,
    val error: AppError? = null,
    /** Coins just won; the screen shows it once, then calls [RewardsViewModel.consumeEarned]. */
    val justEarned: Int = 0,
    val partnerName: String = "",
    val lastSpinResult: SpinResult? = null,
    val lastScratchResult: ScratchResult? = null,
)

/** One ViewModel for the Rewards page and the Home card. The server owns every number. */
@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val repository: RewardsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RewardsUiState())
    val state: StateFlow<RewardsUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch { _state.update { it.copy(partnerName = repository.partnerName()) } }
    }

    fun refresh(withHistory: Boolean = false) {
        viewModelScope.launch {
            when (val r = repository.summary()) {
                is AppResult.Success -> _state.update { it.copy(summary = r.data, loading = false, error = null) }
                // Keep an older result on screen if a refresh fails; only show the error when there is nothing to show.
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = if (it.summary == null) r.error else null) }
            }
            if (withHistory) loadHistory()
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            val r = repository.history()
            if (r is AppResult.Success) _state.update { it.copy(history = r.data) }
        }
    }

    fun claim() {
        val current = _state.value
        if (current.claiming || current.summary?.checkedInToday != false) return
        _state.update { it.copy(claiming = true) }
        viewModelScope.launch {
            when (val r = repository.checkIn()) {
                is AppResult.Success -> {
                    _state.update { it.copy(summary = r.data, claiming = false, error = null, justEarned = if (r.data.claimed) r.data.earned else 0) }
                    loadHistory()
                }
                is AppResult.Failure -> _state.update { it.copy(claiming = false, error = if (it.summary == null) r.error else it.error) }
            }
        }
    }

    fun spin(onSuccess: (SpinResult) -> Unit = {}) {
        val current = _state.value
        if (current.spinning || current.summary?.spunToday != false) return
        _state.update { it.copy(spinning = true) }
        viewModelScope.launch {
            val tapId = "spin_" + UUID.randomUUID().toString()
            when (val r = repository.spin(tapId)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            spinning = false,
                            lastSpinResult = r.data,
                            summary = it.summary?.copy(
                                balance = r.data.balance,
                                spunToday = true,
                                spinsAvailable = 0,
                            ),
                            justEarned = r.data.coins,
                        )
                    }
                    loadHistory()
                    onSuccess(r.data)
                }
                is AppResult.Failure -> _state.update { it.copy(spinning = false, error = r.error) }
            }
        }
    }

    fun scratch(onSuccess: (ScratchResult) -> Unit = {}) {
        val current = _state.value
        if (current.scratching || current.summary?.scratchedToday != false) return
        _state.update { it.copy(scratching = true) }
        viewModelScope.launch {
            val tapId = "scratch_" + UUID.randomUUID().toString()
            when (val r = repository.scratch(tapId)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            scratching = false,
                            lastScratchResult = r.data,
                            summary = it.summary?.copy(
                                balance = r.data.balance,
                                scratchedToday = true,
                                scratchesAvailable = 0,
                            ),
                            justEarned = r.data.coins,
                        )
                    }
                    loadHistory()
                    onSuccess(r.data)
                }
                is AppResult.Failure -> _state.update { it.copy(scratching = false, error = r.error) }
            }
        }
    }

    fun consumeEarned() = _state.update { it.copy(justEarned = 0) }
}
