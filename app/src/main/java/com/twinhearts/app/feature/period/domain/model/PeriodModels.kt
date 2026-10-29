package com.twinhearts.app.feature.period.domain.model

import androidx.annotation.StringRes
import com.twinhearts.app.R
import java.time.LocalDate

/** One logged period (web `S.periods` item: id, start, flow, cycle, note). */
data class Period(
    val id: String,
    val start: LocalDate,
    val flow: Int,
    val cycle: Int,
    val note: String,
)

/** What the "Log Period" form hands to the repository. */
data class NewPeriod(val start: LocalDate, val flow: Int, val cycle: Int, val note: String)

/**
 * The symptom chips, in the web order. [key] is what is stored in `symptoms`, so never rename it.
 */
enum class Symptom(val key: String, @StringRes val labelRes: Int) {
    Cramps("Cramps", R.string.period_sym_cramps),
    MoodSwings("Mood Swings", R.string.period_sym_mood_swings),
    Bloating("Bloating", R.string.period_sym_bloating),
    Fatigue("Fatigue", R.string.period_sym_fatigue),
    Headache("Headache", R.string.period_sym_headache),
    BackPain("Back Pain", R.string.period_sym_back_pain),
    Tender("Tender", R.string.period_sym_tender),
    Nausea("Nausea", R.string.period_sym_nausea),
    HighEnergy("High Energy", R.string.period_sym_high_energy),
    Acne("Acne", R.string.period_sym_acne),
}

/** Cycle phase, from the last logged period. */
enum class CyclePhase(@StringRes val labelRes: Int, @StringRes val noteRes: Int) {
    Menstrual(R.string.period_phase_menstrual, R.string.period_note_menstrual),
    Follicular(R.string.period_phase_follicular, R.string.period_note_follicular),
    Ovulation(R.string.period_phase_ovulation, R.string.period_note_ovulation),
    Luteal(R.string.period_phase_luteal, R.string.period_note_luteal),
    Unknown(R.string.period_phase_unknown, R.string.period_note_unknown),
}

/** Top card numbers. [daysToNext] is null when nothing is logged yet (shown as "—"). */
data class PhaseInfo(val phase: CyclePhase, val daysToNext: Int?, val cycleLength: Int, val flowDays: Int)

/** Marks on the cycle calendar. A day has at most one mark (period wins, then ovulation). */
enum class DayMark { Period, Ovulation, Fertile }

/** Cycle trend line (web `periodInsightCard`). */
sealed interface CycleInsight {
    data object NeedMore : CycleInsight
    data class Regular(val averageDays: Int) : CycleInsight
    data class Varies(val averageDays: Int, val varianceDays: Int) : CycleInsight
}

/** One bar of "Recent Cycle Lengths": days between two period starts, labelled by the later start. */
data class CycleBar(val days: Int, val label: LocalDate)

/** What this person may do on the page (web `Gender`). */
enum class PeriodAccess {
    /** Girl: log, save symptoms, delete. */
    Edit,

    /** Boy: sees everything, read-only. */
    ReadOnly,

    /** Not chosen yet: read-only, and we ask once. */
    Unknown,
}

/** Everything the page shows. [periods] are newest first. */
data class PeriodData(
    val periods: List<Period>,
    /** Date text (yyyy-MM-dd) to the symptoms saved that day. */
    val symptoms: Map<String, List<String>>,
    val access: PeriodAccess,
    val partnerName: String,
)
