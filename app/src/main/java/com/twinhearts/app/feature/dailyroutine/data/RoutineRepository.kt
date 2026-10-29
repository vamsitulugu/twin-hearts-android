package com.twinhearts.app.feature.dailyroutine.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.dailyroutine.data.mapper.toBody
import com.twinhearts.app.feature.dailyroutine.data.mapper.toRoutines
import com.twinhearts.app.feature.dailyroutine.data.remote.CreateRoutineBody
import com.twinhearts.app.feature.dailyroutine.data.remote.RoutineApi
import com.twinhearts.app.feature.dailyroutine.data.remote.StatusBody
import com.twinhearts.app.feature.dailyroutine.data.remote.UpdateRoutineBody
import com.twinhearts.app.feature.dailyroutine.domain.model.Routine
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineDraft
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineStatus
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import java.time.LocalDate
import javax.inject.Inject

/** Who is using the screen: needed to tell "mine" from "partner" and for the new routine's owner. */
data class RoutineIdentity(val role: String, val myName: String, val partnerName: String) {
    val partnerRole: String get() = if (role == "user1") "user2" else "user1"
}

interface RoutineRepository {
    suspend fun identity(): RoutineIdentity?

    /** Every routine of the couple, both people. */
    suspend fun load(): AppResult<List<Routine>>
    suspend fun create(draft: RoutineDraft, date: LocalDate): AppResult<Unit>
    suspend fun update(id: String, draft: RoutineDraft, date: LocalDate): AppResult<Unit>
    suspend fun setStatus(id: String, date: LocalDate, status: RoutineStatus): AppResult<Unit>
    suspend fun duplicate(id: String): AppResult<Unit>
    suspend fun delete(id: String): AppResult<Unit>
}

class RoutineRepositoryImpl @Inject constructor(
    private val api: RoutineApi,
    private val sessionStore: SessionStore,
) : RoutineRepository {

    private suspend fun session(): Session? = sessionStore.session.first()

    override suspend fun identity(): RoutineIdentity? =
        session()?.let { RoutineIdentity(it.role, it.myName, it.partnerName) }

    override suspend fun load(): AppResult<List<Routine>> {
        val session = session() ?: return AppResult.Failure(AppError.Unauthorized())
        return when (val result = safeApiCall<JsonElement> { api.getRoutines(session.coupleId) }) {
            is AppResult.Success -> AppResult.Success(result.data.toRoutines())
            is AppResult.Failure -> result
        }
    }

    override suspend fun create(draft: RoutineDraft, date: LocalDate): AppResult<Unit> {
        val session = session() ?: return AppResult.Failure(AppError.Unauthorized())
        val body = CreateRoutineBody(session.coupleId, session.role, draft.toBody(date), session.myName)
        return unit(safeApiCall<JsonElement> { api.create(body) })
    }

    override suspend fun update(id: String, draft: RoutineDraft, date: LocalDate): AppResult<Unit> =
        unit(safeApiCall<JsonElement> { api.update(id, UpdateRoutineBody(draft.toBody(date))) })

    override suspend fun setStatus(id: String, date: LocalDate, status: RoutineStatus): AppResult<Unit> =
        unit(safeApiCall<JsonElement> { api.setStatus(id, StatusBody(date.toString(), status.key)) })

    override suspend fun duplicate(id: String): AppResult<Unit> = unit(safeApiCall<JsonElement> { api.duplicate(id) })

    override suspend fun delete(id: String): AppResult<Unit> = unit(safeApiCall { api.delete(id) })

    private fun <T> unit(result: AppResult<T>): AppResult<Unit> = when (result) {
        is AppResult.Success -> AppResult.Success(Unit)
        is AppResult.Failure -> result
    }
}
