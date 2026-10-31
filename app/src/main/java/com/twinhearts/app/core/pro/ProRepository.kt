package com.twinhearts.app.core.pro

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

interface ProRepository {
    suspend fun status(): AppResult<ProStatus>
    suspend fun createOrder(plan: ProPlan): AppResult<ProOrder>
    suspend fun verify(orderId: String, paymentId: String, signature: String): AppResult<ProStatus>
    suspend fun history(): AppResult<List<ProPayment>>
}

@Singleton
class ProRepositoryImpl @Inject constructor(
    private val api: ProApi,
    private val sessionStore: SessionStore,
) : ProRepository {

    private suspend fun <T> withSession(block: suspend (Session) -> AppResult<T>): AppResult<T> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return block(s)
    }

    override suspend fun status() = withSession { s ->
        safeApiCall { api.status(s.coupleId) }.map { it.toDomain() }
    }

    override suspend fun createOrder(plan: ProPlan) = withSession { s ->
        safeApiCall { api.createOrder(s.userId, CreateOrderBody(s.coupleId, s.userId, plan.id)) }
            .map { dto ->
                ProOrder(
                    keyId = dto.keyId,
                    orderId = dto.orderId,
                    amountPaise = dto.amount,
                    currency = dto.currency,
                    plan = ProPlan.fromId(dto.plan) ?: plan,
                    planLabel = dto.planLabel.ifBlank { plan.name },
                    name = dto.prefill?.name?.ifBlank { null } ?: s.myName,
                    contact = dto.prefill?.contact?.ifBlank { null } ?: s.phoneNumber,
                    email = s.email,
                )
            }
    }

    override suspend fun verify(orderId: String, paymentId: String, signature: String) = withSession { s ->
        safeApiCall { api.verify(s.userId, VerifyBody(s.coupleId, s.userId, orderId, paymentId, signature)) }
            .map { it.toDomain() }
    }

    override suspend fun history() = withSession { s ->
        safeApiCall { api.history(s.userId, s.coupleId) }.map { dto ->
            dto.payments.map { ProPayment(ProPlan.fromId(it.plan), it.amount.toInt(), it.paymentId, it.paidAt) }
        }
    }
}

internal fun ProStatusDto.toDomain() = ProStatus(isPro = isPro, plan = ProPlan.fromId(plan), expiresAt = expiresAt)
