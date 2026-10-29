package com.twinhearts.app.feature.surprises.data.mapper

import com.twinhearts.app.feature.surprises.domain.mediaKindOf
import com.twinhearts.app.feature.surprises.domain.model.Capsule
import com.twinhearts.app.feature.surprises.domain.model.MediaKind
import com.twinhearts.app.feature.surprises.domain.model.Surprise
import com.twinhearts.app.feature.surprises.domain.model.SurprisesData
import com.twinhearts.app.feature.surprises.domain.newestFirstCapsules
import com.twinhearts.app.feature.surprises.domain.newestFirstSurprises
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate

/**
 * Why this file exists: turns the web app's JSON (`surprises`, `capsules`, `xp`) into plain models
 * and back. Field names are the web's own (`addSurprise()` / `addCapsule()` in index.html), so
 * both apps read each other's items. Items are always handled as raw JSON when saved, so a field
 * this app does not know about is never lost.
 */

const val KEY_SURPRISES = "surprises"
const val KEY_CAPSULES = "capsules"
const val KEY_XP = "xp"

/** Same rewards as the web's `addXP()` calls. */
const val XP_SURPRISE = 10L
const val XP_CAPSULE = 15L

internal fun JsonElement?.text(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()

internal fun JsonObject.rawList(key: String): List<JsonElement> = (this[key] as? JsonArray)?.toList().orEmpty()

internal fun JsonObject.xp(): Long = (this[KEY_XP] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()?.toLong() ?: 0L

internal fun JsonElement.idOrEmpty(): String = (this as? JsonObject)?.get("id").text()

private fun parseDate(raw: String): LocalDate? = runCatching { LocalDate.parse(raw.take(DATE_LENGTH)) }.getOrNull()

private const val DATE_LENGTH = 10 // "yyyy-MM-dd"

/** Skips an item with no title or no readable date, so one bad item never hides the rest. */
fun JsonObject.toSurprisesData(myRole: String): SurprisesData {
    val surprises = rawList(KEY_SURPRISES).mapNotNull { item ->
        val obj = item as? JsonObject ?: return@mapNotNull null
        val unlock = parseDate(obj["unlock"].text()) ?: return@mapNotNull null
        val id = obj["id"].text().ifBlank { return@mapNotNull null }
        val url = obj["mediaUrl"].text()
        val emoji = obj["type"].text()
        Surprise(
            id = id,
            emoji = emoji,
            title = obj["title"].text(),
            message = obj["msg"].text(),
            mediaUrl = url,
            mediaKind = mediaKindOf(url, obj["mediaKind"].text().ifBlank { null }, emoji),
            unlock = unlock,
            mine = obj["by"].text() == myRole,
        )
    }
    val capsules = rawList(KEY_CAPSULES).mapNotNull { item ->
        val obj = item as? JsonObject ?: return@mapNotNull null
        val unlock = parseDate(obj["unlock"].text()) ?: return@mapNotNull null
        val id = obj["id"].text().ifBlank { return@mapNotNull null }
        Capsule(
            id = id,
            message = obj["msg"].text(),
            unlock = unlock,
            opened = (obj["opened"] as? JsonPrimitive)?.booleanOrNull == true,
        )
    }
    return SurprisesData(newestFirstSurprises(surprises), newestFirstCapsules(capsules))
}

/**
 * Web `addSurprise()` item: id, type, title, msg, mediaUrl, unlock, by, opened. Two extra fields are
 * added for this app (the web ignores them): `mediaKind` so a video or voice note is recognised, and
 * `mediaPath` so deleting the surprise can also delete the uploaded file.
 */
fun surpriseJson(
    id: Long,
    emoji: String,
    title: String,
    message: String,
    unlock: LocalDate,
    role: String,
    mediaUrl: String,
    mediaKind: MediaKind?,
    mediaPath: String?,
): JsonObject = buildJsonObject {
    put("id", id)
    put("type", emoji)
    put("title", title)
    put("msg", message)
    put("mediaUrl", mediaUrl)
    put("unlock", unlock.toString())
    put("by", role)
    put("opened", false)
    if (mediaKind != null) put("mediaKind", mediaKind.key)
    if (!mediaPath.isNullOrBlank()) put("mediaPath", mediaPath)
}

/** Web `addCapsule()` item: id, msg, unlock, by, created, opened. */
fun capsuleJson(id: Long, message: String, unlock: LocalDate, role: String, created: LocalDate): JsonObject =
    buildJsonObject {
        put("id", id)
        put("msg", message)
        put("unlock", unlock.toString())
        put("by", role)
        put("created", created.toString())
        put("opened", false)
    }
