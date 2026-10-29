package com.twinhearts.app.feature.globe.data.mapper

import com.twinhearts.app.feature.globe.data.remote.LocationRowDto
import com.twinhearts.app.feature.globe.data.remote.MediaDto
import com.twinhearts.app.feature.globe.data.remote.MemoryDto
import com.twinhearts.app.feature.globe.data.remote.NominatimHitDto
import com.twinhearts.app.feature.globe.data.remote.OpenMeteoDto
import com.twinhearts.app.feature.globe.data.remote.OsrmDto
import com.twinhearts.app.feature.globe.domain.parseCost
import com.twinhearts.app.feature.globe.domain.weatherChoiceFor
import com.twinhearts.app.feature.globe.domain.model.GeoPoint
import com.twinhearts.app.feature.globe.domain.model.LivePerson
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.Memory
import com.twinhearts.app.feature.globe.domain.model.MemoryDraft
import com.twinhearts.app.feature.globe.domain.model.MemoryMedia
import com.twinhearts.app.feature.globe.domain.model.PlaceHit
import com.twinhearts.app.feature.globe.domain.model.WeatherNow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.math.roundToInt

/*
 * Why this file exists: turns the server's JSON shapes into the plain models the screens use,
 * and the form's draft into the JSON the server expects (same field names as the web app).
 */

private fun JsonElement?.text(): String? = when (this) {
    null, JsonNull -> null
    is JsonPrimitive -> content
    else -> null
}

private fun JsonElement?.number(): Double? = text()?.toDoubleOrNull()

/** A memory without an id or a position cannot be pinned, so it is skipped. */
fun MemoryDto.toDomain(): Memory? {
    val memoryId = id ?: return null
    val latitude = lat.number() ?: return null
    val longitude = lng.number() ?: return null
    return Memory(
        id = memoryId,
        city = city.orEmpty(),
        country = country.orEmpty(),
        state = state.orEmpty(),
        lat = latitude,
        lng = longitude,
        tripName = tripName.orEmpty(),
        dateFrom = dateFrom?.take(DATE_LENGTH)?.takeIf { it.isNotBlank() },
        dateTo = dateTo?.take(DATE_LENGTH)?.takeIf { it.isNotBlank() },
        mood = mood.orEmpty(),
        weather = weather.orEmpty(),
        temperature = temperature.text().orEmpty(),
        tripCost = tripCost.number()?.takeIf { it != 0.0 },
        currency = currency.orEmpty(),
        notes = notes.orEmpty(),
        favoriteMoment = favoriteMoment.orEmpty(),
        restaurants = restaurants.orEmpty(),
        hotels = hotels.orEmpty(),
        gifts = gifts.orEmpty(),
        media = media.orEmpty().mapNotNull { it.toDomain() },
    )
}

private const val DATE_LENGTH = 10

fun MediaDto.toDomain(): MemoryMedia? {
    val mediaId = id.text() ?: return null
    val kind = MediaKind.fromWire(type) ?: return null
    return MemoryMedia(
        id = mediaId,
        kind = kind,
        url = url ?: dataUrl.orEmpty(),
        duration = duration.text().orEmpty().takeIf { it != "—" }.orEmpty(),
        journalText = journalText.orEmpty(),
    )
}

/** The form's draft as the `memory` object of POST / PUT /api/globe (field names of the web app). */
fun MemoryDraft.toWire(role: String): JsonObject = buildJsonObject {
    put("city", city.trim())
    put("country", country.trim())
    put("state", state)
    put("lat", lat)
    put("lng", lng)
    put("trip_name", tripName.trim())
    put("date_from", dateFrom.ifBlank { null })
    put("date_to", dateTo.ifBlank { null })
    put("mood", mood)
    put("weather", weather)
    put("temperature", temperature.trim())
    put("trip_cost", parseCost(cost))
    put("currency", currency)
    put("notes", notes.trim())
    put("favorite_moment", favoriteMoment.trim())
    putJsonArray("restaurants") { restaurants.forEach { add(it) } }
    putJsonArray("hotels") { hotels.forEach { add(it) } }
    putJsonArray("gifts") { gifts.forEach { add(it) } }
    put("by_role", role)
}

/** The `media` object of POST /api/globe/:id/media. */
fun mediaWire(kind: MediaKind, url: String?, duration: String?, journalText: String?): JsonObject = buildJsonObject {
    put("type", kind.wire)
    if (kind == MediaKind.Journal) {
        put("journal_text", journalText)
    } else {
        put("url", url)
        put("duration", duration)
    }
}

fun NominatimHitDto.toPlace(query: String): PlaceHit? {
    val latitude = lat?.toDoubleOrNull() ?: return null
    val longitude = lon?.toDoubleOrNull() ?: return null
    val a = address
    return PlaceHit(
        city = a?.city ?: a?.town ?: a?.village ?: a?.county ?: name ?: query,
        country = a?.country.orEmpty(),
        state = a?.state.orEmpty(),
        lat = latitude,
        lng = longitude,
        displayName = displayName.orEmpty(),
    )
}

fun OpenMeteoDto.toWeather(): WeatherNow? {
    val now = current ?: return null
    val temp = now.temperature ?: return null
    return WeatherNow(option = now.weathercode?.let { weatherChoiceFor(it) }, temperatureC = temp.roundToInt())
}

fun OsrmDto.toRoad(): List<GeoPoint>? {
    val coords = routes?.firstOrNull()?.geometry?.coordinates ?: return null
    val points = coords.mapNotNull { if (it.size >= 2) GeoPoint(it[1], it[0]) else null }
    return points.takeIf { it.size >= 2 }
}

fun LocationRowDto.toPerson(): LivePerson? {
    val latitude = lat ?: return null
    val longitude = lng ?: return null
    return LivePerson(GeoPoint(latitude, longitude), online == true)
}
