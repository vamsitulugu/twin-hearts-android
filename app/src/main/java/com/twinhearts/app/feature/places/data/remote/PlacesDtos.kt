package com.twinhearts.app.feature.places.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** POST /api/data/state body. The server MERGES the top-level keys, so we send only `placesMemories`. (routes/data.js) */
@Serializable
data class SaveStateBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class SaveStateResponse(val ok: Boolean? = null)

/** Answer of the photo upload: `{url, path}`. */
@Serializable
data class UploadResponse(val url: String, val path: String? = null)
