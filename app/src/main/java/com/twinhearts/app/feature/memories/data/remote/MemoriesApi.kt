package com.twinhearts.app.feature.memories.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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
 * The album is the `photos` list in the couple's shared state (same document the web saves), and
 * the files themselves live in the server's storage (routes/media.js). No new backend route.
 */
interface MemoriesApi {
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): JsonElement

    /** The server merges the top-level keys of `state` into the saved document. */
    @POST("api/data/state")
    suspend fun saveState(@Body body: MemoriesSaveBody): MemoriesSaveResponse

    /** Photos and videos (multipart `file` + `coupleId`). */
    @Multipart
    @POST("api/media/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
    ): UploadResult

    /** Removes one stored file. Needs a body, so it is declared by hand. */
    @HTTP(method = "DELETE", path = "api/media/delete", hasBody = true)
    suspend fun deleteFile(@Body body: DeleteFileBody): MemoriesSaveResponse
}

@Serializable
data class MemoriesSaveBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class MemoriesSaveResponse(val ok: Boolean? = null)

@Serializable
data class UploadResult(val url: String, val path: String? = null)

@Serializable
data class DeleteFileBody(
    val path: String,
    val coupleId: String,
    val bucket: String = "couple-photos",
)
