package com.twinhearts.app.feature.places.data.remote

import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** Places live in the shared couple state (key `placesMemories`), exactly like the web app. */
interface PlacesApi {
    /** Whole saved state, or JSON `null` when the couple has none yet. We read only `placesMemories`. */
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): JsonElement

    @POST("api/data/state")
    suspend fun saveState(@Body body: SaveStateBody): SaveStateResponse

    /** routes/media.js `POST /upload` (multipart `file`, `coupleId`). */
    @Multipart
    @POST("api/media/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
    ): UploadResponse
}
