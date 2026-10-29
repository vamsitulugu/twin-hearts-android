package com.twinhearts.app.feature.profile.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** The old Express server, the same routes the web Profile page uses. */
interface ProfileApi {

    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): StateResponse

    @POST("api/data/state")
    suspend fun saveState(@Body body: SaveStateBody): OkResponse

    @GET("api/profile/{userId}")
    suspend fun getProfile(@Path("userId") userId: String): ProfileRowDto

    @GET("api/profile/{userId}/partner")
    suspend fun getPartnerProfile(@Path("userId") userId: String): PartnerProfileResponse

    /** Multipart: file part `avatar` and text part `requestingUserId` (must equal the path id). */
    @Multipart
    @POST("api/profile/{userId}/avatar")
    suspend fun uploadAvatar(
        @Path("userId") userId: String,
        @Part avatar: MultipartBody.Part,
        @Part("requestingUserId") requestingUserId: RequestBody,
    ): ProfileRowDto

    @POST("api/auth/unpair")
    suspend fun unpair(@Body body: UnpairBody): OkResponse

    @POST("api/partner/request")
    suspend fun sendRequest(@Body body: SendRequestBody): OkResponse

    @GET("api/partner/status/{userId}")
    suspend fun partnerStatus(@Path("userId") userId: String): PartnerStatusResponse

    @GET("api/location/{coupleId}")
    suspend fun getLocations(@Path("coupleId") coupleId: String): LocationsResponse
}
