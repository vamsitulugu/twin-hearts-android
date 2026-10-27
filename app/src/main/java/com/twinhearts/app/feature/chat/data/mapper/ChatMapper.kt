package com.twinhearts.app.feature.chat.data.mapper

import kotlinx.serialization.json.booleanOrNull
import com.twinhearts.app.feature.chat.data.remote.ChatMessageDto
import com.twinhearts.app.feature.chat.data.remote.GifDto
import com.twinhearts.app.feature.chat.data.remote.PresenceDto
import com.twinhearts.app.feature.chat.data.remote.ScheduledDto
import com.twinhearts.app.feature.chat.data.remote.WallpaperDto
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.DeliveryState
import com.twinhearts.app.feature.chat.domain.model.GifItem
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import com.twinhearts.app.feature.chat.domain.model.ReactionGroup
import com.twinhearts.app.feature.chat.domain.model.ScheduledMessage
import com.twinhearts.app.feature.chat.domain.model.Wallpaper
import com.twinhearts.app.feature.chat.domain.model.WallpaperMode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
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
    val isViewOnce = viewOnce.asFlag()
    val hiddenMedia = mediaUrl == null && isViewOnce
    return ChatMessage(
        id = id,
        clientId = clientId?.takeIf { it.isNotBlank() } ?: "id-$id",
        mine = senderRole == myRole,
        kind = kind,
        text = if (deleted) null else text?.takeIf { it.isNotEmpty() },
        mediaUrl = if (deleted) null else mediaUrl,
        groupUrls = if (kind == MessageKind.ImageGroup) groupUrls(meta, mediaUrl) else emptyList(),
        fileName = meta?.get("name")?.stringOrNull(),
        fileSize = meta?.get("size")?.numberOrNull()?.toLong() ?: 0L,
        durationSec = meta?.get("duration")?.numberOrNull()?.toInt() ?: 0,
        replyToId = replyTo?.stringOrNull()?.toLongOrNull(),
        createdAt = created,
        delivery = delivery,
        deliveredAt = parseServerTime(deliveredAt.asText()),
        readAt = parseServerTime(readAt.asText()),
        edited = edited,
        deleted = deleted,
        starred = starredBy.orEmpty().contains(myRole),
        pinned = pinned.asFlag(),
        forwarded = forwarded.asFlag(),
        reactions = reactionGroups(reactions, myRole),
        viewOnce = isViewOnce,
        viewOncePending = viewOncePending.asFlag() || (hiddenMedia && viewedAt.asText() == null),
        viewed = viewedAt.asText() != null,
        emoji = meta?.get("emoji")?.stringOrNull(),
        label = meta?.get("name")?.stringOrNull(),
        latitude = meta?.get("lat")?.numberOrNull(),
        longitude = meta?.get("lng")?.numberOrNull(),
        pollOptions = (meta?.get("options") as? JsonArray)?.mapNotNull { it.stringOrNull() }.orEmpty(),
        effect = effect.asText(),
        rawType = type,
        metaJson = mediaMeta?.takeIf { it !is JsonNull }?.toString(),
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
    "location" -> MessageKind.Location
    "gift" -> MessageKind.Gift
    "contact" -> MessageKind.Contact
    "poll" -> MessageKind.Poll
    "call_log" -> MessageKind.CallLog
    else -> MessageKind.Other
}

/** The pictures of a group; an old row without `items` still shows its single `media_url`. */
private fun groupUrls(meta: JsonObject?, mediaUrl: String?): List<String> {
    val items = (meta?.get("items") as? JsonArray)
        ?.mapNotNull { (it as? JsonObject)?.get("url")?.stringOrNull() }
        .orEmpty()
    return items.ifEmpty { listOfNotNull(mediaUrl) }
}

/** `{ "❤️": ["user1", "user2"] }` to pills, in the order the server sent them. */
fun reactionGroups(element: JsonElement?, myRole: String): List<ReactionGroup> {
    val obj = element as? JsonObject ?: return emptyList()
    return obj.entries.mapNotNull { (emoji, roles) ->
        val list = (roles as? JsonArray)?.mapNotNull { it.stringOrNull() }.orEmpty()
        if (list.isEmpty()) null else ReactionGroup(emoji, list.size, list.contains(myRole))
    }
}

private fun JsonElement.stringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull
private fun JsonElement.numberOrNull(): Double? = (this as? JsonPrimitive)?.doubleOrNull

fun ScheduledDto.toDomain(): ScheduledMessage? {
    val time = parseServerTime(sendAt) ?: return null
    return ScheduledMessage(id = id.content, text = text.orEmpty(), sendAt = time)
}

/** The partner's row out of the presence rows the server returns (one per person). */
fun List<PresenceDto>.forRole(role: String): PresenceDto? = firstOrNull { it.role == role }

fun WallpaperDto.toDomain(): Wallpaper = Wallpaper(
    mode = WallpaperMode.fromApi(mode),
    swatch = swatch,
    image = imageUrl,
    dim = dim.coerceIn(0, MAX_DIM),
)

fun GifDto.toDomain(): GifItem? {
    val url = images?.small?.url ?: images?.fixed?.url ?: images?.downsized?.url ?: return null
    return GifItem(id = id.ifBlank { url }, url = url)
}

const val MAX_DIM = 90

/** Text of a JSON value, or null for missing, null or non-text values. */
internal fun JsonElement?.asText(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull

/** True for `true`, `"true"`, `"t"`, `1`; false for anything else, so an odd server value never breaks the list. */
internal fun JsonElement?.asFlag(): Boolean {
    val p = this as? JsonPrimitive ?: return false
    return p.booleanOrNull ?: (p.contentOrNull?.lowercase() in setOf("true", "t", "1"))
}
