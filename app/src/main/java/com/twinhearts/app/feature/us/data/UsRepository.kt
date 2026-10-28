package com.twinhearts.app.feature.us.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.us.data.mapper.toCollectionItems
import com.twinhearts.app.feature.us.data.mapper.toDreamGoals
import com.twinhearts.app.feature.us.data.mapper.toLoveCounterState
import com.twinhearts.app.feature.us.data.remote.BucketItemDto
import com.twinhearts.app.feature.us.data.remote.GetStateResponse
import com.twinhearts.app.feature.us.data.remote.MediaItemDto
import com.twinhearts.app.feature.us.data.remote.bucketList
import com.twinhearts.app.feature.us.data.remote.mediaItems
import com.twinhearts.app.feature.us.data.remote.SaveStateBody
import com.twinhearts.app.feature.us.data.remote.UsApi
import com.twinhearts.app.feature.us.domain.model.CollectionItem
import com.twinhearts.app.feature.us.domain.model.DreamGoal
import com.twinhearts.app.feature.us.domain.model.LoveCounterState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.LocalDate
import javax.inject.Inject

interface UsRepository {
    suspend fun loveCounter(): AppResult<LoveCounterState>
    suspend fun collection(): AppResult<List<CollectionItem>>
    suspend fun dreamGoals(): AppResult<List<DreamGoal>>
    suspend fun saveDreamGoal(goal: DreamGoal): AppResult<Unit>
}

class UsRepositoryImpl @Inject constructor(
    private val api: UsApi,
    private val sessionStore: SessionStore,
) : UsRepository {

    private suspend fun session(): Session? = sessionStore.session.first()
    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    private suspend fun readState(): AppResult<JsonObject> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall<GetStateResponse> { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data.state as? JsonObject ?: JsonObject(emptyMap()))
            is AppResult.Failure -> result
        }
    }

    override suspend fun loveCounter(): AppResult<LoveCounterState> {
        val session = session() ?: return notSignedIn
        return readState().map { state ->
            val anniversary = state["anniversary"]?.text()?.takeIf { it.isNotBlank() }
            toLoveCounterState(anniversary, session)
        }
    }

    override suspend fun collection(): AppResult<List<CollectionItem>> {
        return readState().map { state ->
            val items = state.mediaItems()
            toCollectionItems(items)
        }
    }

    override suspend fun dreamGoals(): AppResult<List<DreamGoal>> {
        val session = session() ?: return notSignedIn
        return readState().map { state ->
            val items = state.bucketList()
            toDreamGoals(items, session.role)
        }
    }

    override suspend fun saveDreamGoal(goal: DreamGoal): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        val state = when (val read = readState()) {
            is AppResult.Success -> read.data
            is AppResult.Failure -> return read
        }
        val existing = state.bucketList()
        val next = existing + BucketItemDto(
            id = goal.id,
            title = goal.title,
            emoji = goal.emoji,
            desc = goal.description,
            target = goal.targetDate,
            priority = goal.priority,
            progress = goal.progress,
            done = goal.completed,
            by = session.role,
        )
        val patch = buildJsonObject {
            putJsonArray("bucket") {
                next.forEach { item -> add(toJsonObject(item)) }
            }
        }
        return save(session, patch)
    }

    private suspend fun save(session: Session, patch: JsonObject): AppResult<Unit> {
        val body = SaveStateBody(session.coupleId, patch, session.role, session.myName)
        return when (val result = safeApiCall { api.saveState(body) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    private fun toJsonObject(item: BucketItemDto): JsonObject =
        buildJsonObject {
            item.id?.let { put("id", it) }
            item.title?.let { put("title", it) }
            item.emoji?.let { put("emoji", it) }
            item.desc?.let { put("desc", it) }
            item.target?.let { put("target", it) }
            item.priority?.let { put("priority", it) }
            item.progress?.let { put("progress", it) }
            item.done?.let { put("done", it) }
            item.by?.let { put("by", it) }
        }

    /** Local helper so no cross-module imports are needed. */
    private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.content?.toString() ?: ""
}
