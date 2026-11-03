package com.twinhearts.app.feature.rewards.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Twin Rewards server routes (us-app: routes/rewards.js). The server decides every amount;
 * the app only asks to claim. These routes need the signed-in user id in the `x-user-id` header.
 */
interface RewardsApi {
    @GET("api/rewards/summary")
    suspend fun summary(
        @Query("coupleId") coupleId: String,
        @Query("userId") userId: String,
        @Header("x-user-id") header: String,
    ): RewardsSummaryDto

    @POST("api/rewards/checkin")
    suspend fun checkIn(@Body body: CheckInBody, @Header("x-user-id") header: String): RewardsSummaryDto

    @GET("api/rewards/history")
    suspend fun history(
        @Query("coupleId") coupleId: String,
        @Query("userId") userId: String,
        @Query("limit") limit: Int,
        @Header("x-user-id") header: String,
    ): HistoryDto

    /** Step 2: Gift Shop catalog and my balance. */
    @GET("api/rewards/gifts")
    suspend fun gifts(
        @Query("coupleId") coupleId: String,
        @Query("userId") userId: String,
        @Header("x-user-id") header: String,
    ): GiftsDto

    /** Step 2: buy a gift. The server charges the coins and posts the gift in the chat. */
    @POST("api/rewards/gift")
    suspend fun sendGift(@Body body: SendGiftBody, @Header("x-user-id") header: String): SendGiftDto

    /** Step 3: Spin & Win. */
    @POST("api/rewards/spin")
    suspend fun spin(@Body body: SpinBody, @Header("x-user-id") header: String): SpinDto

    /** Step 3: Lucky Scratch card. */
    @POST("api/rewards/scratch")
    suspend fun scratch(@Body body: ScratchBody, @Header("x-user-id") header: String): ScratchDto

    /** Phase 1: what can be earned today (server-owned amounts and daily caps). */
    @GET("api/rewards/earn/status")
    suspend fun earnStatus(
        @Query("coupleId") coupleId: String,
        @Query("userId") userId: String,
        @Header("x-user-id") header: String,
    ): EarnStatusDto

    /** Phase 1: ask the server to pay for an everyday action. Only the action name is sent. */
    @POST("api/rewards/earn")
    suspend fun earn(@Body body: EarnBody, @Header("x-user-id") header: String): EarnDto

    /** Phase 3: ask for a one-time ticket before showing a rewarded ad (purpose = "coins" or "spin"). */
    @POST("api/rewards/ad/start")
    suspend fun adStart(@Body body: AdStartBody, @Header("x-user-id") header: String): AdStartDto

    /** Phase 3: cash in the ticket after the ad was watched (purpose "coins"). A spin ticket goes to /spin instead. */
    @POST("api/rewards/ad/claim")
    suspend fun adClaim(@Body body: AdClaimBody, @Header("x-user-id") header: String): AdClaimDto

    /** Step 4: Twin Store catalog */
    @GET("api/rewards/store")
    suspend fun store(
        @Query("coupleId") coupleId: String,
        @Query("userId") userId: String,
        @Header("x-user-id") header: String,
    ): StoreCatalogDto

    /** Step 4: Buy a store item */
    @POST("api/rewards/buy")
    suspend fun buy(@Body body: BuyItemBody, @Header("x-user-id") header: String): BuyItemDto

    /** Step 4: Equip or unequip a store item */
    @POST("api/rewards/store/equip")
    suspend fun equip(@Body body: EquipItemBody, @Header("x-user-id") header: String): EquipItemDto
}

@Serializable
data class CheckInBody(val coupleId: String, val userId: String)

@Serializable
data class CycleDayDto(
    val day: Int = 0,
    val coins: Int = 0,
    val done: Boolean = false,
    val today: Boolean = false,
)

@Serializable
data class SpinSliceDto(
    val index: Int = 0,
    val coins: Int = 0,
    val label: String = "",
    val emoji: String = "🪙",
    val color: String = "#F59E0B",
)

@Serializable
data class RewardsSummaryDto(
    val balance: Int = 0,
    val lifetimeEarned: Int = 0,
    val streak: Int = 0,
    val checkedInToday: Boolean = false,
    val todayCoins: Int = 0,
    val cyclePosition: Int = 1,
    val cycle: List<CycleDayDto> = emptyList(),
    val coupleStreak: Int = 0,
    val coupleDoneToday: Boolean = false,
    val partnerCheckedInToday: Boolean = false,
    val spinsAvailable: Int = 1,
    val spunToday: Boolean = false,
    val scratchesAvailable: Int = 1,
    val scratchedToday: Boolean = false,
    /** Phase 2: price and cap of extra (paid) spins, set by the server. */
    val paidSpinPrice: Int = 20,
    val paidSpinsLeft: Int = 0,
    /** Phase 3: rewarded ads (coins per ad and how many are left today). */
    val adCoinsReward: Int = 10,
    val adCoinsLeft: Int = 0,
    /** True when coin ads have no daily cap (the server sends adCoinsLeft = 999 then). */
    val adCoinsUnlimited: Boolean = false,
    val adSpinsLeft: Int = 0,
    val wheelSlices: List<SpinSliceDto> = emptyList(),
    val unlockedItemIds: List<String> = emptyList(),
    val equipped: Map<String, String> = emptyMap(),
    /** Partner's equipped profile frame id ("" = none). Lets my screens draw it on their avatar. */
    val partnerFrame: String = "",
    /** Only on the check-in answer: true when THIS call paid the coins. */
    val claimed: Boolean = false,
    val earned: Int = 0,
)

