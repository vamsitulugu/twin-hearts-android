package com.twinhearts.app.feature.chat.data

import android.net.Uri
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.chat.data.mapper.forRole
import com.twinhearts.app.feature.chat.data.mapper.parseServerTime
import com.twinhearts.app.feature.chat.data.mapper.toDomain
import com.twinhearts.app.feature.chat.data.mapper.toMessageDto
import com.twinhearts.app.feature.chat.data.mapper.toMessageDtos
import com.twinhearts.app.feature.chat.data.remote.ChatApi
import com.twinhearts.app.feature.chat.data.remote.DeleteBody
import com.twinhearts.app.feature.chat.data.remote.DisappearBody
import com.twinhearts.app.feature.chat.data.remote.EditBody
import com.twinhearts.app.feature.chat.data.remote.PinBody
import com.twinhearts.app.feature.chat.data.remote.PresenceBody
import com.twinhearts.app.feature.chat.data.remote.ReactBody
import com.twinhearts.app.feature.chat.data.remote.RoleBody
import com.twinhearts.app.feature.chat.data.remote.ScheduleBody
import com.twinhearts.app.feature.chat.data.remote.SendMessageBody
import com.twinhearts.app.feature.chat.data.remote.StarBody
import com.twinhearts.app.feature.chat.data.remote.UploadResponse
import com.twinhearts.app.feature.chat.data.remote.ViewBody
import com.twinhearts.app.feature.chat.data.remote.WallpaperBody
import com.twinhearts.app.feature.chat.domain.model.ChatIdentity
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.GifItem
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import com.twinhearts.app.feature.chat.domain.model.PartnerPresence
import com.twinhearts.app.feature.chat.domain.model.PresenceState
import com.twinhearts.app.feature.chat.domain.model.ScheduledMessage
import com.twinhearts.app.feature.chat.domain.model.SearchFilter
import com.twinhearts.app.feature.chat.domain.model.ViewOnceResult
import com.twinhearts.app.feature.chat.domain.model.Wallpaper
import com.twinhearts.app.feature.chat.domain.presenceFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject

/** One picture chosen for sending, with the turn (0, 90, 180, 270) the person gave it. */
data class PhotoPick(val uri: Uri, val rotation: Int = 0)

/**
 * Why this file exists: the only place Chat talks to the old server. Every call returns an
 * [AppResult] so the ViewModel never sees an exception (R-46).
 */
interface ChatRepository {
    /** Who is chatting, from the saved Session; `null` when nobody is signed in. */
    suspend fun identity(): ChatIdentity?

    /** The newest 200 messages, oldest first. */
    suspend fun recent(who: ChatIdentity): AppResult<List<ChatMessage>>

    /** Only messages newer than [after]. */
    suspend fun since(who: ChatIdentity, after: Instant): AppResult<List<ChatMessage>>

    // ── sending ──
    suspend fun sendText(who: ChatIdentity, clientId: String, text: String, replyTo: Long?, effect: String? = null): AppResult<ChatMessage>

    /** One or more photos as a single message (type `image_group`), with an optional caption. */
    suspend fun sendPhotos(who: ChatIdentity, clientId: String, photos: List<PhotoPick>, caption: String, replyTo: Long?, viewOnce: Boolean = false): AppResult<ChatMessage>
    suspend fun sendVideo(who: ChatIdentity, clientId: String, uri: Uri, replyTo: Long?): AppResult<ChatMessage>
    suspend fun sendFile(who: ChatIdentity, clientId: String, uri: Uri, replyTo: Long?): AppResult<ChatMessage>
    suspend fun sendAudioFile(who: ChatIdentity, clientId: String, uri: Uri, replyTo: Long?): AppResult<ChatMessage>
    suspend fun sendVoice(who: ChatIdentity, clientId: String, audio: File, durationSec: Int, replyTo: Long?): AppResult<ChatMessage>
    suspend fun sendGif(who: ChatIdentity, clientId: String, url: String, replyTo: Long?): AppResult<ChatMessage>

