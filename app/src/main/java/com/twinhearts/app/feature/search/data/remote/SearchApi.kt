package com.twinhearts.app.feature.search.data.remote

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Why this file exists: the only door from Search to the old Express server (R-40, R-60).
 * Both answers stay raw JSON on purpose: rows come from many pages with odd columns, and one odd
 * value must never make the whole search fail. `SearchReaders.kt` reads each field defensively.
 */
interface SearchApi {

    /** Messages that contain [query] (the same server search the Chat page uses). */
    @GET("api/chat/{coupleId}/search")
    suspend fun chat(
        @Path("coupleId") coupleId: String,
        @Query("q") query: String,
        @Query("filter") filter: String = "all",
    ): JsonElement

    /** The couple's saved document: events, bucket list, notes, fights, money and so on. */
    @GET("api/data/state/{coupleId}")
    suspend fun state(@Path("coupleId") coupleId: String): JsonElement
}
