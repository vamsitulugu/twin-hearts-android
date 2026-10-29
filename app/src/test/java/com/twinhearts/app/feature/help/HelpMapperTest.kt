package com.twinhearts.app.feature.help

import com.twinhearts.app.feature.help.data.mapper.toDomain
import com.twinhearts.app.feature.help.data.remote.FaqDto
import com.twinhearts.app.feature.help.data.remote.MessageDto
import com.twinhearts.app.feature.help.data.remote.SendMessageResponse
import com.twinhearts.app.feature.help.data.remote.TicketDto
import com.twinhearts.app.feature.help.domain.model.SenderKind
import com.twinhearts.app.feature.help.domain.model.SupportMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class HelpMapperTest {
    @Test
    fun faqIsCopiedAsIs() {
        val item = FaqDto("f", "Cat", "Q", "A", listOf("t")).toDomain()
        assertEquals("Q", item.question)
        assertEquals(listOf("t"), item.tags)
    }

    @Test
    fun messageMapsSenderNameAndTime() {
        val m = MessageDto("1", "admin", null, "Hi", "2026-10-29T10:15:30Z").toDomain()
        assertEquals(SenderKind.Admin, m.sender)
        assertEquals("", m.senderName)
        assertEquals(Instant.parse("2026-10-29T10:15:30Z"), m.createdAt)
    }

    @Test
    fun missingSenderIsTheUserAndBadTimeIsNull() {
        val m = MessageDto("1", null, "Vamsi", "Hi", "nope").toDomain()
        assertEquals(SenderKind.User, m.sender)
        assertNull(m.createdAt)
    }

    @Test
    fun ticketMapsModeAndMessages() {
        val t = TicketDto("t1", "human", listOf(MessageDto("1", "user", "V", "Hi"))).toDomain()
        assertEquals(SupportMode.Human, t.mode)
        assertEquals(1, t.messages.size)
    }

    @Test
    fun sendResponseKeepsWhatIsThere() {
        val r = SendMessageResponse(userMessage = MessageDto("1", "user", "V", "Hi")).toDomain()
        assertNotNull(r.userMessage)
        assertNull(r.aiMessage)
        assertNull(r.ticket)
    }
}
