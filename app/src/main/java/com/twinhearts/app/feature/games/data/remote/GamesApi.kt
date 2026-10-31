package com.twinhearts.app.feature.games.data.remote

import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Games has no server routes of its own. Like Home and Us it saves into the couple's shared
 * state document (routes/data.js), under keys that start with `gm_`. Photos and names come
 * from the profile routes (routes/profile.js).
 */
interface GamesApi {
    /** Whole saved state, or JSON `null` when the couple has none yet. */
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): JsonElement

    /** Merges the top-level keys of `state` into the couple's saved state. */
    @POST("api/data/state")
    suspend fun saveState(@Body body: SaveStateBody): SaveStateResponse

    /** `GET /api/profile/:userId`: my own profile row (photo as `avatar_url`). */
    @GET("api/profile/{userId}")
    suspend fun getMyProfile(@Path("userId") userId: String): ProfileRowDto

    /** `GET /api/profile/:userId/partner`: `{ profile: row | null }`. */
    @GET("api/profile/{userId}/partner")
    suspend fun getPartnerProfile(@Path("userId") userId: String): PartnerProfileDto

    /** routes/media.js `POST /upload` (multipart `file`, `coupleId`): the Picture Puzzle photo. */
    @Multipart
    @POST("api/media/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("coupleId") coupleId: RequestBody,
    ): UploadDto
}
