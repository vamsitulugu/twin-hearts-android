package com.twinhearts.app.feature.games.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SaveStateBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

/** Answer of the save: `{ ok: true, savedAt: "..." }`. */
@Serializable
data class SaveStateResponse(val ok: Boolean? = null)

/** The few columns of a `profiles` row that Games needs. */
@Serializable
data class ProfileRowDto(
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class PartnerProfileDto(val profile: ProfileRowDto? = null)
