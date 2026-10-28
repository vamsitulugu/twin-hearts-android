package com.twinhearts.app.feature.study.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Study lives in the couple's shared state: `mySubjects` / `myTimetable` and
 * `ptSubjects` / `ptTimetable` (plus `reminders` for slot alarms). Same endpoints the web
 * uses (routes/data.js), so no new backend route is needed.
 */
interface StudyApi {
    @GET("api/data/state/{coupleId}")
    suspend fun getState(@Path("coupleId") coupleId: String): StudyStateResponse

    /** The server merges the top-level keys of `state` into the saved document. */
    @POST("api/data/state")
    suspend fun saveState(@Body body: StudySaveBody): StudySaveResponse
}

@Serializable
data class StudyStateResponse(val state: JsonElement? = null)

@Serializable
data class StudySaveBody(
    val coupleId: String,
    val state: JsonObject,
    val senderRole: String,
    val myName: String,
)

@Serializable
data class StudySaveResponse(val ok: Boolean? = null)
