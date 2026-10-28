package com.twinhearts.app.feature.study.domain.model

import androidx.annotation.StringRes
import com.twinhearts.app.R
import java.time.DayOfWeek

/**
 * Days of the timetable. [key] is the English name the web stores in `day`, so never rename it
 * (the screen shows the day in the phone's language through [dayOfWeek]).
 */
enum class StudyDay(val key: String, val dayOfWeek: DayOfWeek) {
    Monday("Monday", DayOfWeek.MONDAY),
    Tuesday("Tuesday", DayOfWeek.TUESDAY),
    Wednesday("Wednesday", DayOfWeek.WEDNESDAY),
    Thursday("Thursday", DayOfWeek.THURSDAY),
    Friday("Friday", DayOfWeek.FRIDAY),
    Saturday("Saturday", DayOfWeek.SATURDAY),
    Sunday("Sunday", DayOfWeek.SUNDAY);

    companion object {
        fun fromKey(key: String?): StudyDay? = entries.firstOrNull { it.key == key }
    }
}

/** Who gets the reminder of a slot (`slotAlarm` on the web: "", "self", "both"). */
enum class SlotAlarm(val key: String, @StringRes val labelRes: Int) {
    None("", R.string.study_alarm_none),
    Self("self", R.string.study_alarm_self),
    Both("both", R.string.study_alarm_both),
}

data class Subject(
    val id: String,
    val name: String,
    val emoji: String,
    val total: Int,
    val done: Int,
) {
    val percent: Int get() = if (total > 0) Math.round(done * PERCENT / total).toInt() else 0

    private companion object {
        const val PERCENT = 100f
    }
}

/** One timetable slot. [start] / [end] are "HH:mm" or empty (the web stores them the same way). */
data class StudySlot(
    val id: String,
    val day: StudyDay,
    val start: String,
    val end: String,
    val subject: String,
    val done: Boolean,
)

data class StudyPerson(
    val subjects: List<Subject>,
    val slots: List<StudySlot>,
)

/** Everything the Study page shows. [partner] is empty until the partner has added something. */
data class StudyData(
    val me: StudyPerson,
    val partner: StudyPerson,
    val paired: Boolean,
    val myName: String,
    val partnerName: String,
)

/** What the "Add Subject" form hands to the repository. */
data class NewSubject(val name: String, val total: Int, val done: Int, val emoji: String)

/** What the "Add Timetable Slot" form hands to the repository. */
data class NewSlot(
    val day: StudyDay,
    val start: String,
    val end: String,
    val subject: String,
    val alarm: SlotAlarm,
)

/** Top numbers of the page (web `renderStudyHero`). */
data class StudyHero(
    val percent: Int,
    val weeklyHours: Double,
    val doneChapters: Int,
    val totalChapters: Int,
    val subjectCount: Int,
)

/** The one-line tip under the hero card (web `studyInsightCard`). */
sealed interface StudyInsight {
    data object NoSlots : StudyInsight
    data class Great(val percent: Int) : StudyInsight
    data class Some(val done: Int, val total: Int) : StudyInsight
    data class Behind(val done: Int, val total: Int) : StudyInsight
}
