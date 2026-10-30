package com.twinhearts.app.feature.livemap.domain.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavTrackerTest {

    // ~1.1 km east along the equator, then ~0.55 km north. 0.001 deg = ~111 m.
    private fun p(lat: Double, lng: Double) = GeoPoint(lat, lng)

    private fun raw(): RawRoute {
        val s1 = (0..10).map { p(0.0, it * 0.001) }
        val s2 = (0..5).map { p(it * 0.001, 0.010) }
        return RawRoute(
            distanceM = 1_670.0,
            durationS = 200.0,
            line = s1 + s2.drop(1),
            steps = listOf(
                RawStep("depart", null, null, "Main Street", "", 1_113.0, 120.0, s1, s1.first()),
                RawStep("turn", "left", null, "Oak Avenue", "", 556.0, 80.0, s2, s2.first()),
                RawStep("arrive", "right", null, "Oak Avenue", "", 0.0, 0.0, listOf(s2.last()), s2.last()),
            ),
        )
    }

    private fun route() = requireNotNull(OsrmRouteMapper.toRoute(raw(), TravelMode.Drive, 0))

    private fun fix(lat: Double, lng: Double, t: Long = 0L) = NavFix(lat, lng, 5.0, 10.0, null, t)

    @Test
    fun mapperJoinsStepLinesWithoutDuplicates() {
        val r = route()
        assertEquals(16, r.line.size)
        assertEquals(0, r.steps[0].startIndex)
        assertEquals(10, r.steps[1].startIndex)
        assertEquals(15, r.steps[2].startIndex)
        assertEquals("Main Street", r.via)
        assertEquals(ManeuverKind.TurnLeft, r.steps[1].kind)
        assertEquals(ArriveSide.Right, r.steps[2].side)
    }

    @Test
    fun kindsAreMapped() {
        assertEquals(ManeuverKind.SlightRight, OsrmRouteMapper.kindOf("turn", "slight right"))
        assertEquals(ManeuverKind.Roundabout, OsrmRouteMapper.kindOf("roundabout", "right"))
        assertEquals(ManeuverKind.RampLeft, OsrmRouteMapper.kindOf("off ramp", "slight left"))
        assertEquals(ManeuverKind.UTurn, OsrmRouteMapper.kindOf("turn", "uturn"))
        assertEquals(ManeuverKind.Continue, OsrmRouteMapper.kindOf("new name", "straight"))
        assertTrue(!ManeuverKind.Continue.spoken)
    }

    @Test
    fun firstFixSaysDepartureThenCuesFireOncePerBand() {
        val t = NavTracker(route())
        val first = t.update(fix(0.0, 0.0))
        assertEquals(NavStatus.OnRoute, first.status)
        assertNotNull(first.cue)
        assertTrue(first.cue!!.isDeparture)
        assertEquals(ManeuverKind.TurnLeft, first.next?.kind)
        assertEquals(1_113.0, first.distanceToNextM, 5.0)

        // ~700 m before the turn: early band (800 m) is crossed.
        val early = t.update(fix(0.0, 0.0044))
        assertEquals(CueBand.Early, early.cue?.band)
        assertEquals(ManeuverKind.TurnLeft, early.cue?.kind)
        // same band again: silent
        assertNull(t.update(fix(0.0, 0.0046)).cue)
        val approach = t.update(fix(0.0, 0.0080))
        assertEquals(CueBand.Approach, approach.cue?.band)
        val now = t.update(fix(0.0, 0.0097))
        assertEquals(CueBand.Now, now.cue?.band)
    }

    @Test
    fun gpsJumpSaysOnlyTheSmallestBand() {
        val t = NavTracker(route())
        t.update(fix(0.0, 0.0))
        val jump = t.update(fix(0.0, 0.0098))
        assertEquals(CueBand.Now, jump.cue?.band)
        assertNull(t.update(fix(0.0, 0.0099)).cue)
    }

    @Test
    fun remainingShrinksAndStepAdvances() {
        val t = NavTracker(route())
        val a = t.update(fix(0.0, 0.0))
        val b = t.update(fix(0.0, 0.0105))
        assertTrue(b.remainingM < a.remainingM)
        assertEquals(1, b.stepIndex)
        assertTrue(b.remainingS < a.remainingS)
    }

    @Test
    fun offRouteNeedsThreeFixesAndRecovers() {
        val t = NavTracker(route())
        t.update(fix(0.0, 0.0))
        // ~110 m south of the road
        assertEquals(NavStatus.OnRoute, t.update(fix(-0.001, 0.003)).status)
        assertEquals(NavStatus.OnRoute, t.update(fix(-0.001, 0.0031)).status)
        assertEquals(NavStatus.OffRoute, t.update(fix(-0.001, 0.0032)).status)
        assertEquals(NavStatus.OnRoute, t.update(fix(0.0, 0.0033)).status)
    }

    @Test
    fun poorAccuracyDoesNotCauseReroute() {
        val t = NavTracker(route())
        t.update(fix(0.0, 0.0))
        repeat(5) {
            val s = t.update(NavFix(-0.0004, 0.003, 60.0, 5.0, null, 0L))
            assertEquals(NavStatus.OnRoute, s.status)
        }
    }

    @Test
    fun arrivalIsReportedOnce() {
        val t = NavTracker(route())
        t.update(fix(0.0, 0.0))
        t.update(fix(0.0, 0.0099))
        t.update(fix(0.003, 0.010))
        val done = t.update(fix(0.0549, 0.010).copy(lat = 0.0049))
        assertEquals(NavStatus.Arrived, done.status)
        assertEquals(ManeuverKind.Arrive, done.cue?.kind)
        assertNull(t.update(fix(0.0049, 0.010)).cue)
    }

    @Test
    fun speechRounding() {
        assertEquals(800, NavTracker.roundForSpeech(820.0))
        assertEquals(1_200, NavTracker.roundForSpeech(1_240.0))
        assertEquals(100, NavTracker.roundForSpeech(95.0))
        assertEquals(40, NavTracker.roundForSpeech(43.0))
        assertEquals(10, NavTracker.roundForSpeech(4.0))
    }

    @Test
    fun geometryProjection() {
        val g = RouteGeometry(listOf(p(0.0, 0.0), p(0.0, 0.01)))
        val pr = requireNotNull(g.project(0.0005, 0.005))
        assertEquals(556.0, pr.alongM, 3.0)
        assertEquals(55.6, pr.offM, 1.0)
        assertEquals(90.0, g.bearingAt(0), 0.5)
    }
}
