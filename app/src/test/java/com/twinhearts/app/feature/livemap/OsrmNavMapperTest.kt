package com.twinhearts.app.feature.livemap

import com.twinhearts.app.feature.livemap.data.mapper.toNavRoutes
import com.twinhearts.app.feature.livemap.data.remote.OsrmGeometryDto
import com.twinhearts.app.feature.livemap.data.remote.OsrmLegDto
import com.twinhearts.app.feature.livemap.data.remote.OsrmManeuverDto
import com.twinhearts.app.feature.livemap.data.remote.OsrmResponse
import com.twinhearts.app.feature.livemap.data.remote.OsrmRouteDto
import com.twinhearts.app.feature.livemap.data.remote.OsrmStepDto
import com.twinhearts.app.feature.livemap.domain.nav.ManeuverKind
import com.twinhearts.app.feature.livemap.domain.nav.TravelMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OsrmNavMapperTest {

    private fun line(vararg p: Pair<Double, Double>) = OsrmGeometryDto(p.map { listOf(it.first, it.second) })

    private fun response(): OsrmResponse {
        val s1 = OsrmStepDto(
            distance = 200.0, duration = 30.0, name = "Main Street",
            geometry = line(80.0 to 16.0, 80.001 to 16.0, 80.002 to 16.0),
            maneuver = OsrmManeuverDto("depart", "right", null, listOf(80.0, 16.0)),
        )
        val s2 = OsrmStepDto(
            distance = 100.0, duration = 20.0, name = "", ref = "NH16",
            geometry = line(80.002 to 16.0, 80.002 to 16.001),
            maneuver = OsrmManeuverDto("turn", "left", null, listOf(80.002, 16.0)),
        )
        val s3 = OsrmStepDto(
            distance = 0.0, duration = 0.0, name = "NH16",
            geometry = line(80.002 to 16.001),
            maneuver = OsrmManeuverDto("arrive", "left", null, listOf(80.002, 16.001)),
        )
        val main = OsrmRouteDto(distance = 300.0, duration = 50.0, legs = listOf(OsrmLegDto(listOf(s1, s2, s3))))
        val alt = OsrmRouteDto(
            distance = 320.0, duration = 60.0,
            geometry = line(80.0 to 16.0, 80.002 to 16.001),
            legs = emptyList(),
        )
        return OsrmResponse(listOf(main, alt))
    }

    @Test
    fun stepsBecomeALineWithStartIndexes() {
        val routes = response().toNavRoutes(TravelMode.Walk)
        assertEquals(2, routes.size)
        val r = routes[0]
        assertEquals(TravelMode.Walk, r.mode)
        assertEquals(4, r.line.size)
        assertEquals(listOf(0, 2, 3), r.steps.map { it.startIndex })
        assertEquals(ManeuverKind.TurnLeft, r.steps[1].kind)
        assertEquals("NH16", r.steps[1].road)
        // lat/lng order: OSRM sends [lng, lat]
        assertEquals(16.0, r.line[0].lat, 1e-9)
        assertEquals(80.0, r.line[0].lng, 1e-9)
        assertEquals("Main Street", r.via)
    }

    @Test
    fun routeWithoutStepsStillGetsDepartAndArrive() {
        val alt = response().toNavRoutes(TravelMode.Drive)[1]
        assertEquals(2, alt.steps.size)
        assertEquals(ManeuverKind.Depart, alt.steps.first().kind)
        assertEquals(ManeuverKind.Arrive, alt.steps.last().kind)
    }

    @Test
    fun emptyAnswerGivesNoRoutes() {
        assertTrue(OsrmResponse().toNavRoutes(TravelMode.Bike).isEmpty())
    }
}
