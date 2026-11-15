package com.twinhearts.app.feature.cinema.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Why this file exists: the only door from the Cinema feature to the server (R-40, R-60).
 * Read from `routes/cinema.js` in us-app (R-65). Errors come back as `{ "error": "text" }`.
 */
interface CinemaApi {
    /**
     * Opens the couple's cinema room. The server decides the role: the first phone in is the host, a phone that arrives while the
     * partner's room is live joins it as the guest. Answer: `{ session, url, role }`; [url] is the cinema link for ME.
     */
    @POST("api/cinema/create") suspend fun create(@Body body: CreateBody): LinkDto

    /** The invite waiting for me, or `{ invite: null }`. */
    @GET("api/cinema/pending/{coupleId}") suspend fun pending(
        @Path("coupleId") coupleId: String,
        @Query("role") role: String,
    ): PendingDto

    /** Partner accepts. Answer: `{ ok, url }`. 410 = expired, 409 = no longer open. */
    @POST("api/cinema/{id}/accept") suspend fun accept(@Path("id") id: String, @Body body: WhoBody): LinkDto

    @POST("api/cinema/{id}/decline") suspend fun decline(@Path("id") id: String, @Body body: WhoBody): OkDto
}

@Serializable data class CreateBody(val coupleId: String, val role: String, val slot: Int, val invite: Boolean = false)

@Serializable data class WhoBody(val coupleId: String, val role: String)

/** The cinema link for this person (avatar, room and name are inside it). */
@Serializable data class LinkDto(val url: String)

@Serializable data class OkDto(val ok: Boolean = true)

@Serializable data class PendingDto(val invite: InviteDto? = null)

@Serializable data class InviteDto(
    val id: String,
    val hostName: String? = null,
    val movie: String? = null,
)
