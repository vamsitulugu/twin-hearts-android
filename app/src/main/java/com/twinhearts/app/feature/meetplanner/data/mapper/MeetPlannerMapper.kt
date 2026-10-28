package com.twinhearts.app.feature.meetplanner.data.mapper

import com.twinhearts.app.feature.meetplanner.data.remote.ChecklistDto
import com.twinhearts.app.feature.meetplanner.data.remote.CreatePlanBody
import com.twinhearts.app.feature.meetplanner.data.remote.NewPlanDto
import com.twinhearts.app.feature.meetplanner.data.remote.NominatimDto
import com.twinhearts.app.feature.meetplanner.data.remote.OsrmDto
import com.twinhearts.app.feature.meetplanner.data.remote.OverpassDto
import com.twinhearts.app.feature.meetplanner.data.remote.PatchPlanBody
import com.twinhearts.app.feature.meetplanner.data.remote.PatchPlanDto
import com.twinhearts.app.feature.meetplanner.data.remote.PhotonDto
import com.twinhearts.app.feature.meetplanner.data.remote.PlanDto
import com.twinhearts.app.feature.meetplanner.data.remote.RouteGeometryDto
import com.twinhearts.app.feature.meetplanner.data.remote.StopDto
import com.twinhearts.app.feature.meetplanner.domain.NotesCodec
import com.twinhearts.app.feature.meetplanner.domain.NotesMeta
import com.twinhearts.app.feature.meetplanner.domain.PlanDraft
import com.twinhearts.app.feature.meetplanner.domain.haversineKm
import com.twinhearts.app.feature.meetplanner.domain.model.ChecklistItem
import com.twinhearts.app.feature.meetplanner.domain.model.City
import com.twinhearts.app.feature.meetplanner.domain.model.GeoPoint
import com.twinhearts.app.feature.meetplanner.domain.model.MeetIdentity
import com.twinhearts.app.feature.meetplanner.domain.model.MeetStatus
import com.twinhearts.app.feature.meetplanner.domain.model.Meetup
import com.twinhearts.app.feature.meetplanner.domain.model.OsmKind
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.domain.model.RouteLeg
import com.twinhearts.app.feature.meetplanner.domain.model.RoutePlan
import com.twinhearts.app.feature.meetplanner.domain.model.TravelMode
import com.twinhearts.app.feature.meetplanner.domain.model.TripType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.time.LocalDate

/*
 * Why this file exists: turns server/map-service JSON into the plain models the screens use,
 * and turns a finished draft into the body the old server expects. No Android and no network
 * in here, so it is fully unit-tested.
 */

private const val CURRENCY = "INR"
private const val DEFAULT_TITLE = "Our Meetup"
private const val DEFAULT_TITLE_PREFIX = "Meetup in "
private const val ZERO = 0.0

private fun parseDate(raw: String?): LocalDate? =
    raw?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun JsonPrimitive?.text(): String? = this?.contentOrNull
private fun JsonPrimitive?.number(): Double? = this?.doubleOrNull

// ───────────────────────────── saved plans ─────────────────────────────

internal fun StopDto.toPlace(index: Int): Place {
    val category = PlaceCategory.fromKey(cat)
    return Place(
        id = id?.takeIf { it.isNotBlank() } ?: "stop_$index",
        name = name.orEmpty(),
        category = category,
        emoji = icon?.takeIf { it.isNotBlank() } ?: PlaceCategory.Custom.emoji,
        lat = lat,
        lng = lng,
        address = address,
        distKm = distKm,
        isAnchor = isAnchor,
    )
}

internal fun Place.toDto(order: Int): StopDto = StopDto(
    id = id, name = name, cat = category.key, icon = emoji, lat = lat, lng = lng,
    address = address, distKm = distKm, isAnchor = isAnchor, order = order,
)

private fun RouteGeometryDto?.toPoints(): List<GeoPoint> =
    this?.coordinates.orEmpty().filter { it.size >= 2 }.map { GeoPoint(lat = it[1], lng = it[0]) }

