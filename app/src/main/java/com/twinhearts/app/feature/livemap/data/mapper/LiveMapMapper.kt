package com.twinhearts.app.feature.livemap.data.mapper

import com.twinhearts.app.feature.livemap.data.remote.GeofenceEventDto
import com.twinhearts.app.feature.livemap.data.remote.LocationRowDto
import com.twinhearts.app.feature.livemap.data.remote.LocationsResponse
import com.twinhearts.app.feature.livemap.data.remote.NominatimReverseDto
import com.twinhearts.app.feature.livemap.data.remote.OpenMeteoResponse
import com.twinhearts.app.feature.livemap.data.remote.OsrmResponse
import com.twinhearts.app.feature.livemap.data.remote.PhotonResponse
import com.twinhearts.app.feature.livemap.data.remote.RouteDayResponse
import com.twinhearts.app.feature.livemap.data.remote.RouteSettingsResponse
import com.twinhearts.app.feature.livemap.data.remote.TimelineResponse
import com.twinhearts.app.feature.livemap.data.remote.TrailPointDto
import com.twinhearts.app.feature.livemap.domain.model.AlertPrefs
import com.twinhearts.app.feature.livemap.domain.model.AreaName
import com.twinhearts.app.feature.livemap.domain.model.CouplePositions
import com.twinhearts.app.feature.livemap.domain.model.DaySummary
import com.twinhearts.app.feature.livemap.domain.model.DrivingRoute
import com.twinhearts.app.feature.livemap.domain.model.FavoritePlace
import com.twinhearts.app.feature.livemap.domain.model.GeofenceEvent
import com.twinhearts.app.feature.livemap.domain.model.LivePosition
import com.twinhearts.app.feature.livemap.domain.model.LoveNote
import com.twinhearts.app.feature.livemap.domain.model.RouteDay
import com.twinhearts.app.feature.livemap.domain.model.RoutePoint
import com.twinhearts.app.feature.livemap.domain.model.RouteSettings
import com.twinhearts.app.feature.livemap.domain.model.RouteStop
import com.twinhearts.app.feature.livemap.domain.model.SavedPlace
import com.twinhearts.app.feature.livemap.domain.model.SearchHit
import com.twinhearts.app.feature.livemap.domain.model.TimelineEvent
import com.twinhearts.app.feature.livemap.domain.model.Vehicle
import com.twinhearts.app.feature.livemap.domain.model.WeatherNow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.OffsetDateTime
import kotlin.math.roundToInt

/** Turns server answers into the models the screens use (R-03). Pure functions, covered by unit tests. */

/** The server sends ISO times with "Z" or "+00:00". Returns null when the text is missing or odd. */
fun parseIsoMs(text: String?): Long? =
    if (text.isNullOrBlank()) null else runCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }.getOrNull()

fun LocationRowDto.toPosition(): LivePosition? {
    val la = lat ?: return null
    val ln = lng ?: return null
    return LivePosition(
        lat = la,
        lng = ln,
        accuracyM = accuracy,
        headingDeg = heading,
        speedMps = speed,
        moving = moving == true,
        updatedAtMs = parseIsoMs(updatedAt) ?: 0L,
        online = online == true,
        paused = status == "paused",
        emergency = emergency == true,
        vehicle = Vehicle.fromWire(vehicleType),
    )
}

/** [myRole] is "user1" or "user2": the other row is the partner. */
fun LocationsResponse.toCouple(myRole: String): CouplePositions {
    val mine = if (myRole == "user2") user2 else user1
    val theirs = if (myRole == "user2") user1 else user2
    return CouplePositions(me = mine?.toPosition(), partner = theirs?.toPosition())
}

fun List<TrailPointDto>.toTrail(): List<Pair<Double, Double>> =
    mapNotNull { p -> if (p.lat != null && p.lng != null) p.lat to p.lng else null }

