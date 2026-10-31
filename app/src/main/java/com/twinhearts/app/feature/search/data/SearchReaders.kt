package com.twinhearts.app.feature.search.data

import com.twinhearts.app.feature.search.domain.StateSource
import com.twinhearts.app.feature.search.domain.canSeeItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/*
 * Why this file exists: defensive readers for the raw server JSON. A row we cannot read is
 * skipped, never fatal (same idea as Chat's ChatLenient.kt).
 */

private const val MAX_FIELD_CHARS = 600

/** One plain text value, or "" when missing, not text, a link or an embedded picture. */
internal fun JsonElement?.text(): String {
    val value = (this as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
    if (value.startsWith("data:") || value.startsWith("http://") || value.startsWith("https://")) return ""
    return value.take(MAX_FIELD_CHARS)
}

private fun JsonElement?.flag(): Boolean =
    (this as? JsonPrimitive)?.let { it.booleanOrNull ?: (it.contentOrNull == "1") } == true

/** One message the server found: its id, text, who sent it and when. */
internal data class FoundMessage(val id: String, val text: String, val senderRole: String, val createdAt: String)

/** Rows of a chat-search answer: a list, or `{ data: [...] }` / `{ messages: [...] }`. */
internal fun JsonElement.toFoundMessages(role: String): List<FoundMessage> {
    val rows: JsonArray = when (this) {
        is JsonArray -> this
        is JsonObject -> (this["data"] ?: this["messages"]) as? JsonArray
        else -> null
    } ?: return emptyList()
    return rows.mapNotNull { element ->
        val row = element as? JsonObject ?: return@mapNotNull null
        val id = row["id"].text().ifBlank { return@mapNotNull null }
        val body = row["text"].text().ifBlank { return@mapNotNull null }
        if (row["deleted"].flag()) return@mapNotNull null
        // "deleted for me" is stored as a list (or JSON text) of roles; never show those.
        if (role.isNotBlank() && row["deleted_for"].toString().contains(role)) return@mapNotNull null
        FoundMessage(id, body, row["sender_role"].text(), row["created_at"].text())
    }
}

/** One readable item of a saved list, ready to be matched. */
internal data class StateItem(val id: String, val title: String, val body: String, val date: String)

/** The items of [source] I may see: privacy rules applied, rows without text skipped. */
internal fun JsonObject.itemsOf(source: StateSource, role: String): List<StateItem> {
    val list = this[source.key] as? JsonArray ?: return emptyList()
    return list.mapIndexedNotNull { index, element ->
        val obj = element as? JsonObject ?: return@mapIndexedNotNull null
        val by = obj["by"].text()
        val mine = obj["me"].flag()
        if (source.mineOnly && by != role && !mine) return@mapIndexedNotNull null
        val visibility = obj["visibility"].text().ifBlank { obj["forWhom"].text() }
        if (!canSeeItem(visibility, by, mine, role)) return@mapIndexedNotNull null
        val title = source.titleFields.firstNotNullOfOrNull { obj[it].text().ifBlank { null } }
            ?: return@mapIndexedNotNull null
        val body = source.bodyFields.mapNotNull { obj[it].text().ifBlank { null } }.joinToString(" ")
        StateItem(
            id = obj["id"].text().ifBlank { index.toString() },
            title = title,
            body = body,
            date = obj["date"].text(),
        )
    }
}
