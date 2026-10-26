package com.twinhearts.app.feature.home.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.home.data.mapper.asLong
import com.twinhearts.app.feature.home.data.mapper.asText
import com.twinhearts.app.feature.home.data.mapper.toLoveCardState
import com.twinhearts.app.feature.home.data.remote.HomeApi
import com.twinhearts.app.feature.home.data.remote.SaveStateBody
import com.twinhearts.app.feature.home.domain.model.LoveCardState
import com.twinhearts.app.feature.home.domain.model.LoveKind
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject

/** Reads and writes the Stay Connected signals. The couple and role come from the saved Session. */
interface HomeRepository {
    suspend fun loveCard(): AppResult<LoveCardState>
    suspend fun send(kind: LoveKind): AppResult<Unit>

    /** Accepts the hug my partner sent. */
    suspend fun acceptHug(): AppResult<Unit>
}

class HomeRepositoryImpl @Inject constructor(
    private val api: HomeApi,
    private val sessionStore: SessionStore,
) : HomeRepository {

    private suspend fun session(): Session? = sessionStore.session.first()
    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun loveCard(): AppResult<LoveCardState> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data.toLoveCardState(session.role))
            is AppResult.Failure -> result
        }
    }

    override suspend fun send(kind: LoveKind): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val now = System.currentTimeMillis()
        val patch: JsonObject = when (kind) {
            LoveKind.Touch -> buildJsonObject {
                putJsonObject("touch") { put("from", session.role); put("ts", now) }
            }
            LoveKind.Hug -> buildJsonObject {
                putJsonObject("hug") {
                    put("id", now); put("from", session.role); put("ts", now); put("status", "pending")
                }
            }
            LoveKind.MissYou -> when (val read = readState(session)) {
                is AppResult.Success -> missYouPatch(read.data, session, now)
                is AppResult.Failure -> return read
            }
        }
        return save(session, patch)
    }

    override suspend fun acceptHug(): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val hug = state["hug"] as? JsonObject
        val from = hug?.get("from").asText()
        val id = hug?.get("id").asLong()
        if (hug == null || from == null || id == null || hug["status"].asText() != "pending") {
            return AppResult.Failure(AppError.Unknown) // nothing to accept (already handled elsewhere)
        }
        val now = System.currentTimeMillis()
        val stats = state["hugStats"] as? JsonObject
        val user1 = (stats?.get("user1").asLong() ?: 0L) + if (from == "user1") 1 else 0
        val user2 = (stats?.get("user2").asLong() ?: 0L) + if (from == "user2") 1 else 0
        val total = (stats?.get("total").asLong() ?: 0L) + 1
        val patch = buildJsonObject {
            putJsonObject("hug") {
                put("id", id); put("from", from); put("ts", hug["ts"].asLong() ?: id)
                put("status", "accepted"); put("acceptedTs", now); put("acceptedBy", session.role)
            }
            putJsonObject("hugStats") { put("total", total); put("user1", user1); put("user2", user2) }
        }
        return save(session, patch)
    }

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        when (val result = safeApiCall<JsonElement> { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data as? JsonObject ?: JsonObject(emptyMap()))
            is AppResult.Failure -> result
        }

    /** The web also counts how often each of you said "Miss You" (`missCounts`). */
    private fun missYouPatch(state: JsonObject, session: Session, now: Long): JsonObject {
        val counts = state["missCounts"] as? JsonObject
        var user1 = counts?.get("user1").asLong() ?: 0L
        var user2 = counts?.get("user2").asLong() ?: 0L
        if (session.role == "user2") user2++ else user1++
        return buildJsonObject {
            putJsonObject("missYou") {
                put("id", now); put("from", session.role); put("ts", now); put("status", "sent")
            }
            putJsonObject("missCounts") { put("user1", user1); put("user2", user2) }
        }
    }

    private suspend fun save(session: Session, patch: JsonObject): AppResult<Unit> {
        val body = SaveStateBody(session.coupleId, patch, session.role, session.myName)
        return when (val result = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }
}