/** Null when the row has no id (it could not be opened or deleted anyway). */
fun PlanDto.toMeetup(): Meetup? {
    val planId = id.text() ?: return null
    val parsed = NotesCodec.parse(notes)
    return Meetup(
        id = planId,
        title = title?.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE,
        meetDate = parseDate(meetDate),
        endDate = parseDate(parsed.meta.to),
        tripType = TripType.fromKey(parsed.meta.type),
        notes = parsed.text,
        budget = budget.number(),
        cityName = cityName?.takeIf { it.isNotBlank() } ?: midCity?.takeIf { it.isNotBlank() },
        stops = stops.orEmpty()
            .withIndex()
            .sortedBy { it.value.order ?: 0 } // sortedBy is stable, like the web sort
            .map { (i, dto) -> dto.toPlace(i) },
        routePoints = routeGeometry.toPoints(),
        distanceKm = totalDistanceKm.number(),
        durationMin = totalDurationMin.number(),
        travelMode = TravelMode.fromKey(travelMode),
        checklist = checklist.orEmpty().map { ChecklistItem(it.id, it.text, it.done) },
        status = if (status == "completed") MeetStatus.Completed else MeetStatus.Planned,
        globeMemoryId = globeMemoryId.text(),
    )
}

// ───────────────────────────── saving ─────────────────────────────

/** Builds the `POST /api/meetplanner` body from a finished draft. The draft must have a built route. */
fun PlanDraft.toCreateBody(identity: MeetIdentity): CreatePlanBody? {
    val built = route ?: return null
    val cityName = city?.name
    return CreatePlanBody(
        coupleId = identity.coupleId,
        senderRole = identity.role,
        myName = identity.myName,
        plan = NewPlanDto(
            title = title.trim().ifEmpty { DEFAULT_TITLE_PREFIX + cityName.orEmpty() },
            meetDate = from.toString(),
            budget = budget.toDoubleOrNull()?.takeIf { it != ZERO },
            currency = CURRENCY,
            cityName = cityName,
            cityLat = city?.lat,
            cityLng = city?.lng,
            stops = stops.mapIndexed { i, s -> s.toDto(i) },
            routeGeometry = RouteGeometryDto(
                coordinates = built.points.map { listOf(it.lng, it.lat) },
            ),
            totalDistanceKm = built.distanceKm,
            totalDurationMin = built.durationMin,
            travelMode = mode.key,
            checklist = checklist.map { ChecklistDto(it.id, it.text, it.done) },
            notes = NotesCodec.build(notes, NotesMeta(to = to.toString(), type = tripType?.key)),
            createdBy = identity.role,
        ),
    )
}

private fun String?.orNull(): JsonElement = if (this == null) JsonNull else JsonPrimitive(this)
private fun Double?.orNull(): JsonElement = if (this == null) JsonNull else JsonPrimitive(this)

/** "Edit Meetup" form: changes title, dates, budget and notes. A blank title keeps the old one. */
fun editBody(
    identity: MeetIdentity,
    title: String,
    from: LocalDate?,
    to: LocalDate?,
    budget: String,
    notes: String,
    tripType: TripType?,
): PatchPlanBody {
    val end = if (from != null && to != null && to.isBefore(from)) from else to
    return PatchPlanBody(
        coupleId = identity.coupleId,
        plan = PatchPlanDto(
            title = title.trim().ifEmpty { null },
            meetDate = from?.toString().orNull(),
            budget = budget.toDoubleOrNull()?.takeIf { it != ZERO }.orNull(),
            notes = NotesCodec.build(notes, NotesMeta(to = end?.toString(), type = tripType?.key)).orNull(),
        ),
    )
}

fun checklistBody(identity: MeetIdentity, items: List<ChecklistItem>): PatchPlanBody = PatchPlanBody(
    coupleId = identity.coupleId,
    plan = PatchPlanDto(checklist = items.map { ChecklistDto(it.id, it.text, it.done) }),
)

// ───────────────────────────── places and cities ─────────────────────────────

