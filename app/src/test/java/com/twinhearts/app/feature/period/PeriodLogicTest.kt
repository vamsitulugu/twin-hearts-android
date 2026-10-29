package com.twinhearts.app.feature.period

import com.twinhearts.app.feature.period.data.accessFor
import com.twinhearts.app.feature.period.data.toPeriods
import com.twinhearts.app.feature.period.domain.cycleInsight
import com.twinhearts.app.feature.period.domain.dayMarks
import com.twinhearts.app.feature.period.domain.model.CycleInsight
import com.twinhearts.app.feature.period.domain.model.CyclePhase
import com.twinhearts.app.feature.period.domain.model.DayMark
import com.twinhearts.app.feature.period.domain.model.Period
import com.twinhearts.app.feature.period.domain.model.PeriodAccess
import com.twinhearts.app.feature.period.domain.phaseInfo
import com.twinhearts.app.feature.period.domain.recentCycleBars
import com.twinhearts.app.feature.period.domain.topSymptoms
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PeriodLogicTest {

    private fun period(start: String, flow: Int = 5, cycle: Int = 28) =
        Period(id = start, start = LocalDate.parse(start), flow = flow, cycle = cycle, note = "")

    @Test fun `nothing logged gives defaults and unknown phase`() {
        val info = phaseInfo(emptyList(), LocalDate.parse("2026-10-25"))
        assertEquals(CyclePhase.Unknown, info.phase)
        assertNull(info.daysToNext)
        assertEquals(28, info.cycleLength)
        assertEquals(5, info.flowDays)
    }

    @Test fun `phase follows the days since the last start`() {
        val periods = listOf(period("2026-10-01"))
        fun phaseOn(day: String) = phaseInfo(periods, LocalDate.parse(day)).phase
        assertEquals(CyclePhase.Menstrual, phaseOn("2026-10-03"))   // day 2 < flow 5
        assertEquals(CyclePhase.Follicular, phaseOn("2026-10-08"))  // day 7 < 12.6
        assertEquals(CyclePhase.Ovulation, phaseOn("2026-10-14"))   // day 13 < 15.4
        assertEquals(CyclePhase.Luteal, phaseOn("2026-10-20"))      // day 19
    }

    @Test fun `days to next never goes below zero`() {
        val periods = listOf(period("2026-10-01"))
        assertEquals(4, phaseInfo(periods, LocalDate.parse("2026-10-25")).daysToNext)
        assertEquals(0, phaseInfo(periods, LocalDate.parse("2026-12-01")).daysToNext)
    }

    @Test fun `calendar marks period, ovulation and fertile days with period winning`() {
        val marks = dayMarks(listOf(period("2026-10-01")), YearMonth.of(2026, 10))
        assertEquals(DayMark.Period, marks[1])
        assertEquals(DayMark.Period, marks[5])
        assertNull(marks[6])
        assertEquals(DayMark.Ovulation, marks[15]) // 1 Oct + (28 - 14)
        assertEquals(DayMark.Fertile, marks[13])
        assertEquals(DayMark.Fertile, marks[17])
    }

    @Test fun `insight needs two periods, then says regular or varies`() {
        assertEquals(CycleInsight.NeedMore, cycleInsight(listOf(period("2026-10-01"))))
        val regular = listOf(period("2026-10-01"), period("2026-09-03"), period("2026-08-06"))
        assertEquals(CycleInsight.Regular(28), cycleInsight(regular))
        val varying = listOf(period("2026-10-01"), period("2026-09-01"), period("2026-08-10"))
        assertEquals(CycleInsight.Varies(26, 8), cycleInsight(varying))
    }

    @Test fun `recent bars run oldest to newest and are labelled by the later start`() {
        val bars = recentCycleBars(listOf(period("2026-10-01"), period("2026-09-03"), period("2026-08-06")))
        assertEquals(listOf(28, 28), bars.map { it.days })
        assertEquals(LocalDate.parse("2026-09-03"), bars.first().label)
        assertEquals(emptyList<Any>(), recentCycleBars(listOf(period("2026-10-01"))))
    }

    @Test fun `top symptoms are counted across days, biggest first, max five`() {
        val top = topSymptoms(
            mapOf(
                "2026-10-01" to listOf("Cramps", "Acne"),
                "2026-10-02" to listOf("Cramps"),
                "2026-10-03" to listOf("Cramps", "Fatigue", "Tender", "Nausea", "Bloating"),
            ),
        )
        assertEquals("Cramps" to 3, top.first())
        assertEquals(5, top.size)
    }

    @Test fun `periods parse newest first and skip unreadable items`() {
        val json = Json.parseToJsonElement(
            """[{"id":1,"start":"2026-09-03","flow":4,"cycle":30,"note":"x"},
               {"id":2,"start":"2026-10-01"},
               {"id":3,"start":"not a date"}]""",
        ).jsonArray
        val periods = json.toPeriods()
        assertEquals(listOf("2026-10-01", "2026-09-03"), periods.map { it.start.toString() })
        assertEquals(5, periods[0].flow)   // web default
        assertEquals(28, periods[0].cycle) // web default
        assertEquals(30, periods[1].cycle)
    }

    @Test fun `access follows the shared profile first, then the sign-up choice`() {
        val profile = Json.parseToJsonElement("""{"u1":{"gender":"male"},"u2":{"gender":"female"}}""").jsonObject
        assertEquals(PeriodAccess.ReadOnly, accessFor(profile, "user1", "female"))
        assertEquals(PeriodAccess.Edit, accessFor(profile, "user2", null))
        assertEquals(PeriodAccess.Edit, accessFor(null, "user1", "female"))
        assertEquals(PeriodAccess.Unknown, accessFor(null, "user1", null))
        assertEquals(PeriodAccess.Unknown, accessFor(null, "user1", "other"))
    }
}
