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
import com.twinhearts.app.feature.chat.data.ChatFiles
import com.twinhearts.app.feature.chat.data.ChatLocalStore
import com.twinhearts.app.feature.chat.data.ChatRepository
import com.twinhearts.app.feature.chat.data.PhotoPick
import com.twinhearts.app.feature.chat.domain.MAX_SCHEDULE_MINUTES
import com.twinhearts.app.feature.chat.domain.MIN_SCHEDULE_MINUTES
import com.twinhearts.app.feature.chat.domain.CropRect
import com.twinhearts.app.feature.chat.domain.mergeMessages
import com.twinhearts.app.feature.chat.domain.voteText
import com.twinhearts.app.feature.chat.domain.model.ChatIdentity
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.ChatSettings
import com.twinhearts.app.feature.chat.domain.model.DeliveryState
import com.twinhearts.app.feature.chat.domain.model.GifItem
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import com.twinhearts.app.feature.chat.domain.model.PartnerPresence
import com.twinhearts.app.feature.chat.domain.model.PresenceState
import com.twinhearts.app.feature.chat.domain.model.ScheduledMessage
import com.twinhearts.app.feature.chat.domain.model.SearchFilter
import com.twinhearts.app.feature.chat.domain.model.SendEffect
import com.twinhearts.app.feature.chat.domain.model.ViewOnceResult
import com.twinhearts.app.feature.chat.domain.model.Wallpaper
import com.twinhearts.app.feature.chat.domain.model.WallpaperMode
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

/*
 * Why this file exists: state and logic of the Chat screen.
 */

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

/** The GIF picker: results, loading, or an error (a missing Giphy key shows as [needsKey]). */
data class GifState(
    val items: List<GifItem> = emptyList(),
    val loading: Boolean = false,
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
    val editing: ChatMessage? = null,
    val banner: ChatBanner? = null,
    val search: SearchState = SearchState(),
    val scheduled: List<ScheduledMessage> = emptyList(),
    val scheduling: Boolean = false,
    /** clientIds picked in select mode; empty = not selecting. */
    val selected: Set<String> = emptySet(),
    val settings: ChatSettings = ChatSettings(),
    val recentEmoji: List<String> = emptyList(),
    val disappearingSeconds: Int = 0,
    val wallpaper: Wallpaper = Wallpaper(),
    val wallpaperShared: Boolean = false,
    val wallpaperSaving: Boolean = false,
    val gifs: GifState = GifState(),
    /** A view-once item that is open on screen right now. */
    val viewOnce: ViewOnceResult? = null,
    /** The animation that is playing now (confetti, hearts, balloons). */
    val effect: SendEffect? = null,
    /** The exported chat text waiting to be shared. */
    val exportText: String? = null,
    /** The saved unsent text of this chat (the composer reads it once). */
    val draft: String = "",
) {
    val starred: List<ChatMessage> get() = messages.filter { it.starred && !it.deleted }.sortedByDescending { it.createdAt }
    val pinned: List<ChatMessage> get() = messages.filter { it.pinned && !it.deleted }
    val media: List<ChatMessage>
        get() = messages.filter {
            !it.deleted && it.mediaUrl != null &&
                (it.kind == MessageKind.Image || it.kind == MessageKind.ImageGroup || it.kind == MessageKind.Video || it.kind == MessageKind.Gif)
        }
    val selectedMessages: List<ChatMessage> get() = messages.filter { it.clientId in selected }
}