/** Overpass answer to places. Unnamed ones get the kind's plain label, like the web app. */
fun OverpassDto.toPlaces(kind: OsmKind, category: PlaceCategory, origin: GeoPoint): List<Place> =
    elements.mapNotNull { el ->
        val lat = el.center?.lat ?: el.lat
        val lon = el.center?.lon ?: el.lon
        // The web app treats 0 as "missing" too.
        if (lat == null || lon == null || lat == ZERO || lon == ZERO) return@mapNotNull null
        val tags = el.tags.orEmpty()
        Place(
            id = "osm_${el.type.orEmpty()}${el.id ?: 0}",
            name = tags["name"]?.takeIf { it.isNotBlank() } ?: kind.label,
            category = category,
            emoji = kind.emoji,
            lat = lat,
            lng = lon,
            address = listOfNotNull(
                tags["addr:housenumber"], tags["addr:street"], tags["addr:suburb"], tags["addr:city"],
            ).filter { it.isNotBlank() }.joinToString(", ").ifEmpty { null },
            distKm = haversineKm(origin, GeoPoint(lat, lon)),
        )
    }

private fun NominatimDto.point(): GeoPoint? {
    val la = lat?.toDoubleOrNull() ?: return null
    val lo = lon?.toDoubleOrNull() ?: return null
    return GeoPoint(la, lo)
}

private fun String.firstPart(): String = substringBefore(',').trim()

fun NominatimDto.toCity(): City? {
    val p = point() ?: return null
    val display = displayName.orEmpty()
    return City(name = display.firstPart(), display = display, lat = p.lat, lng = p.lng)
}

/** Reverse lookup: prefers city, then town, then county, then the first part of the full name. */
fun NominatimDto.toCityFromReverse(at: GeoPoint): City {
    val display = displayName.orEmpty()
    val name = address?.get("city") ?: address?.get("town") ?: address?.get("county") ?: display.firstPart()
    return City(name = name, display = display, lat = at.lat, lng = at.lng)
}

fun NominatimDto.toPlace(origin: GeoPoint?): Place? {
    val p = point() ?: return null
    val display = displayName.orEmpty()
    return Place(
        id = "nom_${placeId ?: display.hashCode()}",
        name = display.firstPart(),
        category = PlaceCategory.Custom,
        emoji = PlaceCategory.Custom.emoji,
        lat = p.lat,
        lng = p.lng,
        address = display.ifEmpty { null },
        distKm = origin?.let { haversineKm(it, p) },
    )
}

fun PhotonDto.toPlaces(origin: GeoPoint?): List<Place> = features.mapNotNull { f ->
    val c = f.geometry?.coordinates.orEmpty()
    if (c.size < 2) return@mapNotNull null
    val props = f.properties
    val parts = listOfNotNull(props?.name, props?.street, props?.city, props?.state, props?.country).filter { it.isNotBlank() }
    val point = GeoPoint(lat = c[1], lng = c[0])
    Place(
        id = "photon_${props?.osmType ?: "x"}${props?.osmId ?: "${point.lat}_${point.lng}"}",
        name = props?.name?.takeIf { it.isNotBlank() } ?: parts.firstOrNull() ?: "Unknown place",
        category = PlaceCategory.Custom,
        emoji = PlaceCategory.Custom.emoji,
        lat = point.lat,
        lng = point.lng,
        address = parts.drop(1).joinToString(", ").ifEmpty { null },
        distKm = origin?.let { haversineKm(it, point) },
    )
}

// ───────────────────────────── routes ─────────────────────────────

/**
 * OSRM only knows driving. For a car we keep its road time; for the other modes the distance is
 * the road distance and the time is distance divided by that mode's speed (same as the web app).
 */
fun OsrmDto.toRoute(mode: TravelMode): RoutePlan? {
    val route = routes?.firstOrNull() ?: return null
    val kmh = mode.kmh
    val legs = route.legs.map { leg ->
        val km = leg.distance / 1000
        RouteLeg(distanceKm = km, durationMin = if (kmh != null) km / kmh * 60 else leg.duration / 60)
    }
    val km = route.distance / 1000
    return RoutePlan(
        points = route.geometry.toPoints(),
        distanceKm = km,
        durationMin = if (kmh != null) km / kmh * 60 else route.duration / 60,
        legs = legs,
    )
}
