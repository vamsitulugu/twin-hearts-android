package com.twinhearts.app.feature.livemap.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Shapes of the old server's answers and bodies for the Live Map (R-65). Read from
 * routes/location.js, routes/route.js and routes/tracking.js. Every field is optional on the way in,
 * because the server leaves out what it does not know.
 */

/** GET /api/location/{coupleId}: `{ user1: Row|null, user2: Row|null }`. */
@Serializable
data class LocationsResponse(
    val user1: LocationRowDto? = null,
    val user2: LocationRowDto? = null,
)

@Serializable
data class LocationRowDto(
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracy: Double? = null,
    val heading: Double? = null,
    val speed: Double? = null,
    val moving: Boolean? = null,
    val updatedAt: String? = null,
    val online: Boolean? = null,
    val status: String? = null,
    val emergency: Boolean? = null,
    val vehicleType: String? = null,
)

/** POST /api/location/ping. Needs coupleId, role, lat, lng; everything else is optional. */
@Serializable
data class PingBody(
    val coupleId: String,
    val role: String,
    val lat: Double,
    val lng: Double,
    val accuracy: Double? = null,
    val heading: Double? = null,
    val speed: Double? = null,
    val altitude: Double? = null,
    val moving: Boolean? = null,
    val emergency: Boolean? = null,
    val vehicleType: String? = null,
    val mockLocation: Boolean? = null,
    val activityType: String? = null,
    val localDate: String? = null,
    val source: String? = null,
)

/** POST /api/location/status: pause or resume sharing. `untilMinutes` is only used with "paused". */
@Serializable
data class StatusBody(
    val coupleId: String,
    val role: String,
    val status: String,
    val untilMinutes: Int? = null,
)

/** POST /api/location/stop: marks this phone offline at once. */
@Serializable
data class StopBody(val coupleId: String, val role: String)

/** POST /api/tracking/batch: points saved while offline, sent in one go (max 2000). */
@Serializable
data class BatchBody(
    val coupleId: String,
    val role: String,
    val points: List<BatchPointDto>,
)

@Serializable
data class BatchPointDto(
    val lat: Double,
    val lng: Double,
    val accuracy: Double? = null,
    val heading: Double? = null,
    val speed: Double? = null,
    val altitude: Double? = null,
    val moving: Boolean? = null,
    val mockLocation: Boolean? = null,
    val localDate: String? = null,
    val ts: Long? = null,
)

/** POST /api/tracking/safety-alert. `type` is battery_low | gps_disabled | permission_revoked | internet_lost. */
@Serializable
data class SafetyAlertBody(
    val coupleId: String,
    val role: String,
    val type: String,
    val senderName: String,
)

/** Generic `{ ok: true }` answer. */
@Serializable
data class OkResponse(val ok: Boolean? = null)

