package com.twinhearts.app.feature.globe

import com.twinhearts.app.feature.globe.data.WorldAtlas
import com.twinhearts.app.feature.globe.domain.Flight
import com.twinhearts.app.feature.globe.domain.GlobeCamera
import com.twinhearts.app.feature.globe.domain.GlobeView
import com.twinhearts.app.feature.globe.domain.PinPoint
import com.twinhearts.app.feature.globe.domain.RingClipper
import com.twinhearts.app.feature.globe.domain.Vec3
import com.twinhearts.app.feature.globe.domain.clusterPins
import com.twinhearts.app.feature.globe.domain.wrapLongitude
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.hypot

class GlobeProjectionTest {
    private val atlas: WorldAtlas by lazy {
        File("src/main/assets/globe_countries.bin").inputStream().use { WorldAtlas.parse(it) }
    }

    @Test fun projection_centerEastNorthAndBack() {
        val v = GlobeView(0.0, 0.0)
        val o = Vec3()
        v.project(0.0, 0.0, o)
        assertTrue(o.z > 0.999f)
        v.project(0.0, 90.0, o)
        assertTrue(o.x > 0.999f)
        v.project(90.0, 0.0, o)
        assertTrue(o.y > 0.999f)
        v.project(0.0, 180.0, o)
        assertTrue(o.z < -0.999f)
    }

    @Test fun longitudeWraps() {
        assertEquals(-170.0, wrapLongitude(190.0), 1e-9)
        assertEquals(170.0, wrapLongitude(-190.0), 1e-9)
    }

    @Test fun camera_isClamped() {
        val c = GlobeCamera(120.0, 200.0, 50.0).normalized()
        assertEquals(GlobeCamera.MAX_LAT, c.lat, 0.0)
        assertEquals(-160.0, c.lon, 1e-9)
        assertEquals(GlobeCamera.MAX_ZOOM, c.zoom, 0.0)
    }

    @Test fun flight_endsExactlyAtTarget_andTakesShortWayAroundTheDateLine() {
        val f = Flight(GlobeCamera(0.0, 170.0, 1.0), GlobeCamera(10.0, -170.0, 3.0), 1000)
        val end = f.at(1000)
        assertEquals(10.0, end.lat, 0.0)
        assertEquals(-170.0, end.lon, 1e-9)
        assertEquals(3.0, end.zoom, 0.0)
        assertTrue(Math.abs(f.at(500).lon) > 170)
    }

    @Test fun clusters_mergeNearPins() {
        val result = clusterPins(listOf(PinPoint("a", 0f, 0f, 3), PinPoint("b", 10f, 0f, 1), PinPoint("c", 500f, 500f, 1)), 60f)
        assertEquals(2, result.size)
        assertEquals(2, result.first().members.size)
    }

    @Test fun atlas_hasCountriesAndAliases() {
        assertEquals(253, atlas.countries.size)
        assertEquals("AE", atlas.find("UAE")?.iso2)
        assertEquals("US", atlas.find("United States")?.iso2)
        val india = atlas.find("India")
        assertNotNull(india)
        assertTrue(india!!.rings.isNotEmpty())
        // Tiny states have a flag and a name but no outline.
        assertTrue(atlas.find("Singapore")!!.rings.isEmpty())
    }

    @Test fun clipping_neverLeavesTheBall() {
        val clip = RingClipper()
        var drawn = 0
        for ((lat, lon) in listOf(0.0 to 0.0, 15.0 to 78.0, 60.0 to -100.0, -80.0 to 20.0, 85.0 to 0.0, 0.0 to 180.0)) {
            val view = GlobeView(lat, lon)
            for (country in atlas.countries) for (ring in country.rings) {
                val n = clip.clip(ring, view)
                if (n > 0) drawn++
                for (i in 0 until n) assertTrue(hypot(clip.xs[i].toDouble(), clip.ys[i].toDouble()) < 1.0001)
            }
        }
        assertTrue(drawn > 100)
    }
}
