package com.twinhearts.app.feature.chat.data

import android.net.Uri
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.chat.data.mapper.forRole
import com.twinhearts.app.feature.chat.data.mapper.parseServerTime
import com.twinhearts.app.feature.chat.data.mapper.toDomain
import com.twinhearts.app.feature.chat.data.remote.ChatApi
import com.twinhearts.app.feature.chat.data.remote.PresenceBody
import com.twinhearts.app.feature.chat.data.remote.RoleBody
import com.twinhearts.app.feature.chat.data.remote.ScheduleBody
import com.twinhearts.app.feature.chat.data.remote.SendMessageBody
import com.twinhearts.app.feature.chat.data.remote.StarBody
import com.twinhearts.app.feature.chat.domain.model.ChatIdentity
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.PartnerPresence
import com.twinhearts.app.feature.chat.domain.model.PresenceState
import com.twinhearts.app.feature.chat.domain.model.ScheduledMessage
import com.twinhearts.app.feature.chat.domain.model.SearchFilter
import com.twinhearts.app.feature.chat.domain.presenceFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import javax.inject.Inject

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

    suspend fun sendText(who: ChatIdentity, clientId: String, text: String, replyTo: Long?): AppResult<ChatMessage>
    suspend fun sendPhoto(who: ChatIdentity, clientId: String, photo: Uri, replyTo: Long?): AppResult<ChatMessage>
    suspend fun sendVoice(who: ChatIdentity, clientId: String, audio: File, durationSec: Int, replyTo: Long?): AppResult<ChatMessage>

    /** Marks everything my partner sent as read (blue ticks on their side). */
    suspend fun markRead(who: ChatIdentity): AppResult<Unit>

    suspend fun toggleStar(who: ChatIdentity, messageId: Long): AppResult<ChatMessage>
    suspend fun search(who: ChatIdentity, query: String, filter: SearchFilter): AppResult<List<ChatMessage>>
    suspend fun partnerPresence(who: ChatIdentity): AppResult<PartnerPresence>

    /** "I am here" heartbeat; the server also turns my partner's single tick into a double tick. */
    suspend fun heartbeat(who: ChatIdentity, online: Boolean): AppResult<Unit>

    suspend fun schedule(who: ChatIdentity, clientId: String, text: String, sendAt: Instant): AppResult<ScheduledMessage>
    suspend fun scheduled(who: ChatIdentity): AppResult<List<ScheduledMessage>>
    suspend fun cancelScheduled(scheduledId: String): AppResult<Unit>
}

class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val sessionStore: SessionStore,
    private val compressor: ImageCompressor,
) : ChatRepository {

    override suspend fun identity(): ChatIdentity? =
        sessionStore.session.first()?.takeIf { it.coupleId.isNotBlank() }?.let {
            ChatIdentity(it.coupleId, it.role, it.myName, it.partnerName)
        }

    override suspend fun recent(who: ChatIdentity): AppResult<List<ChatMessage>> =
        when (val r = safeApiCall { api.messages(who.coupleId, limit = RECENT_LIMIT) }) {
            is AppResult.Success -> AppResult.Success(r.data.toDomain(who.myRole))
            is AppResult.Failure -> r
        }

    override suspend fun since(who: ChatIdentity, after: Instant): AppResult<List<ChatMessage>> =
        when (val r = safeApiCall { api.messages(who.coupleId, after = after.toString()) }) {
            is AppResult.Success -> AppResult.Success(r.data.toDomain(who.myRole))
            is AppResult.Failure -> r
        }

    override suspend fun sendText(who: ChatIdentity, clientId: String, text: String, replyTo: Long?): AppResult<ChatMessage> =
        post(who, SendMessageBody(who.coupleId, clientId, who.myRole, type = "text", text = text, replyTo = replyTo))

    override suspend fun sendPhoto(who: ChatIdentity, clientId: String, photo: Uri, replyTo: Long?): AppResult<ChatMessage> {
        val jpeg = compressor.toJpeg(photo) ?: return AppResult.Failure(AppError.Unknown)
        val part = MultipartBody.Part.createFormData("file", "photo.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
        val couple = who.coupleId.toRequestBody("text/plain".toMediaType())
        val upload = when (val r = safeApiCall { api.uploadImage(part, couple) }) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return r
        }
        val meta = buildJsonObject { put("path", upload.path.orEmpty()) }
        return post(
            who,
            SendMessageBody(who.coupleId, clientId, who.myRole, type = "image", mediaUrl = upload.url, mediaMeta = meta, replyTo = replyTo),
        )
    }

    override suspend fun sendVoice(who: ChatIdentity, clientId: String, audio: File, durationSec: Int, replyTo: Long?): AppResult<ChatMessage> {
        val bytes = withContext(Dispatchers.IO) { runCatching { audio.readBytes() }.getOrNull() }
        if (bytes == null || bytes.isEmpty()) return AppResult.Failure(AppError.Unknown)
        val part = MultipartBody.Part.createFormData("file", "voice.m4a", bytes.toRequestBody("audio/mp4".toMediaType()))
        val couple = who.coupleId.toRequestBody("text/plain".toMediaType())
        val role = who.myRole.toRequestBody("text/plain".toMediaType())
        val upload = when (val r = safeApiCall { api.uploadVoice(part, couple, role) }) {
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

    private suspend fun post(who: ChatIdentity, body: SendMessageBody): AppResult<ChatMessage> =
        when (val r = safeApiCall { api.send(body) }) {
            is AppResult.Success -> r.data.toDomain(who.myRole)?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> r
        }

    override suspend fun markRead(who: ChatIdentity): AppResult<Unit> =
        when (val r = safeApiCall { api.markRead(who.coupleId, RoleBody(who.myRole)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> r
        }

    override suspend fun toggleStar(who: ChatIdentity, messageId: Long): AppResult<ChatMessage> =
        when (val r = safeApiCall { api.toggleStar(messageId, StarBody(who.coupleId, who.myRole)) }) {
            is AppResult.Success -> r.data.toDomain(who.myRole)?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> r
        }

    override suspend fun search(who: ChatIdentity, query: String, filter: SearchFilter): AppResult<List<ChatMessage>> =
        when (val r = safeApiCall { api.search(who.coupleId, query, filter.apiValue) }) {
            is AppResult.Success -> AppResult.Success(r.data.toDomain(who.myRole))
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

    private companion object {
        const val RECENT_LIMIT = 200
    }
}
