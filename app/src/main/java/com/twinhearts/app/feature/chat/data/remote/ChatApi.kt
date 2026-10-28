package com.twinhearts.app.feature.chat.data.remote

import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Why this file exists: the only door from Chat to the old Express server (R-40, R-60).
 *
 * Message answers come back as raw JSON on purpose: the old table has odd columns, and one odd
 * value must never make the whole chat fail. `ChatLenient.kt` reads each row defensively.
 */
interface ChatApi {

    /** Newest [limit] messages, oldest first. With [after] (an ISO time) only newer ones. */
    @GET("api/chat/{coupleId}")
    suspend fun messages(
        @Path("coupleId") coupleId: String,
        @Query("limit") limit: Int? = null,
        @Query("after") after: String? = null,
    ): JsonElement

    @POST("api/chat")
    suspend fun send(@Body body: SendMessageBody): JsonElement

    @PATCH("api/chat/{id}")
    suspend fun edit(@Path("id") id: Long, @Body body: EditBody): JsonElement

    /** DELETE with a JSON body: the old server reads `mode` from it. */
    @HTTP(method = "DELETE", path = "api/chat/{id}", hasBody = true)
    suspend fun delete(@Path("id") id: Long, @Body body: DeleteBody): OkResponse

    @POST("api/chat/{coupleId}/read")
    suspend fun markRead(@Path("coupleId") coupleId: String, @Body body: RoleBody): OkResponse

    @POST("api/chat/{id}/star")
    suspend fun toggleStar(@Path("id") id: Long, @Body body: StarBody): JsonElement

    @POST("api/chat/{id}/react")
    suspend fun react(@Path("id") id: Long, @Body body: ReactBody): JsonElement

    @POST("api/chat/{id}/pin")
    suspend fun pin(@Path("id") id: Long, @Body body: PinBody): JsonElement

    /** Opens (and burns) a view-once photo or video. 410 when it was already viewed. */
    @POST("api/chat/{id}/view")
    suspend fun view(@Path("id") id: Long, @Body body: ViewBody): ViewOnceResponse

    @GET("api/chat/{coupleId}/search")
    suspend fun search(
        @Path("coupleId") coupleId: String,
        @Query("q") query: String,
        @Query("filter") filter: String,
    ): JsonElement

    @GET("api/chat/{coupleId}/presence")
    suspend fun presence(@Path("coupleId") coupleId: String): List<PresenceDto>

    @POST("api/chat/{coupleId}/presence")
    suspend fun setPresence(@Path("coupleId") coupleId: String, @Body body: PresenceBody): OkResponse

    @POST("api/chat/{coupleId}/schedule")
    suspend fun schedule(@Path("coupleId") coupleId: String, @Body body: ScheduleBody): ScheduledDto

    @GET("api/chat/{coupleId}/scheduled")
    suspend fun scheduled(@Path("coupleId") coupleId: String): List<ScheduledDto>

    @DELETE("api/chat/scheduled/{id}")
    suspend fun cancelScheduled(@Path("id") id: String): OkResponse

    @GET("api/chat/{coupleId}/disappearing")
    suspend fun disappearing(@Path("coupleId") coupleId: String): DisappearResponse

    @POST("api/chat/{coupleId}/disappearing")
    suspend fun setDisappearing(@Path("coupleId") coupleId: String, @Body body: DisappearBody): DisappearResponse

    @GET("api/chat/{coupleId}/wallpaper")
    suspend fun wallpaper(@Path("coupleId") coupleId: String): WallpaperDto

    @POST("api/chat/{coupleId}/wallpaper")
    suspend fun setWallpaper(@Path("coupleId") coupleId: String, @Body body: WallpaperBody): WallpaperDto

    /** The whole conversation as plain text (WhatsApp-style export). */
    @GET("api/chat/{coupleId}/export")
    suspend fun export(@Path("coupleId") coupleId: String): ResponseBody

    /** routes/media.js `POST /upload` (multipart `file`, `coupleId`). Photos and videos. */
    @Multipart
    @POST("api/media/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
    ): UploadResponse

    /** routes/media.js `POST /upload-doc`: PDF, Office files, text, zip. */
    @Multipart
    @POST("api/media/upload-doc")
    suspend fun uploadDoc(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
    ): UploadResponse

    /** routes/media.js `POST /upload-audio`: songs and audio files. */
    @Multipart
    @POST("api/media/upload-audio")
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
    ): UploadResponse

    /** routes/media.js `POST /upload-voice` (multipart `file`, `coupleId`, `senderRole`). */
    @Multipart
    @POST("api/media/upload-voice")
    suspend fun uploadVoice(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
        @Part("senderRole") senderRole: RequestBody,
    ): UploadResponse

    /** Giphy search; [url] is the full address, built by the repository. */
    @GET
    suspend fun gifs(@Url url: String): GifSearchResponse
}
