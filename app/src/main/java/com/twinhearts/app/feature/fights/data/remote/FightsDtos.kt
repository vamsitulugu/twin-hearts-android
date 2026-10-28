package com.twinhearts.app.feature.fights.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Why this file exists: the shared-state save body (routes/data.js `POST /state`). Top-level keys are merged. */
@Serializable
data class SaveStateBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class SaveStateAck(val ok: Boolean? = null)
