package com.twinhearts.app.feature.movie.cinema.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Why this file exists: the only door from Watch Together to the Twin Cinema routes (R-40, R-60). Same calls the web app makes. */
interface CinemaApi {
    @POST("api/cinema/create") suspend fun create(@Body b: CinemaCreateBody): CinemaCreateDto
    @POST("api/cinema/{id}/invite") suspend fun invite(@Path("id") id: String, @Body b: CinemaRoleBody): CinemaAckDto
    @GET("api/cinema/pending/{c}") suspend fun pending(@Path("c") c: String, @Query("role") role: String): CinemaPendingDto
    @POST("api/cinema/{id}/accept") suspend fun accept(@Path("id") id: String, @Body b: CinemaRoleBody): CinemaAcceptDto
    @POST("api/cinema/{id}/decline") suspend fun decline(@Path("id") id: String, @Body b: CinemaRoleBody): CinemaAckDto
}
