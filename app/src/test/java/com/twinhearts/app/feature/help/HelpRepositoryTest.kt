package com.twinhearts.app.feature.help

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.feature.help.data.HelpRepositoryImpl
import com.twinhearts.app.feature.help.data.HelpTicketStore
import com.twinhearts.app.feature.help.data.remote.HelpApi
import com.twinhearts.app.feature.help.data.remote.MessageDto
import com.twinhearts.app.feature.help.data.remote.SendMessageRequest
import com.twinhearts.app.feature.help.data.remote.SendMessageResponse
import com.twinhearts.app.feature.help.data.remote.TicketDto
import com.twinhearts.app.feature.help.data.remote.TicketRequest
import com.twinhearts.app.feature.help.data.remote.TicketResponse
import com.twinhearts.app.feature.help.domain.model.SupportMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpRepositoryTest {
    private val api = mockk<HelpApi>()
    private val sessionStore = mockk<SessionStore>()
    private val ticketStore = mockk<HelpTicketStore>(relaxed = true)
    private val session = Session("u1", "c1", "Vamsi", "v@x.com", "9999999999", "Likitha", "", "user1", true, null)

    private fun repo() = HelpRepositoryImpl(api, sessionStore, ticketStore)

    @Test
    fun openTicketSendsMyIdsAndSavesTheTicketId() = runTest {
        every { sessionStore.session } returns flowOf(session)
        coEvery { ticketStore.savedId() } returns "old"
        val body = slot<TicketRequest>()
        coEvery { api.openTicket(capture(body)) } returns TicketResponse(TicketDto("t9", "ai"))

        val result = repo().openTicket(SupportMode.Ai)

        assertEquals("t9", (result as AppResult.Success).data.id)
        assertEquals(TicketRequest("old", "u1", "c1", "Vamsi", "9999999999", "ai"), body.captured)
        coVerify { ticketStore.save("t9") }
    }

    @Test
    fun ticketMissingInAnswerIsAnError() = runTest {
        every { sessionStore.session } returns flowOf(session)
        coEvery { ticketStore.savedId() } returns null
        coEvery { api.openTicket(any()) } returns TicketResponse(null)
        val result = repo().openTicket(SupportMode.Ai)
        assertEquals(AppError.Unknown, (result as AppResult.Failure).error)
    }

    @Test
    fun signedOutGivesUnauthorized() = runTest {
        every { sessionStore.session } returns flowOf(null)
        val result = repo().send("t", "hi", SupportMode.Ai)
        assertTrue((result as AppResult.Failure).error is AppError.Unauthorized)
    }

    @Test
    fun sendAsksTheAiOnlyInAiModeAndUsesDefaultName() = runTest {
        every { sessionStore.session } returns flowOf(session.copy(myName = ""))
        val body = slot<SendMessageRequest>()
        coEvery { api.sendMessage("t", capture(body)) } returns SendMessageResponse(userMessage = MessageDto("1"))

        repo().send("t", "hi", SupportMode.Ai)
        assertEquals(SendMessageRequest("hi", "Valued User", true), body.captured)

        repo().send("t", "hi", SupportMode.Human)
        assertEquals(false, body.captured.askAi)
    }
}
