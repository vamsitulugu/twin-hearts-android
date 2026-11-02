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

data class CoinTx(val id: Long, val amount: Int, val kind: String, val createdAtIso: String)
