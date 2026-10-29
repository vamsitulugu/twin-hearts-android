package com.twinhearts.app.feature.help.data.mapper

import com.twinhearts.app.feature.help.data.remote.FaqDto
import com.twinhearts.app.feature.help.data.remote.MessageDto
import com.twinhearts.app.feature.help.data.remote.SendMessageResponse
import com.twinhearts.app.feature.help.data.remote.TicketDto
import com.twinhearts.app.feature.help.domain.model.FaqItem
import com.twinhearts.app.feature.help.domain.model.SendResult
import com.twinhearts.app.feature.help.domain.model.SenderKind
import com.twinhearts.app.feature.help.domain.model.SupportMessage
import com.twinhearts.app.feature.help.domain.model.SupportMode
import com.twinhearts.app.feature.help.domain.model.SupportTicket
import com.twinhearts.app.feature.help.domain.parseTimestamp

/* Server answers (DTOs) turned into the models the UI uses (README section 5). */

fun FaqDto.toDomain() = FaqItem(
    id = id,
    category = category,
    question = question,
    answer = answer,
    tags = tags,
)

fun MessageDto.toDomain() = SupportMessage(
    id = id,
    sender = SenderKind.fromWire(sender),
    senderName = senderName.orEmpty(),
    text = text,
    createdAt = parseTimestamp(createdAt),
)

fun TicketDto.toDomain() = SupportTicket(
    id = id,
    mode = SupportMode.fromWire(mode),
    messages = messages.map { it.toDomain() },
)

fun SendMessageResponse.toDomain() = SendResult(
    ticket = ticket?.toDomain(),
    userMessage = userMessage?.toDomain(),
    aiMessage = aiMessage?.toDomain(),
)
