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
    /** Free-form JSON: `{duration, path}` for voice, `{name, size}` for files, `{items}` for groups ... */
    @SerialName("media_meta") val mediaMeta: JsonElement? = null,
    /** The id of the message this one answers. Read as a primitive so a number or text both work. */
    @SerialName("reply_to") val replyTo: JsonElement? = null,
    val delivered: Boolean = false,
    val read: Boolean = false,
    @SerialName("delivered_at") val deliveredAt: JsonElement? = null,
    @SerialName("read_at") val readAt: JsonElement? = null,
    @SerialName("created_at") val createdAt: String = "",
    val edited: Boolean = false,
    val deleted: Boolean = false,
    @SerialName("deleted_for") val deletedFor: String? = null,
    /** `{ "emoji": ["user1"] }` */
    val reactions: JsonElement? = null,
    @SerialName("starred_by") val starredBy: List<String>? = null,
    val pinned: JsonElement? = null,
    val forwarded: JsonElement? = null,
    @SerialName("view_once") val viewOnce: JsonElement? = null,
    @SerialName("viewed_at") val viewedAt: JsonElement? = null,
    @SerialName("view_once_pending") val viewOncePending: JsonElement? = null,
    val effect: JsonElement? = null,
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
    val forwarded: Boolean = false,
    val viewOnce: Boolean = false,
    val effect: String? = null,
)

@Serializable
data class RoleBody(val role: String)

@Serializable
data class StarBody(val coupleId: String, val role: String)

@Serializable
data class EditBody(val coupleId: String, val senderRole: String, val text: String)

/** `mode` is "everyone" or "me". */
@Serializable
data class DeleteBody(val coupleId: String, val senderRole: String, val mode: String)

@Serializable
data class ReactBody(val coupleId: String, val role: String, val emoji: String)

@Serializable
data class PinBody(val coupleId: String, val pinned: Boolean)

@Serializable
data class ViewBody(val coupleId: String, val role: String)

@Serializable
data class ViewOnceResponse(
    val mediaUrl: String? = null,
    val mediaMeta: JsonElement? = null,
    val alreadyOpen: Boolean = false,
)

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

@Serializable
data class DisappearBody(val seconds: Int)

@Serializable
data class DisappearResponse(val disappearingSeconds: Int = 0)

@Serializable
data class WallpaperDto(
    val mode: String? = null,
    val swatch: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val dim: Int = 0,
)

@Serializable
data class WallpaperBody(
    val mode: String,
    val swatch: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val dim: Int = 0,
    val role: String,
)

/** routes/media.js answers `{url, path}`; documents add `name` and `size`. */
@Serializable
data class UploadResponse(
    val url: String,
    val path: String? = null,
    val name: String? = null,
    val size: Long? = null,
)

/** The small part of Giphy's answer we use. */
@Serializable
data class GifSearchResponse(val data: List<GifDto> = emptyList())

@Serializable
data class GifDto(val id: String = "", val images: GifImagesDto? = null)

@Serializable
data class GifImagesDto(
    @SerialName("fixed_height_small") val small: GifImageDto? = null,
    @SerialName("fixed_height") val fixed: GifImageDto? = null,
    val downsized: GifImageDto? = null,
)

@Serializable
data class GifImageDto(val url: String? = null)
