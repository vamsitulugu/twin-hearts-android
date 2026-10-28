package com.twinhearts.app.feature.chat.data.mapper

import com.twinhearts.app.feature.chat.data.remote.ChatMessageDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Why this file exists: this is the "never break the chat" reader. The old `chat_messages` table
 * has columns that are sometimes text, sometimes JSON, sometimes null. The strict reader failed on
 * the first odd value and the whole chat showed "Something went wrong". Here every field is read
 * on its own, so one odd value only costs that one field, and a row we cannot read at all is
 * skipped instead of killing the list.
 */

/** The rows of a list answer. `null` only when the answer is not a list at all (for example a web page). */
fun JsonElement.toMessageDtos(): List<ChatMessageDto>? {
    val rows: JsonArray = when (this) {
        is JsonArray -> this
        is JsonObject -> (this["data"] ?: this["messages"]) as? JsonArray
        else -> null
    } ?: return null
    return rows.mapNotNull { (it as? JsonObject)?.toMessageDto() }
}

/** One row, or `null` when it has no id or is not an object. */
fun JsonElement.toMessageDto(): ChatMessageDto? {
    val row = this as? JsonObject ?: return null
    val idText = (row["id"] as? JsonPrimitive)?.contentOrNull ?: return null
    // Ids are numbers on the live table. If a table ever used text ids, a stable number keeps the row visible.
    val id = idText.toLongOrNull() ?: (idText.hashCode().toLong() and 0x7FFFFFFFL).coerceAtLeast(1L)
    return ChatMessageDto(
        id = id,
        clientId = row["client_id"].asText(),
        senderRole = row["sender_role"].asText().orEmpty(),
        type = row["type"].asText(),
        text = row["text"].asText(),
        mediaUrl = row["media_url"].asText(),
        mediaMeta = row["media_meta"].parsedJson(),
        replyTo = row["reply_to"],
        delivered = row["delivered"].asFlag(),
        read = row["read"].asFlag(),
        deliveredAt = row["delivered_at"],
        readAt = row["read_at"],
        createdAt = row["created_at"].asText().orEmpty(),
        edited = row["edited"].asFlag(),
        deleted = row["deleted"].asFlag(),
        deletedFor = row["deleted_for"].asTextList(),
        reactions = row["reactions"].parsedJson(),
        starredBy = (row["starred_by"].parsedJson() as? JsonArray)?.mapNotNull { it.asText() },
        pinned = row["pinned"],
        forwarded = row["forwarded"],
        viewOnce = row["view_once"],
        viewedAt = row["viewed_at"],
        viewOncePending = row["view_once_pending"],
        effect = row["effect"],
    )
}

/** JSON columns sometimes arrive as a text that holds JSON; open it. Anything else is kept as it is. */
private fun JsonElement?.parsedJson(): JsonElement? {
    if (this == null || this is JsonNull) return null
    val text = (this as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim() ?: return this
    if (!(text.startsWith("{") || text.startsWith("["))) return this
    return try {
        Json.parseToJsonElement(text)
    } catch (e: Exception) {
        null
    }
}

/** `deleted_for` is text like "user1" or "user1,user2"; an array is joined the same way. */
private fun JsonElement?.asTextList(): String? =
    if (this is JsonArray) mapNotNull { it.asText() }.joinToString(",").ifEmpty { null } else asText()
