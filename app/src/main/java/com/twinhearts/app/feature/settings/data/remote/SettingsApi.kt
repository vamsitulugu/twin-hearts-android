package com.twinhearts.app.feature.settings.data.remote

import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** Every endpoint the Settings pages call. Errors come back as `{ "error": "friendly text" }`. */
interface SettingsApi {
    /** Whole shared state, or JSON `null` when the couple has none yet. */
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): JsonElement

    @POST("api/data/state")
    suspend fun saveState(@Body body: SaveStateBody): OkDto

    @GET("api/profile/{userId}")
    suspend fun myProfile(@Path("userId") userId: String): ProfileDto

    @GET("api/profile/{userId}/partner")
    suspend fun partnerProfile(@Path("userId") userId: String): PartnerProfileDto

    /** routes/profile.js: multipart `avatar` + `requestingUserId`. Answer: the updated profile row. */
    @Multipart
    @POST("api/profile/{userId}/avatar")
    suspend fun uploadAvatar(
        @Path("userId") userId: String,
        @Part avatar: MultipartBody.Part,
        @Part("requestingUserId") requestingUserId: RequestBody,
    ): ProfileDto

    @POST("api/auth/change-pin")
    suspend fun changePin(@Body body: ChangePinBody): OkDto

    @POST("api/auth/unpair")
    suspend fun unpair(@Body body: UnpairBody): OkDto

    @GET("api/partner/status/{userId}")
    suspend fun partnerStatus(@Path("userId") userId: String): StatusDto

    @GET("api/partner/pending-list/{userId}")
    suspend fun pendingList(@Path("userId") userId: String): PendingListDto

    @POST("api/partner/request")
    suspend fun sendRequest(@Body body: SendRequestBody): OkDto

    @POST("api/partner/accept")
    suspend fun accept(@Body body: RequestActionBody): AcceptResponseDto

    @POST("api/partner/reject")
    suspend fun reject(@Body body: RequestActionBody): OkDto

    @POST("api/partner/cancel")
    suspend fun cancel(@Body body: RequestActionBody): OkDto
}
