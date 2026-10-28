package com.twinhearts.app.feature.meetplanner.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Why this file exists: the three free, key-less public map services the web app calls straight
 * from the browser (there is no server route for them). No secret and no login is involved, and
 * the client that sends these calls never carries anything from the user's session.
 * This is the one documented exception to R-60 (see docs/features/meetplanner.md, "Decisions").
 *
 * The Retrofit base URL is Nominatim; the other two use full URLs.
 */
interface GeoApi {
    /** Nominatim search. [featureType] = "city" for the city box; [viewbox]/[bounded] bias place search. */
    @GET("search")
    suspend fun nominatimSearch(
        @Query("format") format: String,
        @Query("addressdetails") addressDetails: Int,
        @Query("limit") limit: Int,
        @Query("featureType") featureType: String?,
        @Query("viewbox") viewbox: String?,
        @Query("bounded") bounded: Int?,
        @Query("q") query: String,
    ): List<NominatimDto>

    /** Nominatim reverse lookup: coordinates to a place name (for "use my current location"). */
    @GET("reverse")
    suspend fun nominatimReverse(
        @Query("format") format: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
    ): NominatimDto

    /** Photon (typo-friendly place search), biased toward a point. */
    @GET("https://photon.komoot.io/api")
    suspend fun photon(
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("lang") lang: String,
        @Query("lat") lat: Double?,
        @Query("lon") lon: Double?,
    ): PhotonDto

    /** OSRM driving route through [coordinates] = "lng,lat;lng,lat;...". */
    @GET("https://router.project-osrm.org/route/v1/driving/{coordinates}")
    suspend fun osrmRoute(
        @Path(value = "coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String,
        @Query("geometries") geometries: String,
        @Query("steps") steps: String,
    ): OsrmDto
}
