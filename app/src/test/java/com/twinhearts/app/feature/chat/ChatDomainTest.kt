package com.twinhearts.app.feature.chat

import com.twinhearts.app.feature.chat.data.mapper.parseServerTime
import com.twinhearts.app.feature.chat.domain.AgoAmount
import com.twinhearts.app.feature.chat.domain.ChatItem
import com.twinhearts.app.feature.chat.domain.DayLabel
import com.twinhearts.app.feature.chat.domain.agoAmount
import com.twinhearts.app.feature.chat.domain.buildChatItems
import com.twinhearts.app.feature.chat.domain.formatDuration
import com.twinhearts.app.feature.chat.domain.formatTimer
import com.twinhearts.app.feature.chat.domain.mergeMessages
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.DeliveryState
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import com.twinhearts.app.feature.chat.domain.model.PresenceState
import com.twinhearts.app.feature.chat.domain.presenceFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ChatDomainTest {
    private val now = Instant.parse("2026-10-26T10:00:00Z")

    private fun msg(id: Long, clientId: String, at: String, mine: Boolean = true, delivery: DeliveryState = DeliveryState.Sent) =
        ChatMessage(
            id = id, clientId = clientId, mine = mine, kind = MessageKind.Text, text = "t$id", mediaUrl = null,
            createdAt = Instant.parse(at), delivery = delivery,
        )

    @Test
    fun serverTime_acceptsZ_andOffsets_andRejectsJunk() {
        assertEquals(Instant.parse("2026-10-25T18:20:00Z"), parseServerTime("2026-10-25T18:20:00+00:00"))
        assertEquals(Instant.parse("2026-10-25T18:20:00.123Z"), parseServerTime("2026-10-25T18:20:00.123Z"))
        assertNull(parseServerTime("nope"))
        assertNull(parseServerTime(null))
    }

    @Test
    fun formats_durationAndTimer() {
        assertEquals("0:07", formatDuration(7))
        assertEquals("1:05", formatDuration(65))
        assertEquals("00:07", formatTimer(7))
    }

    @Test
    fun presence_followsWebThresholds() {
        assertEquals(PresenceState.Online, presenceFrom("online", now.minusSeconds(10), now).state)
        assertEquals(PresenceState.Away, presenceFrom("online", now.minusSeconds(60), now).state)
        assertEquals(PresenceState.Offline, presenceFrom("online", now.minusSeconds(600), now).state)
        assertEquals(PresenceState.Offline, presenceFrom("online", null, now).state)
    }

    @Test
    fun ago_picksTheRightUnit() {
        assertEquals(AgoAmount.JustNow, agoAmount(now.minusSeconds(20), now))
        assertEquals(AgoAmount.Minutes(5), agoAmount(now.minusSeconds(300), now))
        assertEquals(AgoAmount.Hours(3), agoAmount(now.minusSeconds(3 * 3600), now))
        assertEquals(AgoAmount.Days(3), agoAmount(now.minusSeconds(3 * 86_400L), now))
    }

    @Test
    fun merge_serverCopyReplacesLocalOne_andKeepsUnsentLocal() {
        val local = msg(ChatMessage.LOCAL_ID, "a", "2026-10-26T09:59:00Z", delivery = DeliveryState.Sending)
        val unsent = msg(ChatMessage.LOCAL_ID, "b", "2026-10-26T09:59:30Z", delivery = DeliveryState.Failed)
        val server = msg(41, "a", "2026-10-26T09:59:01Z")
        val merged = mergeMessages(listOf(local, unsent), listOf(server))
        assertEquals(listOf("a", "b"), merged.map { it.clientId })
        assertEquals(41L, merged.first().id)
        assertEquals(DeliveryState.Failed, merged.last().delivery)
    }

    @Test
    fun items_getADayPillPerDay() {
        val today = LocalDate.of(2026, 10, 26)
        val items = buildChatItems(
            listOf(msg(1, "a", "2026-10-25T10:00:00Z"), msg(2, "b", "2026-10-26T08:00:00Z"), msg(3, "c", "2026-10-26T09:00:00Z")),
            today, ZoneOffset.UTC,
        )
        val labels = items.filterIsInstance<ChatItem.Day>().map { it.label }
        assertEquals(listOf(DayLabel.Yesterday, DayLabel.Today), labels)
        assertEquals(5, items.size)
        assertTrue(items.first() is ChatItem.Day)
    }
}
