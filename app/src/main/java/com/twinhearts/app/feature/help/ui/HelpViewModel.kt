package com.twinhearts.app.feature.help.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.help.data.HelpRepository
import com.twinhearts.app.feature.help.domain.PENDING_ID_PREFIX
import com.twinhearts.app.feature.help.domain.POLL_INTERVAL_MS
import com.twinhearts.app.feature.help.domain.isPending
import com.twinhearts.app.feature.help.domain.mergeSendResult
import com.twinhearts.app.feature.help.domain.model.HelpCategory
import com.twinhearts.app.feature.help.domain.model.SenderKind
import com.twinhearts.app.feature.help.domain.model.SupportMessage
import com.twinhearts.app.feature.help.domain.model.SupportMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject

/**
 * Help & Customer Care: the question list (search, topics, open/close) and the support chat
 * (Twin Care AI or a live agent). The tab, the search text and the topic survive process death
 * through [SavedStateHandle]; the conversation itself lives on the server.
 */
@HiltViewModel
class HelpViewModel @Inject constructor(
    private val repository: HelpRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        HelpUiState(
            tab = savedState.get<String>(KEY_TAB)
                ?.let { name -> HelpTab.entries.firstOrNull { it.name == name } }
                ?: HelpTab.Faq,
            query = savedState.get<String>(KEY_QUERY).orEmpty(),
            category = savedState.get<String>(KEY_CATEGORY)
                ?.let { name -> HelpCategory.entries.firstOrNull { it.name == name } }
                ?: HelpCategory.All,
        ),
    )
    val state: StateFlow<HelpUiState> = _state.asStateFlow()

    private val _events = Channel<HelpEvent>(Channel.BUFFERED)
    val events: Flow<HelpEvent> = _events.receiveAsFlow()

    // Connecting, switching mode and sending all read and change the same conversation, so they
    // run one after another. A quick chip tapped while the chat is still connecting simply waits.
    private val chatLock = Mutex()

    init {
        loadFaqs()
        viewModelScope.launch {
            repository.myName.collect { name -> _state.update { it.copy(myName = name) } }
        }
        if (_state.value.tab == HelpTab.Chat) connectInBackground()
    }

    // ── Questions ───────────────────────────────────────────────

    fun loadFaqs() {
        viewModelScope.launch {
            _state.update { it.copy(faqs = UiState.Loading) }
            when (val result = repository.faqs()) {
                is AppResult.Success -> _state.update {
                    it.copy(faqs = if (result.data.isEmpty()) UiState.Empty else UiState.Content(result.data))
                }
                is AppResult.Failure -> _state.update { it.copy(faqs = UiState.Error(result.error)) }
            }
        }
    }

    fun onQueryChange(query: String) {
        savedState[KEY_QUERY] = query
        _state.update { it.copy(query = query) }
    }

    fun onCategory(category: HelpCategory) {
        savedState[KEY_CATEGORY] = category.name
        _state.update { it.copy(category = category) }
    }

    fun toggleFaq(id: String) {
        _state.update { s ->
            s.copy(expandedIds = if (id in s.expandedIds) s.expandedIds - id else s.expandedIds + id)
        }
    }

    // ── Tabs and chat mode ──────────────────────────────────────

    fun selectTab(tab: HelpTab) {
        savedState[KEY_TAB] = tab.name
        _state.update { it.copy(tab = tab) }
        if (tab == HelpTab.Chat) connectInBackground()
    }

    /** Opens the chat for a given helper, and sends [firstMessage] when there is one. */
    fun startChat(mode: SupportMode, firstMessage: String? = null) {
        savedState[KEY_TAB] = HelpTab.Chat.name
        _state.update { it.copy(tab = HelpTab.Chat, chat = it.chat.copy(mode = mode)) }
        viewModelScope.launch {
            chatLock.withLock {
                if (!connectLocked(mode)) {
                    if (firstMessage != null) _events.trySend(HelpEvent.ConnectFailed)
                    return@withLock
                }
                // A conversation that already exists keeps its old mode until it is switched.
                if (_state.value.chat.ticketMode != mode && !switchLocked(mode)) return@withLock
                if (firstMessage != null) sendLocked(firstMessage, restoreDraft = false)
            }
        }
    }

    /** The AI / Live Agent toggle. The toggle moves at once; the server is told afterwards. */
    fun selectMode(mode: SupportMode) {
        if (_state.value.chat.mode == mode) return
        _state.update { it.copy(chat = it.chat.copy(mode = mode)) }
        viewModelScope.launch {
            chatLock.withLock {
                val chat = _state.value.chat
                if (chat.ticketId != null && chat.ticketMode != mode) switchLocked(mode)
            }
        }
    }

    fun retryConnect() = connectInBackground()

    // ── Messages ────────────────────────────────────────────────

    fun onDraftChange(text: String) {
        _state.update { it.copy(chat = it.chat.copy(draft = text)) }
    }

    /** The send button: sends what is typed. A failed send puts the text back in the box. */
    fun send() {
        val text = _state.value.chat.draft.trim()
        if (text.isEmpty()) return
        _state.update { it.copy(chat = it.chat.copy(draft = "")) }
        launchSend(text, restoreDraft = true)
    }

    /** A quick chip or "Ask AI" button: sends the given sentence at once. */
    fun sendQuick(text: String) = launchSend(text, restoreDraft = false)

    private fun launchSend(text: String, restoreDraft: Boolean) {
        viewModelScope.launch { chatLock.withLock { sendLocked(text, restoreDraft) } }
    }

    // ── Live replies ────────────────────────────────────────────

    /**
     * Asks the server for new replies every few seconds while it is running. The screen runs this
     * only while the chat tab is open and the app is on screen, and cancels it otherwise (R-66).
     */
    suspend fun pollWhileVisible() {
        while (true) {
            delay(POLL_INTERVAL_MS)
            pollOnce()
        }
    }

    /** One check for new messages. Skipped while a send or a switch is running. */
    internal suspend fun pollOnce() {
        if (!chatLock.tryLock()) return
        try {
            val id = _state.value.chat.ticketId ?: return
            val result = repository.refresh(id)
            if (result is AppResult.Success) {
                val known = _state.value.chat.messages.count { !it.isPending }
                if (result.data.messages.size > known) {
                    _state.update { it.copy(chat = it.chat.copy(messages = result.data.messages)) }
                }
            }
        } finally {
            chatLock.unlock()
        }
    }

    // ── Work done while holding chatLock ────────────────────────

    private fun connectInBackground() {
        if (_state.value.chat.ticketId != null) return
        viewModelScope.launch { chatLock.withLock { connectLocked(_state.value.chat.mode) } }
    }

    /** Makes sure there is a conversation. Returns false (and shows the error state) when not. */
    private suspend fun connectLocked(createMode: SupportMode): Boolean {
        if (_state.value.chat.ticketId != null) return true
        _state.update { it.copy(chat = it.chat.copy(link = ChatLink.Connecting)) }
        return when (val result = repository.openTicket(createMode)) {
            is AppResult.Success -> {
                val ticket = result.data
                _state.update {
                    it.copy(
                        chat = it.chat.copy(
                            link = ChatLink.Connected,
                            ticketId = ticket.id,
                            mode = ticket.mode,
                            ticketMode = ticket.mode,
                            messages = ticket.messages,
                        ),
                    )
                }
                true
            }
            is AppResult.Failure -> {
                _state.update { it.copy(chat = it.chat.copy(link = ChatLink.Failed(result.error))) }
                false
            }
        }
    }

    /** Tells the server to change helper. On failure the toggle goes back and a toast shows why. */
    private suspend fun switchLocked(mode: SupportMode): Boolean {
        val id = _state.value.chat.ticketId ?: return false
        return when (val result = repository.switchMode(id, mode)) {
            is AppResult.Success -> {
                val ticket = result.data
                _state.update {
                    it.copy(chat = it.chat.copy(mode = ticket.mode, ticketMode = ticket.mode, messages = ticket.messages))
                }
                true
            }
            is AppResult.Failure -> {
                _state.update { it.copy(chat = it.chat.copy(mode = it.chat.ticketMode ?: it.chat.mode)) }
                _events.trySend(HelpEvent.Failed(result.error))
                false
            }
        }
    }

    private suspend fun sendLocked(text: String, restoreDraft: Boolean) {
        if (!connectLocked(_state.value.chat.mode)) {
            _events.trySend(HelpEvent.ConnectFailed)
            if (restoreDraft) restoreDraftText(text)
            return
        }
        val chat = _state.value.chat
        val ticketId = chat.ticketId ?: return
        val mode = chat.mode

        // My message shows at once (like the web) and is swapped for the server's copy afterwards.
        val pending = SupportMessage(
            id = PENDING_ID_PREFIX + System.nanoTime(),
            sender = SenderKind.User,
            senderName = _state.value.myName,
            text = text,
            createdAt = Instant.now(),
        )
        _state.update { it.copy(chat = it.chat.copy(messages = it.chat.messages + pending, sending = true)) }

        when (val result = repository.send(ticketId, text, mode)) {
            is AppResult.Success -> {
                _state.update {
                    it.copy(
                        chat = it.chat.copy(
                            messages = mergeSendResult(it.chat.messages, result.data),
                            sending = false,
                        ),
                    )
                }
                if (mode == SupportMode.Human) {
                    _events.trySend(HelpEvent.MessageSent)
                } else if (result.data.aiMessage == null) {
                    _events.trySend(HelpEvent.AiUnavailable)
                }
            }
            is AppResult.Failure -> {
                _state.update {
                    it.copy(
                        chat = it.chat.copy(
                            messages = it.chat.messages.filterNot { message -> message.id == pending.id },
                            sending = false,
                        ),
                    )
                }
                _events.trySend(HelpEvent.SendFailed)
                if (restoreDraft) restoreDraftText(text)
            }
        }
    }

    /** Puts a message that was not sent back in the box, unless I already started typing again. */
    private fun restoreDraftText(text: String) {
        _state.update { s ->
            if (s.chat.draft.isBlank()) s.copy(chat = s.chat.copy(draft = text)) else s
        }
    }

    private companion object {
        const val KEY_TAB = "help_tab"
        const val KEY_QUERY = "help_query"
        const val KEY_CATEGORY = "help_category"
    }
}
