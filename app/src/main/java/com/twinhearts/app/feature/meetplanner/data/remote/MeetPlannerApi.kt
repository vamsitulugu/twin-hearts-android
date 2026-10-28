package com.twinhearts.app.feature.meetplanner.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Why this file exists: the only door from Meet Planner to the Twin Hearts server (R-40, R-60).
 * Each line names the old route file it was read from (R-65).
 */
interface MeetPlannerApi {
    /** routes/meetplanner.js `GET /:coupleId`: all plans, soonest date first. */
    @GET("api/meetplanner/{coupleId}")
    suspend fun plans(@Path("coupleId") coupleId: String): List<PlanDto>

    /** routes/meetplanner.js `POST /`: saves a plan and pushes "New Meetup Planned" to the partner. */
    @POST("api/meetplanner")
    suspend fun create(@Body body: CreatePlanBody): PlanDto

    /** routes/meetplanner.js `PATCH /:id`. */
    @PATCH("api/meetplanner/{id}")
    suspend fun update(@Path("id") id: String, @Body body: PatchPlanBody): PlanDto

    /** routes/meetplanner.js `DELETE /:id` (the couple id travels in the body). */
    @HTTP(method = "DELETE", path = "api/meetplanner/{id}", hasBody = true)
    suspend fun delete(@Path("id") id: String, @Body body: CoupleBody): OkDto

    /** routes/meetplanner.js `POST /:id/complete`: marks done and creates the Memory Globe entry. */
    @POST("api/meetplanner/{id}/complete")
    suspend fun complete(@Path("id") id: String, @Body body: CompleteBody): CompleteDto

    /** routes/globe.js `GET /:coupleId`: memories with their photos (used by "View Photos"). */
    @GET("api/globe/{coupleId}")
    suspend fun globeMemories(@Path("coupleId") coupleId: String): List<GlobeMemoryDto>

    /** routes/media.js `POST /upload` (multipart `file`, `coupleId`). */
    @Multipart
    @POST("api/media/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
    ): UploadDto

    /** routes/search.js `POST /overpass`: nearby places by category (server-side cache and mirrors). */
    @POST("api/search/overpass")
    suspend fun overpass(@Body body: OverpassBody): OverpassDto
}
