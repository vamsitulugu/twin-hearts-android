package com.twinhearts.app.feature.livemap.domain.nav

/** Everything the navigation screen needs to draw one moment of a trip. Built by the navigation engine. */
data class NavSession(
    val route: NavRoute,
    val destinationName: String,
    val destination: GeoPoint,
    /** Null until the first GPS fix arrives. */
    val progress: NavProgress? = null,
    val muted: Boolean = false,
    val rerouting: Boolean = false,
    /** True when the map follows the car; false after the person moved the map by hand. */
    val following: Boolean = true,
    /** True when no GPS fix came for a while (tunnel, indoors). */
    val gpsLost: Boolean = false,
    val startedAtMs: Long = 0L,
    /** The last reroute failed (usually no internet); the old route stays and the trip goes on. */
    val rerouteFailed: Boolean = false,
) {
    val arrived: Boolean get() = progress?.status == NavStatus.Arrived

    /** The part of the line still ahead, starting at the car. Empty before the first fix. */
    fun remainingLine(): List<GeoPoint> {
        val p = progress ?: return route.line
        return listOf(p.snapped) + route.line.drop(p.segment + 1)
    }
}
