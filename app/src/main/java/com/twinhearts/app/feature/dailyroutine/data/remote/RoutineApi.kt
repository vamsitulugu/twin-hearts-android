package com.twinhearts.app.feature.dailyroutine.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Calls of routes/daily-routine.js, mounted at /api/daily-routine on the old server (R-65). */
interface RoutineApi {
    /** Every routine of the couple (both people), not archived. Read as raw JSON, see RoutineMapper. */
    @GET("api/daily-routine/{coupleId}")
    suspend fun getRoutines(@Path("coupleId") coupleId: String): JsonElement

    @POST("api/daily-routine")
    suspend fun create(@Body body: CreateRoutineBody): JsonElement

    @PUT("api/daily-routine/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdateRoutineBody): JsonElement

    /** Sets the status of one day of a routine. */
    @PATCH("api/daily-routine/{id}/status")
    suspend fun setStatus(@Path("id") id: String, @Body body: StatusBody): JsonElement

    @POST("api/daily-routine/{id}/duplicate")
    suspend fun duplicate(@Path("id") id: String): JsonElement

    /** Archives the routine (the server only removes it for good with `?hard=true`, which the web never uses). */
    @DELETE("api/daily-routine/{id}")
    suspend fun delete(@Path("id") id: String): DeleteAck
}

@Serializable
data class CreateRoutineBody(val coupleId: String, val role: String, val routine: JsonObject, val myName: String)

@Serializable
data class UpdateRoutineBody(val routine: JsonObject)

@Serializable
data class StatusBody(val date: String, val status: String)

@Serializable
data class DeleteAck(val ok: Boolean? = null)
