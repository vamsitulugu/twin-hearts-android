package com.twinhearts.app.feature.bucket.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.bucket.data.remote.BucketApi
import com.twinhearts.app.feature.bucket.data.remote.BucketSaveBody
import com.twinhearts.app.feature.bucket.domain.model.BucketDream
import com.twinhearts.app.feature.bucket.domain.model.NewDream
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject

interface BucketRepository {
    suspend fun dreams(): AppResult<List<BucketDream>>
    suspend fun addDream(dream: NewDream): AppResult<List<BucketDream>>

    /** Ticks or un-ticks a dream. Completing one gives the same +8 XP as the web app. */
    suspend fun toggleDone(id: String): AppResult<List<BucketDream>>

    suspend fun deleteDreams(ids: Set<String>): AppResult<List<BucketDream>>
}

class BucketRepositoryImpl @Inject constructor(
    private val api: BucketApi,
    private val sessionStore: SessionStore,
) : BucketRepository {

    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun dreams(): AppResult<List<BucketDream>> {
        val session = sessionStore.session.first() ?: return notSignedIn
        return when (val state = readState(session)) {
            is AppResult.Success -> AppResult.Success(state.data.rawBucket().toDreams(session.role))
            is AppResult.Failure -> state
        }
    }

    override suspend fun addDream(dream: NewDream): AppResult<List<BucketDream>> =
        change { session, state ->
            // Field names match the web `addBucket()`: id, title, done, date, cat, visibility, by.
            val item = buildJsonObject {
                put("id", System.currentTimeMillis())
                put("title", dream.title)
                put("done", false)
                put("date", dream.date?.toString().orEmpty())
                put("cat", dream.category.key)
                put("visibility", dream.visibility.key)
                put("by", session.role)
            }
            JsonArray(state.rawBucket().toList() + item) to emptyMap<String, JsonElement>()
        }

    override suspend fun toggleDone(id: String): AppResult<List<BucketDream>> =
        change { _, state ->
            var nowDone = false
            val next = state.rawBucket().map { item ->
                val obj = item as? JsonObject
                if (obj != null && obj["id"].text() == id) {
                    nowDone = (obj["done"] as? JsonPrimitive)?.content != "true"
                    JsonObject(obj + ("done" to JsonPrimitive(nowDone)))
                } else {
                    item
                }
            }
            val extra: Map<String, JsonElement> =
                if (nowDone) mapOf("xp" to JsonPrimitive(currentXp(state) + COMPLETE_XP)) else emptyMap()
            JsonArray(next) to extra
        }

    override suspend fun deleteDreams(ids: Set<String>): AppResult<List<BucketDream>> =
        change { _, state ->
            // Filter the raw items so dreams hidden from us (partner's private ones) stay safe.
            JsonArray(state.rawBucket().filterNot { (it as? JsonObject)?.get("id").text() in ids }) to emptyMap<String, JsonElement>()
        }

    /** Read the newest state, change only `bucket` (and maybe `xp`), save, return what we may see. */
    private suspend fun change(
        build: (Session, JsonObject) -> Pair<JsonArray, Map<String, JsonElement>>,
    ): AppResult<List<BucketDream>> {
        val session = sessionStore.session.first() ?: return notSignedIn
        val state = when (val read = readState(session)) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val (bucket, extra) = build(session, state)
        val patch = JsonObject(extra + ("bucket" to bucket))
        val body = BucketSaveBody(session.coupleId, patch, session.role, session.myName)
        return when (val saved = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> AppResult.Success(bucket.toDreams(session.role))
            is AppResult.Failure -> saved
        }
    }

    private suspend fun readState(session: Session): AppResult<JsonObject> =
        when (val result = safeApiCall { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data.state as? JsonObject ?: JsonObject(emptyMap()))
            is AppResult.Failure -> result
        }

    private fun JsonObject.rawBucket(): JsonArray = this["bucket"] as? JsonArray ?: JsonArray(emptyList())

    private fun currentXp(state: JsonObject): Long =
        (state["xp"] as? JsonPrimitive)?.doubleOrNull?.toLong() ?: 0L

    private companion object {
        const val COMPLETE_XP = 8L
    }
}
