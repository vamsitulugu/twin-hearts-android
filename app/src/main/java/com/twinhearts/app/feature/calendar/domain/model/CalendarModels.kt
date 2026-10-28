package com.twinhearts.app.feature.calendar.domain.model

import androidx.annotation.StringRes
import com.twinhearts.app.R
import java.time.LocalDate

/**
 * Event categories, same keys and emoji as the web calendar (`#evCat`).
 * [key] is what is stored in the shared state (`cat`), so never rename it.
 */
enum class EventCategory(val key: String, val emoji: String, @StringRes val labelRes: Int) {
    Anniversary("Anniversary", "💕", R.string.calendar_cat_anniversary),
    Date("Date", "🌹", R.string.calendar_cat_date),
    Birthday("Birthday", "🎂", R.string.calendar_cat_birthday),
    Trip("Trip", "✈️", R.string.calendar_cat_trip),
    Special("Special", "⭐", R.string.calendar_cat_special),
    Study("Study", "📚", R.string.calendar_cat_study),
    Other("Other", "📌", R.string.calendar_cat_other);

    companion object {
        fun fromKey(key: String?): EventCategory = entries.firstOrNull { it.key == key } ?: Other
    }
}

/** Who gets the reminder (`alarm` on the web: "", "self", "both"). */
enum class EventAlarm(val key: String, @StringRes val labelRes: Int) {
    None("", R.string.calendar_alarm_none),
    Self("self", R.string.calendar_alarm_self),
    Both("both", R.string.calendar_alarm_both);

    companion object {
        fun fromKey(key: String?): EventAlarm = entries.firstOrNull { it.key == key } ?: None
    }
}

/** One calendar event. [time] is "HH:mm" or empty (the web stores it the same way). */
data class CalendarEvent(
    val id: String,
    val title: String,
    val date: LocalDate,
    val time: String,
    val note: String,
    val category: EventCategory,
    val alarm: EventAlarm,
)

/** What the "Add Event" form hands to the repository. */
data class NewEvent(
    val title: String,
    val date: LocalDate,
    val time: String,
    val note: String,
    val category: EventCategory,
    val alarm: EventAlarm,
)
