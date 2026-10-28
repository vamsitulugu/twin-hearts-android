package com.twinhearts.app.feature.livemap

import com.twinhearts.app.feature.livemap.domain.GeoMath
import com.twinhearts.app.feature.livemap.domain.LiveMapFormat
import com.twinhearts.app.feature.livemap.domain.model.RoutePoint
import com.twinhearts.app.feature.livemap.domain.model.SavedPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    @Test
    fun distance_betweenSamePoint_isZero() {
        assertEquals(0.0, GeoMath.distanceM(16.5, 80.6, 16.5, 80.6), 0.001)
    }

    @Test
    fun distance_oneDegreeOfLatitude_isAbout111km() {
        assertEquals(111_195.0, GeoMath.distanceM(0.0, 0.0, 1.0, 0.0), 200.0)
    }

    @Test
    fun driveMinutes_usesFortyKmh_andNeverBelowOne() {
        assertEquals(30, GeoMath.driveMinutes(20_000.0))
        assertEquals(1, GeoMath.driveMinutes(10.0))
    }

    @Test
    fun activity_bandsMatchWeb() {
        assertEquals(GeoMath.Activity.Stationary, GeoMath.activity(null))
        assertEquals(GeoMath.Activity.Walking, GeoMath.activity(1.0)) // 3.6 km/h
        assertEquals(GeoMath.Activity.Running, GeoMath.activity(2.5)) // 9 km/h
        assertEquals(GeoMath.Activity.Cycling, GeoMath.activity(6.0)) // 21.6 km/h
        assertEquals(GeoMath.Activity.Driving, GeoMath.activity(15.0)) // 54 km/h
    }

    @Test
    fun nearestSaved_picksClosestWithinRadius() {
        val near = place("a", 16.5000, 80.6000)
        val nearer = place("b", 16.5001, 80.6000)
        val far = place("c", 17.0, 81.0)
        assertEquals("b", GeoMath.nearestSaved(16.50011, 80.6, listOf(near, nearer, far))?.id)
        assertNull(GeoMath.nearestSaved(10.0, 10.0, listOf(near, far)))
    }

    @Test
    fun pathKm_sumsSegments() {
        val points = listOf(RoutePoint(0.0, 0.0, null, 0), RoutePoint(1.0, 0.0, null, 1), RoutePoint(2.0, 0.0, null, 2))
        assertEquals(222.4, GeoMath.pathKm(points), 0.5)
    }

    @Test
    fun bearing_eastIs90() {
        assertEquals(90.0, GeoMath.bearing(0.0, 0.0, 0.0, 1.0), 0.01)
    }

    @Test
    fun format_distanceAndMinutes() {
        assertEquals("850 m", LiveMapFormat.distance(850.0))
        assertEquals("2.4 km", LiveMapFormat.distance(2400.0))
        assertEquals("42 min", LiveMapFormat.minutes(42))
        assertEquals("1 h 05 min", LiveMapFormat.minutes(65))
        assertEquals("5s", LiveMapFormat.ago(10_000, 5_000))
        assertEquals("3h", LiveMapFormat.ago(3 * 3_600_000L, 0))
        assertTrue(LiveMapFormat.ago(0, 5_000) == "0s")
    }

    private fun place(id: String, lat: Double, lng: Double) =
        SavedPlace(id, "user1", "Home", id, lat, lng, "", 0L)
}
