package com.twinhearts.app.feature.calendar.domain

import com.twinhearts.app.feature.calendar.domain.model.CalendarEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Days of one month as a grid, Sunday first (like the web). `null` = empty cell before day 1. */
fun monthCells(month: YearMonth): List<LocalDate?> {
    val leading = month.atDay(1).dayOfWeek.value % DAYS_IN_WEEK // Sunday = 7 -> 0, Monday = 1 ...
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    return List(leading) { null } + days
}

/** Events from [today] on (today included), soonest first, at most [limit] (web shows 10). */
fun upcoming(events: List<CalendarEvent>, today: LocalDate, limit: Int = UPCOMING_LIMIT): List<CalendarEvent> =
    events.filter { !it.date.isBefore(today) }
        .sortedWith(compareBy<CalendarEvent> { it.date }.thenBy { it.time })
        .take(limit)

fun eventsOn(events: List<CalendarEvent>, date: LocalDate): List<CalendarEvent> =
    events.filter { it.date == date }.sortedBy { it.time }

/** "HH:mm" -> short local time ("8:00 PM"). Anything unreadable is shown as it was saved. */
fun formatEventTime(time: String): String = try {
    LocalTime.parse(time).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
} catch (_: Exception) {
    time
}

fun LocalTime.toStoredTime(): String = "%02d:%02d".format(hour, minute)

private const val DAYS_IN_WEEK = 7
private const val UPCOMING_LIMIT = 10
