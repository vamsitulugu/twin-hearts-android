package com.twinhearts.app.feature.chat.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.feature.chat.data.ChatRepository
import com.twinhearts.app.feature.chat.domain.MAX_SCHEDULE_MINUTES
import com.twinhearts.app.feature.chat.domain.MIN_SCHEDULE_MINUTES
import com.twinhearts.app.feature.chat.domain.mergeMessages
import com.twinhearts.app.feature.chat.domain.model.ChatIdentity
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.DeliveryState
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import com.twinhearts.app.feature.chat.domain.model.PartnerPresence
import com.twinhearts.app.feature.chat.domain.model.PresenceState
import com.twinhearts.app.feature.chat.domain.model.ScheduledMessage
import com.twinhearts.app.feature.chat.domain.model.SearchFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** A short note above the composer: green for news, red for problems. */
sealed interface ChatBanner {
    data class Info(@StringRes val id: Int) : ChatBanner
    data class Failure(val error: AppError) : ChatBanner
}

@Composable
fun ChatBanner.asText(): String = when (this) {
    is ChatBanner.Info -> stringResource(id)
    is ChatBanner.Failure -> error.asText()
}

data class SearchState(
    val results: List<ChatMessage> = emptyList(),
    val searching: Boolean = false,
    val failed: AppError? = null,
)

data class ChatUiState(
    val identity: ChatIdentity? = null,
    /** True until the first load answers. */
    val loading: Boolean = true,
    /** Set only when the first load failed and there is nothing to show. */
    val loadError: AppError? = null,
    val messages: List<ChatMessage> = emptyList(),
    val presence: PartnerPresence = PartnerPresence(PresenceState.Offline, null),
    val replyingTo: ChatMessage? = null,
    val banner: ChatBanner? = null,
    val search: SearchState = SearchState(),
    val scheduled: List<ScheduledMessage> = emptyList(),
    val scheduling: Boolean = false,
) {
    val starred: List<ChatMessage> get() = messages.filter { it.starred && !it.deleted }.sortedByDescending { it.createdAt }
}

