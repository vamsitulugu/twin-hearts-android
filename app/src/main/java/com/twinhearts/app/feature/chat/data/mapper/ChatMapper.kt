package com.twinhearts.app.feature.chat.data.mapper

import com.twinhearts.app.feature.chat.data.remote.ChatMessageDto
import com.twinhearts.app.feature.chat.data.remote.PresenceDto
import com.twinhearts.app.feature.chat.data.remote.ScheduledDto
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.DeliveryState
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import com.twinhearts.app.feature.chat.domain.model.ScheduledMessage
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Why this file exists: turns the server's rows into [ChatMessage]. All the "what does this
 * odd field mean" knowledge of the old table lives here, in one place.
 */

/** `null` when the server sent a time we cannot read. Handles `Z` and `+00:00` endings. */
fun parseServerTime(text: String?): Instant? {
    if (text.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(text).toInstant()
    } catch (e: DateTimeParseException) {
        try {
            Instant.parse(text)
        } catch (e2: DateTimeParseException) {
            null
        }
    }
}

/** A message I deleted "for me" must never show again. */
fun ChatMessageDto.isHiddenFor(myRole: String): Boolean =
    deletedFor.orEmpty().split(',').any { it.trim() == myRole }

fun ChatMessageDto.toDomain(myRole: String): ChatMessage? {
    val created = parseServerTime(createdAt) ?: return null
    val meta = meta()
    val kind = kindOf(type)
    val delivery = when {
        read -> DeliveryState.Read
        delivered -> DeliveryState.Delivered
        else -> DeliveryState.Sent
    }
    return ChatMessage(
        id = id,
        clientId = clientId?.takeIf { it.isNotBlank() } ?: "id-$id",
        mine = senderRole == myRole,
        kind = kind,
        text = if (deleted) null else text?.takeIf { it.isNotEmpty() },
        mediaUrl = if (deleted) null else mediaUrl,
        groupUrls = if (kind == MessageKind.ImageGroup) groupUrls(meta) else emptyList(),
        fileName = meta?.get("name")?.stringOrNull(),
        durationSec = meta?.get("duration")?.numberOrNull()?.toInt() ?: 0,
        replyToId = replyTo?.stringOrNull()?.toLongOrNull(),
        createdAt = created,
        delivery = delivery,
        edited = edited,
        deleted = deleted,
        starred = starredBy.orEmpty().contains(myRole),
        reactions = reactionEmojis(reactions),
        viewOncePending = viewOncePending,
    )
}

fun List<ChatMessageDto>.toDomain(myRole: String): List<ChatMessage> =
    filterNot { it.isHiddenFor(myRole) }.mapNotNull { it.toDomain(myRole) }

private fun ChatMessageDto.meta(): JsonObject? = mediaMeta as? JsonObject

private fun kindOf(type: String?): MessageKind = when (type) {
    null, "text" -> MessageKind.Text
    "image" -> MessageKind.Image
    "image_group" -> MessageKind.ImageGroup
    "video" -> MessageKind.Video
    "voice" -> MessageKind.Voice
    "audio" -> MessageKind.Audio
    "file" -> MessageKind.File
    "gif" -> MessageKind.Gif
    "sticker" -> MessageKind.Sticker
    else -> MessageKind.Other
}

private fun groupUrls(meta: JsonObject?): List<String> =
    (meta?.get("items") as? JsonArray)
        ?.mapNotNull { (it as? JsonObject)?.get("url")?.stringOrNull() }
        .orEmpty()

private fun reactionEmojis(element: JsonElement?): List<String> {
    val obj = element as? JsonObject ?: return emptyList()
    return obj.entries.filter { (it.value as? JsonArray)?.isNotEmpty() == true }.map { it.key }
}

private fun JsonElement.stringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull
private fun JsonElement.numberOrNull(): Double? = (this as? JsonPrimitive)?.doubleOrNull

fun ScheduledDto.toDomain(): ScheduledMessage? {
    val time = parseServerTime(sendAt) ?: return null
    return ScheduledMessage(id = id.content, text = text.orEmpty(), sendAt = time)
}

/** The partner's row out of the presence rows the server returns (one per person). */
fun List<PresenceDto>.forRole(role: String): PresenceDto? = firstOrNull { it.role == role }
