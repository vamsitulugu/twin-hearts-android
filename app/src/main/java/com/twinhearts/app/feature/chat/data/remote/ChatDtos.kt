package com.twinhearts.app.feature.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Why this file exists: the exact JSON the old server (routes/chat.js, routes/media.js) sends
 * and receives. Field names follow the `chat_messages` table. Unknown fields are ignored.
 */

@Serializable
data class ChatMessageDto(
    val id: Long,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("sender_role") val senderRole: String = "",
    val type: String? = null,
    val text: String? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    /** Free-form JSON: `{duration, path}` for voice, `{name, size}` for files, `{items}` for groups. */
    @SerialName("media_meta") val mediaMeta: JsonElement? = null,
    /** The id of the message this one answers. Read as a primitive so a number or text both work. */
    @SerialName("reply_to") val replyTo: JsonElement? = null,
    val delivered: Boolean = false,
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    val edited: Boolean = false,
    @SerialName("edited_at") val editedAt: String? = null,
    val deleted: Boolean = false,
    @SerialName("deleted_for") val deletedFor: String? = null,
    /** `{ "emoji": ["user1"] }` */
    val reactions: JsonElement? = null,
    @SerialName("starred_by") val starredBy: List<String>? = null,
    @SerialName("view_once_pending") val viewOncePending: Boolean = false,
    @SerialName("viewed_at") val viewedAt: String? = null,
    @SerialName("view_once") val viewOnce: Boolean = false,
)

@Serializable
data class SendMessageBody(
    val coupleId: String,
    val clientId: String,
    val senderRole: String,
    val type: String,
    val text: String? = null,
    val mediaUrl: String? = null,
    val mediaMeta: JsonElement? = null,
    val replyTo: Long? = null,
)

@Serializable
data class RoleBody(val role: String)

@Serializable
data class StarBody(val coupleId: String, val role: String)

@Serializable
data class PresenceBody(val role: String, val status: String)

@Serializable
data class OkResponse(val ok: Boolean = true)

@Serializable
data class PresenceDto(
    val role: String = "",
    val status: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
)

@Serializable
data class ScheduleBody(
    val clientId: String,
    val senderRole: String,
    val type: String,
    val text: String,
    val sendAt: String,
)

@Serializable
data class ScheduledDto(
    /** A number or a uuid depending on the table, so it is read as a primitive. */
    val id: JsonPrimitive,
    @SerialName("sender_role") val senderRole: String = "",
    val text: String? = null,
    @SerialName("send_at") val sendAt: String = "",
)

/** routes/media.js answers `{url, path}`; the voice route adds `bucket` and `contentType`. */
@Serializable
data class UploadResponse(
    val url: String,
    val path: String? = null,
)

/** PATCH /api/chat/:id — edit a message I sent. */
@Serializable
data class EditMessageBody(
    val coupleId: String,
    val senderRole: String,
    val text: String,
)

/** DELETE /api/chat/:id — delete for me or for everyone. */
@Serializable
data class DeleteMessageBody(
    val coupleId: String,
    val senderRole: String,
    val mode: String, // "me" or "everyone"
)

/** POST /api/chat/:id/react — toggle an emoji reaction. */
@Serializable
data class ReactBody(
    val coupleId: String,
    val role: String,
    val emoji: String,
)

/** POST /api/chat/:id/view — open (and burn) view-once media. */
@Serializable
data class ViewOnceBody(
    val coupleId: String,
    val role: String,
)

/** The server answers `{ mediaUrl, mediaMeta, alreadyOpen }`. */
@Serializable
data class ViewOnceResponse(
    val mediaUrl: String?,
    val mediaMeta: JsonElement? = null,
    val alreadyOpen: Boolean = false,
)
