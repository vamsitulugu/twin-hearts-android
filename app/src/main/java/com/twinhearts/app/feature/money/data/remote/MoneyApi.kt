package com.twinhearts.app.feature.money.data.remote

import com.twinhearts.app.core.network.StateEnvelopeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Money lives in the couple's shared state: `transactions` (the list) and `finGoals`
 * (monthly goal per partner). Same endpoints the web app uses (routes/data.js), so no
 * new backend route is needed.
 */
interface MoneyApi {
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): MoneyStateResponse

    /** The server merges the top-level keys of `state` into the saved document. */
    @POST("api/data/state")
    suspend fun saveState(@Body body: MoneySaveBody): MoneySaveResponse
}

@Serializable(with = MoneyStateResponseSerializer::class)
data class MoneyStateResponse(val state: JsonElement? = null)

/** Reads the saved document in either shape (see [unwrapState]). */
object MoneyStateResponseSerializer : StateEnvelopeSerializer<MoneyStateResponse>("MoneyStateResponse", { MoneyStateResponse(it) }, { it.state })

@Serializable
data class MoneySaveBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class MoneySaveResponse(val ok: Boolean? = null)
