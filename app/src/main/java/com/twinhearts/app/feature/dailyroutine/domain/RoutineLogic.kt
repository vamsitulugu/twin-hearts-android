package com.twinhearts.app.feature.dailyroutine.domain

import com.twinhearts.app.feature.dailyroutine.domain.model.DayRow
import com.twinhearts.app.feature.dailyroutine.domain.model.DaySummary
import com.twinhearts.app.feature.dailyroutine.domain.model.NextUp
import com.twinhearts.app.feature.dailyroutine.domain.model.Occurrence
import com.twinhearts.app.feature.dailyroutine.domain.model.Repeat
import com.twinhearts.app.feature.dailyroutine.domain.model.Routine
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineStatus
import com.twinhearts.app.feature.dailyroutine.domain.model.Visibility
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The schedule rules of Daily Routine as plain functions (so they can be tested). Same rules as
 * the web page public/daily-routine.html: which routines fall on a day, what status each has,
 * overlaps, free time and the summary.
 */

private const val DAY_MINUTES = 1440
private const val MIN_FREE_GAP = 20
private const val NOON_HOUR = 12
private const val HOUR = 60

/** "07:30" -> 450. Anything that is not HH:mm counts as 0. */
fun toMinutes(time: String): Int {
    val parts = time.split(":")
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: return 0
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return 0
    return hours * HOUR + minutes
}

/** "07:30" -> "7:30 AM", "00:05" -> "12:05 AM". The web shows 12-hour times. */
fun formatTime12(time: String): String {
    val minutes = toMinutes(time)
    val hours = minutes / HOUR
    val suffix = if (hours >= NOON_HOUR) "PM" else "AM"
    val shown = (hours % NOON_HOUR).let { if (it == 0) NOON_HOUR else it }
    return "%d:%02d %s".format(shown, minutes % HOUR, suffix)
}

/** Length of an activity in minutes; an end before the start means it goes past midnight. */
fun durationMinutes(start: String, end: String): Int = (toMinutes(end) - toMinutes(start) + DAY_MINUTES) % DAY_MINUTES

/** 0 = Sunday ... 6 = Saturday, as the web's `getDay()`. */
private fun weekDay(date: LocalDate): Int = date.dayOfWeek.value % 7

fun occursOn(routine: Routine, date: LocalDate): Boolean {
    if (routine.repeat == Repeat.None) return routine.date == date
    val start = routine.date
    if (start != null && start.isAfter(date)) return false // has not started yet
    val day = weekDay(date)
    return when (routine.repeat) {
        Repeat.Daily -> true
        Repeat.Weekdays -> day in 1..5
        Repeat.Weekends -> day == 0 || day == 6
        Repeat.Custom -> day in routine.repeatDays
        Repeat.None -> false
    }
}

fun occurrenceFor(routine: Routine, date: LocalDate): Occurrence {
    val key = date.toString()
    val override = routine.timeByDate[key]
    val saved = routine.statusByDate[key] ?: RoutineStatus.Upcoming
    return Occurrence(
        routine = routine,
        date = date,
        startTime = override?.start ?: routine.startTime,
        endTime = override?.end ?: routine.endTime,
        saved = saved,
        status = saved,
    )
}

/**
 * Activities of [role] on [date], earliest first. Another person's routines are only visible
 * when they chose "Shared" (privacy rule of the web page), so pass [isPartner] for that tab.
 */
fun occurrencesForDate(routines: List<Routine>, role: String, isPartner: Boolean, date: LocalDate, now: LocalDateTime): List<Occurrence> =
    routines
        .filter { it.role == role }
        .filter { !isPartner || it.visibility == Visibility.Shared }
        .filter { occursOn(it, date) }
        .map { occurrenceFor(it, date) }
        .sortedWith(compareBy({ toMinutes(it.startTime) }, { it.endTime }))
        .map { it.copy(status = computeStatus(it, now)) }

/** Completed and Skipped are kept; the rest depends on the clock. */
fun computeStatus(occurrence: Occurrence, now: LocalDateTime): RoutineStatus {
    if (occurrence.saved == RoutineStatus.Completed || occurrence.saved == RoutineStatus.Skipped) return occurrence.saved
    val today = now.toLocalDate()
    if (occurrence.date != today) return if (occurrence.date.isBefore(today)) RoutineStatus.Missed else RoutineStatus.Upcoming
    val nowMinute = now.hour * HOUR + now.minute
    val start = toMinutes(occurrence.startTime)
    val end = toMinutes(occurrence.endTime)
    val overnight = end <= start
    val inRange = if (overnight) nowMinute >= start || nowMinute < end else nowMinute in start until end
    return when {
        inRange -> RoutineStatus.InProgress
        !overnight && nowMinute >= end -> RoutineStatus.Missed
        else -> RoutineStatus.Upcoming
    }
}

/** True when the new time overlaps another activity of the day (an end before the start runs past midnight). */
fun hasConflict(others: List<Occurrence>, start: String, end: String): Boolean {
    val s1 = toMinutes(start)
    val e1 = toMinutes(end).let { if (it <= s1) it + DAY_MINUTES else it }
    return others.any { other ->
        val s2 = toMinutes(other.startTime)
        val e2 = toMinutes(other.endTime).let { if (it <= s2) it + DAY_MINUTES else it }
        s1 < e2 && s2 < e1
    }
}

fun summarize(occurrences: List<Occurrence>, isToday: Boolean, now: LocalDateTime): DaySummary {
    val total = occurrences.size
    val done = occurrences.count { it.status == RoutineStatus.Completed }
    val open = occurrences.filter { it.status == RoutineStatus.Upcoming || it.status == RoutineStatus.InProgress }
    val nowMinute = now.hour * HOUR + now.minute
    val next = if (isToday) open.firstOrNull()?.let { NextUp(it, toMinutes(it.startTime) - nowMinute) } else null
    return DaySummary(
        total = total,
        done = done,
        percent = if (total == 0) 0 else Math.round(done * 100f / total),
        remaining = open.size,
        next = next,
    )
}

/** The list the screen draws: activities, free-time gaps of 20 minutes or more, and the NOW line on today. */
fun buildRows(occurrences: List<Occurrence>, isToday: Boolean, now: LocalDateTime): List<DayRow> {
    val rows = mutableListOf<DayRow>()
    val nowMinute = now.hour * HOUR + now.minute
    val nowText = "%02d:%02d".format(now.hour, now.minute)
    var nowInserted = !isToday
    occurrences.forEachIndexed { index, occurrence ->
        if (!nowInserted && toMinutes(occurrence.startTime) > nowMinute) {
            rows += DayRow.Now(nowText)
            nowInserted = true
        }
        rows += DayRow.Activity(occurrence)
        occurrences.getOrNull(index + 1)?.let { next ->
            val gap = toMinutes(next.startTime) - toMinutes(occurrence.endTime)
            if (gap >= MIN_FREE_GAP) rows += DayRow.FreeTime(occurrence.endTime, next.startTime, gap)
        }
    }
    if (!nowInserted) rows += DayRow.Now(nowText)
    return rows
}
