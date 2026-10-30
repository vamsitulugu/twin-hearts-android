package com.twinhearts.app.feature.profile.data.remote

import com.twinhearts.app.core.network.StateEnvelopeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Shapes of the old server's answers and bodies for the Profile (R-65), read from
 * routes/profile.js, routes/partner.js, routes/auth.js (unpair), routes/data.js and routes/location.js.
 */

/** GET /api/data/state/:coupleId → `{ state: {...} | null }`. */
@Serializable(with = StateResponseSerializer::class)
data class StateResponse(val state: JsonElement? = null)

/** Reads the saved document in either shape (see [unwrapState]). */
object StateResponseSerializer : StateEnvelopeSerializer<StateResponse>("StateResponse", { StateResponse(it) }, { it.state })

/** POST /api/data/state. The server merges the top-level keys of [state]; `profile` is merged per person. */
@Serializable
data class SaveStateBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class OkResponse(val ok: Boolean? = null)

/** A row of `profiles`: GET /api/profile/:userId and the answer of POST .../avatar. */
@Serializable
data class ProfileRowDto(
    val id: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

/** GET /api/profile/:userId/partner → `{ profile: row | null }`. */
@Serializable
data class PartnerProfileResponse(val profile: ProfileRowDto? = null)

/** POST /api/auth/unpair. */
@Serializable
data class UnpairBody(val coupleId: String, val requestingRole: String)

/** POST /api/partner/request. `partnerPhone` is normalised by the server. */
@Serializable
data class SendRequestBody(val userId: String, val partnerPhone: String)

/** GET /api/partner/status/:userId → status is connected | waiting | invitation_received | not_connected. */
@Serializable
data class PartnerStatusResponse(val status: String? = null)

/** GET /api/location/:coupleId → one row per person, only the time is needed here. */
@Serializable
data class LocationsResponse(
    val user1: LocationRow? = null,
    val user2: LocationRow? = null,
)

@Serializable
data class LocationRow(val updatedAt: String? = null)
