package com.twinhearts.app.feature.myspace.domain.model

import androidx.annotation.StringRes
import com.twinhearts.app.R

/**
 * Who can see an item. Same keys and emoji as the web `priv-select` ("both", "self", "partner").
 * [key] is stored in the shared state as `visibility`, so never rename it.
 */
enum class Visibility(val key: String, val emoji: String, @StringRes val labelRes: Int) {
    Both("both", "👥", R.string.myspace_vis_both),
    Self("self", "🙋", R.string.myspace_vis_self),
    Partner("partner", "💌", R.string.myspace_vis_partner);

    companion object {
        /** A missing value means "both", the same default as the web `canSeeItem`. */
        fun fromKey(key: String?): Visibility = entries.firstOrNull { it.key == key } ?: Both
    }
}

/** The seven moods of the web journal (`#moodRow`). The text, emoji included, is what is stored. */
val JournalMoods: List<String> = listOf(
    "😊 Happy", "🥰 Loved", "😌 Peaceful", "😔 Sad", "😤 Stressed", "🥱 Tired", "🤩 Excited",
)

const val DefaultMood = "😊 Happy"

/** Who may be reminded (`forWhom` on the web: "both", "self", "partner"). Same keys as [Visibility]. */
typealias ReminderTarget = Visibility

/** [date] is `yyyy-MM-dd`. [by] is "user1" / "user2", or empty for items older than the role field. */
data class JournalEntry(
    val id: String,
    val date: String,
    val mood: String,
    val body: String,
    val visibility: Visibility,
    val by: String,
)

data class Habit(
    val id: String,
    val name: String,
    val emoji: String,
    val goal: Int,
    /** Days the habit was done, `yyyy-MM-dd`. */
    val doneDates: List<String>,
    val streak: Int,
)

data class Note(
    val id: String,
    val text: String,
    val date: String,
    /** The web stores a CSS colour like `rgba(107,9,58,0.12)`. Kept as is so the web can still show it. */
    val color: String,
    val visibility: Visibility,
    val by: String,
    /** The web's own flag: true when the person who wrote it was looking at it. */
    val me: Boolean,
    val reminderDate: String,
    val reminderTime: String,
)

data class Reminder(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val forWhom: ReminderTarget,
    val note: String,
    /** Role of the person who made it; web reminders have none. */
    val by: String = "",
)

/** One day of the Wellness tab (`wellness[date]` on the web). */
data class WellnessDay(
    val water: Int = 0,
    val sleep: Double = 0.0,
    val exercise: Int = 0,
    val mood: String = "",
) {
    val isEmpty: Boolean get() = water == 0 && sleep == 0.0 && exercise == 0
}

/** Everything the My Space page shows, read from the couple's shared state in one go. */
data class MySpaceData(
    val journal: List<JournalEntry>,
    val habits: List<Habit>,
    val notes: List<Note>,
    val reminders: List<Reminder>,
    val wellness: Map<String, WellnessDay>,
    val role: String,
    val myName: String,
    val partnerName: String,
)

/** What the "add" forms hand to the repository. */
data class NewJournal(val body: String, val mood: String, val visibility: Visibility)
data class NewHabit(val name: String, val emoji: String, val goal: Int)
data class NewNote(val text: String, val visibility: Visibility, val reminderDate: String, val reminderTime: String)
data class NewReminder(val title: String, val date: String, val time: String, val forWhom: ReminderTarget, val note: String)
