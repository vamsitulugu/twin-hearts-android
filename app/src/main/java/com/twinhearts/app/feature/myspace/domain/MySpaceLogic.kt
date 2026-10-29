package com.twinhearts.app.feature.myspace.domain

import com.twinhearts.app.feature.myspace.domain.model.Habit
import com.twinhearts.app.feature.myspace.domain.model.JournalEntry
import com.twinhearts.app.feature.myspace.domain.model.Note
import com.twinhearts.app.feature.myspace.domain.model.Reminder
import com.twinhearts.app.feature.myspace.domain.model.Visibility
import com.twinhearts.app.feature.myspace.domain.model.WellnessDay
import java.time.LocalDate
import kotlin.math.roundToInt

/** Pure rules of the My Space page. They copy the web functions of the same name, so both apps agree. */
object MySpaceLogic {

    const val WATER_GOAL = 8
    const val SLEEP_GOAL = 7.0
    const val EXERCISE_GOAL = 30
    const val HEATMAP_DAYS = 84 // 12 weeks, as on the web

    /** Web `canSeeItem`: "self" is mine only, "partner" is for the other person, "both" is everyone. */
    fun canSee(visibility: Visibility, by: String, me: Boolean, myRole: String): Boolean = when (visibility) {
        Visibility.Both -> true
        Visibility.Self -> by == myRole || me || by.isBlank()
        Visibility.Partner -> by != myRole && !me
    }

    /** An entry is mine when I wrote it. Items from before the role field existed count as mine. */
    fun isMine(by: String, myRole: String): Boolean = by.isBlank() || by == myRole

    fun myJournal(all: List<JournalEntry>, role: String): List<JournalEntry> =
        all.filter { isMine(it.by, role) }

    /** The partner's entries that were shared with me ("both" or "partner"), never their private ones. */
    fun partnerJournal(all: List<JournalEntry>, role: String): List<JournalEntry> =
        all.filter { !isMine(it.by, role) && it.visibility != Visibility.Self }

    fun visibleNotes(all: List<Note>, role: String): List<Note> =
        all.filter { canSee(it.visibility, it.by, it.me && it.by.isBlank(), role) }

    fun visibleReminders(all: List<Reminder>): List<Reminder> = all

    /** Web `calcStreak`: days in a row ending today (or yesterday when today is not done yet). */
    fun streak(doneDates: Collection<String>, today: LocalDate = LocalDate.now()): Int {
        if (doneDates.isEmpty()) return 0
        val set = doneDates.toHashSet()
        var cursor = if (today.toString() in set) today else today.minusDays(1)
        var count = 0
        repeat(365) {
            if (cursor.toString() in set) {
                count++
                cursor = cursor.minusDays(1)
            } else {
                return count
            }
        }
        return count
    }

    fun doneToday(habit: Habit, today: LocalDate = LocalDate.now()): Boolean = today.toString() in habit.doneDates

    data class HabitSummary(
        val total: Int,
        val doneToday: Int,
        val avgStreak: Int,
        /** Share of the last seven days that were completed, 0..100. */
        val consistency: Int,
    )

    /** Web `renderHabitAnalytics` numbers: today's progress, average streak and 7-day consistency. */
    fun habitSummary(habits: List<Habit>, today: LocalDate = LocalDate.now()): HabitSummary {
        if (habits.isEmpty()) return HabitSummary(0, 0, 0, 0)
        val done = habits.count { doneToday(it, today) }
        val avg = (habits.sumOf { it.streak }.toDouble() / habits.size).roundToInt()
        val last7 = (0..6).map { today.minusDays(it.toLong()).toString() }
        val possible = habits.size * 7
        val actual = habits.sumOf { h -> last7.count { it in h.doneDates } }
        val consistency = if (possible > 0) (actual.toDouble() / possible * 100).roundToInt() else 0
        return HabitSummary(habits.size, done, avg, consistency)
    }

    /** 0 = none, 1 = under a third, 2 = under two thirds, 3 = two thirds or more (web heatmap shades). */
    fun heatLevel(done: Int, total: Int): Int {
        if (total <= 0 || done <= 0) return 0
        val frac = done.toDouble() / total
        return when {
            frac < 0.34 -> 1
            frac < 0.67 -> 2
            else -> 3
        }
    }

    /** Oldest first: one level for each of the last [HEATMAP_DAYS] days. */
    fun heatmap(habits: List<Habit>, today: LocalDate = LocalDate.now()): List<Int> =
        (HEATMAP_DAYS - 1 downTo 0).map { back ->
            val ds = today.minusDays(back.toLong()).toString()
            heatLevel(habits.count { ds in it.doneDates }, habits.size)
        }

    /** Number of wellness goals met on a day: water 8, sleep 7h, exercise 30 min. */
    fun goalsMet(day: WellnessDay): Int =
        (if (day.water >= WATER_GOAL) 1 else 0) +
            (if (day.sleep >= SLEEP_GOAL) 1 else 0) +
            (if (day.exercise >= EXERCISE_GOAL) 1 else 0)

    /** The last seven days, oldest first, with whatever was logged on each. */
    fun lastSevenDays(wellness: Map<String, WellnessDay>, today: LocalDate = LocalDate.now()): List<Pair<LocalDate, WellnessDay?>> =
        (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            d to wellness[d.toString()]
        }

    data class WellnessAverages(val water: Double, val sleep: Double)

    /** Web `renderWellnessSummary`: averages over every logged day. */
    fun wellnessAverages(wellness: Map<String, WellnessDay>): WellnessAverages {
        val days = wellness.values
        if (days.isEmpty()) return WellnessAverages(0.0, 0.0)
        return WellnessAverages(days.sumOf { it.water }.toDouble() / days.size, days.sumOf { it.sleep } / days.size)
    }

    fun parseDateOrNull(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()

    /** Soonest first; items without a readable time sort by date only. */
    fun sortedReminders(all: List<Reminder>): List<Reminder> = all.sortedBy { it.date + it.time }
}
