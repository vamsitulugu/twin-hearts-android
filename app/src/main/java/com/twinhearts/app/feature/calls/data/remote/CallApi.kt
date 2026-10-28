package com.twinhearts.app.feature.calls.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Why this file exists: the only door from Calls to the old server (routes/call.js).
 * Answers are raw JSON so one odd field can never break a call; `CallRepository` reads them.
 */
interface CallApi {
    /** Short-lived relay (TURN) servers; the server falls back to a public STUN server. */
    @GET("api/call/turn-creds")
    suspend fun turnCreds(): JsonElement

    /** Puts one signal (offer, answer, ice, end, decline) in the shared table for my partner. */
    @POST("api/call/signal")
    suspend fun postSignal(@Body body: SignalBody): JsonElement

    /** Signals my partner sent (their role is [role]), newer than [after]. */
    @GET("api/call/signal/{coupleId}")
    suspend fun signals(
        @Path("coupleId") coupleId: String,
        @Query("role") role: String,
        @Query("after") after: Long? = null,
    ): JsonElement

    /** Asks the server to push "incoming call" to my partner's phone. */
    @POST("api/call/notify")
    suspend fun notify(@Body body: NotifyBody): JsonElement

    /** Writes the "Voice call · 2:31" / "Missed call" line into the chat. */
    @POST("api/call/log")
    suspend fun log(@Body body: CallLogBody): JsonElement
}

@Serializable
data class SignalBody(val coupleId: String, val role: String, val payload: JsonObject)

@Serializable
data class NotifyBody(val coupleId: String, val callerRole: String, val type: String)

/** [status] is "missed", "declined" or "ended"; [type] is "voice" or "video". */
@Serializable
data class CallLogBody(
    val coupleId: String,
    val callerRole: String,
    val type: String,
    val status: String,
    val duration: Int,
)
