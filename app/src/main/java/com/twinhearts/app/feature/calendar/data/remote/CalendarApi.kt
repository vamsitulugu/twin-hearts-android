package com.twinhearts.app.feature.calendar.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * The calendar lives in the couple's shared state (`events` key), the same document the web app
 * saves (routes/data.js). No new backend route is needed.
 */
interface CalendarApi {
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): CalendarStateResponse

    /** The server merges the top-level keys of `state` into the saved document. */
    @POST("api/data/state")
    suspend fun saveState(@Body body: CalendarSaveBody): CalendarSaveResponse
}

@Serializable
data class CalendarStateResponse(val state: JsonElement? = null)

@Serializable
data class CalendarSaveBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class CalendarSaveResponse(val ok: Boolean? = null)
