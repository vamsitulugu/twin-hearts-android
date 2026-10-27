package com.twinhearts.app.feature.twinai.data.remote

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** Why this file exists: the only door from Twin AI to routes/ai.js and routes/ai-both.js (R-40, R-60). */
interface TwinAiApi {

    /** Server-sent events: lines like `data: {"delta":"Hi"}` and a final `data: [DONE]`. */
    @Streaming
    @POST("api/ai/chat/stream")
    suspend fun chatStream(@Body body: AiChatBody): ResponseBody

    /** Whole answer in one piece. Used only when the stream does not work. */
    @POST("api/ai/chat")
    suspend fun chat(@Body body: AiChatBody): AiReplyDto

    /** The couple's saved app state (profile photos, journal). Read only; the server hands back JSON or `null`. */
    @GET("api/data/state/{coupleId}")
    suspend fun state(@Path("coupleId") coupleId: String): ResponseBody

    /** Real names of both partners, the web falls back to this when its own copy is empty. */
    @GET("api/ai-both/names/{coupleId}")
    suspend fun names(@Path("coupleId") coupleId: String): NamesDto

    // ── BOTH mode ──

    @GET("api/pro/status")
    suspend fun proStatus(@Query("coupleId") coupleId: String): ProStatusDto

    @GET("api/ai-both/sessions/{coupleId}")
    suspend fun sessions(@Path("coupleId") coupleId: String, @Query("limit") limit: Int = 20): List<SessionDto>

    @POST("api/ai-both/sessions")
    suspend fun startSession(@Body body: NewSessionBody): NewSessionDto

    @GET("api/ai-both/session/{id}")
    suspend fun session(@Path("id") id: String, @Query("role") role: String): SessionDetailDto

    @GET("api/ai-both/rounds/{id}")
    suspend fun round(@Path("id") id: String, @Query("role") role: String): RoundDto

    @POST("api/ai-both/rounds/{id}/submit")
    suspend fun submit(@Path("id") id: String, @Body body: SubmitBody): StatusDto

    @POST("api/ai-both/rounds/{id}/edit")
    suspend fun edit(@Path("id") id: String, @Body body: SubmitBody): StatusDto

    @POST("api/ai-both/rounds/{id}/nudge")
    suspend fun nudge(@Path("id") id: String, @Body body: RoleBody): NudgeDto

    @POST("api/ai-both/rounds/{id}/retry")
    suspend fun retry(@Path("id") id: String, @Body body: CoupleBody): StatusDto

    @POST("api/ai-both/sessions/{id}/next-round")
    suspend fun nextRound(@Path("id") id: String): NextRoundDto

    /** Retrofit's @DELETE cannot carry a body, and this route reads `coupleId` from one. */
    @HTTP(method = "DELETE", path = "api/ai-both/session/{id}", hasBody = true)
    suspend fun deleteSession(@Path("id") id: String, @Body body: CoupleBody): StatusDto
}
