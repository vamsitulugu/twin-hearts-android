package com.twinhearts.app.feature.livemap.data.remote

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url

/** The old Express server (routes/location.js, route.js, tracking.js, data.js). */
interface LiveMapApi {

    @GET("api/location/{coupleId}")
    suspend fun getLocations(@Path("coupleId") coupleId: String): LocationsResponse

    @POST("api/location/ping")
    suspend fun ping(@Body body: PingBody): OkResponse

    @POST("api/location/status")
    suspend fun setStatus(@Body body: StatusBody): OkResponse

    @POST("api/location/stop")
    suspend fun stop(@Body body: StopBody): OkResponse

    @GET("api/location/{coupleId}/trail/{role}")
    suspend fun getTrail(@Path("coupleId") coupleId: String, @Path("role") role: String): List<TrailPointDto>

    @POST("api/tracking/batch")
    suspend fun sendBatch(@Body body: BatchBody): OkResponse

    @POST("api/tracking/safety-alert")
    suspend fun sendSafetyAlert(@Body body: SafetyAlertBody): OkResponse

    @GET("api/tracking/{coupleId}/{role}/timeline/{date}")
    suspend fun getTimeline(
        @Path("coupleId") coupleId: String,
        @Path("role") role: String,
        @Path("date") date: String,
    ): TimelineResponse

    @GET("api/tracking/{coupleId}/{role}/geofence-events")
    suspend fun getGeofenceEvents(
        @Path("coupleId") coupleId: String,
        @Path("role") role: String,
    ): List<GeofenceEventDto>

    /** One day's totals, or JSON `null` when the day has fewer than two points. */
    @GET("api/tracking/{coupleId}/{role}/daily-report/{date}")
    suspend fun getDailyReport(
        @Path("coupleId") coupleId: String,
        @Path("role") role: String,
        @Path("date") date: String,
    ): JsonElement

    @GET("api/route/{coupleId}/{role}/dates")
    suspend fun getRouteDates(@Path("coupleId") coupleId: String, @Path("role") role: String): RouteDatesResponse

    @GET("api/route/{coupleId}/{role}/settings")
    suspend fun getRouteSettings(@Path("coupleId") coupleId: String, @Path("role") role: String): RouteSettingsResponse

    @POST("api/route/settings")
    suspend fun saveRouteSettings(@Body body: RouteSettingsBody): RouteSettingsResponse

    @GET("api/route/{coupleId}/{role}/{date}")
    suspend fun getRouteDay(
        @Path("coupleId") coupleId: String,
        @Path("role") role: String,
        @Path("date") date: String,
    ): RouteDayResponse

    @HTTP(method = "DELETE", path = "api/route/{coupleId}/{role}/history")
    suspend fun deleteRouteHistory(@Path("coupleId") coupleId: String, @Path("role") role: String): OkResponse

    /** Whole saved state, or JSON `null` when the couple has none yet. We read and write only a few keys. */
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): JsonElement

    @POST("api/data/state")
    suspend fun saveState(@Body body: SaveStateBody): OkResponse
}

/**
 * Free public map services the web app also uses (OSRM routing, Photon search, Nominatim
 * address lookup, Open-Meteo weather). They hold no secrets. Full URLs are passed in with @Url,
 * so one Retrofit can reach all four hosts.
 */
interface GeoServicesApi {

    @GET
    suspend fun route(@Url url: String): OsrmResponse

    @Headers("User-Agent: TwinHearts-Android/1.0 (couples app)")
    @GET
    suspend fun search(@Url url: String): PhotonResponse

    @Headers("User-Agent: TwinHearts-Android/1.0 (couples app)", "Accept: application/json")
    @GET
    suspend fun reverse(@Url url: String): NominatimReverseDto

    @GET
    suspend fun weather(@Url url: String): OpenMeteoResponse
}
