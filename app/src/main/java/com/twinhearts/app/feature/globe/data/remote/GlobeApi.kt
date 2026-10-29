package com.twinhearts.app.feature.globe.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/** The old Express server: routes/globe.js, media.js, location.js and pro.js. */
interface GlobeApi {

    @GET("api/globe/{coupleId}")
    suspend fun memories(@Path("coupleId") coupleId: String): List<MemoryDto>

    @POST("api/globe")
    suspend fun create(@Body body: SaveMemoryBody): SavedMemoryDto

    @PUT("api/globe/{id}")
    suspend fun update(@Path("id") id: String, @Body body: SaveMemoryBody): SavedMemoryDto

    /** The server reads `coupleId` from the query for DELETE, so a request body is not needed. */
    @DELETE("api/globe/{id}")
    suspend fun deleteMemory(@Path("id") id: String, @Query("coupleId") coupleId: String): OkDto

    @POST("api/globe/{memoryId}/media")
    suspend fun addMedia(@Path("memoryId") memoryId: String, @Body body: AddMediaBody): MediaDto

    @DELETE("api/globe/media/{mediaId}")
    suspend fun deleteMedia(@Path("mediaId") mediaId: String, @Query("coupleId") coupleId: String): OkDto

    /** Photos and videos only: the server rejects audio on this route. */
    @Multipart
    @POST("api/media/upload")
    suspend fun upload(@Part file: MultipartBody.Part, @Part("coupleId") coupleId: RequestBody): UploadDto

    /** Voice notes (any audio file, up to 15 MB). */
    @Multipart
    @POST("api/media/upload-voice")
    suspend fun uploadVoice(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
        @Part("senderRole") senderRole: RequestBody,
    ): UploadDto

    @GET("api/pro/status")
    suspend fun proStatus(@Query("coupleId") coupleId: String): ProStatusDto

    @GET("api/location/{coupleId}")
    suspend fun locations(@Path("coupleId") coupleId: String): LocationsDto

    @POST("api/location/ping")
    suspend fun ping(@Body body: PingBody): OkDto

    @POST("api/location/stop")
    suspend fun stop(@Body body: StopBody): OkDto

    @GET("api/location/{coupleId}/trail/{role}")
    suspend fun trail(@Path("coupleId") coupleId: String, @Path("role") role: String): List<TrailPointDto>
}

/**
 * Free public services the web app also calls (Nominatim search, Open-Meteo weather, OSRM roads).
 * They hold no secrets; full URLs are passed with @Url so one Retrofit reaches all three hosts.
 */
interface GlobeGeoApi {

    @Headers("User-Agent: TwinHearts-Android/1.0 (couples app)", "Accept-Language: en", "Accept: application/json")
    @GET
    suspend fun search(@Url url: String): List<NominatimHitDto>

    @GET
    suspend fun weather(@Url url: String): OpenMeteoDto

    @GET
    suspend fun route(@Url url: String): OsrmDto
}