    /** [type] is "sticker" or "gift"; both carry an emoji and a name. */
    suspend fun sendEmojiCard(who: ChatIdentity, clientId: String, type: String, emoji: String, name: String, replyTo: Long?): AppResult<ChatMessage>
    suspend fun sendLocation(who: ChatIdentity, clientId: String, latitude: Double, longitude: Double, replyTo: Long?): AppResult<ChatMessage>
    suspend fun sendContact(who: ChatIdentity, clientId: String, name: String, replyTo: Long?): AppResult<ChatMessage>
    suspend fun sendPoll(who: ChatIdentity, clientId: String, question: String, options: List<String>, replyTo: Long?): AppResult<ChatMessage>

    /** Sends [message] again as "Forwarded". */
    suspend fun forward(who: ChatIdentity, clientId: String, message: ChatMessage): AppResult<ChatMessage>

    // ── message actions ──
    /** Marks everything my partner sent as read (blue ticks on their side). */
    suspend fun markRead(who: ChatIdentity): AppResult<Unit>
    suspend fun toggleStar(who: ChatIdentity, messageId: Long): AppResult<ChatMessage>
    suspend fun edit(who: ChatIdentity, messageId: Long, text: String): AppResult<ChatMessage>

    /** [forEveryone] true removes it for both people (only my own messages); false only for me. */
    suspend fun delete(who: ChatIdentity, messageId: Long, forEveryone: Boolean): AppResult<Unit>
    suspend fun react(who: ChatIdentity, messageId: Long, emoji: String): AppResult<ChatMessage>
    suspend fun pin(who: ChatIdentity, messageId: Long, pinned: Boolean): AppResult<ChatMessage>

    /** Opens a view-once photo or video. The server hides it again for the receiver afterwards. */
    suspend fun openViewOnce(who: ChatIdentity, message: ChatMessage): AppResult<ViewOnceResult>

    suspend fun search(who: ChatIdentity, query: String, filter: SearchFilter): AppResult<List<ChatMessage>>
    suspend fun partnerPresence(who: ChatIdentity): AppResult<PartnerPresence>

    /** "I am here" heartbeat; the server also turns my partner's single tick into a double tick. */
    suspend fun heartbeat(who: ChatIdentity, online: Boolean): AppResult<Unit>

    // ── scheduled messages ──
    suspend fun schedule(who: ChatIdentity, clientId: String, text: String, sendAt: Instant): AppResult<ScheduledMessage>
    suspend fun scheduled(who: ChatIdentity): AppResult<List<ScheduledMessage>>
    suspend fun cancelScheduled(scheduledId: String): AppResult<Unit>

    // ── chat settings kept on the server ──
    suspend fun disappearingSeconds(who: ChatIdentity): AppResult<Int>
    suspend fun setDisappearingSeconds(who: ChatIdentity, seconds: Int): AppResult<Int>
    suspend fun sharedWallpaper(who: ChatIdentity): AppResult<Wallpaper>
    suspend fun saveSharedWallpaper(who: ChatIdentity, wallpaper: Wallpaper): AppResult<Wallpaper>

    /** Uploads a cut wallpaper picture and returns its web address. */
    suspend fun uploadWallpaper(who: ChatIdentity, file: File): AppResult<String>

    /** The whole chat as plain text. */
    suspend fun exportText(who: ChatIdentity): AppResult<String>
    suspend fun gifs(query: String): AppResult<List<GifItem>>
}

