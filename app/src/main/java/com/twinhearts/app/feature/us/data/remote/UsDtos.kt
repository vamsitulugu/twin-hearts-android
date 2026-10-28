package com.twinhearts.app.feature.us.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class GetStateResponse(val state: JsonElement?)

@Serializable
data class SaveStateBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class SaveStateResponse(val ok: Boolean? = null)

@Serializable
data class BucketItemDto(
    val id: String?,
    val title: String?,
    val emoji: String?,
    val desc: String?,
    val target: String?,
    val priority: String?,
    val progress: Int?,
    val done: Boolean?,
    val by: String?,
)

@Serializable
data class MediaItemDto(
    val id: String?,
    val type: String?,
    val url: String?,
    val by: String?,
    val ts: Long?,
    val duration: Int?,
)

/** Local JSON helpers so no cross-module imports are needed. */
private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull ?: ""
private fun JsonElement?.intValue(): Int? = (this as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
private fun JsonElement?.longValue(): Long? = (this as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
private fun JsonElement?.boolValue(): Boolean? = (this as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull()

internal fun JsonObject?.bucketList(): List<BucketItemDto> {
    val arr = this?.get("bucket") as? JsonArray ?: return emptyList()
    return arr.mapNotNull { (it as? JsonObject)?.let {
        BucketItemDto(
            id = it["id"]?.text()?.takeIf { it.isNotBlank() },
            title = it["title"]?.text()?.takeIf { it.isNotBlank() },
            emoji = it["emoji"]?.text()?.takeIf { it.isNotBlank() },
            desc = it["desc"]?.text()?.takeIf { it.isNotBlank() },
            target = it["target"]?.text()?.takeIf { it.isNotBlank() },
            priority = it["priority"]?.text()?.takeIf { it.isNotBlank() },
            progress = it["progress"]?.intValue()?.takeIf { it in 0..100 },
            done = it["done"]?.boolValue(),
            by = it["by"]?.text()?.takeIf { it.isNotBlank() },
        )
    }}
}

internal fun JsonObject?.mediaItems(): List<MediaItemDto> {
    val arr = this?.get("photos") as? JsonArray ?: return emptyList()
    return arr.mapNotNull { (it as? JsonObject)?.let {
        MediaItemDto(
            id = it["id"]?.text()?.takeIf { it.isNotBlank() },
            type = it["type"]?.text()?.takeIf { it.isNotBlank() },
            url = it["url"]?.text()?.takeIf { it.isNotBlank() },
            by = it["by"]?.text()?.takeIf { it.isNotBlank() },
            ts = it["ts"]?.longValue(),
            duration = it["duration"]?.intValue(),
        )
    }}
}
