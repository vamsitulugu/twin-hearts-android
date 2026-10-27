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
import com.twinhearts.app.feature.twinai.domain.model.AiStreamEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
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
    val messages: List<AiMessage> = emptyList(),
    val input: String = "",
    val streaming: Boolean = false,
    val quickOpen: Boolean = false,
    val syncing: Boolean = false,
    /** Full-screen voice mode (the mic button). */
    val voiceOpen: Boolean = false,
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val menuOpen: Boolean = false,
) {
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
    }

    /** Loads the name for the greeting and checks whether BOTH is switched on. Also the Sync button. */
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

    fun selectTab(tab: TwinTab) = _state.update { it.copy(tab = tab, quickOpen = false) }

    fun onInputChange(text: String) = _state.update { it.copy(input = text) }

    fun toggleQuick() = _state.update { it.copy(quickOpen = !it.quickOpen) }

    fun closeQuick() = _state.update { it.copy(quickOpen = false) }

    fun toggleSearch() = _state.update { it.copy(searchOpen = !it.searchOpen, searchQuery = "", menuOpen = false) }

    fun onSearchChange(text: String) = _state.update { it.copy(searchQuery = text) }

    fun setMenu(open: Boolean) = _state.update { it.copy(menuOpen = open) }

    /** Menu > New chat: stops any answer in progress and clears the conversation. */
    fun newChat() {
        replyJob?.cancel()
        _state.update { it.copy(messages = emptyList(), streaming = false, menuOpen = false, searchOpen = false, searchQuery = "", quickOpen = false) }
    }

    fun openVoice() = _state.update { it.copy(voiceOpen = true, quickOpen = false) }

    fun closeVoice() = _state.update { it.copy(voiceOpen = false) }

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
