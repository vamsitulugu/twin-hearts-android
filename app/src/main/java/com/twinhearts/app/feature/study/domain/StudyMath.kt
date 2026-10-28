package com.twinhearts.app.feature.study.domain

import com.twinhearts.app.feature.study.domain.model.StudyHero
import com.twinhearts.app.feature.study.domain.model.StudyInsight
import com.twinhearts.app.feature.study.domain.model.StudySlot
import com.twinhearts.app.feature.study.domain.model.Subject
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private const val MINUTES_PER_HOUR = 60.0
private const val GREAT_RATE = 80
private const val OK_RATE = 40

/** Sunday first, like the web "Weekly Study Hours" bars. */
val weekOrderSundayFirst = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

/** Length of a slot in hours. A slot without a start or end, or ending before it starts, counts 0. */
fun slotHours(slot: StudySlot): Double {
    val start = parseTime(slot.start) ?: return 0.0
    val end = parseTime(slot.end) ?: return 0.0
    val minutes = (end.toSecondOfDay() - start.toSecondOfDay()) / 60
    return if (minutes > 0) minutes / MINUTES_PER_HOUR else 0.0
}

/** Scheduled hours for each weekday, in [weekOrderSundayFirst] order. */
fun hoursByDay(slots: List<StudySlot>): List<Pair<DayOfWeek, Double>> =
    weekOrderSundayFirst.map { day ->
        day to slots.filter { it.day.dayOfWeek == day }.sumOf(::slotHours)
    }

fun studyHero(subjects: List<Subject>, slots: List<StudySlot>): StudyHero {
    val total = subjects.sumOf { it.total }
    val done = subjects.sumOf { it.done }
    return StudyHero(
        percent = if (total > 0) (done * 100f / total).roundToInt() else 0,
        weeklyHours = slots.sumOf(::slotHours),
        doneChapters = done,
        totalChapters = total,
        subjectCount = subjects.size,
    )
}

fun studyInsight(slots: List<StudySlot>): StudyInsight {
    if (slots.isEmpty()) return StudyInsight.NoSlots
    val done = slots.count { it.done }
    val rate = (done * 100f / slots.size).roundToInt()
    return when {
        rate >= GREAT_RATE -> StudyInsight.Great(rate)
        rate >= OK_RATE -> StudyInsight.Some(done, slots.size)
        else -> StudyInsight.Behind(done, slots.size)
    }
}

/** Slots of one day, earliest first. */
fun slotsOfDay(slots: List<StudySlot>, day: DayOfWeek): List<StudySlot> =
    slots.filter { it.day.dayOfWeek == day }.sortedBy { it.start }

fun parseTime(time: String): LocalTime? = runCatching { LocalTime.parse(time) }.getOrNull()

/** "HH:mm" -> short local time ("6:30 PM"). Empty or unreadable becomes "—". */
fun formatSlotTime(time: String): String {
    val parsed = parseTime(time) ?: return "—"
    return parsed.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}

fun LocalTime.toStoredTime(): String = "%02d:%02d".format(hour, minute)
