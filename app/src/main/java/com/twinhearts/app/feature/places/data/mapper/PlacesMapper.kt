package com.twinhearts.app.feature.places.data.mapper

import com.twinhearts.app.feature.places.domain.model.MemoryNote
import com.twinhearts.app.feature.places.domain.model.PlaceMemory
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.time.LocalDate
import java.time.format.DateTimeParseException

/*
 * Why this file exists: turns the web app's saved JSON (`placesMemories`) into [PlaceMemory] and back.
 * It never throws on odd data: a broken entry is skipped, a missing field gets a calm default.
 */

const val PLACES_KEY = "placesMemories"

private fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull

private fun JsonElement?.date(): LocalDate? = str()?.takeIf { it.isNotBlank() }?.let {
    try { LocalDate.parse(it.take(DATE_LENGTH)) } catch (e: DateTimeParseException) { null }
}

private fun JsonElement?.number(): Double? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.doubleOrNull
    ?.takeIf { !it.isNaN() && !it.isInfinite() }

private fun JsonElement?.flag(): Boolean = (this as? JsonPrimitive)?.booleanOrNull ?: false

private const val DATE_LENGTH = 10

/** Reads the whole state answer (may be JSON null or an object without our key) into places. */
fun JsonElement.toPlaces(): List<PlaceMemory> {
    val list = (this as? JsonObject)?.get(PLACES_KEY) as? JsonArray ?: return emptyList()
    return list.mapNotNull { (it as? JsonObject)?.toPlace() }
}

private fun JsonObject.toPlace(): PlaceMemory? {
    val id = get("id").str()?.takeIf { it.isNotBlank() } ?: return null
    val name = get("name").str().orEmpty()
    val photos = (get("photos") as? JsonArray).orEmpty().mapNotNull { it.str()?.takeIf { s -> s.isNotBlank() } }
    val notes = (get("memories") as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.toNote() }
    return PlaceMemory(
        id = id,
        name = name,
        location = get("location").str().orEmpty(),
        visitDate = get("visitDate").date(),
        description = get("description").str().orEmpty(),
        banner = get("banner").str()?.takeIf { it.isNotBlank() },
        photos = photos,
        lat = get("lat").number(),
        lng = get("lng").number(),
        favorite = get("favorite").flag(),
        memories = notes,
        createdAt = (get("createdAt") as? JsonPrimitive)?.longOrNull ?: 0L,
    )
}

private fun JsonObject.toNote(): MemoryNote? {
    val id = get("id").str()?.takeIf { it.isNotBlank() } ?: return null
    return MemoryNote(id = id, date = get("date").date(), text = get("text").str().orEmpty(), favorite = get("favorite").flag())
}

/** The JSON array the server keeps. Same field names as the web app so both apps stay in step. */
fun List<PlaceMemory>.toJson(): JsonArray = buildJsonArray {
    forEach { p ->
        add(
            buildJsonObject {
                put("id", JsonPrimitive(p.id))
                put("name", JsonPrimitive(p.name))
                put("location", JsonPrimitive(p.location))
                put("visitDate", JsonPrimitive(p.visitDate?.toString().orEmpty()))
                put("description", JsonPrimitive(p.description))
                put("banner", JsonPrimitive(p.banner.orEmpty()))
                put("photos", buildJsonArray { p.photos.forEach { add(JsonPrimitive(it)) } })
                put("lat", p.lat?.let { JsonPrimitive(it) } ?: JsonNull)
                put("lng", p.lng?.let { JsonPrimitive(it) } ?: JsonNull)
                put("favorite", JsonPrimitive(p.favorite))
                put(
                    "memories",
                    buildJsonArray {
                        p.memories.forEach { m ->
                            add(
                                buildJsonObject {
                                    put("id", JsonPrimitive(m.id))
                                    put("date", JsonPrimitive(m.date?.toString().orEmpty()))
                                    put("text", JsonPrimitive(m.text))
                                    put("favorite", JsonPrimitive(m.favorite))
                                },
                            )
                        }
                    },
                )
                put("createdAt", JsonPrimitive(p.createdAt))
            },
        )
    }
}

/** The partial state we POST: only our own key. */
fun List<PlaceMemory>.toStateBody(): JsonObject = buildJsonObject { put(PLACES_KEY, toJson()) }
