package com.twinhearts.app.feature.fights.data.remote

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Why this file exists: the only door from Fight Log to the server (R-40, R-60). Fights live inside the
 * couple's shared state under the key `fights`, the same place the web page keeps them.
 */
interface FightsApi {
    /** The whole shared state (the server answers with the state itself, or `null` when none exists yet). */
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): JsonElement

    @POST("api/data/state")
    suspend fun saveState(@Body body: SaveStateBody): SaveStateAck
}
