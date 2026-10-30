package com.twinhearts.app.feature.period.data.remote

import com.twinhearts.app.core.network.StateEnvelopeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Period data lives in the couple's shared state (`periods`, `symptoms`, and the per-person
 * `profile.u1|u2.gender`), the same document the web app saves (routes/data.js).
 * The server ignores `periods`/`symptoms` sent by an account marked "male".
 */
interface PeriodApi {
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): PeriodStateResponse

    /** The server merges the top-level keys of `state` into the saved document. */
    @POST("api/data/state")
    suspend fun saveState(@Body body: PeriodSaveBody): PeriodSaveResponse
}

@Serializable(with = PeriodStateResponseSerializer::class)
data class PeriodStateResponse(val state: JsonElement? = null)

/** Reads the saved document in either shape (see [unwrapState]). */
object PeriodStateResponseSerializer : StateEnvelopeSerializer<PeriodStateResponse>("PeriodStateResponse", { PeriodStateResponse(it) }, { it.state })

@Serializable
data class PeriodSaveBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class PeriodSaveResponse(val ok: Boolean? = null)
