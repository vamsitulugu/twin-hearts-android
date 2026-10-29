package com.twinhearts.app.feature.myspace.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * My Space lives in the couple's shared state (`journal`, `habits`, `notes`, `reminders`,
 * `wellness`), the same document the web app saves (routes/data.js). No new backend route.
 */
interface MySpaceApi {
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): MySpaceStateResponse

    /** The server merges the top-level keys of `state` into the saved document. */
    @POST("api/data/state")
    suspend fun saveState(@Body body: MySpaceSaveBody): MySpaceSaveResponse
}

@Serializable
data class MySpaceStateResponse(val state: JsonElement? = null)

@Serializable
data class MySpaceSaveBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class MySpaceSaveResponse(val ok: Boolean? = null)
