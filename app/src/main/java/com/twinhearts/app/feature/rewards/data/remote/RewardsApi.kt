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
