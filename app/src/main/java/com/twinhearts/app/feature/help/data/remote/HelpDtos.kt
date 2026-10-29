package com.twinhearts.app.feature.help.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Shapes of the old server's `/api/support/*` answers (routes/support.js, lib/supportStore.js).
 * Every field has a default so one missing or null value never fails the whole screen.
 * Request fields that must always be sent have NO default: the app's Json does not write defaults.
 */

@Serializable
data class FaqsResponse(val faqs: List<FaqDto> = emptyList())

@Serializable
data class FaqDto(
    val id: String = "",
    val category: String = "",
    val question: String = "",
    val answer: String = "",
    val tags: List<String> = emptyList(),
)

/** `POST /api/support/ticket`: start a conversation, or fetch the saved one when [ticketId] is known. */
@Serializable
data class TicketRequest(
    val ticketId: String?,
    val userId: String?,
    @SerialName("couple_id") val coupleId: String?,
    val userName: String,
    val userPhone: String?,
    val mode: String,
)

@Serializable
data class TicketResponse(val ticket: TicketDto? = null)

@Serializable
data class TicketDto(
    val id: String = "",
    val mode: String? = null,
    val messages: List<MessageDto> = emptyList(),
)

@Serializable
data class MessageDto(
    val id: String = "",
    val sender: String? = null,
    @SerialName("sender_name") val senderName: String? = null,
    val text: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)

/** `POST /api/support/ticket/:id/message`. [askAi] asks the server to answer with the AI. */
@Serializable
data class SendMessageRequest(
    val text: String,
    val senderName: String,
    val askAi: Boolean,
)

@Serializable
data class SendMessageResponse(
    val userMessage: MessageDto? = null,
    val aiMessage: MessageDto? = null,
    val ticket: TicketDto? = null,
)

/** `POST /api/support/ticket/:id/switch-mode` with "ai" or "human". */
@Serializable
data class SwitchModeRequest(val mode: String)
