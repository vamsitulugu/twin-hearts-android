package com.twinhearts.app.feature.livemap.domain.nav

/**
 * Turns an OSRM answer into [NavRoute]s. The input is described by the small plain classes below,
 * which the network layer fills from its JSON, so this file has no serialization or Android types.
 */
data class RawStep(
    val type: String,
    val modifier: String?,
    val exit: Int?,
    val name: String,
    val ref: String,
    val distanceM: Double,
    val durationS: Double,
    /** (lat, lng) pairs of this step. */
    val line: List<GeoPoint>,
    val at: GeoPoint?,
)

data class RawRoute(
    val distanceM: Double,
    val durationS: Double,
    /** Whole-route line, used only when no step carries its own line. */
    val line: List<GeoPoint>,
    val steps: List<RawStep>,
)

object OsrmRouteMapper {

    fun toRoutes(raw: List<RawRoute>, mode: TravelMode): List<NavRoute> =
        raw.mapIndexedNotNull { index, r -> toRoute(r, mode, index) }

    fun toRoute(raw: RawRoute, mode: TravelMode, id: Int): NavRoute? {
        val line = ArrayList<GeoPoint>()
        val steps = ArrayList<RouteStep>()
        for (s in raw.steps) {
            val joinsPrevious = line.isNotEmpty() && s.line.isNotEmpty() && line.last() == s.line.first()
            val start = if (joinsPrevious) line.size - 1 else line.size
            s.line.forEachIndexed { i, p -> if (!(i == 0 && joinsPrevious)) line.add(p) }
            val kind = kindOf(s.type, s.modifier)
            val road = s.name.ifBlank { s.ref }
            val at = s.at ?: s.line.firstOrNull() ?: line.lastOrNull() ?: continue
            steps.add(
                RouteStep(
                    kind = kind,
                    road = road,
                    exit = if (kind == ManeuverKind.Roundabout) s.exit else null,
                    side = if (kind == ManeuverKind.Arrive) sideOf(s.modifier) else null,
                    distanceM = s.distanceM,
                    durationS = s.durationS,
                    startIndex = start.coerceIn(0, maxOf(0, line.size - 1)),
                    at = at,
                ),
            )
        }
        val finalLine = if (line.size >= 2) line else raw.line
        if (finalLine.size < 2) return null
        val finalSteps = if (steps.isNotEmpty()) steps else listOf(
            RouteStep(ManeuverKind.Depart, "", null, null, raw.distanceM, raw.durationS, 0, finalLine.first()),
            RouteStep(ManeuverKind.Arrive, "", null, ArriveSide.Straight, 0.0, 0.0, finalLine.size - 1, finalLine.last()),
        )
        return NavRoute(id, mode, raw.distanceM, raw.durationS, finalLine, finalSteps, viaRoad(finalSteps))
    }

    private fun viaRoad(steps: List<RouteStep>): String {
        val byRoad = HashMap<String, Double>()
        steps.forEach { if (it.road.isNotBlank()) byRoad[it.road] = (byRoad[it.road] ?: 0.0) + it.distanceM }
        return byRoad.maxByOrNull { it.value }?.key.orEmpty()
    }

    private fun sideOf(modifier: String?): ArriveSide = when (modifier) {
        "left", "slight left", "sharp left" -> ArriveSide.Left
        "right", "slight right", "sharp right" -> ArriveSide.Right
        else -> ArriveSide.Straight
    }

    fun kindOf(type: String, modifier: String?): ManeuverKind {
        val turn = when (modifier) {
            "left" -> ManeuverKind.TurnLeft
            "right" -> ManeuverKind.TurnRight
            "slight left" -> ManeuverKind.SlightLeft
            "slight right" -> ManeuverKind.SlightRight
            "sharp left" -> ManeuverKind.SharpLeft
            "sharp right" -> ManeuverKind.SharpRight
            "uturn" -> ManeuverKind.UTurn
            else -> ManeuverKind.Continue
        }
        return when (type) {
            "depart" -> ManeuverKind.Depart
            "arrive" -> ManeuverKind.Arrive
            "merge" -> ManeuverKind.Merge
            "on ramp", "off ramp" -> when (modifier) {
                "left", "slight left", "sharp left" -> ManeuverKind.RampLeft
                "right", "slight right", "sharp right" -> ManeuverKind.RampRight
                else -> ManeuverKind.Continue
            }
            "fork" -> when (modifier) {
                "left", "slight left", "sharp left" -> ManeuverKind.ForkLeft
                "right", "slight right", "sharp right" -> ManeuverKind.ForkRight
                else -> ManeuverKind.Continue
            }
            "roundabout", "rotary", "roundabout turn", "exit roundabout", "exit rotary" -> ManeuverKind.Roundabout
            "turn", "end of road", "new name", "continue" -> turn
            else -> turn
        }
    }
}
