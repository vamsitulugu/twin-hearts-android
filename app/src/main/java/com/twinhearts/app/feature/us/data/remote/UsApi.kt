package com.twinhearts.app.feature.us.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Reads/writes the couple's shared state (routes/data.js). Reuses the same
 * /api/data/state endpoints that Home uses — no new backend routes needed.
 */
interface UsApi {
    /** Whole saved state document, or null when the couple has none yet. */
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): GetStateResponse

    /** Merges the top-level keys of [state] into the couple's saved state. */
    @POST("api/data/state")
    suspend fun saveState(@Body body: SaveStateBody): SaveStateResponse
}
