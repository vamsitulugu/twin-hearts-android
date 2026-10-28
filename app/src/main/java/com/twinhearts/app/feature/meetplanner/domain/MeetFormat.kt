package com.twinhearts.app.feature.meetplanner.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

/*
 * Why this file exists: dates, durations and clock times shown on the Meet Planner screens,
 * formatted exactly like the web app ("5 Nov 2026", "1h 05m", "9:00 AM").
 */

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun monthShort(date: LocalDate): String = MONTHS[date.monthValue - 1]

/** "5 Nov 2026". */
fun formatDate(date: LocalDate): String = "${date.dayOfMonth} ${monthShort(date)} ${date.year}"

/** "5 Nov – 7 Nov 2026", or a single date when there is no (different) end date. Null when no start date. */
fun formatRange(from: LocalDate?, to: LocalDate?): String? {
    if (from == null) return null
    if (to == null || to == from) return formatDate(from)
    val start = formatDate(from).replace(Regex(" \\d{4}$"), "")
    return "$start – ${formatDate(to)}"
}

/** "1h 05m" for an hour or more, otherwise "45 min". */
fun formatDuration(minutes: Double?): String {
    val m = Math.round(minutes ?: 0.0).toInt()
    return if (m >= 60) "${m / 60}h ${(m % 60).toString().padStart(2, '0')}m" else "$m min"
}

/** "12.3 km". */
fun formatKm(km: Double): String = String.format(Locale.US, "%.1f km", km)

/** Minutes after midnight to "9:00 AM". Wraps past midnight. */
fun formatClock(minutes: Int): String {
    val wrapped = ((minutes % 1440) + 1440) % 1440
    val h = wrapped / 60
    val m = wrapped % 60
    val hour12 = if (h % 12 == 0) 12 else h % 12
    return "$hour12:${m.toString().padStart(2, '0')} ${if (h < 12) "AM" else "PM"}"
}

/** Whole days from [today] to [date] (negative when it is in the past). */
fun daysUntil(date: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(today, date)

/** The pill on a meetup card: Today, Tomorrow, In N days, or N days ago. */
sealed interface WhenLabel {
    data object Today : WhenLabel
    data object Tomorrow : WhenLabel
    data class InDays(val days: Long) : WhenLabel
    data class DaysAgo(val days: Long) : WhenLabel
}

fun whenLabel(date: LocalDate, today: LocalDate): WhenLabel = when (val n = daysUntil(date, today)) {
    0L -> WhenLabel.Today
    1L -> WhenLabel.Tomorrow
    else -> if (n > 1) WhenLabel.InDays(n) else WhenLabel.DaysAgo(-n)
}

/** Nights between start and end dates, or null when there is no end date. */
fun nights(from: LocalDate?, to: LocalDate?): Long? =
    if (from == null || to == null) null else maxOf(0L, ChronoUnit.DAYS.between(from, to))

data class CountdownParts(val days: Long, val hours: Long, val minutes: Long)

/** Time left until midnight at the start of [date]. Null once that moment has passed. */
fun countdownParts(date: LocalDate, now: LocalDateTime): CountdownParts? {
    val diff = Duration.between(now, date.atStartOfDay())
    if (diff.isZero || diff.isNegative) return null
    return CountdownParts(diff.toDays(), diff.toHours() % 24, diff.toMinutes() % 60)
}
