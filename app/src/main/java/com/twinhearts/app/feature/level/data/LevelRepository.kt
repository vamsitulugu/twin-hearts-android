package com.twinhearts.app.feature.level.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.level.data.remote.LevelApi
import com.twinhearts.app.feature.level.domain.LevelState
import com.twinhearts.app.feature.level.domain.levelState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

interface LevelRepository {
    suspend fun level(): AppResult<LevelState>
}

class LevelRepositoryImpl @Inject constructor(
    private val api: LevelApi,
    private val sessionStore: SessionStore,
) : LevelRepository {

    override suspend fun level(): AppResult<LevelState> {
        val session = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return when (val r = safeApiCall { api.getState(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(levelState(parseXp(r.data.state)))
            is AppResult.Failure -> r
        }
    }
}

/** The web page stores XP as a number in the shared state. A missing or odd value counts as 0 XP. */
internal fun parseXp(state: Any?): Int {
    val value = (state as? JsonObject)?.get("xp") as? JsonPrimitive ?: return 0
    return value.content.toDoubleOrNull()?.takeIf { it.isFinite() }?.toInt()?.coerceAtLeast(0) ?: 0
}
