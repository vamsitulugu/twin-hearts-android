package com.twinhearts.app.feature.home.data.mapper

import com.twinhearts.app.feature.home.domain.model.CardPhoto
import com.twinhearts.app.feature.home.domain.model.CardPhotoSlot
import com.twinhearts.app.feature.home.domain.model.EventTarget
import com.twinhearts.app.feature.home.domain.model.EventTitle
import com.twinhearts.app.feature.home.domain.model.HomeOverview
import com.twinhearts.app.feature.home.domain.model.MoodBoard
import com.twinhearts.app.feature.home.domain.model.MoodKind
import com.twinhearts.app.feature.home.domain.model.UpcomingEvent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Turns the couple's shared state (one JSON document, see HomeApi) into what Home shows.
 * Rules are the ones in the web file dashboard-widgets.js (`collectEvents`, `moodOf`).
 */
private const val MAX_EVENTS = 8
private val DAY_MARKS = listOf(100, 200, 300, 365, 500, 730, 1000, 1500, 2000, 2500, 3000, 3650)
private val EVENT_ICONS = mapOf(
    "Anniversary" to "💕", "Date" to "🌹", "Birthday" to "🎂", "Trip" to "✈️",
    "Special" to "⭐", "Study" to "📚", "Other" to "📌",
)
private const val DEFAULT_EVENT_ICON = "📌"
private const val DEFAULT_MILESTONE_ICON = "💫"

fun JsonElement?.toHomeOverview(myRole: String, fallbackAnniversary: String, today: LocalDate): HomeOverview =
    HomeOverview(
        loveCard = toLoveCardState(myRole),
        events = toUpcomingEvents(fallbackAnniversary, today),
        mood = toMoodBoard(myRole),
        cardPhotos = toCardPhotos(),
    )

/** First 10 characters "YYYY-MM-DD" of a saved date, or null when it is not a date. */
internal fun String?.asIsoDate(): LocalDate? {
    val text = this?.trim().orEmpty()
    if (text.length < 10) return null
    return runCatching { LocalDate.parse(text.substring(0, 10)) }.getOrNull()
}

private fun nextYearly(date: LocalDate, today: LocalDate): LocalDate {
    // 29 Feb falls back to 28 Feb in non-leap years (java.time does this for us).
    val thisYear = date.withYear(today.year)
    return if (thisYear.isBefore(today)) date.withYear(today.year + 1) else thisYear
}

fun JsonElement?.toUpcomingEvents(fallbackAnniversary: String, today: LocalDate): List<UpcomingEvent> {
    val state = this as? JsonObject
    val out = mutableListOf<UpcomingEvent>()
    fun add(icon: String, title: EventTitle, date: LocalDate, target: EventTarget, detail: String = "") {
        out += UpcomingEvent(icon, title, date, ChronoUnit.DAYS.between(today, date).toInt(), detail, target)
    }

    state?.get("myBday").asText().asIsoDate()?.let {
        add("🎂", EventTitle.YourBirthday, nextYearly(it, today), EventTarget.LoveCounter)
    }
    state?.get("ptBday").asText().asIsoDate()?.let {
        add("🎂", EventTitle.PartnerBirthday, nextYearly(it, today), EventTarget.LoveCounter)
    }

    val anniversary = (state?.get("anniversary").asText() ?: fallbackAnniversary).asIsoDate()
    if (anniversary != null && !anniversary.isAfter(today)) {
        val yearly = nextYearly(anniversary, today)
        val years = yearly.year - anniversary.year
        if (years > 0) add("💕", EventTitle.Anniversary(years), yearly, EventTarget.LoveCounter)

        // Monthly anniversary: the next day-of-month, shortened for short months.
        fun monthDate(ym: YearMonth) = ym.atDay(minOf(anniversary.dayOfMonth, ym.lengthOfMonth()))
        var nextMonth = monthDate(YearMonth.from(today))
        if (nextMonth.isBefore(today)) nextMonth = monthDate(YearMonth.from(today).plusMonths(1))
        val months = (nextMonth.year - anniversary.year) * 12 + nextMonth.monthValue - anniversary.monthValue
        if (months > 0 && months % 12 != 0) add("🥂", EventTitle.MonthsTogether(months), nextMonth, EventTarget.LoveCounter)

        val together = ChronoUnit.DAYS.between(anniversary, today).toInt()
        DAY_MARKS.firstOrNull { it >= together }?.let {
            add("✨", EventTitle.DayMark(it), anniversary.plusDays(it.toLong()), EventTarget.LoveCounter)
        }
    }

    (state?.get("events") as? JsonArray)?.forEach { item ->
        val event = item as? JsonObject ?: return@forEach
        val date = event["date"].asText().asIsoDate() ?: return@forEach
        val title = event["title"].asText()?.takeIf { it.isNotBlank() } ?: return@forEach
        if (date.isBefore(today)) return@forEach
        add(
            EVENT_ICONS[event["cat"].asText()] ?: DEFAULT_EVENT_ICON,
            EventTitle.Custom(title), date, EventTarget.Calendar, event["time"].asText().orEmpty(),
        )
    }
    (state?.get("milestones") as? JsonArray)?.forEach { item ->
        val milestone = item as? JsonObject ?: return@forEach
        val date = milestone["date"].asText().asIsoDate() ?: return@forEach
        val title = milestone["title"].asText()?.takeIf { it.isNotBlank() } ?: return@forEach
        if (date.isBefore(today)) return@forEach
        add(
            milestone["type"].asText()?.takeIf { it.isNotBlank() } ?: DEFAULT_MILESTONE_ICON,
            EventTitle.Custom(title), date, EventTarget.LoveCounter,
        )
    }
    return out.sortedBy { it.daysLeft }.take(MAX_EVENTS)
}

/** `profile.u1.moods` / `profile.u2.moods`: `{ "2026-10-26": "Happy" }`. user1 → u1, user2 → u2. */
fun JsonElement?.toMoodBoard(myRole: String): MoodBoard {
    val profile = (this as? JsonObject)?.get("profile") as? JsonObject ?: return MoodBoard()
    val myKey = profileKey(myRole)
    val partnerKey = if (myKey == "u1") "u2" else "u1"
    return MoodBoard(mine = profile.moodsOf(myKey), partner = profile.moodsOf(partnerKey))
}

internal fun profileKey(role: String) = if (role == "user2") "u2" else "u1"

private fun JsonObject.moodsOf(key: String): Map<String, MoodKind> {
    val moods = ((this[key] as? JsonObject)?.get("moods") as? JsonObject) ?: return emptyMap()
    return buildMap {
        moods.forEach { (date, value) -> MoodKind.fromKey(value.asText())?.let { put(date, it) } }
    }
}

/** `dashImgs`: `{ idle|hug|touch|miss: { url, path } }`. A slot without a usable url is ignored. */
fun JsonElement?.toCardPhotos(): Map<CardPhotoSlot, CardPhoto> {
    val images = (this as? JsonObject)?.get("dashImgs") as? JsonObject ?: return emptyMap()
    return buildMap {
        CardPhotoSlot.entries.forEach { slot ->
            val entry = images[slot.key] as? JsonObject ?: return@forEach
            val url = entry["url"].asText()?.takeIf { it.isNotBlank() } ?: return@forEach
            put(slot, CardPhoto(url = url, path = entry["path"].asText().orEmpty()))
        }
    }
}
