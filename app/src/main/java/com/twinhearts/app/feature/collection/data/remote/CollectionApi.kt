package com.twinhearts.app.feature.collection.data.remote

import com.twinhearts.app.core.network.StateEnvelopeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Collection reads the same two places as collection.html: voice messages and chat photos come from
 * `chat_messages`, saved photos and the voice Library from the shared state document.
 * No new backend route is needed.
 */
interface CollectionApi {
    @GET("api/chat/{coupleId}")
    suspend fun chat(@Path("coupleId") coupleId: String, @Query("limit") limit: Int? = 1000): JsonElement

    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): CollectionStateResponse

    @POST("api/data/state")
    suspend fun saveState(@Body body: CollectionSaveBody): CollectionOk

    /** Same star toggle Chat uses, so favourites stay shared with Chat. */
    @POST("api/chat/{id}/star")
    suspend fun toggleStar(@Path("id") id: Long, @Body body: StarRequest): JsonElement

    @HTTP(method = "DELETE", path = "api/chat/{id}", hasBody = true)
    suspend fun deleteMessage(@Path("id") id: Long, @Body body: DeleteRequest): CollectionOk
}

@Serializable(with = CollectionStateResponseSerializer::class)
data class CollectionStateResponse(val state: JsonElement? = null)

/** Reads the saved document in either shape (see [unwrapState]). */
object CollectionStateResponseSerializer : StateEnvelopeSerializer<CollectionStateResponse>("CollectionStateResponse", { CollectionStateResponse(it) }, { it.state })

@Serializable
data class CollectionSaveBody(val coupleId: String, val state: JsonObject, val senderRole: String, val myName: String)

@Serializable
data class CollectionOk(val ok: Boolean? = null)

@Serializable
data class StarRequest(val coupleId: String, val role: String)

@Serializable
data class DeleteRequest(val coupleId: String, val senderRole: String, val mode: String)
