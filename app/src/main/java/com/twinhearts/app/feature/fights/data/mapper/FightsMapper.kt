package com.twinhearts.app.feature.fights.data.mapper

import com.twinhearts.app.feature.fights.domain.model.Fight
import com.twinhearts.app.feature.fights.domain.model.FightForm
import com.twinhearts.app.feature.fights.domain.model.FightStatus
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Why this file exists: turns the shared-state `fights` list (written by the web page too) into [Fight]s and back. */

/** The `fights` array of the shared state, or empty. Accepts the state with or without a `state` wrapper. */
fun JsonElement?.fightsArray(): JsonArray {
    val root = this as? JsonObject ?: return JsonArray(emptyList())
    val direct = root["fights"] as? JsonArray
    if (direct != null) return direct
    return ((root["state"] as? JsonObject)?.get("fights") as? JsonArray) ?: JsonArray(emptyList())
}

private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull.orEmpty()

private fun JsonElement?.texts(): List<String> =
    (this as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()

/** Ids are `Date.now()` numbers on the web; text ids are accepted too. Entries without an id are skipped. */
fun JsonObject.fightId(): Long? {
    val p = this["id"] as? JsonPrimitive ?: return null
    return p.longOrNull ?: p.contentOrNull?.toLongOrNull()
}

fun JsonObject.toFight(): Fight? {
    val id = fightId() ?: return null
    return Fight(
        id = id,
        title = this["title"].text(),
        reason = this["reason"].text(),
        category = this["category"].text().ifBlank { "Other" },
        intensity = ((this["intensity"] as? JsonPrimitive)?.intOrNull ?: 5).coerceIn(1, 10),
        startedBy = this["startedBy"].text(),
        feelings = this["feelings"].texts(),
        need = this["need"].text(),
        approach = this["approach"].texts(),
        notes = this["notes"].text(),
        status = if (this["status"].text() == "resolved") FightStatus.Resolved else FightStatus.Open,
        date = this["date"].text(),
        resolvedDate = this["resolvedDate"].text().ifBlank { null },
    )
}

fun JsonArray.toFights(): List<Fight> = mapNotNull { (it as? JsonObject)?.toFight() }

/** A new fight in the same shape the web page saves (`S.fights.unshift({...})`). */
fun newFightJson(id: Long, form: FightForm, startedBy: String, today: String, role: String): JsonObject = buildJsonObject {
    put("id", id)
    put("title", form.title.trim())
    put("reason", form.reason.trim())
    put("category", form.category)
    put("intensity", form.intensity)
    put("startedBy", startedBy)
    put("feelings", buildJsonArray { form.feelings.forEach { add(JsonPrimitive(it)) } })
    put("need", form.need.trim())
    put("approach", buildJsonArray { form.approach.forEach { add(JsonPrimitive(it)) } })
    put("notes", form.notes.trim())
    put("status", if (form.status == FightStatus.Resolved) "resolved" else "open")
    put("date", today)
    if (form.status == FightStatus.Resolved) put("resolvedDate", today)
    put("visibility", "both")
    put("by", role)
}

/** The same fight marked resolved. Every other field (even ones this app does not know) is kept. */
fun JsonObject.asResolved(today: String): JsonObject = JsonObject(
    this + mapOf("status" to JsonPrimitive("resolved"), "resolvedDate" to JsonPrimitive(today)),
)
