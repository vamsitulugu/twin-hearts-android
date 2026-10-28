package com.twinhearts.app.feature.fights.data

import android.content.Context
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.fights.data.mapper.asResolved
import com.twinhearts.app.feature.fights.data.mapper.fightId
import com.twinhearts.app.feature.fights.data.mapper.fightsArray
import com.twinhearts.app.feature.fights.data.mapper.newFightJson
import com.twinhearts.app.feature.fights.data.mapper.toFights
import com.twinhearts.app.feature.fights.data.remote.FightsApi
import com.twinhearts.app.feature.fights.data.remote.SaveStateBody
import com.twinhearts.app.feature.fights.domain.FightMath
import com.twinhearts.app.feature.fights.domain.model.Fight
import com.twinhearts.app.feature.fights.domain.model.FightDraft
import com.twinhearts.app.feature.fights.domain.model.FightForm
import com.twinhearts.app.feature.fights.domain.model.FightIdentity
import com.twinhearts.app.feature.fights.domain.model.FightStarter
import com.twinhearts.app.feature.fights.domain.model.FightStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import javax.inject.Inject

/**
 * Why this file exists: the only place Fight Log reads and writes data. Every call returns an
 * [AppResult] so screens never see an exception (R-46).
 */
interface FightsRepository {
    suspend fun identity(): FightIdentity?
    suspend fun fights(): AppResult<List<Fight>>

    /** Saves a new fight at the top of the list and returns the updated list. */
    suspend fun add(form: FightForm): AppResult<List<Fight>>
    suspend fun resolve(id: Long): AppResult<List<Fight>>
    suspend fun delete(id: Long): AppResult<List<Fight>>

    /** A half-finished wizard kept on this phone only. */
    fun loadDraft(): FightDraft?
    fun saveDraft(draft: FightDraft): Boolean
    fun clearDraft()
}

class FightsRepositoryImpl @Inject constructor(
    private val api: FightsApi,
    private val sessionStore: SessionStore,
    private val json: Json,
    @ApplicationContext context: Context,
) : FightsRepository {

    private val prefs = context.getSharedPreferences("fight_log", Context.MODE_PRIVATE)

    override suspend fun identity(): FightIdentity? = sessionStore.session.first()?.let {
        FightIdentity(it.coupleId, it.role, it.myName, it.partnerName)
    }

    /** The newest `fights` array from the server (read right before every write so a partner's changes are not lost). */
    private suspend fun readArray(who: FightIdentity): AppResult<JsonArray> =
        when (val r = safeApiCall { api.getState(who.coupleId) }) {
            is AppResult.Success -> AppResult.Success(r.data.fightsArray())
            is AppResult.Failure -> r
        }

    private suspend fun writeArray(who: FightIdentity, array: JsonArray): AppResult<List<Fight>> {
        val body = SaveStateBody(who.coupleId, JsonObject(mapOf("fights" to array)), who.role, who.myName)
        return when (val r = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> AppResult.Success(array.toFights())
            is AppResult.Failure -> r
        }
    }

    private suspend fun change(edit: (JsonArray, FightIdentity) -> JsonArray?): AppResult<List<Fight>> {
        val who = identity() ?: return AppResult.Failure(AppError.Unauthorized())
        val current = when (val r = readArray(who)) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return r
        }
        val next = edit(current, who) ?: return AppResult.Success(current.toFights())
        return writeArray(who, next)
    }

    override suspend fun fights(): AppResult<List<Fight>> {
        val who = identity() ?: return AppResult.Failure(AppError.Unauthorized())
        return when (val r = readArray(who)) {
            is AppResult.Success -> AppResult.Success(r.data.toFights())
            is AppResult.Failure -> r
        }
    }

    override suspend fun add(form: FightForm): AppResult<List<Fight>> = change { current, who ->
        val today = LocalDate.now().toString()
        val startedBy = FightMath.startedByFor(form.started, who.role)
        val fresh = newFightJson(System.currentTimeMillis(), form, startedBy, today, who.role)
        JsonArray(listOf(fresh) + current)
    }

    override suspend fun resolve(id: Long): AppResult<List<Fight>> = change { current, _ ->
        val today = LocalDate.now().toString()
        var changed = false
        val next = current.map { el ->
            val obj = el as? JsonObject
            val status = (obj?.get("status") as? kotlinx.serialization.json.JsonPrimitive)?.content
            if (obj != null && obj.fightId() == id && status != "resolved") {
                changed = true
                obj.asResolved(today)
            } else {
                el
            }
        }
        if (changed) JsonArray(next) else null
    }

    override suspend fun delete(id: Long): AppResult<List<Fight>> = change { current, _ ->
        val next = current.filterNot { (it as? JsonObject)?.fightId() == id }
        if (next.size == current.size) null else JsonArray(next)
    }

    // ── Draft (this phone only, never sent) ──

    @Serializable
    private class DraftDto(
        val title: String = "", val reason: String = "", val category: String = "Communication",
        val intensity: Int = 5, val started: String = "both", val status: String = "open",
        val feelings: List<String> = emptyList(), val need: String = "",
        val approach: List<String> = emptyList(), val notes: String = "", val step: Int = 1,
    )

    override fun saveDraft(draft: FightDraft): Boolean = runCatching {
        val f = draft.form
        val dto = DraftDto(
            f.title, f.reason, f.category, f.intensity, f.started.key,
            if (f.status == FightStatus.Resolved) "resolved" else "open",
            f.feelings, f.need, f.approach, f.notes, draft.step,
        )
        prefs.edit().putString(KEY_DRAFT, json.encodeToString(DraftDto.serializer(), dto)).apply()
    }.isSuccess

    override fun loadDraft(): FightDraft? = runCatching {
        val raw = prefs.getString(KEY_DRAFT, null) ?: return null
        val d = json.decodeFromString(DraftDto.serializer(), raw)
        FightDraft(
            form = FightForm(
                title = d.title, reason = d.reason, need = d.need, notes = d.notes,
                category = com.twinhearts.app.feature.fights.domain.model.FightCategory.entries
                    .firstOrNull { it.label == d.category }?.label ?: "Communication",
                intensity = d.intensity.coerceIn(1, 10),
                started = FightStarter.entries.firstOrNull { it.key == d.started } ?: FightStarter.Both,
                status = if (d.status == "resolved") FightStatus.Resolved else FightStatus.Open,
                feelings = d.feelings, approach = d.approach,
            ),
            step = d.step.takeIf { it in 1..3 } ?: 1,
        )
    }.getOrNull()

    override fun clearDraft() {
        prefs.edit().remove(KEY_DRAFT).apply()
    }

    private companion object {
        const val KEY_DRAFT = "uwl_fightDraft"
    }
}
