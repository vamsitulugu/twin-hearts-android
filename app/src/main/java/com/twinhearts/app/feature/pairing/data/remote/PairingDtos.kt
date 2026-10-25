package com.twinhearts.app.feature.pairing.data.remote

import kotlinx.serialization.Serializable

// Shapes read from routes/partner.js of the old server (R-65).

@Serializable
data class StatusDto(
    val status: String = "not_connected",
    val partnerName: String? = null,
    val requestId: String? = null,
    val receiverName: String? = null,
    val receiverPhone: String? = null,
    val senderName: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class IncomingRequestDto(
    val id: String,
    val senderName: String? = null,
    val senderPhone: String? = null,
    val senderAvatar: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class PendingListDto(val requests: List<IncomingRequestDto> = emptyList())

@Serializable
data class SendRequestBody(val userId: String, val partnerPhone: String)

/** Body for accept, reject and cancel: all three send `{requestId, userId}`. */
@Serializable
data class RequestActionBody(val requestId: String, val userId: String)

/** Answer of accept. */
@Serializable
data class AcceptResponseDto(
    val coupleId: String? = null,
    val role: String? = null,
    val partnerName: String? = null,
)

/** Answer of request / reject / cancel: `{ok: true}` (request also adds requestId). */
@Serializable
data class OkDto(val ok: Boolean? = null)
