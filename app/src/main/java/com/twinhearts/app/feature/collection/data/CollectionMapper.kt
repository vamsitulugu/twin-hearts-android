package com.twinhearts.app.feature.collection.data

import com.twinhearts.app.feature.collection.domain.model.PhotoItem
import com.twinhearts.app.feature.collection.domain.model.VoiceItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * Reads the old server's rows one field at a time, so one odd value never empties the page
 * (same idea as the chat's lenient reader). Mirrors collection.html: loadVoices, chatMsgToVoice,
 * chatMsgToPhoto and loadFromParent.
 */
internal object CollectionMapper {

    data class ChatRows(val voices: List<VoiceItem>, val photos: List<PhotoItem>)

    /** Voice rows and image/gif rows from an `api/chat/{coupleId}` answer, hiding deleted ones. */
    fun chat(json: JsonElement, myRole: String): ChatRows {
        val rows = json.rows()
        val visible = rows.filter { row ->
            !row["deleted"].flag() && myRole !in row["deleted_for"].text().orEmpty().split(",")
        }
        val voices = visible
            .filter { it["type"].text() == "voice" && !it["media_url"].text().isNullOrBlank() }
            .mapNotNull { it.toVoice() }
            .sortedByDescending { it.createdAt }
        val photos = visible
            .filter { it["type"].text().let { t -> t == "image" || t == "gif" } && !it["media_url"].text().isNullOrBlank() }
            .mapNotNull { row ->
                val id = row["id"].text() ?: return@mapNotNull null
                PhotoItem(
                    id = "chat_$id",
                    url = row["media_url"].text().orEmpty(),
                    isVideo = false,
                    date = row["created_at"].text().orEmpty().take(10),
                    name = "Chat photo",
                    fromChat = true,
                )
            }
        return ChatRows(voices, photos)
    }

    /** Saved album photos in the shared state (`photos`). */
    fun statePhotos(state: JsonObject): List<PhotoItem> =
        (state["photos"] as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val url = o["url"].text()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            PhotoItem(
                id = o["id"].text() ?: url,
                url = url,
                isVideo = o["type"].text() == "video",
                date = o["date"].text().orEmpty(),
                name = o["name"].text().orEmpty(),
                fromChat = false,
            )
        }

    /** Exported voice messages (`voiceLibrary`). */
    fun stateLibrary(state: JsonObject): List<VoiceItem> =
        (state["voiceLibrary"] as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val id = o["id"].text() ?: return@mapNotNull null
            VoiceItem(
                id = id,
                url = (o["audioData"].text() ?: o["url"].text()),
                by = o["by"].text().orEmpty(),
                durationSec = o["durationSec"].number()?.toInt() ?: 0,
                createdAt = o["createdAt"].text() ?: o["date"].text().orEmpty(),
                starredBy = emptyList(),
            )
        }

    /** Library entries as the web saves them, so the web app can read them back. */
    fun libraryToJson(items: List<VoiceItem>): JsonArray = JsonArray(items.map { v ->
        JsonObject(
            mapOf(
                "id" to JsonPrimitive(v.id),
                "audioData" to (v.url?.let { JsonPrimitive(it) } ?: JsonNull),
                "duration" to JsonPrimitive(com.twinhearts.app.feature.collection.domain.CollectionLogic.durationLabel(v.durationSec)),
                "durationSec" to JsonPrimitive(v.durationSec),
                "date" to JsonPrimitive(v.createdAt.take(10)),
                "createdAt" to JsonPrimitive(v.createdAt),
                "by" to JsonPrimitive(v.by),
            ),
        )
    })

    private fun JsonObject.toVoice(): VoiceItem? {
        val id = this["id"].text() ?: return null
        val meta = this["media_meta"].parsed() as? JsonObject
        return VoiceItem(
            id = id,
            url = this["media_url"].text(),
            by = this["sender_role"].text().orEmpty(),
            durationSec = meta?.get("duration").number()?.toInt() ?: 0,
            createdAt = this["created_at"].text().orEmpty(),
            starredBy = (this["starred_by"].parsed() as? JsonArray).orEmpty().mapNotNull { it.text() },
        )
    }

    private fun JsonElement.rows(): List<JsonObject> {
        val arr = when (this) {
            is JsonArray -> this
            is JsonObject -> (this["data"] ?: this["messages"]) as? JsonArray
            else -> null
        }
        return arr.orEmpty().mapNotNull { it as? JsonObject }
    }

    private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull
    private fun JsonElement?.number(): Double? = (this as? JsonPrimitive)?.doubleOrNull
    private fun JsonElement?.flag(): Boolean {
        val p = this as? JsonPrimitive ?: return false
        return p.contentOrNull.let { it == "true" || it == "1" }
    }

    /** JSON columns sometimes arrive as text that holds JSON. */
    private fun JsonElement?.parsed(): JsonElement? {
        if (this == null || this is JsonNull) return null
        val text = (this as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim() ?: return this
        if (!(text.startsWith("{") || text.startsWith("["))) return this
        return try { Json.parseToJsonElement(text) } catch (e: Exception) { null }
    }
}
