package com.twinhearts.app.feature.pairing.data.mapper

import com.twinhearts.app.feature.pairing.data.remote.IncomingRequestDto
import com.twinhearts.app.feature.pairing.data.remote.StatusDto
import com.twinhearts.app.feature.pairing.domain.model.IncomingRequest
import com.twinhearts.app.feature.pairing.domain.model.PairingStatus

/** Same fallbacks as the web: "Partner" and "Someone" when a name is missing. */
fun StatusDto.toDomain(): PairingStatus = when (status) {
    "connected" -> PairingStatus.Connected(partnerName?.takeIf { it.isNotBlank() } ?: "Partner")
    "waiting" -> PairingStatus.Waiting(
        requestId = requestId.orEmpty(),
        receiverName = receiverName?.takeIf { it.isNotBlank() } ?: "Someone",
        receiverPhone = receiverPhone.orEmpty(),
        createdAt = createdAt,
    )
    "invitation_received" -> PairingStatus.InvitationReceived(senderName?.takeIf { it.isNotBlank() } ?: "Someone")
    else -> PairingStatus.NotConnected
}

fun IncomingRequestDto.toDomain(): IncomingRequest = IncomingRequest(
    id = id,
    senderName = senderName?.takeIf { it.isNotBlank() } ?: "Someone",
    senderPhone = senderPhone.orEmpty(),
    senderAvatar = senderAvatar,
    createdAt = createdAt,
)
