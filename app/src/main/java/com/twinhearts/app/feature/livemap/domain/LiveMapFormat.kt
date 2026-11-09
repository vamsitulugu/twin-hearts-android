package com.twinhearts.app.feature.livemap.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/** How a "last seen" moment should be worded. The screen turns each case into text from strings.xml. */
sealed interface LastSeen {
    /** No usable time (never shared, sharing paused, or a broken date). */
    data object Unknown : LastSeen
    data object JustNow : LastSeen
    data class MinutesAgo(val minutes: Int) : LastSeen
    data class HoursAgo(val hours: Int) : LastSeen
    /** Earlier than yesterday-or-today's hours: [day] is "Yesterday" / "Mon" / "12 Oct", [time] is "6:42 PM". */
    data class Yesterday(val time: String) : LastSeen
    data class OnDay(val day: String, val time: String) : LastSeen
}

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

    /** Like [ago], but "—" when the time is a placeholder (1970), so the screen never shows "20000d". */
    fun agoOrDash(nowMs: Long, thenMs: Long): String = if (thenMs < MIN_REAL_MS) "—" else ago(nowMs, thenMs)

    /** Anything at or before this is a placeholder (the server writes 1970 when sharing is stopped). */
    private const val MIN_REAL_MS = 946_684_800_000L // 1 Jan 2000

    /**
     * Turns a last-seen time into a [LastSeen]: minutes / hours while it is recent, then the real
     * day and clock time ("Yesterday 6:42 PM", "Mon 6:42 PM", "12 Oct 6:42 PM") in the phone's zone.
     */
    fun lastSeen(
        nowMs: Long,
        thenMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): LastSeen {
        if (thenMs < MIN_REAL_MS) return LastSeen.Unknown
        val seconds = ((nowMs - thenMs) / 1000).coerceAtLeast(0) // a slightly fast partner clock never shows the future
        val then = Instant.ofEpochMilli(thenMs).atZone(zone)
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(then)
        val days = java.time.temporal.ChronoUnit.DAYS.between(then.toLocalDate(), today)
        return when {
            seconds < 60 -> LastSeen.JustNow
            seconds < 3600 -> LastSeen.MinutesAgo((seconds / 60).toInt())
            seconds < 6 * 3600 && days == 0L -> LastSeen.HoursAgo((seconds / 3600).toInt())
            days == 0L -> LastSeen.OnDay(DateTimeFormatter.ofPattern("EEE", locale).format(then), time)
            days == 1L -> LastSeen.Yesterday(time)
            days < 7 -> LastSeen.OnDay(DateTimeFormatter.ofPattern("EEE", locale).format(then), time)
            else -> LastSeen.OnDay(DateTimeFormatter.ofPattern("d MMM", locale).format(then), time)
        }
    }
}
