package com.twinhearts.app.feature.calls.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.calls.data.remote.CallApi
import com.twinhearts.app.feature.calls.data.remote.CallLogBody
import com.twinhearts.app.feature.calls.data.remote.NotifyBody
import com.twinhearts.app.feature.calls.data.remote.SignalBody
import com.twinhearts.app.feature.calls.domain.CallIdentity
import com.twinhearts.app.feature.calls.domain.IceServerInfo
import com.twinhearts.app.feature.calls.domain.Signal
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject

/** Why this file exists: the only place Calls talks to the server; every call returns an [AppResult] (R-46). */
interface CallRepository {
    /** Who is calling, from the saved session; `null` when nobody is signed in or not paired. */
    suspend fun identity(): CallIdentity?
    suspend fun iceServers(): List<IceServerInfo>
    suspend fun send(who: CallIdentity, payload: JsonObject): AppResult<Unit>
    suspend fun signals(who: CallIdentity, after: Long): AppResult<List<Signal>>
    suspend fun notifyPartner(who: CallIdentity, video: Boolean): AppResult<Unit>
    suspend fun log(who: CallIdentity, video: Boolean, status: String, seconds: Int): AppResult<Unit>
}

class CallRepositoryImpl @Inject constructor(
    private val api: CallApi,
    private val sessionStore: SessionStore,
) : CallRepository {

    override suspend fun identity(): CallIdentity? =
        sessionStore.session.first()
            ?.takeIf { it.coupleId.isNotBlank() && it.paired }
            ?.let { CallIdentity(it.coupleId, it.role, it.partnerName) }

    override suspend fun iceServers(): List<IceServerInfo> {
        val parsed = (safeApiCall { api.turnCreds() } as? AppResult.Success)
            ?.data?.let(::parseIceServers)
            .orEmpty()
        // Same fallback as the web app: a public STUN server still works on friendly networks.
        return parsed.ifEmpty { listOf(IceServerInfo(listOf(FALLBACK_STUN))) }
    }

    override suspend fun send(who: CallIdentity, payload: JsonObject): AppResult<Unit> =
        unit(safeApiCall { api.postSignal(SignalBody(who.coupleId, who.myRole, payload)) })

    override suspend fun signals(who: CallIdentity, after: Long): AppResult<List<Signal>> =
        when (val r = safeApiCall { api.signals(who.coupleId, who.partnerRole, after) }) {
            is AppResult.Success -> (r.data as? JsonArray)
                ?.mapNotNull(::parseSignal)
                ?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> r
        }

    override suspend fun notifyPartner(who: CallIdentity, video: Boolean): AppResult<Unit> =
        unit(safeApiCall { api.notify(NotifyBody(who.coupleId, who.myRole, if (video) "video" else "voice")) })

    override suspend fun log(who: CallIdentity, video: Boolean, status: String, seconds: Int): AppResult<Unit> =
        unit(safeApiCall { api.log(CallLogBody(who.coupleId, who.myRole, if (video) "video" else "voice", status, seconds)) })

    private fun unit(r: AppResult<JsonElement>): AppResult<Unit> = when (r) {
        is AppResult.Success -> AppResult.Success(Unit)
        is AppResult.Failure -> r
    }

    private fun parseSignal(row: JsonElement): Signal? {
        val obj = row as? JsonObject ?: return null
        val id = (obj["id"] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: return null
        val payload = obj["payload"] as? JsonObject ?: return null
        return Signal(id, payload)
    }

    private fun parseIceServers(root: JsonElement): List<IceServerInfo> {
        val list = ((root as? JsonObject)?.get("iceServers") as? JsonArray) ?: return emptyList()
        return list.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val urls = when (val u = obj["urls"]) {
                is JsonArray -> u.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                is JsonPrimitive -> listOfNotNull(u.contentOrNull)
                else -> emptyList()
            }
            if (urls.isEmpty()) null
            else IceServerInfo(
                urls = urls,
                username = (obj["username"] as? JsonPrimitive)?.contentOrNull,
                credential = (obj["credential"] as? JsonPrimitive)?.contentOrNull,
            )
        }
    }

    private companion object {
        const val FALLBACK_STUN = "stun:stun.l.google.com:19302"
    }
}
