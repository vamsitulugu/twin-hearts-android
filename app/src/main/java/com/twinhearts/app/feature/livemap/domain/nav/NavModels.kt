package com.twinhearts.app.feature.livemap.domain.nav

/**
 * Plain Kotlin models for directions and turn-by-turn navigation (R-03). No Android, network or
 * Compose types live here, so the whole navigation brain can be unit tested on a laptop.
 */

/** A point on Earth, (latitude, longitude) in degrees. */
data class GeoPoint(val lat: Double, val lng: Double)

/** How the person travels. [profile] is the routing profile name of the free FOSSGIS OSRM servers. */
enum class TravelMode(val profile: String) {
    Drive("car"),
    Walk("foot"),
    Bike("bike"),
}

/** What the person must do at the end of a step. The UI turns each kind into an arrow and a sentence. */
enum class ManeuverKind {
    Depart,
    Arrive,
    Continue,
    TurnLeft,
    TurnRight,
    SlightLeft,
    SlightRight,
    SharpLeft,
    SharpRight,
    UTurn,
    Merge,
    RampLeft,
    RampRight,
    ForkLeft,
    ForkRight,
    Roundabout,
    ;

    /** Continue-straight steps are listed but never spoken, like Google Maps does. */
    val spoken: Boolean get() = this != Continue
}

/** Which side the destination is on when the last step says "Arrive". */
enum class ArriveSide { Left, Right, Straight }

/**
 * One piece of a route. The [kind] describes the manoeuvre at the START of the step ([at]); the
 * step then runs for [distanceM] along [road] until the next step's manoeuvre.
 */
data class RouteStep(
    val kind: ManeuverKind,
    /** Road name after the manoeuvre. Empty when the road has no name. */
    val road: String,
    /** Roundabout exit number (1 = first exit), null for every other manoeuvre. */
    val exit: Int?,
    val side: ArriveSide?,
    val distanceM: Double,
    val durationS: Double,
    /** Index into [NavRoute.line] where this step begins. */
    val startIndex: Int,
    val at: GeoPoint,
)

/** One complete way from A to B: the line to draw, the steps to read out, the totals. */
data class NavRoute(
    val id: Int,
    val mode: TravelMode,
    val distanceM: Double,
    val durationS: Double,
    val line: List<GeoPoint>,
    val steps: List<RouteStep>,
    /** "Main Street": the road the route spends the longest on. Shown as "via Main Street". */
    val via: String,
)

/** A GPS reading, reduced to what the tracker needs. */
data class NavFix(
    val lat: Double,
    val lng: Double,
    val accuracyM: Double?,
    val speedMps: Double?,
    val headingDeg: Double?,
    val atMs: Long,
)

enum class CueBand { Early, Approach, Now }

/** Something to say out loud. The phone turns it into a sentence in the app language. */
data class VoiceCue(
    /** Index of the step whose manoeuvre is being announced. */
    val stepIndex: Int,
    val band: CueBand,
    val kind: ManeuverKind,
    val road: String,
    val exit: Int?,
    val side: ArriveSide?,
    /** Distance left to the manoeuvre, rounded for speech (0 for [CueBand.Now]). */
    val distanceM: Int,
    /** True for the very first line of a trip ("Head north on Main Street"). */
    val isDeparture: Boolean = false,
)

enum class NavStatus { OnRoute, OffRoute, Arrived }

/** The answer to "where am I on the route right now?". */
data class NavProgress(
    val status: NavStatus,
    /** The fix pulled onto the road line; draw the arrow here. */
    val snapped: GeoPoint,
    /** Metres from the start of the route to [snapped]. */
    val alongM: Double,
    /** How far the real fix is from the line. */
    val offRouteM: Double,
    val remainingM: Double,
    val remainingS: Double,
    /** The step being driven now; its end is the next manoeuvre. */
    val stepIndex: Int,
    /** The manoeuvre coming up (the next step's start); null when already on the last step. */
    val next: RouteStep?,
    val distanceToNextM: Double,
    /** Direction the road is heading here, for turning the map. */
    val roadBearingDeg: Double,
    val cue: VoiceCue?,
    /** Index of the route-line segment the fix was matched to; the line from here on is what is left to drive. */
    val segment: Int = 0,
)

/** Tuning numbers for one travel mode. */
data class NavConfig(
    val arriveRadiusM: Double,
    val earlyM: Double,
    val approachM: Double,
    val nowM: Double,
    val offRouteM: Double,
) {
    companion object {
        fun forMode(mode: TravelMode): NavConfig = when (mode) {
            TravelMode.Drive -> NavConfig(arriveRadiusM = 30.0, earlyM = 800.0, approachM = 250.0, nowM = 40.0, offRouteM = 35.0)
            TravelMode.Bike -> NavConfig(arriveRadiusM = 20.0, earlyM = 300.0, approachM = 100.0, nowM = 25.0, offRouteM = 25.0)
            TravelMode.Walk -> NavConfig(arriveRadiusM = 15.0, earlyM = 120.0, approachM = 50.0, nowM = 15.0, offRouteM = 20.0)
        }
    }
}
