package com.twinhearts.app.feature.chat.domain

import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import java.time.LocalDate
import java.time.ZoneId

/** What the message list shows: a day pill or a message. */
sealed interface ChatItem {
    val key: String

    data class Day(val date: LocalDate, val label: DayLabel) : ChatItem {
        override val key: String get() = "day-$date"
    }

    data class Msg(val message: ChatMessage) : ChatItem {
        override val key: String get() = message.clientId
    }
}

/** Oldest first, with a [ChatItem.Day] pill before the first message of every day. */
fun buildChatItems(messages: List<ChatMessage>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<ChatItem> {
    val items = mutableListOf<ChatItem>()
    var lastDay: LocalDate? = null
    messages.forEach { message ->
        val day = message.createdAt.toLocalDate(zone)
        if (day != lastDay) {
            items += ChatItem.Day(day, dayLabel(day, today))
            lastDay = day
        }
        items += ChatItem.Msg(message)
    }
    return items
}