/** GET /api/location/{coupleId}/trail/{role}: the last 60 breadcrumb points, oldest first. */
@Serializable
data class TrailPointDto(
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/** GET /api/route/{coupleId}/{role}/dates. */
@Serializable
data class RouteDatesResponse(val dates: List<String> = emptyList())

/** GET /api/route/{coupleId}/{role}/{date}. */
@Serializable
data class RouteDayResponse(
    val date: String? = null,
    val points: List<RoutePointDto> = emptyList(),
    val stats: RouteStatsDto? = null,
    val stops: List<RouteStopDto> = emptyList(),
)

@Serializable
data class RoutePointDto(
    val lat: Double? = null,
    val lng: Double? = null,
    val speed: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class RouteStatsDto(
    val distanceKm: Double? = null,
    val durationMin: Int? = null,
    val pointCount: Int? = null,
)

@Serializable
data class RouteStopDto(
    val lat: Double? = null,
    val lng: Double? = null,
    val arrivedAt: String? = null,
    val leftAt: String? = null,
    val minutes: Int? = null,
)

/** GET /api/route/{coupleId}/{role}/settings and the answer of POST /api/route/settings. */
@Serializable
data class RouteSettingsResponse(
    val enabled: Boolean? = null,
    val retentionDays: Int? = null,
)

/** POST /api/route/settings. retentionDays: 7, 30 or null (forever). */
@Serializable
data class RouteSettingsBody(
    val coupleId: String,
    val role: String,
    val enabled: Boolean,
    val retentionDays: Int? = null,
)

/** GET /api/tracking/{coupleId}/{role}/timeline/{date}. */
@Serializable
data class TimelineResponse(val events: List<TimelineEventDto> = emptyList())

@Serializable
data class TimelineEventDto(
    val id: String? = null,
    val label: String? = null,
    val category: String? = null,
    val arrivedAt: String? = null,
    val leftAt: String? = null,
    val durationMin: Int? = null,
    val ongoing: Boolean? = null,
)

/** One row of GET /api/tracking/{coupleId}/{role}/geofence-events (table geofence_events). */
@Serializable
data class GeofenceEventDto(
    val label: String? = null,
    @SerialName("event_type") val eventType: String? = null,
    @SerialName("occurred_at") val occurredAt: String? = null,
)

/** POST /api/data/state. The server merges the top-level keys of [state] (routes/data.js). */
@Serializable
data class SaveStateBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

/** OSRM answer (router.project-osrm.org). */
@Serializable
data class OsrmResponse(val routes: List<OsrmRouteDto> = emptyList())

@Serializable
data class OsrmRouteDto(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val geometry: OsrmGeometryDto? = null,
    val legs: List<OsrmLegDto> = emptyList(),
)

/** One leg of a route (between two waypoints) with its turn-by-turn steps. */
@Serializable
data class OsrmLegDto(val steps: List<OsrmStepDto> = emptyList())

@Serializable
data class OsrmStepDto(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val name: String = "",
    val ref: String = "",
    val geometry: OsrmGeometryDto? = null,
    val maneuver: OsrmManeuverDto? = null,
)

/** The manoeuvre at the start of a step. [location] is `[lng, lat]`. */
@Serializable
data class OsrmManeuverDto(
    val type: String = "",
    val modifier: String? = null,
    val exit: Int? = null,
    val location: List<Double> = emptyList(),
)

/** GeoJSON line: each coordinate is `[lng, lat]`. */
@Serializable
data class OsrmGeometryDto(val coordinates: List<List<Double>> = emptyList())

/** Photon search answer (photon.komoot.io). */
@Serializable
data class PhotonResponse(val features: List<PhotonFeatureDto> = emptyList())

@Serializable
data class PhotonFeatureDto(
    val geometry: PhotonGeometryDto? = null,
    val properties: PhotonPropertiesDto? = null,
)

/** GeoJSON point: `[lng, lat]`. */
@Serializable
data class PhotonGeometryDto(val coordinates: List<Double> = emptyList())

@Serializable
data class PhotonPropertiesDto(
    val name: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val district: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
)

/** Nominatim reverse answer, only the parts the "Near …" card needs. */
@Serializable
data class NominatimReverseDto(
    val name: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val address: NominatimAddressDto? = null,
)

@Serializable
data class NominatimAddressDto(
    val neighbourhood: String? = null,
    val suburb: String? = null,
    val road: String? = null,
    val village: String? = null,
    val town: String? = null,
    val city: String? = null,
    @SerialName("city_district") val cityDistrict: String? = null,
    val county: String? = null,
    val state: String? = null,
)

/** Open-Meteo answer, only the fields the weather card shows. */
@Serializable
data class OpenMeteoResponse(
    val current: OpenMeteoCurrentDto? = null,
    val daily: OpenMeteoDailyDto? = null,
)

@Serializable
data class OpenMeteoCurrentDto(
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("relative_humidity_2m") val humidity: Double? = null,
    @SerialName("wind_speed_10m") val wind: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    val precipitation: Double? = null,
)

@Serializable
data class OpenMeteoDailyDto(
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
)
