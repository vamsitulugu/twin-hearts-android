package com.twinhearts.app.feature.dailyroutine

import com.twinhearts.app.feature.dailyroutine.domain.buildRows
import com.twinhearts.app.feature.dailyroutine.domain.computeStatus
import com.twinhearts.app.feature.dailyroutine.domain.durationMinutes
import com.twinhearts.app.feature.dailyroutine.domain.formatTime12
import com.twinhearts.app.feature.dailyroutine.domain.hasConflict
import com.twinhearts.app.feature.dailyroutine.domain.model.DayRow
import com.twinhearts.app.feature.dailyroutine.domain.model.Reminder
import com.twinhearts.app.feature.dailyroutine.domain.model.Repeat
import com.twinhearts.app.feature.dailyroutine.domain.model.Routine
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineStatus
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineType
import com.twinhearts.app.feature.dailyroutine.domain.model.TimeOverride
import com.twinhearts.app.feature.dailyroutine.domain.model.Visibility
import com.twinhearts.app.feature.dailyroutine.domain.occurrenceFor
import com.twinhearts.app.feature.dailyroutine.domain.occurrencesForDate
import com.twinhearts.app.feature.dailyroutine.domain.occursOn
import com.twinhearts.app.feature.dailyroutine.domain.summarize
import com.twinhearts.app.feature.dailyroutine.domain.toMinutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RoutineLogicTest {
    // Sunday 25 October 2026, 10:30
    private val now = LocalDateTime.of(2026, 10, 25, 10, 30)
    private val today = now.toLocalDate()

    private fun routine(
        id: String = "r",
        start: String = "09:00",
        end: String = "10:00",
        repeat: Repeat = Repeat.None,
        date: LocalDate? = today,
        role: String = "user1",
        visibility: Visibility = Visibility.Private,
        days: List<Int> = emptyList(),
        status: Map<String, RoutineStatus> = emptyMap(),
        times: Map<String, TimeOverride> = emptyMap(),
    ) = Routine(
        id = id, role = role, title = "T$id", description = "", type = RoutineType.Routine, location = "", notes = "",
        startTime = start, endTime = end, date = date, repeat = repeat, repeatDays = days, visibility = visibility,
        reminder = Reminder.None, statusByDate = status, timeByDate = times,
    )

    @Test
    fun time_formatting() {
        assertEquals("7:30 AM", formatTime12("07:30"))
        assertEquals("12:05 AM", formatTime12("00:05"))
        assertEquals("12:00 PM", formatTime12("12:00"))
        assertEquals("11:59 PM", formatTime12("23:59"))
        assertEquals(450, toMinutes("07:30"))
        assertEquals(0, toMinutes("junk"))
    }

    @Test
    fun duration_overMidnight() {
        assertEquals(60, durationMinutes("09:00", "10:00"))
        assertEquals(480, durationMinutes("23:00", "07:00"))
    }

    @Test
    fun oneOff_onlyOnItsDate() {
        val r = routine()
        assertTrue(occursOn(r, today))
        assertFalse(occursOn(r, today.plusDays(1)))
    }

    @Test
    fun daily_startsOnItsDate_notBefore() {
        val r = routine(repeat = Repeat.Daily)
        assertFalse(occursOn(r, today.minusDays(1)))
        assertTrue(occursOn(r, today))
        assertTrue(occursOn(r, today.plusDays(40)))
    }

    @Test
    fun weekdaysAndWeekends() {
        val weekdays = routine(repeat = Repeat.Weekdays, date = null)
        val weekends = routine(repeat = Repeat.Weekends, date = null)
        val sunday = LocalDate.of(2026, 10, 25)
        val monday = sunday.plusDays(1)
        assertFalse(occursOn(weekdays, sunday))
        assertTrue(occursOn(weekdays, monday))
        assertTrue(occursOn(weekends, sunday))
        assertTrue(occursOn(weekends, LocalDate.of(2026, 10, 24)))
        assertFalse(occursOn(weekends, monday))
    }

    @Test
    fun custom_usesSundayIsZero() {
        val r = routine(repeat = Repeat.Custom, date = null, days = listOf(0, 3))
        assertTrue(occursOn(r, LocalDate.of(2026, 10, 25))) // Sunday
        assertTrue(occursOn(r, LocalDate.of(2026, 10, 28))) // Wednesday
        assertFalse(occursOn(r, LocalDate.of(2026, 10, 26))) // Monday
    }

    @Test
    fun overrides_changeThatDayOnly() {
        val key = today.toString()
        val r = routine(
            status = mapOf(key to RoutineStatus.Completed),
            times = mapOf(key to TimeOverride("14:00", "15:00")),
        )
        val occurrence = occurrenceFor(r, today)
        assertEquals("14:00", occurrence.startTime)
        assertEquals(RoutineStatus.Completed, occurrence.saved)
    }

    @Test
    fun status_fromTheClock() {
        fun statusOf(start: String, end: String, at: LocalDateTime, date: LocalDate = at.toLocalDate()) =
            computeStatus(occurrenceFor(routine(start = start, end = end, date = date), date), at)
        assertEquals(RoutineStatus.InProgress, statusOf("10:00", "11:00", now))
        assertEquals(RoutineStatus.Upcoming, statusOf("11:00", "12:00", now))
        assertEquals(RoutineStatus.Missed, statusOf("08:00", "09:00", now))
        assertEquals(RoutineStatus.Missed, statusOf("08:00", "09:00", now, date = today.minusDays(1)))
        assertEquals(RoutineStatus.Upcoming, statusOf("08:00", "09:00", now, date = today.plusDays(1)))
    }

    @Test
    fun status_overnightIsInProgressAfterStartAndBeforeEnd() {
        val late = LocalDateTime.of(2026, 10, 25, 23, 30)
        val early = LocalDateTime.of(2026, 10, 25, 5, 0)
        val sleep = routine(start = "23:00", end = "07:00")
        assertEquals(RoutineStatus.InProgress, computeStatus(occurrenceFor(sleep, today), late))
        assertEquals(RoutineStatus.InProgress, computeStatus(occurrenceFor(sleep, today), early))
        assertEquals(RoutineStatus.Upcoming, computeStatus(occurrenceFor(sleep, today), now))
    }

    @Test
    fun savedCompletedAndSkipped_areKept() {
        val key = today.toString()
        val done = occurrenceFor(routine(status = mapOf(key to RoutineStatus.Completed)), today)
        val skipped = occurrenceFor(routine(status = mapOf(key to RoutineStatus.Skipped)), today)
        assertEquals(RoutineStatus.Completed, computeStatus(done, now))
        assertEquals(RoutineStatus.Skipped, computeStatus(skipped, now))
    }

    @Test
    fun partnerTab_showsOnlySharedRoutinesOfThePartner() {
        val routines = listOf(
            routine("a", role = "user1", visibility = Visibility.Shared),
            routine("b", role = "user2", visibility = Visibility.Private),
            routine("c", role = "user2", visibility = Visibility.Shared),
        )
        assertEquals(listOf("c"), occurrencesForDate(routines, "user2", true, today, now).map { it.routine.id })
        assertEquals(listOf("a"), occurrencesForDate(routines, "user1", false, today, now).map { it.routine.id })
    }

    @Test
    fun sortedByStartTime() {
        val routines = listOf(routine("late", start = "15:00", end = "16:00"), routine("early", start = "06:00", end = "07:00"))
        assertEquals(listOf("early", "late"), occurrencesForDate(routines, "user1", false, today, now).map { it.routine.id })
    }

    @Test
    fun conflict_overlapAndTouching() {
        val others = occurrencesForDate(listOf(routine(start = "09:00", end = "10:00")), "user1", false, today, now)
        assertTrue(hasConflict(others, "09:30", "10:30"))
        assertTrue(hasConflict(others, "08:00", "09:01"))
        assertFalse(hasConflict(others, "10:00", "11:00")) // touching is fine
        assertFalse(hasConflict(others, "08:00", "09:00"))
    }

    @Test
    fun conflict_overnightRoutine() {
        val others = occurrencesForDate(listOf(routine(start = "23:00", end = "07:00")), "user1", false, today, now)
        assertTrue(hasConflict(others, "22:00", "23:30"))
        assertFalse(hasConflict(others, "12:00", "13:00"))
    }

    @Test
    fun summary_percentRemainingAndNext() {
        val key = today.toString()
        val routines = listOf(
            routine("1", start = "07:00", end = "08:00", status = mapOf(key to RoutineStatus.Completed)),
            routine("2", start = "11:00", end = "12:00"),
            routine("3", start = "13:00", end = "14:00"),
        )
        val occurrences = occurrencesForDate(routines, "user1", false, today, now)
        val summary = summarize(occurrences, isToday = true, now = now)
        assertEquals(3, summary.total)
        assertEquals(1, summary.done)
        assertEquals(33, summary.percent)
        assertEquals(2, summary.remaining)
        assertEquals("2", summary.next?.occurrence?.routine?.id)
        assertEquals(30, summary.next?.minutesAway)
    }

    @Test
    fun summary_notToday_hasNoNext() {
        val occurrences = occurrencesForDate(listOf(routine()), "user1", false, today, now)
        assertNull(summarize(occurrences, isToday = false, now = now).next)
        assertEquals(0, summarize(emptyList(), true, now).percent)
    }

    @Test
    fun rows_haveNowLineAndFreeTimeGaps() {
        val routines = listOf(
            routine("1", start = "07:00", end = "08:00"),
            routine("2", start = "11:00", end = "12:00"),
            routine("3", start = "12:10", end = "13:00"),
        )
        val rows = buildRows(occurrencesForDate(routines, "user1", false, today, now), isToday = true, now = now)
        assertEquals(
            listOf("Activity", "FreeTime", "Now", "Activity", "Activity"),
            rows.map { it::class.simpleName },
        )
        val gap = rows[1] as DayRow.FreeTime
        assertEquals(180, gap.minutes)
        // 10-minute gap between 2 and 3 is too small to show
        assertEquals(1, rows.count { it is DayRow.FreeTime })
    }

    @Test
    fun rows_nowLineGoesLastWhenEverythingIsEarlier() {
        val rows = buildRows(occurrencesForDate(listOf(routine(start = "06:00", end = "07:00")), "user1", false, today, now), true, now)
        assertTrue(rows.last() is DayRow.Now)
        assertTrue(buildRows(emptyList(), false, now).isEmpty())
    }
}
