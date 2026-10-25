package com.twinhearts.app.feature.pairing.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.pairing.data.mapper.toDomain
import com.twinhearts.app.feature.pairing.data.remote.PairingApi
import com.twinhearts.app.feature.pairing.data.remote.RequestActionBody
import com.twinhearts.app.feature.pairing.data.remote.SendRequestBody
import com.twinhearts.app.feature.pairing.domain.model.IncomingRequest
import com.twinhearts.app.feature.pairing.domain.model.PairingStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Pairing actions. The signed-in user's id comes from the saved Session, so screens never pass it. */
interface PairingRepository {
    suspend fun status(): AppResult<PairingStatus>
    suspend fun pendingRequests(): AppResult<List<IncomingRequest>>
    suspend fun sendRequest(partnerPhone: String): AppResult<Unit>

    /** Accepts an invitation, saves the new couple in the Session and returns the partner's name. */
    suspend fun accept(requestId: String): AppResult<String>
    suspend fun reject(requestId: String): AppResult<Unit>
    suspend fun cancel(requestId: String): AppResult<Unit>
}

class PairingRepositoryImpl @Inject constructor(
    private val api: PairingApi,
    private val sessionStore: SessionStore,
) : PairingRepository {

    private suspend fun session(): Session? = sessionStore.session.first()
    private val notSignedIn = AppResult.Failure(AppError.Unauthorized())

    override suspend fun status(): AppResult<PairingStatus> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.status(session.userId) }) {
            is AppResult.Success -> {
                val status = result.data.toDomain()
                // Partner accepted while we were waiting: remember it on this phone too.
                if (status is PairingStatus.Connected && !session.paired) {
                    sessionStore.save(session.copy(paired = true, partnerName = status.partnerName))
                }
                AppResult.Success(status)
            }
            is AppResult.Failure -> result
        }
    }

    override suspend fun pendingRequests(): AppResult<List<IncomingRequest>> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.pendingList(session.userId) }) {
            is AppResult.Success -> AppResult.Success(result.data.requests.map { it.toDomain() })
            is AppResult.Failure -> result
        }
    }

    override suspend fun sendRequest(partnerPhone: String): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.sendRequest(SendRequestBody(session.userId, partnerPhone)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    override suspend fun accept(requestId: String): AppResult<String> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.accept(RequestActionBody(requestId, session.userId)) }) {
            is AppResult.Success -> {
                val partnerName = result.data.partnerName?.takeIf { it.isNotBlank() } ?: "Partner"
                sessionStore.save(
                    session.copy(
                        coupleId = result.data.coupleId ?: session.coupleId,
                        role = result.data.role ?: "user2",
                        partnerName = partnerName,
                        paired = true,
                    ),
                )
                AppResult.Success(partnerName)
            }
            is AppResult.Failure -> result
        }
    }

    override suspend fun reject(requestId: String): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.reject(RequestActionBody(requestId, session.userId)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }

    override suspend fun cancel(requestId: String): AppResult<Unit> {
        val session = session() ?: return notSignedIn
        return when (val result = safeApiCall { api.cancel(RequestActionBody(requestId, session.userId)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
    }
}
