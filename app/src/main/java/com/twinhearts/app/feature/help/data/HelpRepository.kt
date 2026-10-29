package com.twinhearts.app.feature.help.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.help.data.mapper.toDomain
import com.twinhearts.app.feature.help.data.remote.HelpApi
import com.twinhearts.app.feature.help.data.remote.SendMessageRequest
import com.twinhearts.app.feature.help.data.remote.SwitchModeRequest
import com.twinhearts.app.feature.help.data.remote.TicketRequest
import com.twinhearts.app.feature.help.data.remote.TicketResponse
import com.twinhearts.app.feature.help.domain.DEFAULT_USER_NAME
import com.twinhearts.app.feature.help.domain.model.FaqItem
import com.twinhearts.app.feature.help.domain.model.SendResult
import com.twinhearts.app.feature.help.domain.model.SupportMode
import com.twinhearts.app.feature.help.domain.model.SupportTicket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface HelpRepository {
    /** My name as saved at sign up. Empty when there is none; the UI then shows a default. */
    val myName: Flow<String>

    suspend fun faqs(): AppResult<List<FaqItem>>

    /**
     * Opens my support conversation: the saved one when this phone has it, a new one otherwise
     * (a new one starts in [mode]). The server may also hand back my other open conversation.
     */
    suspend fun openTicket(mode: SupportMode): AppResult<SupportTicket>

    suspend fun refresh(ticketId: String): AppResult<SupportTicket>

    suspend fun switchMode(ticketId: String, mode: SupportMode): AppResult<SupportTicket>

    /** Sends my message. In [SupportMode.Ai] the server also writes the AI's answer. */
    suspend fun send(ticketId: String, text: String, mode: SupportMode): AppResult<SendResult>
}

class HelpRepositoryImpl @Inject constructor(
    private val api: HelpApi,
    private val sessionStore: SessionStore,
    private val ticketStore: HelpTicketStore,
) : HelpRepository {

    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override val myName: Flow<String> = sessionStore.session.map { it?.myName.orEmpty() }

    override suspend fun faqs(): AppResult<List<FaqItem>> =
        when (val result = safeApiCall { api.faqs() }) {
            is AppResult.Success -> AppResult.Success(result.data.faqs.map { it.toDomain() })
            is AppResult.Failure -> result
        }

    override suspend fun openTicket(mode: SupportMode): AppResult<SupportTicket> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val request = ticketRequest(session, ticketStore.savedId(), mode)
        val ticket = ticketOf(safeApiCall { api.openTicket(request) })
        if (ticket is AppResult.Success) ticketStore.save(ticket.data.id)
        return ticket
    }

    override suspend fun refresh(ticketId: String): AppResult<SupportTicket> =
        ticketOf(safeApiCall { api.ticket(ticketId) })

    override suspend fun switchMode(ticketId: String, mode: SupportMode): AppResult<SupportTicket> =
        ticketOf(safeApiCall { api.switchMode(ticketId, SwitchModeRequest(mode.wire)) })

    override suspend fun send(ticketId: String, text: String, mode: SupportMode): AppResult<SendResult> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val request = SendMessageRequest(
            text = text,
            senderName = session.myName.ifBlank { DEFAULT_USER_NAME },
            askAi = mode == SupportMode.Ai,
        )
        return when (val result = safeApiCall { api.sendMessage(ticketId, request) }) {
            is AppResult.Success -> AppResult.Success(result.data.toDomain())
            is AppResult.Failure -> result
        }
    }

    /** Same fields the web sends (help.html `initChatSession`). */
    private fun ticketRequest(session: Session, savedId: String?, mode: SupportMode) = TicketRequest(
        ticketId = savedId,
        userId = session.userId.ifBlank { null },
        coupleId = session.coupleId.ifBlank { null },
        userName = session.myName.ifBlank { DEFAULT_USER_NAME },
        userPhone = session.phoneNumber.ifBlank { null },
        mode = mode.wire,
    )

    /** A success without a ticket in it cannot be used, so it counts as an unknown error. */
    private fun ticketOf(result: AppResult<TicketResponse>): AppResult<SupportTicket> =
        when (result) {
            is AppResult.Success -> result.data.ticket?.let { AppResult.Success(it.toDomain()) }
                ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> result
        }
}
