package com.twinhearts.app.feature.settings.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Shapes read from routes/data.js, profile.js, auth.js and partner.js of the old server (R-65).

/** POST /api/data/state. The server MERGES the top-level keys of [state] into the couple's saved state. */
@Serializable
data class SaveStateBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class OkDto(val ok: Boolean? = null)

/** A row of the `profiles` table. Only the fields Settings needs. */
@Serializable
data class ProfileDto(
    val id: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

/** GET /api/profile/:userId/partner answers `{ profile: {...} | null }`. */
@Serializable
data class PartnerProfileDto(val profile: ProfileDto? = null)

@Serializable
data class ChangePinBody(val coupleId: String, val currentPin: String, val newPin: String)

@Serializable
data class UnpairBody(val coupleId: String, val requestingRole: String)

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

/** Body of accept, reject and cancel: all three send `{requestId, userId}`. */
@Serializable
data class RequestActionBody(val requestId: String, val userId: String)

@Serializable
data class AcceptResponseDto(
    val coupleId: String? = null,
    val role: String? = null,
    val partnerName: String? = null,
)