@Serializable
data class CoinTxDto(
    val id: Long = 0,
    val amount: Int = 0,
    val kind: String = "",
    @SerialName("created_at") val createdAt: String = "",
    /** Step 2: a gift row carries `{ giftId, name, emoji }`. */
    val meta: JsonObject? = null,
)

@Serializable
data class HistoryDto(val items: List<CoinTxDto> = emptyList())

@Serializable
data class GiftDto(
    val id: String = "",
    val name: String = "",
    val emoji: String = "",
    val price: Int = 0,
    val tier: String = "special",
    val effect: String = "hearts",
    val blurb: String = "",
)

@Serializable
data class GiftsDto(val balance: Int = 0, val gifts: List<GiftDto> = emptyList())

/** [clientId] is one id per Send tap: if the answer is lost and the app retries, the server never charges twice. */
@Serializable
data class SendGiftBody(val coupleId: String, val userId: String, val giftId: String, val clientId: String)

@Serializable
data class SendGiftDto(val balance: Int = 0, val duplicate: Boolean = false, val gift: GiftDto? = null)

/** Step 3: Spin & Win */
@Serializable
data class SpinBody(val coupleId: String, val userId: String, val clientId: String, val paid: Boolean = false, val adToken: String? = null)

@Serializable
data class SpinDto(
    val ok: Boolean = false,
    val duplicate: Boolean = false,
    val sliceIndex: Int = 0,
    val coins: Int = 0,
    val label: String = "",
    val balance: Int = 0,
    val spinsAvailable: Int = 0,
)

/** Step 3: Lucky Scratch */
@Serializable
data class ScratchBody(val coupleId: String, val userId: String, val clientId: String)

@Serializable
data class ScratchDto(
    val ok: Boolean = false,
    val duplicate: Boolean = false,
    val prizeId: String = "",
    val coins: Int = 0,
    val title: String = "",
    val message: String = "",
    val emoji: String = "💖",
    val balance: Int = 0,
    val scratchesAvailable: Int = 0,
)

/** Step 4: Twin Store */
@Serializable
data class StoreItemDto(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Int = 0,
    val emoji: String = "✨",
    val description: String = "",
    val previewColor: String = "",
)

@Serializable
data class StoreCatalogDto(
    val balance: Int = 0,
    val items: List<StoreItemDto> = emptyList(),
    val unlockedItemIds: List<String> = emptyList(),
    val equipped: Map<String, String> = emptyMap(),
    val partnerFrame: String = "",
)

@Serializable
data class BuyItemBody(val coupleId: String, val userId: String, val itemId: String, val clientId: String)

@Serializable
data class BuyItemDto(
    val ok: Boolean = false,
    val balance: Int = 0,
    val itemId: String = "",
    val unlockedItemIds: List<String> = emptyList(),
    val duplicate: Boolean = false,
)

@Serializable
data class EquipItemBody(val coupleId: String, val userId: String, val category: String, val itemId: String)

@Serializable
data class EquipItemDto(
    val ok: Boolean = false,
    val category: String = "",
    val itemId: String = "",
    val equipped: Map<String, String> = emptyMap(),
)



/** Phase 1: earn coins from everyday actions. */
@Serializable
data class EarnBody(val coupleId: String, val userId: String, val action: String)

@Serializable
data class EarnDto(
    val ok: Boolean = false,
    val earned: Int = 0,
    val capped: Boolean = false,
    val remaining: Int = 0,
    val balance: Int = 0,
    val action: String = "",
)

@Serializable
data class EarnItemDto(
    val action: String = "",
    val label: String = "",
    val emoji: String = "🪙",
    val coins: Int = 0,
    val daily: Int = 0,
    val remaining: Int = 0,
)

@Serializable
data class EarnStatusDto(val items: List<EarnItemDto> = emptyList())


/** Phase 3: rewarded ads. */
@Serializable
data class AdStartBody(val coupleId: String, val userId: String, val purpose: String)

@Serializable
data class AdStartDto(val ok: Boolean = false, val token: String = "", val minSeconds: Int = 5, val coins: Int = 0)

@Serializable
data class AdClaimBody(val coupleId: String, val userId: String, val token: String)

@Serializable
data class AdClaimDto(val ok: Boolean = false, val earned: Int = 0, val duplicate: Boolean = false, val balance: Int = 0, val remaining: Int = 0)
