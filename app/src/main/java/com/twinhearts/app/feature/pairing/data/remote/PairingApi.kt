package com.twinhearts.app.feature.pairing.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Partner endpoints (mounted at /api/partner). Errors come back as `{ "error": "..." }`. */
interface PairingApi {
    @GET("api/partner/status/{userId}")
    suspend fun status(@Path("userId") userId: String): StatusDto

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
