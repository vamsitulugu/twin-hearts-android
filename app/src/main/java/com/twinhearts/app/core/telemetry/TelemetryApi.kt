package com.twinhearts.app.core.telemetry

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** The server's routes/telemetry.js. Feeds the admin dashboard (active users, Health page). */
interface TelemetryApi {
    @POST("api/telemetry/ping")
    suspend fun ping(@Header("x-user-id") userId: String, @Body body: PingBody)

    @POST("api/telemetry/error")
    suspend fun error(@Header("x-user-id") userId: String?, @Body body: ErrorBody)
}

@Serializable
data class PingBody(val ok: Boolean = true)

@Serializable
data class ErrorBody(
    val source: String,
    val message: String,
    val stack: String? = null,
    val fatal: Boolean = false,
)
