package com.twinhearts.app.feature.surprises.data.remote

import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Why this file exists: the calls Surprises makes. Surprises and capsules live in the couple's
 * shared state (routes/data.js), the same document the web app saves, so no new backend route is
 * needed. Attached files go through the existing routes/media.js uploads.
 */
interface SurprisesApi {
    /** Whole saved state (the server answers the raw document, not a wrapper), or JSON `null` when none exists yet. */
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): JsonElement

    /** The server merges the top-level keys of `state` into the saved document. */
    @POST("api/data/state")
    suspend fun saveState(@Body body: SaveStateBody): SaveStateResponse

    /** Photos and videos. Answer: `{url, path}`. */
    @Multipart
    @POST("api/media/upload")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
    ): MediaUploadResponse

    /** Voice notes and other audio (any `audio/...`, up to 15 MB). Answer: `{url, path, ...}`. */
    @Multipart
    @POST("api/media/upload-voice")
    suspend fun uploadVoice(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
        @Part("senderRole") senderRole: RequestBody,
    ): MediaUploadResponse

    @HTTP(method = "DELETE", path = "api/media/delete", hasBody = true)
    suspend fun deleteMedia(@Body body: DeleteMediaBody): SaveStateResponse

    @HTTP(method = "DELETE", path = "api/media/delete-voice", hasBody = true)
    suspend fun deleteVoice(@Body body: DeleteMediaBody): SaveStateResponse
}
