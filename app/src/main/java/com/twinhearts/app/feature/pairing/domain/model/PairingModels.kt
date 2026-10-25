package com.twinhearts.app.feature.pairing.domain.model

/** Where this account is in the pairing story (web: GET /api/partner/status). */
sealed interface PairingStatus {
    data object NotConnected : PairingStatus
    data class Waiting(
        val requestId: String,
        val receiverName: String,
        val receiverPhone: String,
        val createdAt: String?,
    ) : PairingStatus
    data class InvitationReceived(val senderName: String) : PairingStatus
    data class Connected(val partnerName: String) : PairingStatus
}

/** An invitation someone sent to me. */
data class IncomingRequest(
    val id: String,
    val senderName: String,
    val senderPhone: String,
    val senderAvatar: String?,
    val createdAt: String?,
)
