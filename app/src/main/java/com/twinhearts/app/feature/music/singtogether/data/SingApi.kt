package com.twinhearts.app.feature.music.singtogether.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Why this file exists: the only door from Sing Together to the old server. The room talks to the
 * partner through the couple's shared state (`ck_user1` / `ck_user2` lists), exactly like the web
 * app's couple-karaoke.js, so a web phone and an Android phone can sing together.
 * Answers are raw JSON so one odd field can never break the room.
 */
interface SingApi {
    /** The whole saved couple document (or JSON null). The partner's messages are inside it. */
    @GET("api/data/state/{coupleId}")
    suspend fun state(@Path("coupleId") coupleId: String): JsonElement

    /** The server merges the top-level keys of `state`, so only my own `ck_<role>` list is sent. */
    @POST("api/data/state")
    suspend fun saveState(@Body body: SingSaveBody): JsonElement

    /** Short-lived relay (TURN) servers, same call the phone calls use. */
    @GET("api/call/turn-creds")
    suspend fun turnCreds(): JsonElement
}

@Serializable
data class SingSaveBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
)
