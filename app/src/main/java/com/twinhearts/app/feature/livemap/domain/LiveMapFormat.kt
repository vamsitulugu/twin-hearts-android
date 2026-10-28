package com.twinhearts.app.feature.livemap.domain

import kotlin.math.roundToInt

/** Number-to-text helpers for distances and durations. Language-neutral, so they live outside strings.xml. */
object LiveMapFormat {

    /** "850 m" below one kilometre, "2.4 km" above. */
    fun distance(metres: Double): String =
        if (metres < 1000) "${metres.roundToInt()} m" else String.format(java.util.Locale.US, "%.1f km", metres / 1000.0)

    /** "42 min" or "1 h 05 min". */
    fun minutes(total: Int): String =
        if (total < 60) "$total min" else String.format(java.util.Locale.US, "%d h %02d min", total / 60, total % 60)

    /** Seconds as "m:ss" for the playback clock. */
    fun clock(totalSeconds: Int): String =
        String.format(java.util.Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)

    /** Short "how long ago": 5s, 12m, 3h, 2d. Matches the web `ago()` helper. */
    fun ago(nowMs: Long, thenMs: Long): String {
        val seconds = ((nowMs - thenMs) / 1000).coerceAtLeast(0)
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m"
            seconds < 86_400 -> "${seconds / 3600}h"
            else -> "${seconds / 86_400}d"
        }
    }
}
