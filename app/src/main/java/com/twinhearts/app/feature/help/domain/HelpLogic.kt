package com.twinhearts.app.feature.help.domain

import com.twinhearts.app.feature.help.domain.model.FaqItem
import com.twinhearts.app.feature.help.domain.model.HelpCategory
import com.twinhearts.app.feature.help.domain.model.SendResult
import com.twinhearts.app.feature.help.domain.model.SupportMessage
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

/** The name the web sends to the server when the account has no name (help.html: 'Valued User'). */
const val DEFAULT_USER_NAME = "Valued User"

/** A message that is shown at once but not yet confirmed by the server starts with this. */
const val PENDING_ID_PREFIX = "pending-"

/** How often the open chat asks the server for new replies (help.html polls every 4 seconds). */
const val POLL_INTERVAL_MS = 4_000L

val SupportMessage.isPending: Boolean get() = id.startsWith(PENDING_ID_PREFIX)

/**
 * The question list for a search text and a topic chip. Same rule as the web `renderFaqs`:
 * the text may appear (any case) in the question, the answer or one of the tags.
 */
fun filterFaqs(faqs: List<FaqItem>, query: String, category: HelpCategory): List<FaqItem> {
    val needle = query.trim().lowercase()
    return faqs.filter { item ->
        val inTopic = category.serverName == null || item.category == category.serverName
        val matches = needle.isEmpty() ||
            item.question.lowercase().contains(needle) ||
            item.answer.lowercase().contains(needle) ||
            item.tags.any { it.lowercase().contains(needle) }
        inTopic && matches
    }
}

/** The server sends ISO times, with "Z" or an offset such as "+00:00". Anything else gives null. */
fun parseTimestamp(text: String?): Instant? {
    if (text.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(text.trim()).toInstant()
    } catch (e: DateTimeParseException) {
        null
    }
}

/** Hour and minute in the phone's own style (web: `toLocaleTimeString` with hour and minute). */
fun formatClock(time: Instant, zone: ZoneId, locale: Locale): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).withZone(zone).format(time)

/** First letter of the name for the little avatar. "U" when there is no name (web default). */
fun initialOf(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "U"
    return String(Character.toChars(trimmed.codePointAt(0))).uppercase()
}

/**
 * The thread after my message was sent. The server's ticket is the truth when it came back;
 * otherwise my not-yet-confirmed message is swapped for what the server confirmed.
 */
fun mergeSendResult(current: List<SupportMessage>, result: SendResult): List<SupportMessage> =
    result.ticket?.messages
        ?: (current.filterNot { it.isPending } + listOfNotNull(result.userMessage, result.aiMessage))
