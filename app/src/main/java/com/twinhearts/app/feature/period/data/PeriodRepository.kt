package com.twinhearts.app.feature.period.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.period.data.remote.PeriodApi
import com.twinhearts.app.feature.period.data.remote.PeriodSaveBody
import com.twinhearts.app.feature.period.domain.model.NewPeriod
import com.twinhearts.app.feature.period.domain.model.PeriodData
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import javax.inject.Inject

interface PeriodRepository {
    suspend fun load(): AppResult<PeriodData>
    suspend fun logPeriod(period: NewPeriod): AppResult<PeriodData>

    /** Saves the symptoms of one day, replacing what was saved for that day. */
    suspend fun saveSymptoms(date: LocalDate, symptoms: List<String>): AppResult<PeriodData>

    suspend fun deletePeriod(id: String): AppResult<PeriodData>

    /** The "Girl / Boy" choice for accounts that have none yet (web `Gender.set`). */
    suspend fun chooseGender(female: Boolean): AppResult<PeriodData>
}

class PeriodRepositoryImpl @Inject constructor(
    private val api: PeriodApi,
    private val sessionStore: SessionStore,
) : PeriodRepository {

    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun load(): AppResult<PeriodData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return when (val state = readState(session)) {
            is AppResult.Success -> AppResult.Success(state.data.toData(session))
            is AppResult.Failure -> state
        }
    }

    override suspend fun logPeriod(period: NewPeriod): AppResult<PeriodData> =
        change { _, state ->
            // Field names match the web `logPeriod()`: id, start, flow, cycle, note. Newest first.
            val item = buildJsonObject {
                put("id", System.currentTimeMillis())
                put("start", period.start.toString())
                put("flow", period.flow)
                put("cycle", period.cycle)
                put("note", period.note)
            }
            val next = (state.rawPeriods().toList() + item).sortedByDescending { (it as? JsonObject)?.get("start").text() }
            mapOf("periods" to JsonArray(next))
        }

    override suspend fun saveSymptoms(date: LocalDate, symptoms: List<String>): AppResult<PeriodData> =
        change { _, state ->
            val existing = state["symptoms"] as? JsonObject ?: JsonObject(emptyMap())
            val day = JsonArray(symptoms.map { JsonPrimitive(it) })
            mapOf("symptoms" to JsonObject(existing + (date.toString() to day)))
        }

    override suspend fun deletePeriod(id: String): AppResult<PeriodData> =
        change { _, state ->
            // Work on the raw items so fields this app does not know about are kept.
            mapOf("periods" to JsonArray(state.rawPeriods().filterNot { (it as? JsonObject)?.get("id").text() == id }))
        }

    override suspend fun chooseGender(female: Boolean): AppResult<PeriodData> =
        change(forceGender = if (female) "female" else "male") { _, _ -> emptyMap() }

    /**
     * Read the newest state, change only the keys we own, save, and return what the page shows.
     * The server merges top-level keys, so everything else in the document is untouched.
     */
    private suspend fun change(
        forceGender: String? = null,
        build: (Session, JsonObject) -> Map<String, JsonElement>,
    ): AppResult<PeriodData> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val changes = build(session, state).toMutableMap()
        // Keep the shared profile's gender in step with this phone, so the server's "boys are
        // read-only" rule also sees it (same as the web, which copies it into the profile).
        val gender = validGender(forceGender) ?: validGender(session.gender)
        if (gender != null) profileWithGender(state, session.role, gender)?.let { changes["profile"] = it }

        val body = PeriodSaveBody(session.coupleId, JsonObject(changes), session.role, session.myName)
        return when (val saved = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> AppResult.Success(JsonObject(state + changes).toData(session, forceGender))
            is AppResult.Failure -> saved
        }
    }

    private fun profileWithGender(state: JsonObject, role: String, gender: String): JsonObject? {
        val profile = state["profile"] as? JsonObject ?: JsonObject(emptyMap())
        val key = profileKey(role)
        val mine = profile[key] as? JsonObject ?: JsonObject(emptyMap())
        if (mine["gender"].text() == gender) return null // already there, nothing to save
        return JsonObject(profile + (key to JsonObject(mine + ("gender" to JsonPrimitive(gender)))))
    }

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        when (val result = safeApiCall { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data.state as? JsonObject ?: JsonObject(emptyMap()))
            is AppResult.Failure -> result
        }

    private fun JsonObject.rawPeriods(): JsonArray = this["periods"] as? JsonArray ?: JsonArray(emptyList())

    private fun JsonObject.toData(session: Session, chosenGender: String? = null): PeriodData =
        toPeriodData(session.role, chosenGender ?: session.gender, session.partnerName)
}
