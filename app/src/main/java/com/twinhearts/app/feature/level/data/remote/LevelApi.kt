package com.twinhearts.app.feature.level.data.remote

import com.twinhearts.app.core.network.StateEnvelopeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path

/** `GET /api/data/state/{coupleId}` (routes/data.js): the couple's saved state; Level reads only `xp`. */
interface LevelApi {
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): LevelStateResponse
}

@Serializable(with = LevelStateResponseSerializer::class)
data class LevelStateResponse(val state: JsonElement? = null)

/** Reads the saved document in either shape (see [unwrapState]). */
object LevelStateResponseSerializer : StateEnvelopeSerializer<LevelStateResponse>("LevelStateResponse", { LevelStateResponse(it) }, { it.state })
