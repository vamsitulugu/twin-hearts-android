package com.twinhearts.app.feature.games.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Plain data the Games screens use. Everything here is also what is saved in the couple's
 * shared state, so the two phones always read the same shapes.
 */

/** Which of the two partners. The web app calls them "user1" and "user2". */
@Serializable
enum class Side {
    @SerialName("user1") One,
    @SerialName("user2") Two;

    val other: Side get() = if (this == One) Two else One

    companion object {
        /** Session role text ("user1" / "user2") to a [Side]. */
        fun fromRole(role: String): Side = if (role == "user2") Two else One
    }
}

/** Result of a round: still playing, a draw, or one side won. */
@Serializable
enum class Outcome {
    None, Draw, One, Two;

    /** True when [side] won this round. */
    fun isWonBy(side: Side): Boolean = this == of(side)

    companion object {
        fun of(side: Side): Outcome = if (side == Side.One) One else Two
    }
}

/** The eight games on the hub, in the same order as the web app. */
enum class GameId(val key: String) {
    TruthOrDare("tod"),
    MindReader("mindreader"),
    Ludo("ludo"),
    TicTacToe("ttt"),
    Draw("draw"),
    Memory("memory"),
    Bomb("bomb"),
    Puzzle("puzzle");

    companion object {
        fun fromKey(key: String): GameId? = entries.firstOrNull { it.key == key }
    }
}

/** One person's game progress (XP, coins, wins, favourites). Saved per person. */
@Serializable
data class PlayerProfile(
    val xp: Int = 0,
    val coins: Int = 120,
    val wins: Int = 0,
    val favorites: List<String> = emptyList(),
    /** Game key to the time it was last opened (turns "Play Now" into "Continue"). */
    val history: Map<String, Long> = emptyMap(),
    val lastPlayDate: String = "",
    val streakDays: Int = 0,
) {
    val level: Int get() = levelFor(xp)

    /** 0f..1f progress inside the current level, for the XP bar. */
    val levelProgress: Float
        get() {
            val base = xpForLevel(level)
            val next = xpForLevel(level + 1)
            return ((xp - base).toFloat() / (next - base)).coerceIn(0f, 1f)
        }

    /** Adds (or removes) XP and keeps the daily play streak up to date. */
    fun withXp(delta: Int, today: LocalDate): PlayerProfile =
        copy(xp = max(0, xp + delta)).touchStreak(today)

    fun withCoins(delta: Int): PlayerProfile = copy(coins = max(0, coins + delta))

    fun withWin(): PlayerProfile = copy(wins = wins + 1)

    fun toggleFavorite(game: GameId): PlayerProfile =
        copy(favorites = if (game.key in favorites) favorites - game.key else favorites + game.key)

    fun withLastGame(game: GameId, nowMillis: Long): PlayerProfile =
        copy(history = history + (game.key to nowMillis))

    private fun touchStreak(today: LocalDate): PlayerProfile {
        val todayText = today.toString()
        if (lastPlayDate == todayText) return this
        val yesterdayText = today.minusDays(1).toString()
        val days = if (lastPlayDate == yesterdayText) streakDays + 1 else 1
        return copy(lastPlayDate = todayText, streakDays = days)
    }

    companion object {
        fun levelFor(xp: Int): Int = sqrt(max(0, xp) / 28.0).toInt() + 1
        fun xpForLevel(level: Int): Int = (level - 1) * (level - 1) * 28
    }
}

/** What a partner is doing right now. "Online" means the last heartbeat is recent. */
@Serializable
data class Presence(
    val game: String? = null,
    val ts: Long = 0,
) {
    fun isOnline(nowMillis: Long): Boolean = ts > 0 && nowMillis - ts < ONLINE_WINDOW_MS

    companion object {
        const val ONLINE_WINDOW_MS = 25_000L
    }
}

/** Names, photos and progress for both people: the top of the hub and every game screen. */
data class Players(
    val side: Side,
    val myName: String,
    val partnerName: String,
    val myAvatar: String?,
    val partnerAvatar: String?,
    val me: PlayerProfile,
    val partner: PlayerProfile,
    val partnerPresence: Presence,
    val partnerOnline: Boolean,
)

/** Wrapper saved around every game so an old write from one phone can never undo a newer one. */
@Serializable
data class GameEnvelope<T>(
    val rev: Long = 0,
    val by: Side = Side.One,
    val data: T,
)
