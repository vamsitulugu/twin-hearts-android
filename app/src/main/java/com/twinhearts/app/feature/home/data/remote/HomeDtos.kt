package com.twinhearts.app.feature.home.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * POST /api/data/state body. The server MERGES the top-level keys of [state] into the couple's
 * saved state, so we only send the keys we change (touch, hug, missYou, ...). (R-65, routes/data.js)
 */
@Serializable
data class SaveStateBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

/** Answer: `{ok: true, savedAt: "..."}`. */
@Serializable
data class SaveStateResponse(val ok: Boolean? = null)

/** Answer of the photo upload: the public [url] and the storage [path] (needed to delete it later). */
@Serializable
data class MediaUploadResponse(val url: String, val path: String? = null)

/** Body of the photo delete call. The server only deletes files under this couple's own folder. */
@Serializable
data class DeleteMediaBody(val path: String, val coupleId: String)
