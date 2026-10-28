package com.twinhearts.app.feature.meetplanner.domain

import com.twinhearts.app.feature.meetplanner.domain.model.GeoPoint
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.RouteLeg
import com.twinhearts.app.feature.meetplanner.domain.model.RoutePlan
import com.twinhearts.app.feature.meetplanner.domain.model.TravelMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Why this file exists: the maths behind "Your Stops" and "Itinerary": rough estimates, the
 * nearest-stop ordering, the straight-line fallback when OSRM is down, and the day timeline.
 */

private const val EARTH_RADIUS_KM = 6371.0

/** Straight-line distance between two points on the globe. */
fun haversineKm(a: GeoPoint, b: GeoPoint): Double {
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val x = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2) * sin(dLng / 2)
    return EARTH_RADIUS_KM * 2 * atan2(sqrt(x), sqrt(1 - x))
}

private fun Place.point() = GeoPoint(lat, lng)

/** Straight line is shorter than a road, so the web multiplies by 1.3 for a rough road distance. */
const val ROAD_FACTOR = 1.3

/** Speed used for a car when OSRM gave us nothing to go on. */
private const val DEFAULT_CAR_KMH = 38.0
private const val FALLBACK_KMH = 30.0

data class RoughEstimate(val km: Double, val minutes: Double)

/** The quick estimate on "Your Stops" (before the real route is built). */
fun roughEstimate(stops: List<Place>, mode: TravelMode): RoughEstimate {
    var km = 0.0
    for (i in 0 until stops.size - 1) km += haversineKm(stops[i].point(), stops[i + 1].point())
    km *= ROAD_FACTOR
    val kmh = mode.kmh ?: DEFAULT_CAR_KMH
    return RoughEstimate(km, km / kmh * 60)
}

/** "~x km from previous" under each stop. Null for the first stop. */
fun roughLegKm(stops: List<Place>, index: Int): Double? =
    if (index <= 0 || index >= stops.size) null else haversineKm(stops[index - 1].point(), stops[index].point()) * ROAD_FACTOR

/**
 * Orders stops so each next stop is the nearest one left. The Start/End stop (the first hotel)
 * goes first and again at the very end, so the day is a round trip back to the hotel.
 */
fun optimizeOrder(stops: List<Place>): List<Place> {
    if (stops.size <= 2) return stops
    val anchor = stops.firstOrNull { it.isAnchor }
    val start = anchor ?: stops.first()
    val pool = if (anchor != null) stops.filter { it.id != anchor.id }.toMutableList() else stops.drop(1).toMutableList()
    val ordered = mutableListOf(start)
    var current = start
    while (pool.isNotEmpty()) {
        var bestIndex = 0
        var bestDist = Double.MAX_VALUE
        pool.forEachIndexed { index, place ->
            val d = haversineKm(current.point(), place.point())
            if (d < bestDist) {
                bestDist = d
                bestIndex = index
            }
        }
        current = pool.removeAt(bestIndex)
        ordered += current
    }
    if (anchor != null) ordered += anchor
    return ordered
}

/** When OSRM cannot be reached we still show a route: straight lines between the stops. */
fun straightLineRoute(stops: List<Place>, mode: TravelMode): RoutePlan {
    val kmh = mode.kmh ?: FALLBACK_KMH
    val legs = (0 until stops.size - 1).map { i ->
        val d = haversineKm(stops[i].point(), stops[i + 1].point())
        RouteLeg(distanceKm = d, durationMin = d / kmh * 60)
    }
    val km = legs.sumOf { it.distanceKm }
    return RoutePlan(
        points = stops.map { it.point() },
        distanceKm = km,
        durationMin = km / kmh * 60,
        legs = legs,
    )
}

/** One row of the day plan: arrive at [startMinutes] (minutes after midnight), stay a while, then travel [leg]. */
data class TimelineRow(
    val startMinutes: Int,
    val stop: Place,
    val stayMinutes: Int,
    val leg: RouteLeg?,
)

/** Builds the "Start the day at 09:00" timeline: each stop's stay time plus the travel to the next one. */
fun buildTimeline(stops: List<Place>, legs: List<RouteLeg>, startMinutes: Int): List<TimelineRow> {
    var t = startMinutes.toDouble()
    return stops.mapIndexed { i, stop ->
        val stay = stop.category.stayMinutes
        val leg = legs.getOrNull(i)
        val row = TimelineRow(Math.round(t).toInt(), stop, stay, leg)
        t += stay + (leg?.durationMin ?: 0.0)
        row
    }
}
