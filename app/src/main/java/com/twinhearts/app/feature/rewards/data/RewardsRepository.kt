package com.twinhearts.app.feature.rewards.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.rewards.data.remote.CheckInBody
import com.twinhearts.app.feature.rewards.data.remote.RewardsApi
import com.twinhearts.app.feature.rewards.data.remote.RewardsSummaryDto
import com.twinhearts.app.feature.rewards.domain.CoinTx
import com.twinhearts.app.feature.rewards.domain.CycleDay
import com.twinhearts.app.feature.rewards.domain.RewardsSummary
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface RewardsRepository {
    suspend fun summary(): AppResult<RewardsSummary>
    suspend fun checkIn(): AppResult<RewardsSummary>
    suspend fun history(): AppResult<List<CoinTx>>
    suspend fun partnerName(): String
}

class RewardsRepositoryImpl @Inject constructor(
    private val api: RewardsApi,
    private val sessionStore: SessionStore,
) : RewardsRepository {

    override suspend fun summary(): AppResult<RewardsSummary> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.summary(s.coupleId, s.userId, s.userId) }.map { it.toDomain() }
    }

    override suspend fun checkIn(): AppResult<RewardsSummary> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.checkIn(CheckInBody(s.coupleId, s.userId), s.userId) }.map { it.toDomain() }
    }

    override suspend fun partnerName(): String = sessionStore.session.first()?.partnerName.orEmpty()

    override suspend fun history(): AppResult<List<CoinTx>> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.history(s.coupleId, s.userId, 20, s.userId) }
            .map { r -> r.items.map { CoinTx(it.id, it.amount, it.kind, it.createdAt) } }
    }
}

private fun RewardsSummaryDto.toDomain() = RewardsSummary(
    balance = balance,
    lifetimeEarned = lifetimeEarned,
    streak = streak,
    checkedInToday = checkedInToday,
    todayCoins = todayCoins,
    cyclePosition = cyclePosition,
    cycle = cycle.map { CycleDay(it.day, it.coins, it.done, it.today) },
    coupleStreak = coupleStreak,
    coupleDoneToday = coupleDoneToday,
    partnerCheckedInToday = partnerCheckedInToday,
    claimed = claimed,
    earned = earned,
)
