package com.twinhearts.app.feature.twinai.ui.pro

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.twinai.data.TwinAiRepository
import com.twinhearts.app.feature.twinai.domain.model.AiIdentity
import com.twinhearts.app.feature.twinai.domain.model.ProPlan
import com.twinhearts.app.feature.twinai.domain.model.ProUpgradeForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProStep { Hidden, Plans, Pay, Form, Success }

data class ProGateState(
    val step: ProStep = ProStep.Hidden,
    val checking: Boolean = false,
    val plan: ProPlan = ProPlan.Monthly,
    val form: ProUpgradeForm = ProUpgradeForm(ProPlan.Monthly),
    val proofUri: String? = null,
    val submitting: Boolean = false,
    val coupleId: String = "",
)

/**
 * Why this file exists: the "Go Pro" gate for BOTH mode (same steps as the web's pro-card.js:
 * plans, pay by UPI, send proof, wait for the admin). Pro is per couple and cached once confirmed.
 */
@HiltViewModel
class ProGateViewModel @Inject constructor(
    private val repository: TwinAiRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProGateState())
    val state: StateFlow<ProGateState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    private var isPro = false
    private var identity: AiIdentity? = null

    /** Runs [onOk] when this couple has Pro; otherwise opens the Go Pro sheet. */
    fun requireBoth(onOk: () -> Unit) {
        if (isPro) {
            onOk()
            return
        }
        if (_state.value.checking) return
        viewModelScope.launch {
            _state.update { it.copy(checking = true) }
            val who = identity ?: repository.identity().also { identity = it }
            val result = who?.let { repository.isPro(it) }
            _state.update { it.copy(checking = false, coupleId = who?.coupleId.orEmpty()) }
            when (result) {
                is AppResult.Success -> if (result.data) {
                    isPro = true
                    onOk()
                } else {
                    _state.update { it.copy(step = ProStep.Plans) }
                }
                else -> say(R.string.twinai_pro_check_failed)
            }
        }
    }

    /** The Sync button: forget the cached answer so a freshly approved Pro is noticed. */
    fun forgetStatus() {
        isPro = false
    }

    fun dismiss() = _state.update { it.copy(step = ProStep.Hidden) }

    fun selectPlan(plan: ProPlan) = _state.update { it.copy(plan = plan, form = it.form.copy(plan = plan)) }

    fun goToPay() = _state.update { it.copy(step = ProStep.Pay) }

    fun paid() = _state.update { it.copy(step = ProStep.Form) }

    fun back() = _state.update {
        it.copy(step = when (it.step) { ProStep.Form -> ProStep.Pay; ProStep.Pay -> ProStep.Plans; else -> ProStep.Hidden })
    }

    fun onForm(change: (ProUpgradeForm) -> ProUpgradeForm) = _state.update { it.copy(form = change(it.form)) }

    fun onProof(uri: String?) = _state.update { it.copy(proofUri = uri) }

    fun submit() {
        val current = _state.value
        if (current.submitting) return
        if (!current.form.isComplete || current.proofUri == null) {
            say(R.string.twinai_pro_form_incomplete)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            val who = identity ?: repository.identity().also { identity = it }
            val result = who?.let { repository.submitUpgrade(it, current.form, current.proofUri) }
            _state.update { it.copy(submitting = false) }
            when (result) {
                is AppResult.Success -> _state.update { it.copy(step = ProStep.Success) }
                else -> say(R.string.twinai_pro_submit_failed)
            }
        }
    }

    private fun say(@StringRes id: Int) {
        _messages.tryEmit(id)
    }
}