fun RouteDayResponse.toRouteDay(requestedDate: String): RouteDay {
    val routePoints = points.mapNotNull { p ->
        val la = p.lat
        val ln = p.lng
        if (la == null || ln == null) null else RoutePoint(la, ln, p.speed, parseIsoMs(p.createdAt) ?: 0L)
    }
    val routeStops = stops.mapNotNull { s ->
        val la = s.lat
        val ln = s.lng
        if (la == null || ln == null) {
            null
        } else {
            RouteStop(la, ln, parseIsoMs(s.arrivedAt) ?: 0L, parseIsoMs(s.leftAt) ?: 0L, s.minutes ?: 0)
        }
    }
    return RouteDay(
        date = date ?: requestedDate,
        points = routePoints,
        distanceKm = stats?.distanceKm ?: 0.0,
        durationMin = stats?.durationMin ?: 0,
        stops = routeStops,
    )
}

fun TimelineResponse.toTimeline(): List<TimelineEvent> = events.mapIndexed { index, e ->
    TimelineEvent(
        id = e.id ?: index.toString(),
        label = e.label ?: "Unknown place",
        category = e.category ?: "other",
        arrivedAtMs = parseIsoMs(e.arrivedAt) ?: 0L,
        leftAtMs = parseIsoMs(e.leftAt),
        durationMin = e.durationMin,
        ongoing = e.ongoing == true,
    )
}

/** The daily-report row, or null when the server answered JSON null. [places] comes from the timeline. */
fun JsonElement.toDaySummary(date: String, places: Int): DaySummary? {
    val o = this as? JsonObject ?: return null
    return DaySummary(
        date = date,
        distanceKm = o["distance_km"].num() ?: 0.0,
        durationMin = (o["duration_min"].num() ?: 0.0).roundToInt(),
        avgKmh = o["avg_speed_kmh"].num() ?: 0.0,
        placesVisited = places,
    )
}

fun RouteSettingsResponse.toSettings(): RouteSettings = RouteSettings(enabled == true, retentionDays)

fun List<GeofenceEventDto>.toGeofenceEvents(): List<GeofenceEvent> = mapNotNull { e ->
    val at = parseIsoMs(e.occurredAt) ?: return@mapNotNull null
    GeofenceEvent(label = e.label ?: "a place", entered = e.eventType == "enter", atMs = at)
}

/** OSRM geometry is `[lng, lat]`; the app uses `(lat, lng)`. Null when no road route was found. */
fun OsrmResponse.toDrivingRoute(): DrivingRoute? {
    val route = routes.firstOrNull() ?: return null
    val line = route.geometry?.coordinates.orEmpty()
        .filter { it.size >= 2 }
        .map { it[1] to it[0] }
    return DrivingRoute(distanceM = route.distance, durationS = route.duration, line = line)
}

fun PhotonResponse.toHits(): List<SearchHit> = features.mapNotNull { f ->
    val c = f.geometry?.coordinates ?: return@mapNotNull null
    if (c.size < 2) return@mapNotNull null
    val p = f.properties
    val street = listOfNotNull(p?.housenumber, p?.street).joinToString(" ").ifBlank { null }
    val address = listOfNotNull(street, p?.district, p?.city, p?.state).distinct().joinToString(", ")
    val name = p?.name ?: street ?: p?.city ?: return@mapNotNull null
    SearchHit(name = name, address = address, lat = c[1], lng = c[0])
}

/** Same naming rules as the web "Near …" card: the most local name first. */
fun NominatimReverseDto.toAreaName(): AreaName {
    val a = address
    val main = a?.neighbourhood ?: a?.suburb ?: a?.road ?: a?.village ?: a?.town ?: a?.cityDistrict ?: a?.city
        ?: name.orEmpty()
    val sub = listOfNotNull(
        a?.suburb?.takeIf { it != main },
        a?.city ?: a?.town ?: a?.village ?: a?.county,
        a?.state,
    ).joinToString(", ").ifBlank { displayName.orEmpty().split(",").take(3).joinToString(",") }
    return AreaName(main = main.ifBlank { "Unknown area" }, sub = sub)
}

fun OpenMeteoResponse.toWeather(): WeatherNow? {
    val c = current ?: return null
    return WeatherNow(
        temperatureC = (c.temperature ?: return null).roundToInt(),
        humidityPct = (c.humidity ?: 0.0).roundToInt(),
        windKmh = (c.wind ?: 0.0).roundToInt(),
        rainMm = c.precipitation ?: 0.0,
        uvIndex = daily?.uvIndexMax?.firstOrNull(),
        sunrise = daily?.sunrise?.firstOrNull()?.let(::clockOf),
        sunset = daily?.sunset?.firstOrNull()?.let(::clockOf),
        code = c.weatherCode ?: -1,
    )
}

