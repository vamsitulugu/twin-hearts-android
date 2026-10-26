package com.twinhearts.app.feature.home.data.remote

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

/** The shared couple state lives in one JSON document on the old server (routes/data.js). */
interface HomeApi {
    /** Whole saved state, or JSON `null` when the couple has none yet. We read only a few keys of it. */
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): JsonElement

    @POST("api/data/state")
    suspend fun saveState(@Body body: SaveStateBody): SaveStateResponse

    /** routes/media.js `POST /upload` (multipart: `file`, `coupleId`). Answer: `{url, path}`. */
    @Multipart
    @POST("api/media/upload")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
    ): MediaUploadResponse

    /** routes/media.js `DELETE /delete` (JSON body: `path`, `coupleId`). Answer: `{ok: true}`. */
    @HTTP(method = "DELETE", path = "api/media/delete", hasBody = true)
    suspend fun deleteMedia(@Body body: DeleteMediaBody): SaveStateResponse
}
