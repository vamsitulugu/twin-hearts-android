package com.twinhearts.app.feature.rewards.domain

/**
 * Other pages call this after a real action (a hug sent, a mood saved, a memory added...).
 * It is fire-and-forget: it never blocks or fails the page, and the server decides if any coins
 * are paid (amount and daily cap). A short "+N Gold Coins" toast is shown when coins arrive.
 */
interface CoinEarner {
    fun earn(action: EarnAction)
}

/** Does nothing; for tests and previews. */
object NoCoinEarner : CoinEarner {
    override fun earn(action: EarnAction) = Unit
}
