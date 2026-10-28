package com.twinhearts.app.feature.livemap.domain

import com.twinhearts.app.feature.livemap.domain.model.RoutePoint
import com.twinhearts.app.feature.livemap.domain.model.SavedPlace
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Small, pure map maths used by several screens. Kept free of Android so it is easy to unit test.
 * The numbers (40 km/h drive speed, 150 m "at a place", activity bands) are the web app's.
 */
object GeoMath {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Straight-line distance in metres between two points. */
    fun distanceM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Estimated drive time in whole minutes at the web app's 40 km/h, never less than one. */
    fun driveMinutes(distanceM: Double): Int = maxOf(1, (distanceM / 1000.0 / DRIVE_KMH * 60).roundToInt())

    /** The saved place within [radiusM] of the point, the closest one when several match. */
    fun nearestSaved(lat: Double, lng: Double, places: List<SavedPlace>, radiusM: Double = AT_PLACE_RADIUS_M): SavedPlace? =
        places
            .map { it to distanceM(lat, lng, it.lat, it.lng) }
            .filter { it.second <= radiusM }
            .minByOrNull { it.second }
            ?.first

    /** Walking / driving label from GPS speed (metres per second), same bands as the web app. */
    fun activity(speedMps: Double?): Activity {
        val kmh = (speedMps ?: 0.0) * 3.6
        return when {
            kmh < 1 -> Activity.Stationary
            kmh < 7 -> Activity.Walking
            kmh < 15 -> Activity.Running
            kmh < 35 -> Activity.Cycling
            else -> Activity.Driving
        }
    }

    /** Total length of a polyline of route points, in kilometres. */
    fun pathKm(points: List<RoutePoint>): Double {
        var total = 0.0
        for (i in 1 until points.size) {
            total += distanceM(points[i - 1].lat, points[i - 1].lng, points[i].lat, points[i].lng)
        }
        return total / 1000.0
    }

    /** Compass bearing in degrees (0 = north) from one point to the next. Used to turn the marker. */
    fun bearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val p1 = lat1 * PI / 180
        val p2 = lat2 * PI / 180
        val dl = (lng2 - lng1) * PI / 180
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    const val DRIVE_KMH = 40.0
    const val AT_PLACE_RADIUS_M = 150.0

    /** Two people closer than this count as "together right now". */
    const val TOGETHER_RADIUS_M = 100.0

    enum class Activity { Stationary, Walking, Running, Cycling, Driving }
}

/** Same-spot test for favorites and destinations: five decimals is about one metre. */
fun spotKey(lat: Double, lng: Double): String = String.format(java.util.Locale.US, "%.5f,%.5f", lat, lng)
