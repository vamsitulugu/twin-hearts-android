package com.twinhearts.app.feature.chat

import com.twinhearts.app.feature.chat.data.mapper.toDomain
import com.twinhearts.app.feature.chat.data.remote.ChatMessageDto
import com.twinhearts.app.feature.chat.domain.model.DeliveryState
import com.twinhearts.app.feature.chat.domain.model.ReactionGroup
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * Why this file exists: unit tests for the Chat feature.
 */

class ChatMapperTest {
    private val json = Json { ignoreUnknownKeys = true }
    private fun dto(text: String) = json.decodeFromString<ChatMessageDto>(text)

    @Test
    fun textRow_mapsSideTicksAndReply() {
        val m = dto(
            """{"id":7,"client_id":"x","sender_role":"user1","type":"text","text":"Hi","reply_to":5,
               "delivered":true,"read":true,"created_at":"2026-10-26T09:00:00+00:00","starred_by":["user1"],
               "reactions":{"❤️":["user2"]},"extra":1}""",
        ).toDomain("user1")!!
        assertTrue(m.mine)
        assertEquals(DeliveryState.Read, m.delivery)
        assertEquals(5L, m.replyToId)
        assertTrue(m.starred)
        assertEquals(listOf(ReactionGroup("❤️", 1, false)), m.reactions)
    }

    @Test
    fun voiceRow_readsDuration_andPartnerRowIsNotMine() {
        val m = dto(
            """{"id":8,"sender_role":"user2","type":"voice","media_url":"https://x/a.m4a",
               "media_meta":{"duration":7,"path":"p"},"created_at":"2026-10-26T09:00:00Z"}""",
        ).toDomain("user1")!!
        assertFalse(m.mine)
        assertEquals(MessageKind.Voice, m.kind)
        assertEquals(7, m.durationSec)
        assertEquals("id-8", m.clientId)
        assertEquals(DeliveryState.Sent, m.delivery)
    }

    @Test
    fun deletedForMe_isHidden_andDeletedTextIsRemoved() {
        val rows = listOf(
            dto("""{"id":1,"sender_role":"user2","text":"a","deleted_for":"user1","created_at":"2026-10-26T09:00:00Z"}"""),
            dto("""{"id":2,"sender_role":"user2","text":"b","deleted":true,"created_at":"2026-10-26T09:00:00Z"}"""),
        ).toDomain("user1")
        assertEquals(1, rows.size)
        assertNull(rows.first().text)
        assertTrue(rows.first().deleted)
    }

    @Test
    fun brokenTime_dropsTheRow() {
        assertEquals(0, listOf(dto("""{"id":3,"sender_role":"user1","text":"x","created_at":"??"}""")).toDomain("user1").size)
    }

    @Test
    fun newKinds_pollLocationGiftFileGroup() {
        val poll = dto("""{"id":1,"sender_role":"user2","type":"poll","text":"Dinner?","media_meta":{"options":["Pizza","Sushi"]},"created_at":"2026-10-26T09:00:00Z"}""").toDomain("user1")!!
        assertEquals(MessageKind.Poll, poll.kind)
        assertEquals(listOf("Pizza", "Sushi"), poll.pollOptions)
        val loc = dto("""{"id":2,"sender_role":"user2","type":"location","media_meta":{"lat":20.5,"lng":85.1},"created_at":"2026-10-26T09:00:00Z"}""").toDomain("user1")!!
        assertEquals(20.5, loc.latitude)
        val gift = dto("""{"id":3,"sender_role":"user2","type":"gift","media_meta":{"emoji":"🌹","name":"Rose"},"created_at":"2026-10-26T09:00:00Z"}""").toDomain("user1")!!
        assertEquals("🌹", gift.emoji)
        assertEquals("Rose", gift.label)
        val file = dto("""{"id":4,"sender_role":"user1","type":"file","media_url":"u","media_meta":{"name":"a.pdf","size":2048},"created_at":"2026-10-26T09:00:00Z"}""").toDomain("user1")!!
        assertEquals("a.pdf", file.fileName)
        assertEquals(2048L, file.fileSize)
        val group = dto("""{"id":5,"sender_role":"user1","type":"image_group","media_url":"u1","media_meta":{"items":[{"url":"u1"},{"url":"u2"}]},"created_at":"2026-10-26T09:00:00Z"}""").toDomain("user1")!!
        assertEquals(listOf("u1", "u2"), group.groupUrls)
    }

    @Test
    fun viewOnce_hiddenMediaIsPendingTile() {
        val m = dto("""{"id":6,"sender_role":"user2","type":"image","view_once":true,"view_once_pending":true,"created_at":"2026-10-26T09:00:00Z"}""").toDomain("user1")!!
        assertTrue(m.viewOncePending)
        assertTrue(m.isViewOnceTile)
    }
}
