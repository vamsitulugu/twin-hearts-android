package com.twinhearts.app.feature.globe.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/*
 * Why this file exists: the exact JSON shapes of the old server's /api/globe, /api/media,
 * /api/location and /api/pro routes (routes/globe.js, media.js, location.js, pro.js) and of the
 * free map services. Numbers that the database may return as text or number are read as JsonElement.
 */

/** One row of `globe_memories` with its `globe_memory_media` rows. */
@Serializable
data class MemoryDto(
    val id: String? = null,
    val city: String? = null,
    val country: String? = null,
    val state: String? = null,
    val lat: JsonElement? = null,
    val lng: JsonElement? = null,
    @SerialName("trip_name") val tripName: String? = null,
    @SerialName("date_from") val dateFrom: String? = null,
    @SerialName("date_to") val dateTo: String? = null,
    val mood: String? = null,
    val weather: String? = null,
    val temperature: JsonElement? = null,
    @SerialName("trip_cost") val tripCost: JsonElement? = null,
    val currency: String? = null,
    val notes: String? = null,
    @SerialName("favorite_moment") val favoriteMoment: String? = null,
    val restaurants: List<String>? = null,
    val hotels: List<String>? = null,
    val gifts: List<String>? = null,
    @SerialName("globe_memory_media") val media: List<MediaDto>? = null,
)

@Serializable
data class MediaDto(
    val id: JsonElement? = null,
    val type: String? = null,
    val url: String? = null,
    @SerialName("data_url") val dataUrl: String? = null,
    val duration: JsonElement? = null,
    @SerialName("journal_text") val journalText: String? = null,
)

/** POST /api/globe and PUT /api/globe/:id. [memory] keeps explicit nulls so a cleared date is really cleared. */
@Serializable
data class SaveMemoryBody(
    val coupleId: String,
    val memory: JsonObject,
    val senderRole: String? = null,
    val myName: String? = null,
)

/** POST /api/globe/:memoryId/media (one item). */
@Serializable
data class AddMediaBody(
    val coupleId: String,
    val media: JsonObject,
    val senderRole: String? = null,
    val myName: String? = null,
)

/** Answer of create / update: only the id is needed, the list is reloaded afterwards. */
@Serializable
data class SavedMemoryDto(val id: String? = null)

@Serializable
data class OkDto(val success: Boolean? = null)

/** `{ url, path }` from /api/media/upload and /api/media/upload-voice. */
@Serializable
data class UploadDto(val url: String, val path: String? = null)

@Serializable
data class ProStatusDto(val isPro: Boolean = false)

// ── live location (routes/location.js) ───────────────────────────────────────────────────────

@Serializable
data class LocationsDto(val user1: LocationRowDto? = null, val user2: LocationRowDto? = null)

@Serializable
data class LocationRowDto(
    val lat: Double? = null,
    val lng: Double? = null,
    val online: Boolean? = null,
)

@Serializable
data class PingBody(
    val coupleId: String,
    val role: String,
    val lat: Double,
    val lng: Double,
    val accuracy: Double? = null,
    val heading: Double? = null,
    val speed: Double? = null,
    val moving: Boolean = false,
)

@Serializable
data class StopBody(val coupleId: String, val role: String)

@Serializable
data class TrailPointDto(val lat: Double? = null, val lng: Double? = null)

// ── free map services ────────────────────────────────────────────────────────────────────────

@Serializable
data class NominatimHitDto(
    val lat: String? = null,
    val lon: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val name: String? = null,
    val address: NominatimAddressDto? = null,
)

@Serializable
data class NominatimAddressDto(
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val county: String? = null,
    val state: String? = null,
    val country: String? = null,
)

@Serializable
data class OpenMeteoDto(@SerialName("current_weather") val current: CurrentWeatherDto? = null)

@Serializable
data class CurrentWeatherDto(val temperature: Double? = null, val weathercode: Int? = null)

@Serializable
data class OsrmDto(val routes: List<OsrmRouteDto>? = null)

@Serializable
data class OsrmRouteDto(val geometry: OsrmGeometryDto? = null)

@Serializable
data class OsrmGeometryDto(val coordinates: List<List<Double>>? = null)
