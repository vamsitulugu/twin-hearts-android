package com.twinhearts.app.feature.livemap.domain.nav

import com.twinhearts.app.feature.livemap.domain.GeoMath
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The brain of turn-by-turn navigation. Feed it GPS fixes one by one with [update]; it answers
 * where you are on the route, what is coming up, how much is left, whether you left the road
 * and whether something should be said out loud. It holds no Android types, so it is unit tested.
 */
class NavTracker(
    val route: NavRoute,
    private val config: NavConfig = NavConfig.forMode(route.mode),
) {
    private val geometry = RouteGeometry(route.line)
    private val stepStartM = route.steps.map { geometry.cum.getOrElse(it.startIndex) { 0.0 } }
    private val announced = HashSet<Long>()
    private var segmentHint = 0
    private var offCount = 0
    private var departureSaid = false
    private var arrived = false

    /** Forget all progress, e.g. after the person asks for the route again. */
    fun reset() {
        announced.clear()
        segmentHint = 0
        offCount = 0
        departureSaid = false
        arrived = false
    }

    fun update(fix: NavFix): NavProgress {
        val last = route.line.lastOrNull() ?: GeoPoint(fix.lat, fix.lng)
        val proj = geometry.project(fix.lat, fix.lng, segmentHint, closeEnoughM = offLimit(fix))
        if (proj == null) {
            return NavProgress(
                NavStatus.OnRoute, GeoPoint(fix.lat, fix.lng), 0.0, 0.0, route.distanceM, route.durationS,
                0, route.steps.getOrNull(1), route.distanceM, 0.0, null,
            )
        }
        segmentHint = proj.segment
        val along = proj.alongM
        val remainingM = max(0.0, geometry.lengthM - along)
        val toEnd = GeoMath.distanceM(fix.lat, fix.lng, last.lat, last.lng)

        val stepIndex = currentStep(along)
        val next = route.steps.getOrNull(stepIndex + 1)
        val stepEndM = if (next != null) stepStartM[stepIndex + 1] else geometry.lengthM
        val toNext = max(0.0, stepEndM - along)
        val remainingS = remainingSeconds(stepIndex, along)

        val off = proj.offM > offLimit(fix)
        offCount = if (off) offCount + 1 else 0

        if (!arrived && (toEnd <= config.arriveRadiusM || (remainingM <= config.arriveRadiusM && !off))) {
            arrived = true
            val finalStep = route.steps.lastOrNull()
            return NavProgress(
                NavStatus.Arrived, proj.point, along, proj.offM, 0.0, 0.0, stepIndex, null, 0.0,
                geometry.bearingAt(proj.segment),
                VoiceCue(route.steps.lastIndex, CueBand.Now, ManeuverKind.Arrive, finalStep?.road.orEmpty(), null, finalStep?.side, 0),
                segment = proj.segment,
            )
        }
        if (arrived) {
            return NavProgress(
                NavStatus.Arrived, proj.point, along, proj.offM, 0.0, 0.0, stepIndex, null, 0.0, geometry.bearingAt(proj.segment), null,
                segment = proj.segment,
            )
        }

        val status = if (offCount >= OFF_ROUTE_FIXES) NavStatus.OffRoute else NavStatus.OnRoute
        val cue = if (status == NavStatus.OnRoute && !off) cueFor(stepIndex, next, toNext) else null
        return NavProgress(
            status, proj.point, along, proj.offM, remainingM, remainingS, stepIndex, next, toNext,
            geometry.bearingAt(proj.segment), cue,
            segment = proj.segment,
        )
    }

    /** Wider tolerance when the GPS itself is vague, so a poor fix does not cause a false reroute. */
    private fun offLimit(fix: NavFix): Double = max(config.offRouteM, (fix.accuracyM ?: 0.0) * 1.5)

    private fun currentStep(along: Double): Int {
        var index = 0
        for (i in route.steps.indices) {
            if (stepStartM[i] <= along + 0.5) index = i else break
        }
        // The last step is "Arrive"; being on it means the previous step is still the one being driven.
        return if (index >= route.steps.lastIndex && route.steps.size > 1) route.steps.lastIndex - 1 else index
    }

    private fun remainingSeconds(stepIndex: Int, along: Double): Double {
        val step = route.steps.getOrNull(stepIndex) ?: return route.durationS
        val end = if (stepIndex + 1 < route.steps.size) stepStartM[stepIndex + 1] else geometry.lengthM
        val span = max(1.0, end - stepStartM[stepIndex])
        val fractionLeft = ((end - along) / span).coerceIn(0.0, 1.0)
        var seconds = step.durationS * fractionLeft
        for (i in stepIndex + 1 until route.steps.size) seconds += route.steps[i].durationS
        return seconds
    }

    private fun cueFor(stepIndex: Int, next: RouteStep?, toNextM: Double): VoiceCue? {
        if (!departureSaid) {
            departureSaid = true
            val first = route.steps.firstOrNull()
            if (first != null && first.kind == ManeuverKind.Depart) {
                return VoiceCue(0, CueBand.Early, ManeuverKind.Depart, first.road, null, null, 0, isDeparture = true)
            }
        }
        if (next == null || !next.kind.spoken) return null
        val nextIndex = stepIndex + 1
        // The smallest band that has been crossed and not said yet. Skipping the bigger ones keeps a
        // GPS jump from reading three sentences in a row.
        val band = when {
            toNextM <= config.nowM -> CueBand.Now
            toNextM <= config.approachM -> CueBand.Approach
            toNextM <= config.earlyM -> CueBand.Early
            else -> return null
        }
        val key = nextIndex * 10L + band.ordinal
        if (key in announced) return null
        // Mark this and all larger bands of the same manoeuvre as done.
        for (b in band.ordinal downTo 0) announced.add(nextIndex * 10L + b)
        return VoiceCue(
            stepIndex = nextIndex,
            band = band,
            kind = next.kind,
            road = next.road,
            exit = next.exit,
            side = next.side,
            distanceM = if (band == CueBand.Now) 0 else roundForSpeech(toNextM),
        )
    }

    companion object {
        private const val OFF_ROUTE_FIXES = 3

        /** 820 m -> 800, 1,240 m -> 1,200, 95 m -> 100, 43 m -> 50. */
        fun roundForSpeech(meters: Double): Int = when {
            meters >= 1000 -> ((meters / 100).roundToInt() * 100)
            meters >= 100 -> ((meters / 50).roundToInt() * 50)
            else -> max(10, (meters / 10).roundToInt() * 10)
        }
    }
}
