package com.twinhearts.app.feature.calendar

import com.twinhearts.app.feature.calendar.domain.eventsOn
import com.twinhearts.app.feature.calendar.domain.formatEventTime
import com.twinhearts.app.feature.calendar.domain.monthCells
import com.twinhearts.app.feature.calendar.domain.model.CalendarEvent
import com.twinhearts.app.feature.calendar.domain.model.EventAlarm
import com.twinhearts.app.feature.calendar.domain.model.EventCategory
import com.twinhearts.app.feature.calendar.domain.upcoming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarMathTest {

    private fun event(id: String, date: String, time: String = "") = CalendarEvent(
        id, "Event $id", LocalDate.parse(date), time, "", EventCategory.Other, EventAlarm.None,
    )

    @Test fun `grid starts on Sunday with empty cells before day one`() {
        // 1 Nov 2026 is a Sunday: no empty cells. 1 Oct 2026 is a Thursday: 4 empty cells.
        assertNull(monthCells(YearMonth.of(2026, 10)).first())
        assertEquals(4, monthCells(YearMonth.of(2026, 10)).takeWhile { it == null }.size)
        assertEquals(LocalDate.of(2026, 11, 1), monthCells(YearMonth.of(2026, 11)).first())
    }

    @Test fun `grid holds every day of the month`() {
        assertEquals(31, monthCells(YearMonth.of(2026, 10)).filterNotNull().size)
        assertEquals(29, monthCells(YearMonth.of(2028, 2)).filterNotNull().size)
    }

    @Test fun `upcoming drops past days keeps today and sorts by date then time`() {
        val today = LocalDate.of(2026, 10, 28)
        val list = listOf(
            event("past", "2026-10-27"),
            event("later", "2026-11-02"),
            event("late-today", "2026-10-28", "20:00"),
            event("early-today", "2026-10-28", "08:00"),
        )
        assertEquals(listOf("early-today", "late-today", "later"), upcoming(list, today).map { it.id })
    }

    @Test fun `upcoming shows at most ten`() {
        val list = (1..15).map { event("e$it", "2026-11-%02d".format(it)) }
        assertEquals(10, upcoming(list, LocalDate.of(2026, 10, 28)).size)
    }

    @Test fun `eventsOn returns only that day sorted by time`() {
        val list = listOf(event("b", "2026-10-28", "18:00"), event("x", "2026-10-29"), event("a", "2026-10-28", "09:00"))
        assertEquals(listOf("a", "b"), eventsOn(list, LocalDate.of(2026, 10, 28)).map { it.id })
    }

    @Test fun `unreadable time is shown as saved`() {
        assertEquals("soon", formatEventTime("soon"))
    }
}
