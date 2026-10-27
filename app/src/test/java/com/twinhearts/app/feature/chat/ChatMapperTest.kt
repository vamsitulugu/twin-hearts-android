package com.twinhearts.app.feature.chat

import com.twinhearts.app.feature.chat.data.mapper.toDomain
import com.twinhearts.app.feature.chat.data.remote.ChatMessageDto
import com.twinhearts.app.feature.chat.domain.model.DeliveryState
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertEquals(listOf("❤️"), m.reactions)
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
    func editedRow_mapsEditedAt() {
        val m = dto(
            """{"id":9,"sender_role":"user1","text":"Hi","edited":true,"edited_at":"2026-10-26T09:30:00Z","created_at":"2026-10-26T09:00:00Z"}""",
        ).toDomain("user1")!!
        assertTrue(m.edited)
        assertEquals(Instant.parse("2026-10-26T09:30:00Z"), m.editedAt)
    }

    @Test
    func viewOnceRow_mapsPendingAndViewedAt() {
        val m = dto(
            """{"id":10,"sender_role":"user2","type":"image","media_url":"https://x/photo.jpg","view_once":true,"view_once_pending":true,"viewed_at":"2026-10-26T09:35:00Z","created_at":"2026-10-26T09:00:00Z"}""",
        ).toDomain("user1")!!
        assertTrue(m.viewOnce)
        assertTrue(m.viewOncePending)
        assertEquals(Instant.parse("2026-10-26T09:35:00Z"), m.viewedAt)
    }
}
