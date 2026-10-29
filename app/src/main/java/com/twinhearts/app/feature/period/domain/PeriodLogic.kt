package com.twinhearts.app.feature.period.domain

import com.twinhearts.app.feature.period.domain.model.CycleBar
import com.twinhearts.app.feature.period.domain.model.CycleInsight
import com.twinhearts.app.feature.period.domain.model.CyclePhase
import com.twinhearts.app.feature.period.domain.model.DayMark
import com.twinhearts.app.feature.period.domain.model.Period
import com.twinhearts.app.feature.period.domain.model.PhaseInfo
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Why this file exists: the cycle maths of the web page (`getPeriodPhase`, `renderPeriodCalendar`,
 * `renderPeriodAnalytics`) as pure functions, so they are easy to test. [periods] is newest first.
 */
const val DEFAULT_CYCLE_DAYS = 28
const val DEFAULT_FLOW_DAYS = 5

fun phaseInfo(periods: List<Period>, today: LocalDate): PhaseInfo {
    val last = periods.firstOrNull()
        ?: return PhaseInfo(CyclePhase.Unknown, null, DEFAULT_CYCLE_DAYS, DEFAULT_FLOW_DAYS)
    val diff = ChronoUnit.DAYS.between(last.start, today).toInt()
    val phase = when {
        diff < 0 -> CyclePhase.Luteal
        diff < last.flow -> CyclePhase.Menstrual
        diff < last.cycle * 0.45 -> CyclePhase.Follicular
        diff < last.cycle * 0.55 -> CyclePhase.Ovulation
        else -> CyclePhase.Luteal
    }
    val next = last.start.plusDays(last.cycle.toLong())
    val daysToNext = ChronoUnit.DAYS.between(today, next).toInt().coerceAtLeast(0)
    return PhaseInfo(phase, daysToNext, last.cycle, last.flow)
}

/** Period days, the ovulation day (cycle − 14) and two fertile days either side, for one month. */
fun dayMarks(periods: List<Period>, month: YearMonth): Map<Int, DayMark> {
    val period = mutableSetOf<Int>()
    val ovulation = mutableSetOf<Int>()
    val fertile = mutableSetOf<Int>()
    fun inMonth(d: LocalDate) = YearMonth.from(d) == month
    periods.forEach { p ->
        for (i in 0 until p.flow) p.start.plusDays(i.toLong()).takeIf(::inMonth)?.let { period += it.dayOfMonth }
        val ov = p.start.plusDays((p.cycle - 14).toLong())
        for (i in -2..2) {
            val d = ov.plusDays(i.toLong())
            if (!inMonth(d)) continue
            if (i == 0) ovulation += d.dayOfMonth else fertile += d.dayOfMonth
        }
    }
    val marks = mutableMapOf<Int, DayMark>()
    fertile.forEach { marks[it] = DayMark.Fertile }
    ovulation.forEach { marks[it] = DayMark.Ovulation }
    period.forEach { marks[it] = DayMark.Period }
    return marks
}

/** Days between each pair of neighbouring period starts. */
private fun gaps(periods: List<Period>): List<Int> =
    periods.zipWithNext { newer, older -> abs(ChronoUnit.DAYS.between(older.start, newer.start).toInt()) }

fun cycleInsight(periods: List<Period>): CycleInsight {
    if (periods.size < 2) return CycleInsight.NeedMore
    val cycles = gaps(periods)
    val average = Math.round(cycles.average()).toInt()
    val variance = cycles.max() - cycles.min()
    return if (variance <= 3) CycleInsight.Regular(average) else CycleInsight.Varies(average, variance)
}

/** Up to the last six periods, oldest to newest, as bars. Empty with fewer than two periods. */
fun recentCycleBars(periods: List<Period>): List<CycleBar> {
    if (periods.size < 2) return emptyList()
    val recent = periods.take(6).reversed()
    return recent.zipWithNext { older, newer ->
        CycleBar(abs(ChronoUnit.DAYS.between(older.start, newer.start).toInt()), newer.start)
    }
}

/** The five most logged symptoms with how often, biggest first. */
fun topSymptoms(symptoms: Map<String, List<String>>): List<Pair<String, Int>> =
    symptoms.values.flatten().groupingBy { it }.eachCount().entries
        .sortedByDescending { it.value }
        .take(5)
        .map { it.key to it.value }
