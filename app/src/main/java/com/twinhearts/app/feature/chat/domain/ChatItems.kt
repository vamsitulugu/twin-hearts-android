package com.twinhearts.app.feature.chat.domain

import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*
 * Why this file exists: turns the message list into rows (day pills, grouped bubbles) and reads poll votes.
 */

/** What the message list shows: a day pill or a message. */
sealed interface ChatItem {
    val key: String

    data class Day(val date: LocalDate, val label: DayLabel) : ChatItem {
        override val key: String get() = "day-$date"
    }

    data class Msg(val message: ChatMessage, val position: GroupPosition = GroupPosition.Single) : ChatItem {
        override val key: String get() = message.clientId
    }
}

/**
 * Oldest first, with a [ChatItem.Day] pill before the first message of every day. Each message also
 * knows its place in a run from the same person on the same day (Telegram-style bubble corners).
 */
fun buildChatItems(messages: List<ChatMessage>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<ChatItem> {
    val items = mutableListOf<ChatItem>()
    var lastDay: LocalDate? = null
    messages.forEachIndexed { index, message ->
        val day = message.createdAt.toLocalDate(zone)
        if (day != lastDay) {
            items += ChatItem.Day(day, dayLabel(day, today))
            lastDay = day
        }
        items += ChatItem.Msg(message, groupPositionAt(messages, index, zone))
    }
    return items
}

private fun ChatMessage.joinsRuns(): Boolean = !deleted && kind != MessageKind.CallLog

/** `Single` when the neighbours are another person, another day, or deleted / call-log rows. */
fun groupPositionAt(messages: List<ChatMessage>, index: Int, zone: ZoneId = ZoneId.systemDefault()): GroupPosition {
    val current = messages.getOrNull(index) ?: return GroupPosition.Single
    if (!current.joinsRuns()) return GroupPosition.Single
    fun sameRun(other: ChatMessage?): Boolean =
        other != null && other.joinsRuns() && other.mine == current.mine &&
            other.createdAt.toLocalDate(zone) == current.createdAt.toLocalDate(zone)
    val previous = sameRun(messages.getOrNull(index - 1))
    val next = sameRun(messages.getOrNull(index + 1))
    return when {
        !previous && !next -> GroupPosition.Single
        !previous -> GroupPosition.First
        !next -> GroupPosition.Last
        else -> GroupPosition.Middle
    }
}

/** The text a poll vote is sent as (same words as the web), and how to read it back. */
fun voteText(option: String): String = "\uD83D\uDDF3\uFE0F Voted \"$option\""

private val VOTE = Regex("""^\S+ Voted "(.*)"$""")

/** How many votes each option of [poll] got: counted from the replies that look like a vote. */
fun pollVotes(poll: ChatMessage, all: List<ChatMessage>): Map<String, Int> {
    val counts = poll.pollOptions.associateWith { 0 }.toMutableMap()
    all.forEach { m ->
        if (m.deleted || m.replyToId != poll.id || m.kind != MessageKind.Text) return@forEach
        val option = VOTE.find(m.text.orEmpty().trim())?.groupValues?.get(1) ?: return@forEach
        if (option in counts) counts[option] = (counts[option] ?: 0) + 1
    }
    return counts
}

/** The newest-first count of messages I have not seen while scrolled up (the "jump to latest" badge). */
fun unseenCount(messages: List<ChatMessage>, seenUpTo: Instant?): Int =
    if (seenUpTo == null) 0 else messages.count { !it.mine && it.createdAt.isAfter(seenUpTo) }
