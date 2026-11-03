package com.twinhearts.app.feature.rewards.domain

/** One box of the 7-day check-in row. */
data class CycleDay(val day: Int, val coins: Int, val done: Boolean, val isToday: Boolean)

/** Slices of the Spin & Win wheel. */
data class SpinWheelSlice(
    val index: Int = 0,
    val coins: Int = 0,
    val label: String = "",
    val emoji: String = "🪙",
    val colorHex: String = "#F59E0B",
)

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
    /** Step 3: Spin & Scratch availability */
    val spinsAvailable: Int = 1,
    val spunToday: Boolean = false,
    val scratchesAvailable: Int = 1,
    val scratchedToday: Boolean = false,
    /** Unscratched cards from couple games (My Scratch Cards page). */
    val cardsWaiting: Int = 0,
    /** Phase 2: extra spins cost coins after the free daily spin. */
    val paidSpinPrice: Int = 20,
    val paidSpinsLeft: Int = 0,
    /** Phase 3: rewarded ads. */
    val adCoinsReward: Int = 10,
    val adCoinsLeft: Int = 0,
    val adCoinsUnlimited: Boolean = false,
    val adSpinsLeft: Int = 0,
    val wheelSlices: List<SpinWheelSlice> = emptyList(),
    /** Step 4: Twin Store unlocks & active equipped items */
    val unlockedItemIds: List<String> = emptyList(),
    val equipped: Map<String, String> = emptyMap(),
    val partnerFrame: String = "",
    /** Set only right after a claim: true if that tap paid, and how much. */
    val claimed: Boolean = false,
    val earned: Int = 0,
)

/** [note] is the gift name or item description for a row, empty otherwise. */
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

/** Step 3: Spin & Win result from server. */
data class SpinResult(
    val sliceIndex: Int,
    val coins: Int,
    val label: String,
    val balance: Int,
    val duplicate: Boolean,
)

/** Step 3: Lucky Scratch result from server. */
data class ScratchResult(
    val prizeId: String,
    val coins: Int,
    val title: String,
    val message: String,
    val emoji: String,
    val balance: Int,
    val duplicate: Boolean,
)

/** Step 4: Twin Store item (themes, frames, decor, game decks). */
data class StoreItem(
    val id: String,
    val name: String,
    val category: String, // "theme", "frame", "decor", "game"
    val price: Int,
    val emoji: String,
    val description: String,
    val previewColor: String = "",
)

data class StoreCatalog(
    val balance: Int,
    val items: List<StoreItem>,
    val unlockedItemIds: List<String>,
    val equipped: Map<String, String>,
    val partnerFrame: String = "",
)

data class StorePurchaseResult(
    val itemId: String,
    val balance: Int,
    val unlockedItemIds: List<String>,
    val duplicate: Boolean,
)

data class StoreEquipResult(
    val category: String,
    val itemId: String,
    val equipped: Map<String, String>,
)


/** Phase 1: everyday actions that pay a few coins (the server owns the amounts and daily caps). */
enum class EarnAction(val key: String) { Mood("mood"), Love("love"), Memory("memory"), Routine("routine"), Game("game") }

/** One row of the "Earn more coins" list. */
data class EarnItem(val action: String, val label: String, val emoji: String, val coins: Int, val daily: Int, val remaining: Int)

data class EarnResult(val earned: Int, val capped: Boolean, val remaining: Int, val balance: Int)

/** Phase 3: a one-time ticket from the server; it is cashed in after the ad was watched. */
data class AdTicket(val token: String, val minSeconds: Int, val coins: Int = 10)

/** An ad was watched and a scratch card is waiting: the coins are paid only when the card is scratched. */
data class PendingAdReward(val token: String, val coins: Int)

data class AdClaim(val earned: Int, val balance: Int, val remaining: Int)

/** A scratch card won by finishing a couple game with your partner. [game] is the game key (ttt, ludo, ...). */
data class GameCard(val id: Long, val game: String)

/** A card that was already scratched (shown in "Recently scratched"). */
data class RecentCard(val id: Long, val game: String, val coins: Int, val title: String, val emoji: String)

data class GameCards(val cards: List<GameCard>, val recent: List<RecentCard>)

/** What a card paid when it was opened (by scratching or with Skip). */
data class CardPrize(
    val cardId: Long,
    val coins: Int,
    val title: String,
    val message: String,
    val emoji: String,
    val balance: Int,
    val duplicate: Boolean,
    val cardsLeft: Int,
)

data class CardGrant(val granted: Boolean, val count: Int)
