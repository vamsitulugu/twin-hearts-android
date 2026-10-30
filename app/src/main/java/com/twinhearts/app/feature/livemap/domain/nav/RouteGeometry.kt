package com.twinhearts.app.feature.livemap.domain.nav

import com.twinhearts.app.feature.livemap.domain.GeoMath
import kotlin.math.cos
import kotlin.math.hypot

/** The nearest spot on a route line to a GPS fix. */
data class Projection(
    val segment: Int,
    val point: GeoPoint,
    /** Metres from the start of the line to [point]. */
    val alongM: Double,
    /** Metres between the fix and [point]. */
    val offM: Double,
)

/**
 * A route line with its running distances, and the maths to find "where on the line am I".
 * Distances use the same haversine as the rest of the Live Map; the nearest-point search works on a
 * small flat patch around the fix, which is exact enough for the few hundred metres involved.
 */
class RouteGeometry(val points: List<GeoPoint>) {

    /** `cum[i]` = metres from the first point to point i. */
    val cum: DoubleArray = DoubleArray(points.size).also { c ->
        for (i in 1 until points.size) {
            c[i] = c[i - 1] + GeoMath.distanceM(points[i - 1].lat, points[i - 1].lng, points[i].lat, points[i].lng)
        }
    }

    val lengthM: Double get() = if (cum.isEmpty()) 0.0 else cum[cum.size - 1]

    /** Direction of the line at segment [segment], degrees clockwise from north. */
    fun bearingAt(segment: Int): Double {
        if (points.size < 2) return 0.0
        val i = segment.coerceIn(0, points.size - 2)
        return GeoMath.bearing(points[i].lat, points[i].lng, points[i + 1].lat, points[i + 1].lng)
    }

    /**
     * Nearest point on the line. The search starts around [hintSegment] (where we were last time)
     * so a long route costs little; when that window finds nothing close, the whole line is searched.
     */
    fun project(lat: Double, lng: Double, hintSegment: Int = 0, closeEnoughM: Double = 60.0): Projection? {
        if (points.size < 2) return null
        val last = points.size - 2
        val from = (hintSegment - WINDOW_BEHIND).coerceIn(0, last)
        val to = (hintSegment + WINDOW_AHEAD).coerceIn(0, last)
        val windowed = scan(lat, lng, from, to)
        if (windowed != null && windowed.offM <= closeEnoughM) return windowed
        val full = scan(lat, lng, 0, last)
        return full ?: windowed
    }

    private fun scan(lat: Double, lng: Double, from: Int, to: Int): Projection? {
        var best: Projection? = null
        val mPerLng = METERS_PER_DEG_LAT * cos(Math.toRadians(lat))
        for (i in from..to) {
            val a = points[i]
            val b = points[i + 1]
            val ax = (a.lng - lng) * mPerLng
            val ay = (a.lat - lat) * METERS_PER_DEG_LAT
            val bx = (b.lng - lng) * mPerLng
            val by = (b.lat - lat) * METERS_PER_DEG_LAT
            val dx = bx - ax
            val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 < 1e-9) 0.0 else ((-ax) * dx + (-ay) * dy).div(len2).coerceIn(0.0, 1.0)
            val px = ax + t * dx
            val py = ay + t * dy
            val off = hypot(px, py)
            if (best == null || off < best.offM) {
                val segLen = cum[i + 1] - cum[i]
                best = Projection(
                    segment = i,
                    point = GeoPoint(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t),
                    alongM = cum[i] + segLen * t,
                    offM = off,
                )
            }
        }
        return best
    }

    private companion object {
        const val METERS_PER_DEG_LAT = 111_320.0
        const val WINDOW_BEHIND = 5
        const val WINDOW_AHEAD = 250
    }
}

/** At most [max] evenly spread items, always keeping the first and the last. Used to fit the camera to a long line. */
fun <T> List<T>.sampled(max: Int): List<T> {
    if (size <= max || max < 2) return this
    val step = (size - 1).toDouble() / (max - 1)
    return List(max) { this[(it * step).toInt().coerceAtMost(size - 1)] }
}