/**
 * Logic of the Chat screen. A message I send shows at once with a clock and is replaced by the
 * server copy (matched by clientId); if sending fails it shows "Failed" with a retry. Everything the
 * web chat can do is here: media, polls, reactions, pin, star, edit, delete, forward, wallpaper,
 * disappearing messages, export, search and scheduled messages.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val local: ChatLocalStore,
    private val files: ChatFiles,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    /** What to run again when the user taps "retry" on a failed message (by clientId). */
    private val retries = mutableMapOf<String, () -> Unit>()

    /** Messages I deleted "for me": the server will not send them again, so a poll must not bring them back. */
    private val hidden = mutableSetOf<String>()
    private var readInFlight = false
    private var searchJob: Job? = null
    private var gifJob: Job? = null
    private var serverWallpaper = Wallpaper()
    private var localWallpaper = Wallpaper()

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
            _state.update { it.copy(identity = who, draft = "") }
            observeLocalSettings(who)
            when (val result = repository.recent(who)) {
                is AppResult.Success -> {
                    _state.update { it.copy(loading = false, messages = incoming(it.messages, result.data, announce = false)) }
                    markReadIfNeeded()
                }
                is AppResult.Failure -> _state.update { it.copy(loading = false, loadError = result.error) }
            }
            refreshPresence()
            refreshScheduled()
            refreshServerSettings(who)
            val draft = local.draft(who.coupleId)
            _state.update { it.copy(draft = draft) }
        }
    }

    private var observing = false

    private fun observeLocalSettings(who: ChatIdentity) {
        if (observing) return
        observing = true
        viewModelScope.launch { local.settings(who.coupleId).collect { s -> _state.update { it.copy(settings = s) } } }
        viewModelScope.launch { local.recentEmoji().collect { e -> _state.update { it.copy(recentEmoji = e) } } }
        viewModelScope.launch { local.wallpaperShared(who.coupleId).collect { v -> _state.update { it.copy(wallpaperShared = v) } } }
        viewModelScope.launch {
            local.localWallpaper(who.coupleId).collect { w ->
                localWallpaper = w
                publishWallpaper()
            }
        }
    }

    private suspend fun refreshServerSettings(who: ChatIdentity) {
        (repository.disappearingSeconds(who) as? AppResult.Success)?.let { r -> _state.update { it.copy(disappearingSeconds = r.data) } }
        (repository.sharedWallpaper(who) as? AppResult.Success)?.let { r ->
            serverWallpaper = r.data
            publishWallpaper()
        }
    }

    /** A wallpaper only I set wins; otherwise the shared one (set by either of us) shows. */
    private fun publishWallpaper() {
        val effective = if (localWallpaper.mode != WallpaperMode.Default) localWallpaper else serverWallpaper
        _state.update { it.copy(wallpaper = effective) }
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
                _state.update { it.copy(messages = incoming(it.messages, result.data, announce = true)) }
                markReadIfNeeded()
            }
        }
    }

    /** Merges server messages, drops the ones I hid, and plays my partner's new "send effect". */
    private fun incoming(current: List<ChatMessage>, fresh: List<ChatMessage>, announce: Boolean): List<ChatMessage> {
        val known = current.map { it.clientId }.toSet()
        if (announce) {
            fresh.firstOrNull { !it.mine && it.clientId !in known && SendEffect.fromApi(it.effect) != null }
                ?.let { m -> _state.update { it.copy(effect = SendEffect.fromApi(m.effect)) } }
        }
        return mergeMessages(current, fresh).filter { it.clientId !in hidden }
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

    /** Sends [text]; while editing, saves the change instead. [effect] is the long-press animation. */
    fun sendText(text: String, effect: SendEffect? = null) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val editing = _state.value.editing
        if (editing != null) {
            submitEdit(editing, clean)
            return
        }
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val local = localMessage(MessageKind.Text, text = clean, replyTo = replyId, effect = effect?.apiValue)
        send(local) { repository.sendText(who, local.clientId, clean, replyId, effect?.apiValue) }
        if (effect != null) _state.update { it.copy(effect = effect) }
    }

    /** One or more photos as one message, with an optional caption. */
    fun sendPhotos(picks: List<PhotoPick>, caption: String, viewOnce: Boolean = false) {
        if (picks.isEmpty()) return
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val kind = if (picks.size == 1) MessageKind.Image else MessageKind.ImageGroup
        val local = localMessage(
            kind, text = caption.trim().ifEmpty { null }, mediaUrl = picks.first().uri.toString(),
            groupUrls = if (picks.size > 1) picks.map { it.uri.toString() } else emptyList(), replyTo = replyId,
        )
        send(local) { repository.sendPhotos(who, local.clientId, picks, caption.trim(), replyId, viewOnce && picks.size == 1) }
    }

    fun sendVideo(uri: Uri) {
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val local = localMessage(MessageKind.Video, mediaUrl = uri.toString(), replyTo = replyId)
        send(local) { repository.sendVideo(who, local.clientId, uri, replyId) }
    }

    fun sendFile(uri: Uri) {
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val local = localMessage(MessageKind.File, mediaUrl = uri.toString(), replyTo = replyId)
        send(local) { repository.sendFile(who, local.clientId, uri, replyId) }
    }

    fun sendAudioFile(uri: Uri) {
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val local = localMessage(MessageKind.Audio, mediaUrl = uri.toString(), replyTo = replyId)
        send(local) { repository.sendAudioFile(who, local.clientId, uri, replyId) }
    }

    fun sendVoice(file: File, durationSec: Int) {
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val local = localMessage(MessageKind.Voice, mediaUrl = file.toURI().toString(), durationSec = durationSec, replyTo = replyId)
        send(local) { repository.sendVoice(who, local.clientId, file, durationSec, replyId) }
    }

    fun sendGif(item: GifItem) {
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val local = localMessage(MessageKind.Gif, mediaUrl = item.url, replyTo = replyId)
        send(local) { repository.sendGif(who, local.clientId, item.url, replyId) }
    }

    /** [type] is "sticker" or "gift". */
    fun sendEmojiCard(type: String, emoji: String, name: String) {
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val kind = if (type == "gift") MessageKind.Gift else MessageKind.Sticker
        val local = localMessage(kind, emoji = emoji, label = name, replyTo = replyId)
        send(local) { repository.sendEmojiCard(who, local.clientId, type, emoji, name, replyId) }
    }

    fun sendContact(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val local = localMessage(MessageKind.Contact, label = clean, replyTo = replyId)
        send(local) { repository.sendContact(who, local.clientId, clean, replyId) }
    }

    fun sendPoll(question: String, options: List<String>) {
        val q = question.trim()
        val opts = options.map { it.trim() }.filter { it.isNotEmpty() }
        if (q.isEmpty() || opts.size < MIN_POLL_OPTIONS) return
        val who = _state.value.identity ?: return
        val replyId = takeReplyId()
        val local = localMessage(MessageKind.Poll, text = q, pollOptions = opts, replyTo = replyId)
        send(local) { repository.sendPoll(who, local.clientId, q, opts, replyId) }
    }

    /** A vote is a small text reply to the poll, exactly like the web ("🗳️ Voted "X""). */
    fun vote(poll: ChatMessage, option: String) {
        val who = _state.value.identity ?: return
        val text = voteText(option)
        val local = localMessage(MessageKind.Text, text = text, replyTo = poll.id.takeIf { it != ChatMessage.LOCAL_ID })
        send(local) { repository.sendText(who, local.clientId, text, local.replyToId) }
    }

    /** Needs the location permission to be granted by the screen before it is called. */
    fun shareLocation() {
        val who = _state.value.identity ?: return
        viewModelScope.launch {
            val fix = files.currentLocation()
            if (fix == null) {
                _state.update { it.copy(banner = ChatBanner.Info(R.string.chat_location_failed)) }
                return@launch
            }
            val replyId = takeReplyId()
            val local = localMessage(MessageKind.Location, latitude = fix.first, longitude = fix.second, replyTo = replyId)
            send(local) { repository.sendLocation(who, local.clientId, fix.first, fix.second, replyId) }
        }
    }

    /** Sends every selected message again as "Forwarded". */
    fun forwardSelected() {
        val who = _state.value.identity ?: return
        val picked = _state.value.selectedMessages.filter { !it.deleted && it.id != ChatMessage.LOCAL_ID }
        clearSelection()
        picked.forEach { message ->
            val local = localMessage(message.kind, text = message.text, mediaUrl = message.mediaUrl, replyTo = null, forwarded = true)
                .copy(fileName = message.fileName, emoji = message.emoji, label = message.label, groupUrls = message.groupUrls)
            send(local) { repository.forward(who, local.clientId, message) }
        }
        if (picked.isNotEmpty()) _state.update { it.copy(banner = ChatBanner.Info(R.string.chat_info_forwarded)) }
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

    @Suppress("LongParameterList")
    private fun localMessage(
        kind: MessageKind,
        text: String? = null,
        mediaUrl: String? = null,
        groupUrls: List<String> = emptyList(),
        durationSec: Int = 0,
        replyTo: Long?,
        emoji: String? = null,
        label: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        pollOptions: List<String> = emptyList(),
        effect: String? = null,
        forwarded: Boolean = false,
    ) = ChatMessage(
        id = ChatMessage.LOCAL_ID,
        clientId = UUID.randomUUID().toString(),
        mine = true,
        kind = kind,
        text = text,
        mediaUrl = mediaUrl,
        groupUrls = groupUrls,
        durationSec = durationSec,
        replyToId = replyTo,
        createdAt = Instant.now(),
        delivery = DeliveryState.Sending,
        emoji = emoji,
        label = label,
        latitude = latitude,
        longitude = longitude,
        pollOptions = pollOptions,
        effect = effect,
        forwarded = forwarded,
    )

    // ───────────── reply, edit ─────────────

    fun startReply(message: ChatMessage) = _state.update { it.copy(replyingTo = message, editing = null) }
    fun cancelReply() = _state.update { it.copy(replyingTo = null) }

    private fun takeReplyId(): Long? {
        val id = _state.value.replyingTo?.id?.takeIf { it != ChatMessage.LOCAL_ID }
        _state.update { it.copy(replyingTo = null) }
        return id
    }

    fun startEdit(message: ChatMessage) {
        if (!message.mine || message.kind != MessageKind.Text || message.id == ChatMessage.LOCAL_ID) return
        _state.update { it.copy(editing = message, replyingTo = null) }
    }

    fun cancelEdit() = _state.update { it.copy(editing = null) }

    private fun submitEdit(message: ChatMessage, text: String) {
        val who = _state.value.identity ?: return
        _state.update { it.copy(editing = null) }
        if (text == message.text) return
        replaceMessage(message.copy(text = text, edited = true))
        viewModelScope.launch {
            when (val result = repository.edit(who, message.id, text)) {
                is AppResult.Success -> replaceMessage(result.data)
                is AppResult.Failure -> {
                    replaceMessage(message)
                    _state.update { it.copy(banner = ChatBanner.Failure(result.error)) }
                }
            }
        }
    }

    // ───────────── star, pin, react ─────────────

    /** The star flips at once; the server's answer is the final word. */
    fun toggleStar(message: ChatMessage) = optimistic(message, message.copy(starred = !message.starred)) { who ->
        repository.toggleStar(who, message.id)
    }

    fun togglePin(message: ChatMessage) = optimistic(message, message.copy(pinned = !message.pinned)) { who ->
        repository.pin(who, message.id, !message.pinned)
    }

    fun react(message: ChatMessage, emoji: String) {
        val who = _state.value.identity ?: return
        if (message.id == ChatMessage.LOCAL_ID) return
        viewModelScope.launch {
            when (val result = repository.react(who, message.id, emoji)) {
                is AppResult.Success -> replaceMessage(result.data)
                is AppResult.Failure -> _state.update { it.copy(banner = ChatBanner.Failure(result.error)) }
            }
        }
        viewModelScope.launch { local.useEmoji(emoji) }
    }

    private fun optimistic(old: ChatMessage, new: ChatMessage, call: suspend (ChatIdentity) -> AppResult<ChatMessage>) {
        val who = _state.value.identity ?: return
        if (old.id == ChatMessage.LOCAL_ID) return
        replaceMessage(new)
        viewModelScope.launch {
            when (val result = call(who)) {
                is AppResult.Success -> replaceMessage(result.data)
                is AppResult.Failure -> {
                    replaceMessage(old)
                    _state.update { it.copy(banner = ChatBanner.Failure(result.error)) }
                }
            }
        }
    }

    private fun replaceMessage(message: ChatMessage) {
        _state.update { s -> s.copy(messages = s.messages.map { if (it.id == message.id && it.clientId == message.clientId) message else it }) }
    }

    // ───────────── select mode ─────────────

    fun toggleSelect(message: ChatMessage) = _state.update { s ->
        val next = if (message.clientId in s.selected) s.selected - message.clientId else s.selected + message.clientId
        s.copy(selected = next)
    }

    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }

    // ───────────── delete ─────────────

    /** [forEveryone] only works for my own messages; otherwise the message leaves only my screen. */
    fun delete(targets: List<ChatMessage>, forEveryone: Boolean) {
        val who = _state.value.identity ?: return
        val real = targets.filter { it.id != ChatMessage.LOCAL_ID }
        clearSelection()
        if (real.isEmpty()) return
        if (forEveryone) {
            _state.update { s ->
                s.copy(
                    messages = s.messages.map { m ->
                        if (real.any { it.clientId == m.clientId }) {
                            m.copy(deleted = true, text = null, mediaUrl = null, groupUrls = emptyList(), reactions = emptyList(), pinned = false)
                        } else {
                            m
                        }
                    },
                )
            }
        } else {
            hidden += real.map { it.clientId }
            _state.update { s -> s.copy(messages = s.messages.filter { it.clientId !in hidden }) }
        }
        viewModelScope.launch {
            var failure: AppError? = null
            real.forEach { m ->
                val r = repository.delete(who, m.id, forEveryone && m.mine)
                if (r is AppResult.Failure) failure = r.error
            }
            failure?.let { error -> _state.update { it.copy(banner = ChatBanner.Failure(error)) } }
        }
    }

    /** "Clear chat": every message leaves my screen only (same as delete-for-me on each). */
    fun clearChat() {
        val all = _state.value.messages
        delete(all, forEveryone = false)
        _state.update { it.copy(banner = ChatBanner.Info(R.string.chat_info_cleared)) }
    }

    // ───────────── view once ─────────────

    fun openViewOnce(message: ChatMessage) {
        val who = _state.value.identity ?: return
        viewModelScope.launch {
            when (val result = repository.openViewOnce(who, message)) {
                is AppResult.Success -> {
                    replaceMessage(message.copy(viewOncePending = false, viewed = true))
                    _state.update { it.copy(viewOnce = result.data) }
                }
                is AppResult.Failure -> {
                    replaceMessage(message.copy(viewOncePending = false, viewed = true))
                    _state.update { it.copy(banner = ChatBanner.Info(R.string.chat_view_once_gone)) }
                }
            }
        }
    }

    fun closeViewOnce() = _state.update { it.copy(viewOnce = null) }

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

    // ───────────── chat settings ─────────────

    fun setMuted(muted: Boolean) = withCouple { local.setMuted(it, muted) }
    fun setNickname(name: String) = withCouple { local.setNickname(it, name.trim()) }
    fun setDnd(start: String?, end: String?) = withCouple { local.setDnd(it, start, end) }
    fun setAutoLock(minutes: Int) = withCouple { local.setAutoLock(it, minutes) }
    fun saveDraft(text: String) = withCouple { local.saveDraft(it, text) }
    fun useEmoji(emoji: String) = viewModelScope.launch { local.useEmoji(emoji) }

    private fun withCouple(block: suspend (String) -> Unit) {
        val id = _state.value.identity?.coupleId ?: return
        viewModelScope.launch { block(id) }
    }

    fun setDisappearing(seconds: Int) {
        val who = _state.value.identity ?: return
        val before = _state.value.disappearingSeconds
        _state.update { it.copy(disappearingSeconds = seconds) }
        viewModelScope.launch {
            when (val r = repository.setDisappearingSeconds(who, seconds)) {
                is AppResult.Success -> _state.update { it.copy(disappearingSeconds = r.data) }
                is AppResult.Failure -> _state.update { it.copy(disappearingSeconds = before, banner = ChatBanner.Failure(r.error)) }
            }
        }
    }

    /**
     * Saves the chat background. [shared] = for both of us (server); otherwise only this phone.
     * [cutImage] is the already cut picture file, when the person chose their own photo.
     */
    fun saveWallpaper(mode: WallpaperMode, swatch: String?, cutImage: File?, dim: Int, shared: Boolean) {
        val who = _state.value.identity ?: return
        _state.update { it.copy(wallpaperSaving = true) }
        viewModelScope.launch {
            local.setWallpaperShared(who.coupleId, shared)
            if (!shared) {
                local.setLocalWallpaper(who.coupleId, Wallpaper(mode, swatch, cutImage?.absolutePath, dim))
                _state.update { it.copy(wallpaperSaving = false, banner = ChatBanner.Info(R.string.chat_info_wallpaper_saved)) }
                return@launch
            }
            var url: String? = null
            if (mode == WallpaperMode.Custom && cutImage != null) {
                when (val up = repository.uploadWallpaper(who, cutImage)) {
                    is AppResult.Success -> url = up.data
                    is AppResult.Failure -> {
                        _state.update { it.copy(wallpaperSaving = false, banner = ChatBanner.Failure(up.error)) }
                        return@launch
                    }
                }
            }
            when (val r = repository.saveSharedWallpaper(who, Wallpaper(mode, swatch, url, dim))) {
                is AppResult.Success -> {
                    serverWallpaper = r.data
                    local.setLocalWallpaper(who.coupleId, Wallpaper())
                    _state.update { it.copy(wallpaperSaving = false, banner = ChatBanner.Info(R.string.chat_info_wallpaper_saved)) }
                }
                is AppResult.Failure -> _state.update { it.copy(wallpaperSaving = false, banner = ChatBanner.Failure(r.error)) }
            }
        }
    }

    suspend fun imageSize(uri: Uri): Pair<Int, Int>? = files.imageSize(uri)

    /** Cuts the chosen photo to [crop] and returns the saved file, or `null` when it failed. */
    suspend fun cutWallpaper(uri: Uri, crop: CropRect): File? {
        val id = _state.value.identity?.coupleId ?: return null
        return files.saveWallpaper(uri, crop, files.wallpaperFile(id))
    }

    fun resetWallpaper() = saveWallpaper(WallpaperMode.Default, null, null, 0, _state.value.wallpaperShared)

    // ───────────── export, gifs ─────────────

    fun exportChat() {
        val who = _state.value.identity ?: return
        viewModelScope.launch {
            when (val r = repository.exportText(who)) {
                is AppResult.Success -> _state.update { it.copy(exportText = r.data) }
                is AppResult.Failure -> _state.update { it.copy(banner = ChatBanner.Failure(r.error)) }
            }
        }
    }

    fun exportShown() = _state.update { it.copy(exportText = null) }

    fun searchGifs(query: String) {
        gifJob?.cancel()
        _state.update { it.copy(gifs = it.gifs.copy(loading = true, failed = null)) }
        gifJob = viewModelScope.launch {
            when (val r = repository.gifs(query.trim())) {
                is AppResult.Success -> _state.update { it.copy(gifs = GifState(items = r.data)) }
                is AppResult.Failure -> _state.update { it.copy(gifs = GifState(failed = r.error)) }
            }
        }
    }

    fun effectDone() = _state.update { it.copy(effect = null) }
    fun showBanner(banner: ChatBanner?) = _state.update { it.copy(banner = banner) }

    private companion object {
        const val SECONDS_PER_MINUTE = 60L
        const val MIN_POLL_OPTIONS = 2
    }
}
