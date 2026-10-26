package com.twinhearts.app.feature.home.domain.model

import java.time.LocalDate

/**
 * The 8 moods of the web app (dashboard-widgets.js `MOODS`). [score] 1..5 is the height on the
 * 7-day graph. The key saved on the server is the English [label] ("Happy").
 */
enum class MoodKind(val label: String, val emoji: String, val score: Int) {
    Excited("Excited", "🤩", 5),
    Loved("Loved", "🥰", 5),
    Happy("Happy", "😊", 4),
    Peaceful("Peaceful", "😌", 4),
    Tired("Tired", "🥱", 2),
    Stressed("Stressed", "😤", 2),
    Sad("Sad", "😔", 1),
    Angry("Angry", "😡", 1),
    ;

    companion object {
        /** The five one-tap moods on the Home card (all eight are in the calendar). */
        val quick = listOf(Happy, Loved, Peaceful, Sad, Angry)

        /** Accepts "Happy", "happy" and the older "😊 Happy" form that Wellness saved. */
        fun fromKey(raw: String?): MoodKind? {
            val word = raw.orEmpty().trimStart { !it.isLetter() }.trim()
            return entries.firstOrNull { it.label.equals(word, ignoreCase = true) }
        }
    }
}

/** Both people's saved moods, one entry per day (key = ISO date "2026-10-26"). */
data class MoodBoard(
    val mine: Map<String, MoodKind> = emptyMap(),
    val partner: Map<String, MoodKind> = emptyMap(),
) {
    /** Days in a row ending today (or yesterday when today is not logged yet) — the 🔥 streak. */
    fun streak(today: LocalDate): Int {
        var day = if (mine.containsKey(today.toString())) today else today.minusDays(1)
        var count = 0
        while (count < MAX_STREAK_LOOKBACK && mine.containsKey(day.toString())) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    private companion object {
        const val MAX_STREAK_LOOKBACK = 400
    }
}

/** What an item in "Coming up" is called; the text is built in the UI from strings.xml. */
sealed interface EventTitle {
    data class Custom(val text: String) : EventTitle
    data object YourBirthday : EventTitle
    data object PartnerBirthday : EventTitle
    data class Anniversary(val years: Int) : EventTitle
    data class MonthsTogether(val months: Int) : EventTitle
    data class DayMark(val day: Int) : EventTitle
}

/** The page an item opens. Both are built in later steps. */
enum class EventTarget { Calendar, LoveCounter }

data class UpcomingEvent(
    val icon: String,
    val title: EventTitle,
    val date: LocalDate,
    val daysLeft: Int,
    /** Time of day for calendar events, empty otherwise. */
    val detail: String,
    val target: EventTarget,
)

/** Everything Home reads from the couple's shared state in one go. */
data class HomeOverview(
    val loveCard: LoveCardState = LoveCardState(),
    val events: List<UpcomingEvent> = emptyList(),
    val mood: MoodBoard = MoodBoard(),
    /** The couple's own card pictures (`dashImgs`); a slot that is missing uses the built-in picture. */
    val cardPhotos: Map<CardPhotoSlot, CardPhoto> = emptyMap(),
)
