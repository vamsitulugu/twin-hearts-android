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
import com.twinhearts.app.feature.chat.domain.groupPositionAt
import com.twinhearts.app.feature.chat.domain.GroupPosition
import com.twinhearts.app.feature.chat.domain.parseRichText
import com.twinhearts.app.feature.chat.domain.formatFileSize
import com.twinhearts.app.feature.chat.domain.linkHost
import com.twinhearts.app.feature.chat.domain.firstLink
import com.twinhearts.app.feature.chat.domain.isDndActive
import com.twinhearts.app.feature.chat.domain.DisappearAmount
import com.twinhearts.app.feature.chat.domain.disappearAmount
import com.twinhearts.app.feature.chat.domain.pollVotes
import com.twinhearts.app.feature.chat.domain.voteText
import com.twinhearts.app.feature.chat.domain.pushRecentEmoji
import com.twinhearts.app.feature.chat.domain.MAX_RECENT_EMOJI
import com.twinhearts.app.feature.chat.domain.searchEmoji
import com.twinhearts.app.feature.chat.domain.minCoverScale
import com.twinhearts.app.feature.chat.domain.clampOffset
import com.twinhearts.app.feature.chat.domain.visibleCrop
import com.twinhearts.app.feature.chat.domain.unseenCount
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/*
 * Why this file exists: unit tests for the Chat feature.
 */

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

    @Test
    fun richText_bold_italic_strike_mono_spoiler_link() {
        val pieces = parseRichText("*hi* _there_ ~no~ ```x``` ||s|| https://a.com")
        assertTrue(pieces.any { it.text == "hi" && it.bold })
        assertTrue(pieces.any { it.text == "there" && it.italic })
        assertTrue(pieces.any { it.text == "no" && it.strike })
        assertTrue(pieces.any { it.text == "x" && it.mono })
        assertTrue(pieces.any { it.text == "s" && it.spoiler })
        assertTrue(pieces.any { it.link == "https://a.com" })
    }

    @Test
    fun richText_leavesMathAndFileNamesAlone() {
        assertEquals(listOf("5*3=15"), parseRichText("5*3=15").map { it.text })
        assertEquals("my_file_name.pdf", parseRichText("my_file_name.pdf").joinToString("") { it.text })
    }

    @Test
    fun fileSizes_andLinks() {
        assertEquals("", formatFileSize(0))
        assertEquals("832 KB", formatFileSize(832 * 1024L))
        assertEquals("1.4 MB", formatFileSize((1.4 * 1024 * 1024).toLong()))
        assertEquals("example.com", linkHost("https://www.example.com/a"))
        assertEquals("https://x.io", firstLink("see https://x.io now"))
    }

    @Test
    fun dnd_handlesOvernightWindow() {
        assertTrue(isDndActive("22:00", "07:00", 23 * 60))
        assertTrue(isDndActive("22:00", "07:00", 6 * 60))
        assertFalse(isDndActive("22:00", "07:00", 12 * 60))
        assertFalse(isDndActive(null, null, 0))
    }

    @Test
    fun disappear_labels() {
        assertEquals(DisappearAmount.Off, disappearAmount(0))
        assertEquals(DisappearAmount.Hours(1), disappearAmount(3600))
        assertEquals(DisappearAmount.Days(7), disappearAmount(604_800))
    }

    @Test
    fun grouping_runsFromSamePersonSameDay() {
        val list = listOf(
            msg(1, "a", "2026-10-26T08:00:00Z"), msg(2, "b", "2026-10-26T08:01:00Z"), msg(3, "c", "2026-10-26T08:02:00Z"),
            msg(4, "d", "2026-10-26T08:03:00Z", mine = false),
        )
        assertEquals(GroupPosition.First, groupPositionAt(list, 0, ZoneOffset.UTC))
        assertEquals(GroupPosition.Middle, groupPositionAt(list, 1, ZoneOffset.UTC))
        assertEquals(GroupPosition.Last, groupPositionAt(list, 2, ZoneOffset.UTC))
        assertEquals(GroupPosition.Single, groupPositionAt(list, 3, ZoneOffset.UTC))
    }

    @Test
    fun pollVotes_countRepliesThatLookLikeVotes() {
        val poll = msg(10, "p", "2026-10-26T08:00:00Z").copy(kind = MessageKind.Poll, pollOptions = listOf("Pizza", "Sushi"))
        val v1 = msg(11, "v1", "2026-10-26T08:01:00Z").copy(text = voteText("Pizza"), replyToId = 10L)
        val v2 = msg(12, "v2", "2026-10-26T08:02:00Z").copy(text = voteText("Pizza"), replyToId = 10L)
        val other = msg(13, "o", "2026-10-26T08:03:00Z").copy(text = "hello", replyToId = 10L)
        assertEquals(mapOf("Pizza" to 2, "Sushi" to 0), pollVotes(poll, listOf(poll, v1, v2, other)))
    }

    @Test
    fun recentEmoji_newestFirst_noDuplicates_capped() {
        assertEquals(listOf("b", "a"), pushRecentEmoji(listOf("a", "b"), "b"))
        assertEquals(MAX_RECENT_EMOJI, pushRecentEmoji((1..40).map { "e$it" }, "x").size)
    }

    @Test
    fun emojiSearch_findsByKeyword() {
        assertTrue("❤️" in searchEmoji("lov"))
        assertTrue(searchEmoji("zzz").isEmpty())
    }

    @Test
    fun wallpaperMath_coversStage_andClamps() {
        assertEquals(2f, minCoverScale(500f, 1000f, 1000f, 1000f))
        assertEquals(0f, clampOffset(50f, 1000f, 1f, 1000f).let { if (it == 0f) 0f else it })
        assertEquals(-500f, clampOffset(-900f, 1000f, 1f, 500f))
        val crop = visibleCrop(1000f, 2000f, 500f, 1000f, 1f, -100f, -200f)
        assertEquals(100f, crop.left)
        assertEquals(200f, crop.top)
        assertEquals(500f, crop.width)
    }

    @Test
    fun deliveryTicks_mapIsStableForSeenMessages() {
        assertEquals(0, unseenCount(emptyList(), null))
    }
}
