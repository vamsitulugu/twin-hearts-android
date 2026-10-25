package com.twinhearts.app.feature.auth

import com.twinhearts.app.feature.auth.data.mapper.toSession
import com.twinhearts.app.feature.auth.data.remote.SessionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AuthMapperTest {
    @Test
    fun registerAnswer_getsWebDefaults() {
        val dto = SessionDto(userId = "u1", coupleId = "c1", myName = "Vamsi", phoneNumber = "123", role = "user1", paired = false)
        val session = dto.toSession(fallbackEmail = "me@mail.com", gender = "female")
        assertEquals("me@mail.com", session.email)
        assertEquals("Partner", session.partnerName)
        assertEquals("female", session.gender)
        assertFalse(session.paired)
    }

    @Test
    fun loginAnswer_keepsServerValues() {
        val dto = SessionDto(
            userId = "u2", coupleId = "c2", myName = "Asha", email = "asha@mail.com",
            partnerName = "Ravi", anniversary = "2024-02-14", role = "user2", paired = true,
        )
        val session = dto.toSession(fallbackEmail = "typed@mail.com", gender = null)
        assertEquals("asha@mail.com", session.email)
        assertEquals("Ravi", session.partnerName)
        assertEquals("user2", session.role)
        assertEquals(true, session.paired)
    }
}