/**
 * Logic of the Chat screen: loading, polling, sending (text, photo, voice), reply, star, search and
 * scheduled messages. A message I send shows at once with a clock and is replaced by the server copy
 * (matched by clientId); if sending fails it shows "Failed" with a retry.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    /** What to run again when the user taps "retry" on a failed message (by clientId). */
    private val retries = mutableMapOf<String, () -> Unit>()
    private var readInFlight = false
    private var searchJob: Job? = null

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            val who = repository.identity()
            if (who == null) {
                _state.update { it.copy(loading = false, loadError = AppError.Unauthorized()) }
                return@launch
            }
            _state.update { it.copy(identity = who) }
            when (val result = repository.recent(who)) {
                is AppResult.Success -> {
                    _state.update { it.copy(loading = false, messages = mergeKeepingLocal(it.messages, result.data)) }
                    markReadIfNeeded()
                }
                is AppResult.Failure -> _state.update { it.copy(loading = false, loadError = result.error) }
            }
            refreshPresence()
            refreshScheduled()
        }
    }

    /**
     * Called every few seconds while the chat is open. [full] re-reads the newest 200 messages so
     * ticks and stars of older messages update; otherwise only newer messages are asked for.
     */
    fun poll(full: Boolean) {
        val who = _state.value.identity ?: return
        viewModelScope.launch {
            val newest = _state.value.messages.filter { it.id != ChatMessage.LOCAL_ID }.maxOfOrNull { it.createdAt }
            val result = if (full || newest == null) repository.recent(who) else repository.since(who, newest)
            if (result is AppResult.Success && (result.data.isNotEmpty() || full)) {
                _state.update { it.copy(messages = mergeKeepingLocal(it.messages, result.data)) }
                markReadIfNeeded()
            }
        }
    }

    /** Heartbeat + my partner's "Online / Last seen". */
    fun heartbeat(online: Boolean) {
        val who = _state.value.identity ?: return
        viewModelScope.launch {
            repository.heartbeat(who, online)
            if (online) refreshPresence()
        }
    }

    private suspend fun refreshPresence() {
        val who = _state.value.identity ?: return
        (repository.partnerPresence(who) as? AppResult.Success)?.let { r -> _state.update { it.copy(presence = r.data) } }
    }

    private fun mergeKeepingLocal(current: List<ChatMessage>, incoming: List<ChatMessage>): List<ChatMessage> =
        mergeMessages(current, incoming)

    private fun markReadIfNeeded() {
        val who = _state.value.identity ?: return
        val unread = _state.value.messages.any { !it.mine && it.delivery != DeliveryState.Read }
        if (!unread || readInFlight) return
        readInFlight = true
        viewModelScope.launch {
            val ok = repository.markRead(who) is AppResult.Success
            readInFlight = false
            if (ok) {
                _state.update { s ->
                    s.copy(messages = s.messages.map { if (!it.mine) it.copy(delivery = DeliveryState.Read) else it })
                }
            }
        }
    }

    // ───────────── sending ─────────────

    fun sendText(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val local = localMessage(MessageKind.Text, text = clean, replyTo = replyId)
        send(local) { repository.sendText(who, local.clientId, clean, replyId) }
    }

    fun sendPhoto(uri: Uri) {
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val local = localMessage(MessageKind.Image, mediaUrl = uri.toString(), replyTo = replyId)
        send(local) { repository.sendPhoto(who, local.clientId, uri, replyId) }
    }

    fun sendVoice(file: File, durationSec: Int) {
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val local = localMessage(MessageKind.Voice, mediaUrl = file.toURI().toString(), durationSec = durationSec, replyTo = replyId)
        send(local) { repository.sendVoice(who, local.clientId, file, durationSec, replyId) }
    }

    fun retry(clientId: String) {
        retries[clientId]?.invoke()
    }

    private fun send(local: ChatMessage, action: suspend () -> AppResult<ChatMessage>) {
        _state.update { it.copy(messages = mergeMessages(it.messages, listOf(local)), banner = null) }
        val run: () -> Unit = {
            setDelivery(local.clientId, DeliveryState.Sending)
            viewModelScope.launch {
                when (val result = action()) {
                    is AppResult.Success -> {
                        retries.remove(local.clientId)
                        _state.update { it.copy(messages = mergeMessages(it.messages, listOf(result.data))) }
                    }
                    is AppResult.Failure -> {
                        setDelivery(local.clientId, DeliveryState.Failed)
                        _state.update { it.copy(banner = ChatBanner.Failure(result.error)) }
                    }
                }
            }
        }
        retries[local.clientId] = run
        run()
    }

    private fun setDelivery(clientId: String, delivery: DeliveryState) {
        _state.update { s ->
            s.copy(messages = s.messages.map { if (it.clientId == clientId) it.copy(delivery = delivery) else it })
        }
    }

    private fun localMessage(
        kind: MessageKind,
        text: String? = null,
        mediaUrl: String? = null,
        durationSec: Int = 0,
        replyTo: Long?,
    ) = ChatMessage(
        id = ChatMessage.LOCAL_ID,
        clientId = UUID.randomUUID().toString(),
        mine = true,
        kind = kind,
        text = text,
        mediaUrl = mediaUrl,
        durationSec = durationSec,
        replyToId = replyTo,
        createdAt = Instant.now(),
        delivery = DeliveryState.Sending,
    )

    // ───────────── reply, star ─────────────

    fun startReply(message: ChatMessage) = _state.update { it.copy(replyingTo = message) }
    fun cancelReply() = _state.update { it.copy(replyingTo = null) }

    private fun takeReplyId(): Long? {
        val id = _state.value.replyingTo?.id?.takeIf { it != ChatMessage.LOCAL_ID }
        _state.update { it.copy(replyingTo = null) }
        return id
    }

    /** The star flips at once; the server's answer is the final word. */
    fun toggleStar(message: ChatMessage) {
        val who = _state.value.identity ?: return
        if (message.id == ChatMessage.LOCAL_ID) return
        replaceMessage(message.copy(starred = !message.starred))
        viewModelScope.launch {
            when (val result = repository.toggleStar(who, message.id)) {
                is AppResult.Success -> replaceMessage(result.data)
                is AppResult.Failure -> {
                    replaceMessage(message)
                    _state.update { it.copy(banner = ChatBanner.Failure(result.error)) }
                }
            }
        }
    }

    private fun replaceMessage(message: ChatMessage) {
        _state.update { s -> s.copy(messages = s.messages.map { if (it.id == message.id) message else it }) }
    }

    // ───────────── search ─────────────

    fun search(query: String, filter: SearchFilter) {
        searchJob?.cancel()
        val who = _state.value.identity ?: return
        if (query.isBlank() && filter == SearchFilter.All) {
            _state.update { it.copy(search = SearchState()) }
            return
        }
        _state.update { it.copy(search = it.search.copy(searching = true, failed = null)) }
        searchJob = viewModelScope.launch {
            when (val result = repository.search(who, query.trim(), filter)) {
                is AppResult.Success -> _state.update { it.copy(search = SearchState(results = result.data)) }
                is AppResult.Failure -> _state.update { it.copy(search = SearchState(failed = result.error)) }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.update { it.copy(search = SearchState()) }
    }

    // ───────────── scheduled messages ─────────────

    fun refreshScheduled() {
        val who = _state.value.identity ?: return
        viewModelScope.launch {
            (repository.scheduled(who) as? AppResult.Success)?.let { r -> _state.update { it.copy(scheduled = r.data) } }
        }
    }

    /** Sends [text] by itself after [minutes] (1 minute to 1 year, like the web). */
    fun schedule(text: String, minutes: Int) {
        val who = _state.value.identity ?: return
        val clean = text.trim()
        if (clean.isEmpty() || _state.value.scheduling) return
        val at = Instant.now().plusSeconds(minutes.coerceIn(MIN_SCHEDULE_MINUTES, MAX_SCHEDULE_MINUTES) * SECONDS_PER_MINUTE)
        _state.update { it.copy(scheduling = true) }
        viewModelScope.launch {
            when (val result = repository.schedule(who, UUID.randomUUID().toString(), clean, at)) {
                is AppResult.Success -> {
                    _state.update { it.copy(scheduling = false, banner = ChatBanner.Info(R.string.chat_info_scheduled)) }
                    refreshScheduled()
                }
                is AppResult.Failure -> _state.update { it.copy(scheduling = false, banner = ChatBanner.Failure(result.error)) }
            }
        }
    }

    fun cancelScheduled(id: String) {
        _state.update { s -> s.copy(scheduled = s.scheduled.filterNot { it.id == id }) }
        viewModelScope.launch {
            if (repository.cancelScheduled(id) is AppResult.Failure) refreshScheduled()
        }
    }

    fun showBanner(banner: ChatBanner?) = _state.update { it.copy(banner = banner) }

    private companion object {
        const val SECONDS_PER_MINUTE = 60L
    }
}
