package com.twinhearts.app.feature.chat.domain

import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.PartnerPresence
import com.twinhearts.app.feature.chat.domain.model.PresenceState
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Why this file exists: all the small "how do we write this time" rules in one pure place,
 * so they can be unit tested. They follow the web chat (`fmtClock`, `fmtDaySep`, `fmtAgo`).
 */

private val clockFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** "11:50 PM" */
fun formatClock(time: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    clockFormat.format(time.atZone(zone))

/** Length of a voice note: "0:07". */
fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}"
}

/** Recording timer: "00:07". */
fun formatTimer(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "${(safe / 60).toString().padStart(2, '0')}:${(safe % 60).toString().padStart(2, '0')}"
}

/** The little pill between days. */
sealed interface DayLabel {
    data object Today : DayLabel
    data object Yesterday : DayLabel
    data class Date(val text: String) : DayLabel
}

fun dayLabel(day: LocalDate, today: LocalDate): DayLabel = when (day) {
    today -> DayLabel.Today
    today.minusDays(1) -> DayLabel.Yesterday
    else -> DayLabel.Date(dateFormat.format(day).uppercase(Locale.ENGLISH))
}

fun Instant.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate = atZone(zone).toLocalDate()

/** "Last seen 3d ago": the amount and the unit; the UI adds the words. */
sealed interface AgoAmount {
    data object JustNow : AgoAmount
    data class Minutes(val value: Int) : AgoAmount
    data class Hours(val value: Int) : AgoAmount
    data class Days(val value: Int) : AgoAmount
}

fun agoAmount(since: Instant, now: Instant): AgoAmount {
    val minutes = Duration.between(since, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> AgoAmount.JustNow
        minutes < 60 -> AgoAmount.Minutes(minutes.toInt())
        minutes < 60 * 24 -> AgoAmount.Hours((minutes / 60).toInt())
        else -> AgoAmount.Days((minutes / (60 * 24)).toInt())
    }
}

private const val ONLINE_WINDOW_MS = 35_000L
private const val AWAY_WINDOW_MS = 120_000L

/** Same thresholds as the web: online under 35 s, away under 2 min, else last seen. */
fun presenceFrom(status: String?, lastSeen: Instant?, now: Instant): PartnerPresence {
    if (lastSeen == null) return PartnerPresence(PresenceState.Offline, null)
    val ageMs = Duration.between(lastSeen, now).toMillis()
    val state = when {
        status == "online" && ageMs < ONLINE_WINDOW_MS -> PresenceState.Online
        ageMs < AWAY_WINDOW_MS -> PresenceState.Away
        else -> PresenceState.Offline
    }
    return PartnerPresence(state, lastSeen)
}

/**
 * Newest list after adding [incoming] to [current]. Matching is by clientId, so the server copy
 * replaces my local "sending" copy. Local copies the server does not know yet are kept.
 */
fun mergeMessages(current: List<ChatMessage>, incoming: List<ChatMessage>): List<ChatMessage> {
    val byKey = LinkedHashMap<String, ChatMessage>()
    current.forEach { byKey[it.clientId] = it }
    incoming.forEach { byKey[it.clientId] = it }
    return byKey.values.sortedWith(
        compareBy<ChatMessage> { it.createdAt }
            .thenBy { if (it.id == ChatMessage.LOCAL_ID) Long.MAX_VALUE else it.id },
    )
}

/** Minutes the user may pick for "send later": at least 1, at most one year. */
const val MIN_SCHEDULE_MINUTES = 1
const val MAX_SCHEDULE_MINUTES = 525_600
