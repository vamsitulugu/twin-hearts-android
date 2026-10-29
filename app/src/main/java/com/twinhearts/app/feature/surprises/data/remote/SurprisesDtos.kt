package com.twinhearts.app.feature.surprises.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** POST /api/data/state body. [state] holds only the keys this screen changes. */
@Serializable
data class SaveStateBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class SaveStateResponse(val ok: Boolean? = null)

@Serializable
data class MediaUploadResponse(val url: String, val path: String? = null)

/** The server only deletes files under this couple's own folder. */
@Serializable
data class DeleteMediaBody(val path: String, val coupleId: String)
