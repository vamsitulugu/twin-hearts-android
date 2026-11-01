package com.twinhearts.app.core.presence

import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.feature.chat.data.remote.ChatApi
import com.twinhearts.app.feature.chat.data.remote.PresenceDto
import com.twinhearts.app.feature.chat.domain.model.PresenceState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** Checks that the side menu reads the PARTNER's row, and shows nothing when not paired. */
class PartnerPresenceSourceTest {

    private val now = Instant.parse("2026-11-02T10:00:00Z")

    private fun session(role: String, paired: Boolean) = Session(
        userId = "u1", coupleId = "c1", myName = "Me", email = "", phoneNumber = "",
        partnerName = "Partner", anniversary = "", role = role, paired = paired, gender = null,
    )

    private fun source(session: Session?, rows: List<PresenceDto>): PartnerPresenceSource {
        val api = mockk<ChatApi>()
        coEvery { api.presence("c1") } returns rows
        val store = mockk<SessionStore>()
        every { store.session } returns flowOf(session)
        return PartnerPresenceSource(api, store)
    }

    @Test
    fun partnerRecentlyOnlineIsOnline() = runTest {
        val rows = listOf(
            PresenceDto("user1", "offline", "2026-11-01T10:00:00Z"),
            PresenceDto("user2", "online", "2026-11-02T09:59:55Z"),
        )
        val result = source(session("user1", true), rows).fetch(now)
        assertEquals(PresenceState.Online, result?.state)
    }

    @Test
    fun usesTheOtherRoleWhenIAmUser2() = runTest {
        val rows = listOf(
            PresenceDto("user1", "online", "2026-11-02T09:59:55Z"),
            PresenceDto("user2", "offline", "2026-11-01T10:00:00Z"),
        )
        val result = source(session("user2", true), rows).fetch(now)
        assertEquals(PresenceState.Online, result?.state)
    }

    @Test
    fun oldHeartbeatIsOffline() = runTest {
        val rows = listOf(PresenceDto("user2", "online", "2026-11-02T08:00:00Z"))
        val result = source(session("user1", true), rows).fetch(now)
        assertEquals(PresenceState.Offline, result?.state)
    }

    @Test
    fun missingPartnerRowIsOffline() = runTest {
        val result = source(session("user1", true), emptyList()).fetch(now)
        assertEquals(PresenceState.Offline, result?.state)
        assertNull(result?.lastSeen)
    }

    @Test
    fun notPairedGivesNothing() = runTest {
        assertNull(source(session("user1", false), emptyList()).fetch(now))
    }

    @Test
    fun signedOutGivesNothing() = runTest {
        assertNull(source(null, emptyList()).fetch(now))
    }
}
