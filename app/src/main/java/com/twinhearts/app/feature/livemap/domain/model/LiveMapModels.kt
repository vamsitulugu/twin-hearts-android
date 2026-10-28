package com.twinhearts.app.feature.livemap.domain.model

/**
 * Plain Kotlin models the Live Map screens use (R-03). They are filled by the mappers in
 * data/mapper and never contain network or database types.
 */

/** Where one person is right now, as the server last saw it (GET /api/location/{coupleId}). */
data class LivePosition(
    val lat: Double,
    val lng: Double,
    val accuracyM: Double?,
    val headingDeg: Double?,
    val speedMps: Double?,
    val moving: Boolean,
    /** Epoch millis of the last ping. */
    val updatedAtMs: Long,
    /** True when the last ping is younger than the server's 60 second window. */
    val online: Boolean,
    val paused: Boolean,
    val emergency: Boolean,
    val vehicle: Vehicle,
)

/** Self-declared travel mode. The server accepts exactly these four values. */
enum class Vehicle(val wire: String) {
    Walking("walking"),
    Bike("bike"),
    Car("car"),
    Bus("bus"),
    ;

    companion object {
        fun fromWire(value: String?): Vehicle = entries.firstOrNull { it.wire == value } ?: Walking
    }
}

/** Both people on the map. Either side can be missing (never shared, or not paired). */
data class CouplePositions(
    val me: LivePosition?,
    val partner: LivePosition?,
)

/** One saved important place (Home, College ...). Stored in the shared couple state as `placesList`. */
data class SavedPlace(
    val id: String,
    /** "user1" or "user2": who added it. */
    val ownerRole: String,
    val category: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val createdAtMs: Long,
)

/** A starred place from search. Stored in the shared couple state as `favorites`. */
data class FavoritePlace(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
)

/** A love note left on the map. Stored in the shared couple state as `loveNotes`. */
data class LoveNote(
    val id: String,
    val fromRole: String,
    val message: String,
    val lat: Double,
    val lng: Double,
    val createdAtMs: Long,
)

/** Which three alert toggles of "Get notified when partner…" and which three of "Let partner know when I…". */
data class AlertPrefs(
    val arrive: Boolean = true,
    val leave: Boolean = true,
    val offline: Boolean = true,
    val battery: Boolean = true,
    val network: Boolean = true,
    val gps: Boolean = true,
)

enum class AlertKey { Arrive, Leave, Offline, Battery, Network, Gps }

/** One recorded point of a day's route. */
data class RoutePoint(
    val lat: Double,
    val lng: Double,
    val speedMps: Double?,
    val atMs: Long,
)

/** A place where someone stayed at least five minutes (computed by the server). */
data class RouteStop(
    val lat: Double,
    val lng: Double,
    val arrivedAtMs: Long,
    val leftAtMs: Long,
    val minutes: Int,
)

/** One day of route history for one person. */
data class RouteDay(
    val date: String,
    val points: List<RoutePoint>,
    val distanceKm: Double,
    val durationMin: Int,
    val stops: List<RouteStop>,
)

/** "Left Home → Arrived College": one line of the daily timeline. */
data class TimelineEvent(
    val id: String,
    val label: String,
    val category: String,
    val arrivedAtMs: Long,
    val leftAtMs: Long?,
    val durationMin: Int?,
    val ongoing: Boolean,
)

/** Route history privacy setting. [retentionDays] null means "forever". */
data class RouteSettings(
    val enabled: Boolean,
    val retentionDays: Int?,
)

/** A geofence line of the "Recent activity" feed on the Location Alerts screen. */
data class GeofenceEvent(
    val label: String,
    val entered: Boolean,
    val atMs: Long,
)

/** One line of the per-day summary lists (Week and Month ranges). */
data class DaySummary(
    val date: String,
    val distanceKm: Double,
    val durationMin: Int,
    val avgKmh: Double,
    val placesVisited: Int,
)

/** A place found by search. */
data class SearchHit(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
)

/** A driving route from OSRM. [line] is a list of (lat, lng) pairs along the road. */
data class DrivingRoute(
    val distanceM: Double,
    val durationS: Double,
    val line: List<Pair<Double, Double>>,
)

/** Weather at one spot, shown by the Weather layer on the map. */
data class WeatherNow(
    val temperatureC: Int,
    val humidityPct: Int,
    val windKmh: Int,
    val rainMm: Double,
    val uvIndex: Double?,
    val sunrise: String?,
    val sunset: String?,
    val code: Int,
)

enum class MapStyleKind { Street, Dark, Satellite, Hybrid, Auto }

/** A readable name for a spot ("Near Gandhi Nagar" / "Vijayawada, Andhra Pradesh"). */
data class AreaName(
    val main: String,
    val sub: String,
)

/** One reading from the phone's GPS. */
data class DeviceFix(
    val lat: Double,
    val lng: Double,
    val accuracyM: Double?,
    val headingDeg: Double?,
    val speedMps: Double?,
    val altitudeM: Double?,
    /** True when Android says the fix came from a mock-location app. */
    val mock: Boolean,
    val atMs: Long,
)

/** Everything the Live Map keeps in the shared couple state. */
data class SharedMapState(
    val places: List<SavedPlace>,
    val favorites: List<FavoritePlace>,
    val loveNotes: List<LoveNote>,
    val alerts: AlertPrefs,
)
