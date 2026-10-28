package com.twinhearts.app.feature.meetplanner.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/*
 * Why this file exists: the exact JSON shapes of routes/meetplanner.js (plans), routes/globe.js
 * (photos), routes/media.js (upload) and routes/search.js (Overpass proxy). Read from the old
 * server code, not guessed (R-65). Ids and numbers are read as JsonPrimitive because the
 * database may send them as numbers or as text, and both must work.
 */

/** One stop inside a saved plan. Field names match what the web app writes, so both apps read each other's plans. */
@Serializable
data class StopDto(
    val id: String? = null,
    val name: String? = null,
    val cat: String? = null,
    val icon: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val address: String? = null,
    val distKm: Double? = null,
    val isAnchor: Boolean = false,
    val order: Int? = null,
)

@Serializable
data class ChecklistDto(val id: Long = 0, val text: String = "", val done: Boolean = false)

/** GeoJSON line: `coordinates` is a list of `[lng, lat]`. */
@Serializable
data class RouteGeometryDto(
    val type: String = "LineString",
    val coordinates: List<List<Double>> = emptyList(),
)

/** A row of `meetup_plans` as `GET /api/meetplanner/:coupleId` returns it. */
@Serializable
data class PlanDto(
    val id: JsonPrimitive? = null,
    val title: String? = null,
    @SerialName("meet_date") val meetDate: String? = null,
    val budget: JsonPrimitive? = null,
    @SerialName("city_name") val cityName: String? = null,
    @SerialName("mid_city") val midCity: String? = null,
    val stops: List<StopDto>? = null,
    @SerialName("route_geometry") val routeGeometry: RouteGeometryDto? = null,
    @SerialName("total_distance_km") val totalDistanceKm: JsonPrimitive? = null,
    @SerialName("total_duration_min") val totalDurationMin: JsonPrimitive? = null,
    @SerialName("travel_mode") val travelMode: String? = null,
    val checklist: List<ChecklistDto>? = null,
    val notes: String? = null,
    val status: String? = null,
    @SerialName("globe_memory_id") val globeMemoryId: JsonPrimitive? = null,
)

/** `POST /api/meetplanner` body. `plan` uses the camelCase names the route reads. */
@Serializable
data class CreatePlanBody(
    val coupleId: String,
    val plan: NewPlanDto,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class NewPlanDto(
    val title: String,
    val meetDate: String? = null,
    val budget: Double? = null,
    val currency: String,
    val cityName: String? = null,
    val cityLat: Double? = null,
    val cityLng: Double? = null,
    val stops: List<StopDto>,
    val routeGeometry: RouteGeometryDto? = null,
    val totalDistanceKm: Double,
    val totalDurationMin: Double,
    val travelMode: String,
    val checklist: List<ChecklistDto>,
    val notes: String? = null,
    val createdBy: String,
)

/**
 * `PATCH /api/meetplanner/:id` body. Only the fields we set are sent. A field that must be
 * cleared is sent as JSON `null`, which is why these are [JsonElement] (a real null would be skipped).
 */
@Serializable
data class PatchPlanBody(val coupleId: String, val plan: PatchPlanDto)

@Serializable
data class PatchPlanDto(
    val title: String? = null,
    val meetDate: JsonElement? = null,
    val budget: JsonElement? = null,
    val notes: JsonElement? = null,
    val checklist: List<ChecklistDto>? = null,
)

/** `DELETE /api/meetplanner/:id` carries the couple id in the body. */
@Serializable
data class CoupleBody(val coupleId: String)

@Serializable
data class OkDto(val ok: Boolean = true)

/** `POST /api/meetplanner/:id/complete` body. */
@Serializable
data class CompleteBody(
    val coupleId: String,
    val mood: String? = null,
    val photos: List<String> = emptyList(),
    val extraNotes: String? = null,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class CompleteDto(
    val ok: Boolean = false,
    val alreadySynced: Boolean = false,
    val globeMemoryId: JsonPrimitive? = null,
)

/** `GET /api/globe/:coupleId`: we only need each memory's id and its photos. */
@Serializable
data class GlobeMemoryDto(
    val id: JsonPrimitive? = null,
    @SerialName("globe_memory_media") val media: List<GlobeMediaDto>? = null,
)

@Serializable
data class GlobeMediaDto(val type: String? = null, val url: String? = null)

/** `POST /api/media/upload` answer. */
@Serializable
data class UploadDto(val url: String)

/** `POST /api/search/overpass`: the server runs this Overpass query and caches the answer. */
@Serializable
data class OverpassBody(val query: String)

@Serializable
data class OverpassDto(val elements: List<OverpassElementDto> = emptyList())

@Serializable
data class OverpassElementDto(
    val type: String? = null,
    val id: Long? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenterDto? = null,
    val tags: Map<String, String>? = null,
)

@Serializable
data class OverpassCenterDto(val lat: Double? = null, val lon: Double? = null)
