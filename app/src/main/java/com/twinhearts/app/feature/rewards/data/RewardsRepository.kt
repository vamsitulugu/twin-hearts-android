package com.twinhearts.app.feature.rewards.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.rewards.data.remote.CheckInBody
import com.twinhearts.app.feature.rewards.data.remote.GiftDto
import com.twinhearts.app.feature.rewards.data.remote.SendGiftBody
import com.twinhearts.app.feature.rewards.data.remote.RewardsApi
import com.twinhearts.app.feature.rewards.data.remote.RewardsSummaryDto
import com.twinhearts.app.feature.rewards.data.remote.ScratchBody
import com.twinhearts.app.feature.rewards.data.remote.SpinBody
import com.twinhearts.app.feature.rewards.data.remote.SpinSliceDto
import com.twinhearts.app.feature.rewards.domain.CoinTx
import com.twinhearts.app.feature.rewards.domain.CycleDay
import com.twinhearts.app.feature.rewards.domain.Gift
import com.twinhearts.app.feature.rewards.domain.GiftSent
import com.twinhearts.app.feature.rewards.domain.GiftShop
import com.twinhearts.app.feature.rewards.domain.RewardsSummary
import com.twinhearts.app.feature.rewards.domain.ScratchResult
import com.twinhearts.app.feature.rewards.domain.SpinResult
import com.twinhearts.app.feature.rewards.domain.SpinWheelSlice
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject

interface RewardsRepository {
    suspend fun summary(): AppResult<RewardsSummary>
    suspend fun checkIn(): AppResult<RewardsSummary>
    suspend fun history(): AppResult<List<CoinTx>>
    suspend fun partnerName(): String

    /** Step 2: the Gift Shop catalog with my balance. */
    suspend fun gifts(): AppResult<GiftShop>

    /** Step 2: buy [giftId]. [tapId] is one id per Send tap (a retry with the same id is never charged twice). */
    suspend fun sendGift(giftId: String, tapId: String): AppResult<GiftSent>

    /** Step 3: Spin & Win wheel. */
    suspend fun spin(tapId: String): AppResult<SpinResult>

    /** Step 3: Lucky Scratch card. */
    suspend fun scratch(tapId: String): AppResult<ScratchResult>
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
            .map { r ->
                r.items.map {
                    CoinTx(it.id, it.amount, it.kind, it.createdAt, note = (it.meta?.get("name") as? JsonPrimitive)?.contentOrNull.orEmpty())
                }
            }
    }

    override suspend fun gifts(): AppResult<GiftShop> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.gifts(s.coupleId, s.userId, s.userId) }
            .map { r -> GiftShop(r.balance, r.gifts.map { it.toDomain() }) }
    }

    override suspend fun sendGift(giftId: String, tapId: String): AppResult<GiftSent> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.sendGift(SendGiftBody(s.coupleId, s.userId, giftId, tapId), s.userId) }
            .map { r -> GiftSent(r.balance, r.gift?.toDomain(), r.duplicate) }
    }

    override suspend fun spin(tapId: String): AppResult<SpinResult> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.spin(SpinBody(s.coupleId, s.userId, tapId), s.userId) }
            .map { SpinResult(it.sliceIndex, it.coins, it.label, it.balance, it.duplicate) }
    }

    override suspend fun scratch(tapId: String): AppResult<ScratchResult> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.scratch(ScratchBody(s.coupleId, s.userId, tapId), s.userId) }
            .map { ScratchResult(it.prizeId, it.coins, it.title, it.message, it.emoji, it.balance, it.duplicate) }
    }
}

private fun GiftDto.toDomain() = Gift(id, name, emoji, price, tier, effect, blurb)

private fun SpinSliceDto.toDomain() = SpinWheelSlice(index, coins, label, emoji, color)

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
    spinsAvailable = spinsAvailable,
    spunToday = spunToday,
    scratchesAvailable = scratchesAvailable,
    scratchedToday = scratchedToday,
    wheelSlices = wheelSlices.map { it.toDomain() },
    claimed = claimed,
    earned = earned,
)
