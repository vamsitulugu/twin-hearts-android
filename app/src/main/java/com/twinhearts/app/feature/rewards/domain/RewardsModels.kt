package com.twinhearts.app.feature.rewards.domain

/** One box of the 7-day check-in row. */
data class CycleDay(val day: Int, val coins: Int, val done: Boolean, val isToday: Boolean)

/** Everything the Rewards page and the Home card show. All numbers come from the server. */
data class RewardsSummary(
    val balance: Int,
    val lifetimeEarned: Int,
    val streak: Int,
    val checkedInToday: Boolean,
    val todayCoins: Int,
    val cyclePosition: Int,
    val cycle: List<CycleDay>,
    val coupleStreak: Int,
    val coupleDoneToday: Boolean,
    val partnerCheckedInToday: Boolean,
    /** Set only right after a claim: true if that tap paid, and how much. */
    val claimed: Boolean = false,
    val earned: Int = 0,
)

/** [note] is the gift name for a gift row, empty otherwise. */
data class CoinTx(val id: Long, val amount: Int, val kind: String, val createdAtIso: String, val note: String = "")

/** One gift in the Gift Shop. [effect] is the chat animation it plays (confetti / hearts / balloons). */
data class Gift(
    val id: String,
    val name: String,
    val emoji: String,
    val price: Int,
    val tier: String,
    val effect: String,
    val blurb: String,
)

data class GiftShop(val balance: Int, val gifts: List<Gift>)

data class GiftSent(val balance: Int, val gift: Gift?, val duplicate: Boolean)