/** "2026-10-27T06:12" -> "06:12". */
private fun clockOf(iso: String): String = iso.substringAfter('T', iso).take(5)

// ── Shared couple state (placesList, favorites, loveNotes, lmAlerts) ─────────────────────────

private fun JsonElement?.obj(): JsonObject? = this as? JsonObject
private fun JsonElement?.arr(): JsonArray? = this as? JsonArray
private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.contentOrNull
private fun JsonElement?.num(): Double? = (this as? JsonPrimitive)?.doubleOrNull

fun JsonObject.toPlaces(): List<SavedPlace> = this["placesList"].arr().orEmpty().mapNotNull { item ->
    val o = item.obj() ?: return@mapNotNull null
    val id = o["id"].text() ?: return@mapNotNull null
    val lat = o["lat"].num() ?: return@mapNotNull null
    val lng = o["lng"].num() ?: return@mapNotNull null
    SavedPlace(
        id = id,
        ownerRole = o["owner"].text() ?: "user1",
        category = o["cat"].text() ?: "Custom",
        name = o["name"].text().orEmpty(),
        lat = lat,
        lng = lng,
        address = o["address"].text().orEmpty(),
        createdAtMs = (o["ts"] as? JsonPrimitive)?.longOrNull ?: 0L,
    )
}

fun List<SavedPlace>.toPlacesJson(): JsonArray = buildJsonArray {
    forEach { p ->
        add(
            buildJsonObject {
                put("id", p.id)
                put("owner", p.ownerRole)
                put("cat", p.category)
                put("name", p.name)
                put("lat", p.lat)
                put("lng", p.lng)
                put("address", p.address)
                put("ts", p.createdAtMs)
            },
        )
    }
}

fun JsonObject.toFavorites(): List<FavoritePlace> = this["favorites"].arr().orEmpty().mapNotNull { item ->
    val o = item.obj() ?: return@mapNotNull null
    FavoritePlace(
        name = o["name"].text() ?: return@mapNotNull null,
        address = o["address"].text().orEmpty(),
        lat = o["lat"].num() ?: return@mapNotNull null,
        lng = o["lng"].num() ?: return@mapNotNull null,
    )
}

fun List<FavoritePlace>.toFavoritesJson(): JsonArray = buildJsonArray {
    forEach { f ->
        add(
            buildJsonObject {
                put("name", f.name)
                put("address", f.address)
                put("lat", f.lat)
                put("lng", f.lng)
            },
        )
    }
}

fun JsonObject.toLoveNotes(): List<LoveNote> = this["loveNotes"].arr().orEmpty().mapNotNull { item ->
    val o = item.obj() ?: return@mapNotNull null
    LoveNote(
        id = o["id"].text() ?: return@mapNotNull null,
        fromRole = o["from"].text().orEmpty(),
        message = o["message"].text().orEmpty(),
        lat = o["lat"].num() ?: return@mapNotNull null,
        lng = o["lng"].num() ?: return@mapNotNull null,
        createdAtMs = (o["ts"] as? JsonPrimitive)?.longOrNull ?: 0L,
    )
}

fun List<LoveNote>.toLoveNotesJson(): JsonArray = buildJsonArray {
    forEach { n ->
        add(
            buildJsonObject {
                put("id", n.id)
                put("from", n.fromRole)
                put("message", n.message)
                put("lat", n.lat)
                put("lng", n.lng)
                put("ts", n.createdAtMs)
            },
        )
    }
}

/** A missing key means "on": the web only turns an alert off when the value is exactly false. */
fun JsonObject.toAlertPrefs(): AlertPrefs {
    val o = this["lmAlerts"].obj() ?: return AlertPrefs()
    fun on(key: String) = (o[key] as? JsonPrimitive)?.booleanOrNull != false
    return AlertPrefs(
        arrive = on("arrive"),
        leave = on("leave"),
        offline = on("offline"),
        battery = on("battery"),
        network = on("network"),
        gps = on("gps"),
    )
}

fun AlertPrefs.toAlertsJson(): JsonObject = buildJsonObject {
    put("arrive", arrive)
    put("leave", leave)
    put("offline", offline)
    put("battery", battery)
    put("network", network)
    put("gps", gps)
}
