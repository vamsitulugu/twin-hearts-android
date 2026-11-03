package com.twinhearts.app.feature.rewards.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.rewards.data.remote.AdClaimBody
import com.twinhearts.app.feature.rewards.data.remote.AdStartBody
import com.twinhearts.app.feature.rewards.data.remote.BuyItemBody
import com.twinhearts.app.feature.rewards.data.remote.CheckInBody
import com.twinhearts.app.feature.rewards.data.remote.EarnBody
import com.twinhearts.app.feature.rewards.domain.AdClaim
import com.twinhearts.app.feature.rewards.domain.AdTicket
import com.twinhearts.app.feature.rewards.domain.EarnAction
import com.twinhearts.app.feature.rewards.domain.EarnItem
import com.twinhearts.app.feature.rewards.domain.EarnResult
import com.twinhearts.app.feature.rewards.data.remote.EquipItemBody
import com.twinhearts.app.feature.rewards.data.remote.GiftDto
import com.twinhearts.app.feature.rewards.data.remote.RewardsApi
import com.twinhearts.app.feature.rewards.data.remote.RewardsSummaryDto
import com.twinhearts.app.feature.rewards.data.remote.ScratchBody
import com.twinhearts.app.feature.rewards.data.remote.SendGiftBody
import com.twinhearts.app.feature.rewards.data.remote.SpinBody
import com.twinhearts.app.feature.rewards.data.remote.SpinSliceDto
import com.twinhearts.app.feature.rewards.data.remote.StoreItemDto
import com.twinhearts.app.feature.rewards.domain.CoinTx
import com.twinhearts.app.feature.rewards.domain.CycleDay
import com.twinhearts.app.feature.rewards.domain.Gift
import com.twinhearts.app.feature.rewards.domain.GiftSent
import com.twinhearts.app.feature.rewards.domain.GiftShop
import com.twinhearts.app.feature.rewards.domain.RewardsSummary
import com.twinhearts.app.feature.rewards.domain.ScratchResult
import com.twinhearts.app.feature.rewards.domain.SpinResult
import com.twinhearts.app.feature.rewards.domain.SpinWheelSlice
import com.twinhearts.app.feature.rewards.domain.StoreCatalog
import com.twinhearts.app.feature.rewards.domain.StoreEquipResult
import com.twinhearts.app.feature.rewards.domain.StoreItem
import com.twinhearts.app.feature.rewards.domain.StorePurchaseResult
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
    suspend fun spin(tapId: String, paid: Boolean = false, adToken: String? = null): AppResult<SpinResult>

    /** Step 3: Lucky Scratch card. */
    suspend fun scratch(tapId: String): AppResult<ScratchResult>

    /** Phase 3: get the one-time ticket before a rewarded ad. purpose = "coins" or "spin". */
    suspend fun adStart(purpose: String): AppResult<AdTicket>

    /** Phase 3: cash in a "coins" ticket after the ad was watched. */
    suspend fun adClaim(token: String): AppResult<AdClaim>

    /** Phase 1: today's earning options. */
    suspend fun earnStatus(): AppResult<List<EarnItem>>

    /** Phase 1: claim the coins for an everyday action (the server decides the amount and the daily cap). */
    suspend fun earn(action: EarnAction): AppResult<EarnResult>

    /** Step 4: Twin Store catalog. */
    suspend fun store(): AppResult<StoreCatalog>

    /** Step 4: Buy store item. */
    suspend fun buyItem(itemId: String, tapId: String): AppResult<StorePurchaseResult>

    /** Step 4: Equip or unequip store item. */
    suspend fun equipItem(category: String, itemId: String): AppResult<StoreEquipResult>
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

    override suspend fun spin(tapId: String, paid: Boolean, adToken: String?): AppResult<SpinResult> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.spin(SpinBody(s.coupleId, s.userId, tapId, paid, adToken), s.userId) }
            .map { SpinResult(it.sliceIndex, it.coins, it.label, it.balance, it.duplicate) }
    }

    override suspend fun scratch(tapId: String): AppResult<ScratchResult> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.scratch(ScratchBody(s.coupleId, s.userId, tapId), s.userId) }
            .map { ScratchResult(it.prizeId, it.coins, it.title, it.message, it.emoji, it.balance, it.duplicate) }
    }

    override suspend fun adStart(purpose: String): AppResult<AdTicket> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.adStart(AdStartBody(s.coupleId, s.userId, purpose), s.userId) }
            .map { AdTicket(it.token, it.minSeconds) }
    }

    override suspend fun adClaim(token: String): AppResult<AdClaim> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.adClaim(AdClaimBody(s.coupleId, s.userId, token), s.userId) }
            .map { AdClaim(it.earned, it.balance, it.remaining) }
    }

    override suspend fun earnStatus(): AppResult<List<EarnItem>> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.earnStatus(s.coupleId, s.userId, s.userId) }
            .map { r -> r.items.map { EarnItem(it.action, it.label, it.emoji, it.coins, it.daily, it.remaining) } }
    }

    override suspend fun earn(action: EarnAction): AppResult<EarnResult> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.earn(EarnBody(s.coupleId, s.userId, action.key), s.userId) }
            .map { EarnResult(it.earned, it.capped, it.remaining, it.balance) }
    }

    override suspend fun store(): AppResult<StoreCatalog> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.store(s.coupleId, s.userId, s.userId) }
            .map { r ->
                StoreCatalog(
                    balance = r.balance,
                    items = r.items.map { it.toDomain() },
                    unlockedItemIds = r.unlockedItemIds,
                    equipped = r.equipped,
                )
            }
    }

    override suspend fun buyItem(itemId: String, tapId: String): AppResult<StorePurchaseResult> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.buy(BuyItemBody(s.coupleId, s.userId, itemId, tapId), s.userId) }
            .map { StorePurchaseResult(it.itemId, it.balance, it.unlockedItemIds, it.duplicate) }
    }

    override suspend fun equipItem(category: String, itemId: String): AppResult<StoreEquipResult> {
        val s = sessionStore.session.first() ?: return AppResult.Failure(AppError.Unauthorized())
        return safeApiCall { api.equip(EquipItemBody(s.coupleId, s.userId, category, itemId), s.userId) }
            .map { StoreEquipResult(it.category, it.itemId, it.equipped) }
    }
}

private fun GiftDto.toDomain() = Gift(id, name, emoji, price, tier, effect, blurb)

private fun SpinSliceDto.toDomain() = SpinWheelSlice(index, coins, label, emoji, color)

private fun StoreItemDto.toDomain() = StoreItem(id, name, category, price, emoji, description, previewColor)

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
    paidSpinPrice = paidSpinPrice,
    paidSpinsLeft = paidSpinsLeft,
    adCoinsReward = adCoinsReward,
    adCoinsLeft = adCoinsLeft,
    adCoinsUnlimited = adCoinsUnlimited,
    adSpinsLeft = adSpinsLeft,
    wheelSlices = wheelSlices.map { it.toDomain() },
    unlockedItemIds = unlockedItemIds,
    equipped = equipped,
    claimed = claimed,
    earned = earned,
)

