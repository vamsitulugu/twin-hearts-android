package com.twinhearts.app.feature.chat.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** Why this file exists: the only door from Chat to the old Express server (R-40, R-60). */
interface ChatApi {

    /** Newest [limit] messages, oldest first. With [after] (an ISO time) only newer ones. */
    @GET("api/chat/{coupleId}")
    suspend fun messages(
        @Path("coupleId") coupleId: String,
        @Query("limit") limit: Int? = null,
        @Query("after") after: String? = null,
    ): List<ChatMessageDto>

    @POST("api/chat")
    suspend fun send(@Body body: SendMessageBody): ChatMessageDto

    @POST("api/chat/{coupleId}/read")
    suspend fun markRead(@Path("coupleId") coupleId: String, @Body body: RoleBody): OkResponse

    @POST("api/chat/{id}/star")
    suspend fun toggleStar(@Path("id") id: Long, @Body body: StarBody): ChatMessageDto

    @GET("api/chat/{coupleId}/search")
    suspend fun search(
        @Path("coupleId") coupleId: String,
        @Query("q") query: String,
        @Query("filter") filter: String,
    ): List<ChatMessageDto>

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

    /** routes/media.js `POST /upload` (multipart `file`, `coupleId`). Photos and videos only. */
    @Multipart
    @POST("api/media/upload")
    suspend fun uploadImage(
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

    /** PATCH /api/chat/:id — edit a message I sent. */
    @PATCH("api/chat/{id}")
    suspend fun editMessage(
        @Path("id") id: Long,
        @Body body: EditMessageBody,
    ): ChatMessageDto

    /** DELETE /api/chat/:id — delete for me or for everyone. */
    @DELETE("api/chat/{id}")
    suspend fun deleteMessage(
        @Path("id") id: Long,
        @Body body: DeleteMessageBody,
    ): OkResponse

    /** POST /api/chat/:id/react — toggle an emoji reaction. */
    @POST("api/chat/{id}/react")
    suspend fun toggleReaction(
        @Path("id") id: Long,
        @Body body: ReactBody,
    ): ChatMessageDto

    /** POST /api/chat/:id/view — open (and burn) view-once media. */
    @POST("api/chat/{id}/view")
    suspend fun openViewOnce(
        @Path("id") id: Long,
        @Body body: ViewOnceBody,
    ): ViewOnceResponse
}
