package com.twinhearts.app.feature.twinai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.twinai.data.TwinAiRepository
import com.twinhearts.app.feature.twinai.domain.model.AiAuthor
import com.twinhearts.app.feature.twinai.domain.model.AiIdentity
import com.twinhearts.app.feature.twinai.domain.model.AiMessage
import com.twinhearts.app.feature.twinai.domain.model.AiMessageState
import com.twinhearts.app.R
import com.twinhearts.app.feature.twinai.domain.model.AiStreamEvent
import com.twinhearts.app.feature.twinai.domain.model.OrbState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** Which of the two tabs is open. */
enum class TwinTab { You, Both }

/** Everything the Twin page needs to draw the YOU tab and the tab switch. */
data class TwinAiUiState(
    val greetingName: String = "",
    val tab: TwinTab = TwinTab.You,
    /** True while the "BOTH is a Pro feature" card shows (the web opens its Pro card the same way). */
    val proPrompt: Boolean = false,
    val checkingPro: Boolean = false,
    val messages: List<AiMessage> = emptyList(),
    val input: String = "",
    val streaming: Boolean = false,
    val quickOpen: Boolean = false,
    val syncing: Boolean = false,
    /** True while the phone has no internet; the orb turns grey. */
    val offline: Boolean = false,
) {
    /** What the Twin orb is doing; the dock orb and the voice screen both draw it. */
    val orb: OrbState
        get() {
            val last = messages.lastOrNull { it.author == AiAuthor.Twin }
            return when {
                offline -> OrbState.Offline
                streaming -> OrbState.Thinking
                last?.state == AiMessageState.Failed -> OrbState.Error
                last != null -> OrbState.Responding
                else -> OrbState.Idle
            }
        }

    /** True while the welcome chips show (no question asked yet). */
    val isWelcome: Boolean get() = messages.isEmpty()
}

/**
 * Why this file exists: runs the YOU conversation with Twin. Messages live only in memory, the
 * same as the web page (a fresh page is a fresh chat); they survive rotation because the
 * ViewModel does.
 */
@HiltViewModel
class TwinAiViewModel @Inject constructor(
    private val repository: TwinAiRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TwinAiUiState())
    val state: StateFlow<TwinAiUiState> = _state.asStateFlow()

    private var identity: AiIdentity? = null
    private var replyJob: Job? = null
    private var nextId = 0L

    init {
        refresh()
        viewModelScope.launch {
            repository.online.collect { isOnline -> _state.update { it.copy(offline = !isOnline) } }
        }
    }

    /** Loads the name for the greeting. Also the Sync button. */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(syncing = true) }
            val who = repository.identity()
            identity = who
            _state.update {
                it.copy(
                    greetingName = who?.myName?.trim()?.substringBefore(' ').orEmpty(),
                    syncing = false,
                )
            }
        }
    }

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    /** One-off texts (string ids) the screen shows as a short toast. */
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    /**
     * YOU is always open. BOTH is a Pro feature on the web, so the same check runs here: a couple
     * without Pro stays on YOU and sees the Pro card instead.
     */
    fun selectTab(tab: TwinTab) {
        if (tab == _state.value.tab || _state.value.checkingPro) return
        if (tab == TwinTab.You) {
            _state.update { it.copy(tab = TwinTab.You, quickOpen = false) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(checkingPro = true) }
            val who = identity ?: repository.identity().also { identity = it }
            val pro = who?.let { repository.isPro(it) }
            _state.update { it.copy(checkingPro = false) }
            when (pro) {
                is AppResult.Success ->
                    if (pro.data) _state.update { it.copy(tab = TwinTab.Both, quickOpen = false) }
                    else _state.update { it.copy(proPrompt = true) }
                is AppResult.Failure -> _messages.tryEmit(R.string.twinai_pro_check_failed)
                null -> _messages.tryEmit(R.string.twinai_no_session)
            }
        }
    }

    fun dismissProPrompt() = _state.update { it.copy(proPrompt = false) }

    /** Back on the BOTH home goes to YOU first (same as tapping the YOU pill on the web). */
    fun backToYou() = _state.update { it.copy(tab = TwinTab.You) }

    fun onInputChange(text: String) = _state.update { it.copy(input = text) }

    fun toggleQuick() = _state.update { it.copy(quickOpen = !it.quickOpen) }

    fun closeQuick() = _state.update { it.copy(quickOpen = false) }

    fun sendInput() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.streaming) return
        _state.update { it.copy(input = "") }
        ask(text)
    }

    /** Sends [question] (typed, a chip, or spoken) and streams Twin's answer into a new bubble. */
    fun ask(question: String, regenerate: Boolean = false) {
        if (_state.value.streaming || question.isBlank()) return
        val now = Instant.now()
        val twinId = newId()
        val mine = if (regenerate) null else AiMessage(newId(), AiAuthor.Me, question, now)
        val twin = AiMessage(twinId, AiAuthor.Twin, "", now, AiMessageState.Streaming, question)
        _state.update {
            it.copy(
                messages = (it.messages + listOfNotNull(mine, twin)).takeLast(MAX_MESSAGES),
                streaming = true,
                quickOpen = false,
            )
        }
        replyJob = viewModelScope.launch {
            val who = identity ?: repository.identity().also { identity = it }
            if (who == null) {
                finish(twinId, AiMessageState.Failed, AppError.Unauthorized())
                return@launch
            }
            repository.reply(who, question).collect { event ->
                when (event) {
                    is AiStreamEvent.Delta -> patch(twinId) { it.copy(text = it.text + event.text) }
                    AiStreamEvent.Done -> finish(twinId, AiMessageState.Done, null)
                    is AiStreamEvent.Failure -> finish(twinId, AiMessageState.Failed, event.error)
                }
            }
            // A stream that simply ends with no event must not leave the bubble spinning.
            patch(twinId) { if (it.state == AiMessageState.Streaming) it.copy(state = AiMessageState.Done) else it }
            _state.update { it.copy(streaming = false) }
        }
    }

    /** Regenerate / Retry: asks the same question again in a new bubble. */
    fun askAgain(message: AiMessage) {
        message.question?.let { ask(it, regenerate = true) }
    }

    private fun finish(id: String, state: AiMessageState, error: AppError?) {
        patch(id) { it.copy(state = state, error = error) }
        _state.update { it.copy(streaming = false) }
    }

    private fun patch(id: String, change: (AiMessage) -> AiMessage) {
        _state.update { s -> s.copy(messages = s.messages.map { if (it.id == id) change(it) else it }) }
    }

    private fun newId() = "ai-${nextId++}"

    private companion object {
        /** Same cap as the web page, so a very long chat never grows without end. */
        const val MAX_MESSAGES = 120
    }
}