class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val sessionStore: SessionStore,
    private val compressor: ImageCompressor,
    private val files: ChatFiles,
    private val gifKey: GifKeyProvider,
    private val json: Json,
) : ChatRepository {

    override suspend fun identity(): ChatIdentity? =
        sessionStore.session.first()?.takeIf { it.coupleId.isNotBlank() }?.let {
            ChatIdentity(it.coupleId, it.role, it.myName, it.partnerName)
        }

    override suspend fun recent(who: ChatIdentity): AppResult<List<ChatMessage>> =
        when (val r = safeApiCall { api.messages(who.coupleId, limit = RECENT_LIMIT) }) {
            is AppResult.Success -> r.data.toMessageDtos()?.let { AppResult.Success(it.toDomain(who.myRole)) } ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> r
        }

    override suspend fun since(who: ChatIdentity, after: Instant): AppResult<List<ChatMessage>> =
        when (val r = safeApiCall { api.messages(who.coupleId, after = after.toString()) }) {
            is AppResult.Success -> r.data.toMessageDtos()?.let { AppResult.Success(it.toDomain(who.myRole)) } ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> r
        }

    // ───────────── sending ─────────────

    override suspend fun sendText(who: ChatIdentity, clientId: String, text: String, replyTo: Long?, effect: String?): AppResult<ChatMessage> =
        post(who, SendMessageBody(who.coupleId, clientId, who.myRole, type = "text", text = text, replyTo = replyTo, effect = effect))

    override suspend fun sendPhotos(
        who: ChatIdentity,
        clientId: String,
        photos: List<PhotoPick>,
        caption: String,
        replyTo: Long?,
        viewOnce: Boolean,
    ): AppResult<ChatMessage> {
        val uploaded = mutableListOf<UploadResponse>()
        for (photo in photos) {
            val jpeg = compressor.toJpeg(photo.uri, photo.rotation)
            val part = if (jpeg != null) {
                MultipartBody.Part.createFormData("file", "photo.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
            } else {
                // The picture could not be shrunk (rare format): send the original file as it is.
                val raw = files.describe(photo.uri)?.takeIf { it.mime.startsWith("image/") }
                    ?: return AppResult.Failure(AppError.Unknown)
                MultipartBody.Part.createFormData("file", raw.name, files.body(raw))
            }
            when (val r = safeApiCall { api.uploadImage(part, who.coupleId.asText()) }) {
                is AppResult.Success -> uploaded += r.data
                is AppResult.Failure -> return r
            }
        }
        if (uploaded.isEmpty()) return AppResult.Failure(AppError.Unknown)
        val text = caption.takeIf { it.isNotBlank() }
        if (uploaded.size == 1) {
            // One picture is a plain "image" message (the only kind the server can make view-once).
            return post(
                who,
                SendMessageBody(
                    who.coupleId, clientId, who.myRole, type = "image",
                    text = text, mediaUrl = uploaded.first().url, replyTo = replyTo, viewOnce = viewOnce,
                ),
            )
        }
        val meta = buildJsonObject {
            put(
                "items",
                buildJsonArray {
                    uploaded.forEach { up ->
                        add(buildJsonObject { put("url", up.url); put("path", up.path.orEmpty()) })
                    }
                },
            )
        }
        return post(
            who,
            SendMessageBody(
                who.coupleId, clientId, who.myRole, type = "image_group",
                text = text, mediaUrl = uploaded.first().url, mediaMeta = meta, replyTo = replyTo,
            ),
        )
    }

    override suspend fun sendVideo(who: ChatIdentity, clientId: String, uri: Uri, replyTo: Long?): AppResult<ChatMessage> {
        val file = files.describe(uri) ?: return AppResult.Failure(AppError.Unknown)
        val part = MultipartBody.Part.createFormData("file", file.name, files.body(file))
        val up = when (val r = safeApiCall { api.uploadImage(part, who.coupleId.asText()) }) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return r
        }
        val meta = buildJsonObject { put("name", file.name); put("size", file.size) }
        return post(who, SendMessageBody(who.coupleId, clientId, who.myRole, type = "video", mediaUrl = up.url, mediaMeta = meta, replyTo = replyTo))
    }

    override suspend fun sendFile(who: ChatIdentity, clientId: String, uri: Uri, replyTo: Long?): AppResult<ChatMessage> {
        val file = files.describe(uri)?.let(files::asDocument) ?: return AppResult.Failure(AppError.Unknown)
        val part = MultipartBody.Part.createFormData("file", file.name, files.body(file))
        val up = when (val r = safeApiCall { api.uploadDoc(part, who.coupleId.asText()) }) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return r
        }
        val meta = buildJsonObject {
            put("name", file.name)
            put("size", file.size)
            put("path", up.path.orEmpty())
        }
        return post(who, SendMessageBody(who.coupleId, clientId, who.myRole, type = "file", mediaUrl = up.url, mediaMeta = meta, replyTo = replyTo))
    }

    override suspend fun sendAudioFile(who: ChatIdentity, clientId: String, uri: Uri, replyTo: Long?): AppResult<ChatMessage> {
        val file = files.describe(uri) ?: return AppResult.Failure(AppError.Unknown)
        val part = MultipartBody.Part.createFormData("file", file.name, files.body(file))
        val up = when (val r = safeApiCall { api.uploadAudio(part, who.coupleId.asText()) }) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return r
        }
        val meta = buildJsonObject { put("name", file.name) }
        return post(who, SendMessageBody(who.coupleId, clientId, who.myRole, type = "audio", mediaUrl = up.url, mediaMeta = meta, replyTo = replyTo))
    }

    override suspend fun sendVoice(who: ChatIdentity, clientId: String, audio: File, durationSec: Int, replyTo: Long?): AppResult<ChatMessage> {
        val bytes = withContext(Dispatchers.IO) { runCatching { audio.readBytes() }.getOrNull() }
        if (bytes == null || bytes.isEmpty()) return AppResult.Failure(AppError.Unknown)
        val part = MultipartBody.Part.createFormData("file", "voice.m4a", bytes.toRequestBody("audio/mp4".toMediaType()))
        val upload = when (val r = safeApiCall { api.uploadVoice(part, who.coupleId.asText(), who.myRole.asText()) }) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return r
        }
        val meta = buildJsonObject {
            put("duration", durationSec)
            put("path", upload.path.orEmpty())
        }
        return post(
            who,
            SendMessageBody(who.coupleId, clientId, who.myRole, type = "voice", mediaUrl = upload.url, mediaMeta = meta, replyTo = replyTo),
        )
    }

    override suspend fun sendGif(who: ChatIdentity, clientId: String, url: String, replyTo: Long?): AppResult<ChatMessage> =
        post(who, SendMessageBody(who.coupleId, clientId, who.myRole, type = "gif", mediaUrl = url, replyTo = replyTo))

    override suspend fun sendEmojiCard(
        who: ChatIdentity,
        clientId: String,
        type: String,
        emoji: String,
        name: String,
        replyTo: Long?,
    ): AppResult<ChatMessage> {
        val meta = buildJsonObject { put("emoji", emoji); put("name", name) }
        return post(who, SendMessageBody(who.coupleId, clientId, who.myRole, type = type, mediaMeta = meta, replyTo = replyTo))
    }

    override suspend fun sendLocation(who: ChatIdentity, clientId: String, latitude: Double, longitude: Double, replyTo: Long?): AppResult<ChatMessage> {
        val meta = buildJsonObject { put("lat", latitude); put("lng", longitude) }
        return post(who, SendMessageBody(who.coupleId, clientId, who.myRole, type = "location", mediaMeta = meta, replyTo = replyTo))
    }

    override suspend fun sendContact(who: ChatIdentity, clientId: String, name: String, replyTo: Long?): AppResult<ChatMessage> {
        val meta = buildJsonObject { put("name", name) }
        return post(who, SendMessageBody(who.coupleId, clientId, who.myRole, type = "contact", mediaMeta = meta, replyTo = replyTo))
    }

    override suspend fun sendPoll(who: ChatIdentity, clientId: String, question: String, options: List<String>, replyTo: Long?): AppResult<ChatMessage> {
        val meta = buildJsonObject { put("options", buildJsonArray { options.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }) }
        return post(who, SendMessageBody(who.coupleId, clientId, who.myRole, type = "poll", text = question, mediaMeta = meta, replyTo = replyTo))
    }

    override suspend fun forward(who: ChatIdentity, clientId: String, message: ChatMessage): AppResult<ChatMessage> {
        val meta: JsonElement? = message.metaJson?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }
        return post(
            who,
            SendMessageBody(
                who.coupleId, clientId, who.myRole, type = message.rawType ?: "text",
                text = message.text, mediaUrl = message.mediaUrl, mediaMeta = meta, forwarded = true,
            ),
        )
    }

    private suspend fun post(who: ChatIdentity, body: SendMessageBody): AppResult<ChatMessage> =
        when (val r = safeApiCall { api.send(body) }) {
            is AppResult.Success -> r.data.toMessageDto()?.toDomain(who.myRole)?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> r
        }

    // ───────────── message actions ─────────────

    override suspend fun markRead(who: ChatIdentity): AppResult<Unit> =
        when (val r = safeApiCall { api.markRead(who.coupleId, RoleBody(who.myRole)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> r
        }

    override suspend fun toggleStar(who: ChatIdentity, messageId: Long): AppResult<ChatMessage> =
        message(who) { api.toggleStar(messageId, StarBody(who.coupleId, who.myRole)) }

    override suspend fun edit(who: ChatIdentity, messageId: Long, text: String): AppResult<ChatMessage> =
        message(who) { api.edit(messageId, EditBody(who.coupleId, who.myRole, text)) }

    override suspend fun delete(who: ChatIdentity, messageId: Long, forEveryone: Boolean): AppResult<Unit> =
        when (val r = safeApiCall { api.delete(messageId, DeleteBody(who.coupleId, who.myRole, if (forEveryone) "everyone" else "me")) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> r
        }

    override suspend fun react(who: ChatIdentity, messageId: Long, emoji: String): AppResult<ChatMessage> =
        message(who) { api.react(messageId, ReactBody(who.coupleId, who.myRole, emoji)) }

    override suspend fun pin(who: ChatIdentity, messageId: Long, pinned: Boolean): AppResult<ChatMessage> =
        message(who) { api.pin(messageId, PinBody(who.coupleId, pinned)) }

    private suspend fun message(who: ChatIdentity, call: suspend () -> JsonElement): AppResult<ChatMessage> =
        when (val r = safeApiCall(call)) {
            is AppResult.Success -> r.data.toMessageDto()?.toDomain(who.myRole)?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> r
        }

    override suspend fun openViewOnce(who: ChatIdentity, message: ChatMessage): AppResult<ViewOnceResult> =
        when (val r = safeApiCall { api.view(message.id, ViewBody(who.coupleId, who.myRole)) }) {
            is AppResult.Success -> r.data.mediaUrl?.let {
                AppResult.Success(ViewOnceResult(it, isVideo = message.kind == MessageKind.Video))
            } ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> r
        }

    override suspend fun search(who: ChatIdentity, query: String, filter: SearchFilter): AppResult<List<ChatMessage>> =
        when (val r = safeApiCall { api.search(who.coupleId, query, filter.apiValue) }) {
            is AppResult.Success -> r.data.toMessageDtos()?.let { AppResult.Success(it.toDomain(who.myRole)) } ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> r
        }

    override suspend fun partnerPresence(who: ChatIdentity): AppResult<PartnerPresence> =
        when (val r = safeApiCall { api.presence(who.coupleId) }) {
            is AppResult.Success -> {
                val row = r.data.forRole(who.partnerRole)
                AppResult.Success(
                    if (row == null) PartnerPresence(PresenceState.Offline, null)
                    else presenceFrom(row.status, parseServerTime(row.lastSeen), Instant.now()),
                )
            }
            is AppResult.Failure -> r
        }

    override suspend fun heartbeat(who: ChatIdentity, online: Boolean): AppResult<Unit> =
        when (val r = safeApiCall { api.setPresence(who.coupleId, PresenceBody(who.myRole, if (online) "online" else "away")) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> r
        }

    // ───────────── scheduled messages ─────────────

    override suspend fun schedule(who: ChatIdentity, clientId: String, text: String, sendAt: Instant): AppResult<ScheduledMessage> =
        when (val r = safeApiCall { api.schedule(who.coupleId, ScheduleBody(clientId, who.myRole, "text", text, sendAt.toString())) }) {
            is AppResult.Success -> r.data.toDomain()?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> r
        }

    override suspend fun scheduled(who: ChatIdentity): AppResult<List<ScheduledMessage>> =
        when (val r = safeApiCall { api.scheduled(who.coupleId) }) {
            is AppResult.Success -> AppResult.Success(r.data.mapNotNull { it.toDomain() })
            is AppResult.Failure -> r
        }

    override suspend fun cancelScheduled(scheduledId: String): AppResult<Unit> =
        when (val r = safeApiCall { api.cancelScheduled(scheduledId) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> r
        }

    // ───────────── chat settings on the server ─────────────

    override suspend fun disappearingSeconds(who: ChatIdentity): AppResult<Int> =
        when (val r = safeApiCall { api.disappearing(who.coupleId) }) {
            is AppResult.Success -> AppResult.Success(r.data.disappearingSeconds)
            is AppResult.Failure -> r
        }

    override suspend fun setDisappearingSeconds(who: ChatIdentity, seconds: Int): AppResult<Int> =
        when (val r = safeApiCall { api.setDisappearing(who.coupleId, DisappearBody(seconds)) }) {
            is AppResult.Success -> AppResult.Success(r.data.disappearingSeconds)
            is AppResult.Failure -> r
        }

    override suspend fun sharedWallpaper(who: ChatIdentity): AppResult<Wallpaper> =
        when (val r = safeApiCall { api.wallpaper(who.coupleId) }) {
            is AppResult.Success -> AppResult.Success(r.data.toDomain())
            is AppResult.Failure -> r
        }

    override suspend fun saveSharedWallpaper(who: ChatIdentity, wallpaper: Wallpaper): AppResult<Wallpaper> {
        val body = WallpaperBody(wallpaper.mode.apiValue, wallpaper.swatch, wallpaper.image, wallpaper.dim, who.myRole)
        return when (val r = safeApiCall { api.setWallpaper(who.coupleId, body) }) {
            is AppResult.Success -> AppResult.Success(r.data.toDomain())
            is AppResult.Failure -> r
        }
    }

    override suspend fun uploadWallpaper(who: ChatIdentity, file: File): AppResult<String> {
        val bytes = withContext(Dispatchers.IO) { runCatching { file.readBytes() }.getOrNull() }
        if (bytes == null || bytes.isEmpty()) return AppResult.Failure(AppError.Unknown)
        val part = MultipartBody.Part.createFormData("file", "wallpaper.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
        return when (val r = safeApiCall { api.uploadImage(part, who.coupleId.asText()) }) {
            is AppResult.Success -> AppResult.Success(r.data.url)
            is AppResult.Failure -> r
        }
    }

    override suspend fun exportText(who: ChatIdentity): AppResult<String> =
        when (val r = safeApiCall { withContext(Dispatchers.IO) { api.export(who.coupleId).string() } }) {
            is AppResult.Success -> AppResult.Success(r.data)
            is AppResult.Failure -> r
        }

    override suspend fun gifs(query: String): AppResult<List<GifItem>> {
        val key = gifKey.key
        if (key.isBlank()) return AppResult.Failure(AppError.Unknown)
        val q = URLEncoder.encode(query.ifBlank { "love" }, "UTF-8")
        val url = "https://api.giphy.com/v1/gifs/search?q=$q&api_key=$key&limit=24&rating=pg-13"
        return when (val r = safeApiCall { api.gifs(url) }) {
            is AppResult.Success -> AppResult.Success(r.data.data.mapNotNull { it.toDomain() })
            is AppResult.Failure -> r
        }
    }

    private fun String.asText() = toRequestBody("text/plain".toMediaType())

    private companion object {
        const val RECENT_LIMIT = 200
    }
}
