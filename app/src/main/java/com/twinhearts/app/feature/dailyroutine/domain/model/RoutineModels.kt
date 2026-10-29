package com.twinhearts.app.feature.dailyroutine.domain.model

import androidx.annotation.StringRes
import com.twinhearts.app.R
import java.time.LocalDate

/**
 * Plain data of the Daily Routine screen. The `key` of each enum is the exact text the server
 * stores (routes/daily-routine.js), so both apps read each other's routines.
 */

enum class RoutineType(val key: String, val emoji: String, @StringRes val labelRes: Int) {
    Routine("routine", "🔁", R.string.dr_type_routine),
    Habit("habit", "✅", R.string.dr_type_habit),
    Task("task", "📋", R.string.dr_type_task),
    Event("event", "📅", R.string.dr_type_event),
    Meal("meal", "🍽️", R.string.dr_type_meal),
    Workout("workout", "💪", R.string.dr_type_workout),
    Couple("couple", "💞", R.string.dr_type_couple),
    Focus("focus", "🎯", R.string.dr_type_focus),
    Break("break", "☕", R.string.dr_type_break),
    Sleep("sleep", "😴", R.string.dr_type_sleep);

    companion object {
        fun fromKey(key: String?): RoutineType = entries.firstOrNull { it.key == key } ?: Routine
    }
}

/** How a routine repeats. [Custom] exists on the server; the form only offers the first four. */
enum class Repeat(val key: String, @StringRes val labelRes: Int) {
    None("none", R.string.dr_repeat_none),
    Daily("daily", R.string.dr_repeat_daily),
    Weekdays("weekdays", R.string.dr_repeat_weekdays),
    Weekends("weekends", R.string.dr_repeat_weekends),
    Custom("custom", R.string.dr_repeat_custom);

    companion object {
        fun fromKey(key: String?): Repeat = entries.firstOrNull { it.key == key } ?: None

        /** What the form lets the person choose. */
        val formOptions = listOf(None, Daily, Weekdays, Weekends)
    }
}

enum class Reminder(val key: String, @StringRes val labelRes: Int) {
    None("none", R.string.dr_reminder_none),
    AtTime("at_time", R.string.dr_reminder_at_time),
    Min5("5min", R.string.dr_reminder_5),
    Min10("10min", R.string.dr_reminder_10),
    Min15("15min", R.string.dr_reminder_15),
    Min30("30min", R.string.dr_reminder_30),
    Hour1("1hour", R.string.dr_reminder_60);

    companion object {
        fun fromKey(key: String?): Reminder = entries.firstOrNull { it.key == key } ?: None
    }
}

enum class Visibility(val key: String, val emoji: String, @StringRes val labelRes: Int) {
    Private("private", "🔒", R.string.dr_vis_private),
    Shared("shared", "👥", R.string.dr_vis_shared);

    companion object {
        fun fromKey(key: String?): Visibility = entries.firstOrNull { it.key == key } ?: Private
    }
}

enum class RoutineStatus(val key: String, @StringRes val labelRes: Int) {
    Upcoming("upcoming", R.string.dr_status_upcoming),
    InProgress("in_progress", R.string.dr_status_in_progress),
    Completed("completed", R.string.dr_status_completed),
    Skipped("skipped", R.string.dr_status_skipped),
    Missed("missed", R.string.dr_status_missed);

    companion object {
        fun fromKey(key: String?): RoutineStatus = entries.firstOrNull { it.key == key } ?: Upcoming
    }
}

/** A different start / end for one single day (set by the server's reschedule call). */
data class TimeOverride(val start: String, val end: String)

/** One routine as stored: a template that may repeat. Times are "HH:mm" (24 hour). */
data class Routine(
    val id: String,
    /** "user1" or "user2": whose routine it is. */
    val role: String,
    val title: String,
    val description: String,
    val type: RoutineType,
    val location: String,
    val notes: String,
    val startTime: String,
    val endTime: String,
    /** First day for a repeating routine, the only day for a one-off. */
    val date: LocalDate?,
    val repeat: Repeat,
    /** Days of the week for [Repeat.Custom]: 0 = Sunday ... 6 = Saturday (same as the web). */
    val repeatDays: List<Int>,
    val visibility: Visibility,
    val reminder: Reminder,
    /** Status chosen for a day, by date text "YYYY-MM-DD". */
    val statusByDate: Map<String, RoutineStatus>,
    val timeByDate: Map<String, TimeOverride>,
)

/** A routine on one day, with that day's time and status worked out. */
data class Occurrence(
    val routine: Routine,
    val date: LocalDate,
    val startTime: String,
    val endTime: String,
    /** What was saved for the day (Upcoming when nothing). */
    val saved: RoutineStatus,
    /** What the screen shows: saved Completed / Skipped, or worked out from the clock. */
    val status: RoutineStatus,
)

/** What the person typed in the Add / Edit form. */
data class RoutineDraft(
    val title: String,
    val startTime: String,
    val endTime: String,
    val type: RoutineType,
    val description: String,
    val repeat: Repeat,
    val reminder: Reminder,
    val visibility: Visibility,
    val location: String,
    val notes: String,
)

/** The "Next" line of the summary card. */
data class NextUp(val occurrence: Occurrence, val minutesAway: Int)

data class DaySummary(
    val total: Int,
    val done: Int,
    val percent: Int,
    val remaining: Int,
    /** Only set for today. */
    val next: NextUp?,
)

/** One row of the day list: an activity, a free-time gap, or the "NOW" line. */
sealed interface DayRow {
    data class Activity(val occurrence: Occurrence) : DayRow
    data class FreeTime(val from: String, val to: String, val minutes: Int) : DayRow
    data class Now(val time: String) : DayRow
}
