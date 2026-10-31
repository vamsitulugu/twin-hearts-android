package com.twinhearts.app.feature.pro

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.pro.PaymentBridge
import com.twinhearts.app.core.pro.PaymentResult
import com.twinhearts.app.core.pro.ProGate
import com.twinhearts.app.core.pro.ProOrder
import com.twinhearts.app.core.pro.ProPayment
import com.twinhearts.app.core.pro.ProPlan
import com.twinhearts.app.core.pro.ProRepository
import com.twinhearts.app.core.pro.ProStatus
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

/** A line under the plans: what just happened, in words the person reads. */
sealed interface ProNotice {
    data class Res(@StringRes val id: Int) : ProNotice
    data class Text(val text: String) : ProNotice
    data class Error(val error: AppError) : ProNotice
}

data class ProUiState(
    val loading: Boolean = true,
    val status: ProStatus = ProStatus.Free,
    val selected: ProPlan = ProPlan.Yearly,
    /** Creating the order or inside Razorpay's window. */
    val paying: Boolean = false,
    /** Razorpay said paid; the server is checking the signature. */
    val verifying: Boolean = false,
    val history: List<ProPayment> = emptyList(),
    val notice: ProNotice? = null,
) {
    val busy: Boolean get() = paying || verifying
    val hasLifetime: Boolean get() = status.isActive() && status.plan == ProPlan.Lifetime
}

sealed interface ProEffect {
    data class LaunchCheckout(val order: ProOrder) : ProEffect
    data object Purchased : ProEffect
}

@HiltViewModel
class ProViewModel @Inject constructor(
    private val repo: ProRepository,
    private val gate: ProGate,
    private val bridge: PaymentBridge,
) : ViewModel() {

    private val _state = MutableStateFlow(ProUiState(status = gate.status.value))
    val state: StateFlow<ProUiState> = _state.asStateFlow()

    private val _effects = Channel<ProEffect>(Channel.BUFFERED)
    val effects: Flow<ProEffect> = _effects.receiveAsFlow()

    init {
        refresh()
        viewModelScope.launch { bridge.results.collect(::onPaymentResult) }
    }

    fun select(plan: ProPlan) = _state.update { it.copy(selected = plan, notice = null) }

    /** Reads Pro from the server (also what "Refresh status" does after paying on another device). */
    fun refresh(keepNotice: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, notice = if (keepNotice) it.notice else null) }
            when (val r = gate.refresh()) {
                is AppResult.Success -> _state.update { it.copy(loading = false, status = r.data) }
                is AppResult.Failure -> _state.update {
                    it.copy(loading = false, notice = if (keepNotice) it.notice else ProNotice.Error(r.error))
                }
            }
            loadHistory()
        }
    }

    private suspend fun loadHistory() {
        val r = repo.history()
        if (r is AppResult.Success) _state.update { it.copy(history = r.data) }
    }

    fun pay() {
        val s = _state.value
        if (s.busy || s.hasLifetime) return
        _state.update { it.copy(paying = true, notice = null) }
        viewModelScope.launch {
            when (val r = repo.createOrder(s.selected)) {
                is AppResult.Success -> _effects.send(ProEffect.LaunchCheckout(r.data))
                is AppResult.Failure -> _state.update { it.copy(paying = false, notice = ProNotice.Error(r.error)) }
            }
        }
    }

    /** The screen could not open Razorpay's window. */
    fun checkoutFailedToOpen() =
        _state.update { it.copy(paying = false, notice = ProNotice.Res(R.string.pro_err_open)) }

    private fun onPaymentResult(result: PaymentResult) {
        when (result) {
            PaymentResult.Cancelled -> _state.update { it.copy(paying = false) }
            is PaymentResult.Failed -> _state.update { it.copy(paying = false, notice = ProNotice.Text(result.message)) }
            is PaymentResult.Success -> viewModelScope.launch {
                _state.update { it.copy(verifying = true, notice = null) }
                when (val r = repo.verify(result.orderId, result.paymentId, result.signature)) {
                    is AppResult.Success -> {
                        gate.onStatus(r.data)
                        _state.update { it.copy(paying = false, verifying = false, status = r.data) }
                        loadHistory()
                        _effects.send(ProEffect.Purchased)
                    }
                    is AppResult.Failure -> {
                        // Money was taken but we could not confirm it. The server also hears from Razorpay
                        // directly (webhook), so check again instead of telling the person it failed.
                        _state.update {
                            it.copy(paying = false, verifying = false, notice = ProNotice.Res(R.string.pro_err_verify))
                        }
                        refresh(keepNotice = true)
                    }
                }
            }
        }
    }
}
