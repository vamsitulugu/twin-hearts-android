package com.twinhearts.app.feature.games.domain.engine

import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
enum class BombDifficulty(
    val size: Int,
    val density: Double,
    val seconds: Int,
    val xp: Int,
    val coins: Int,
    val moving: Boolean,
) {
    Easy(8, 0.13, 90, 15, 6, false),
    Medium(10, 0.16, 110, 20, 8, false),
    Hard(12, 0.19, 135, 28, 11, false),
    Extreme(13, 0.23, 160, 38, 15, true),
}

@Serializable
enum class BombPhase { Lobby, Countdown, Playing, Result }

@Serializable
enum class BombResult { Won, Exploded, Timeout }

/** The guide tapped a tile to point at it. [nonce] changes each time so the same tile can repeat. */
@Serializable
data class BombPing(val idx: Int, val nonce: Long)

/**
 * Don't Touch The Bomb. The guide sees the bombs, the mover crosses the field blind.
 * Tiles are numbered row by row, 0 is the top-left start and the last one is the goal.
 */
@Serializable
data class BombState(
    val phase: BombPhase = BombPhase.Lobby,
    val difficulty: BombDifficulty = BombDifficulty.Easy,
    val hintMode: Boolean = false,
    val fogMode: Boolean = false,
    val size: Int = 8,
    val bombs: List<Int> = emptyList(),
    val start: Int = 0,
    val goal: Int = 63,
    val pos: Int = 0,
    val moves: Int = 0,
    val guide: Side = Side.One,
    val round: Int = 1,
    val countdownAt: Long = 0,
    val startedAt: Long = 0,
    val timeLimit: Int = 90,
    val result: BombResult? = null,
    val explodedAt: Int = -1,
    val finishMs: Long = 0,
    val teamStreak: Int = 0,
    /** 0 means "no record yet". */
    val bestTimeMs: Long = 0,
    val chat: List<ChatLine> = emptyList(),
    val ping: BombPing? = null,
) {
    val mover: Side get() = guide.other
}

object BombEngine {

    const val COUNTDOWN_MS = 3_000L
    const val RELOCATE_MS = 7_000L
    val emojis = listOf("⬆️", "⬇️", "⬅️", "➡️", "⚠️", "✅", "👍", "😱", "🎯", "💕")

    /** The mover only gets the four arrows; the guide gets all of them (same as the web app). */
    val moverEmojis = emojis.take(4)

    fun adjacent(size: Int, a: Int, b: Int): Boolean {
        val ar = a / size
        val ac = a % size
        val br = b / size
        val bc = b % size
        return (ar == br && Math.abs(ac - bc) == 1) || (ac == bc && Math.abs(ar - br) == 1)
    }

    /**
     * Lays out a new field and starts the countdown. The guide swaps every round after the first.
     * The start, the goal and the tiles next to them never hold a bomb, so a path always exists.
     */
    fun start(
        state: BombState,
        difficulty: BombDifficulty,
        hint: Boolean,
        fog: Boolean,
        now: Long,
        random: Random = Random.Default,
    ): BombState? {
        if (state.phase != BombPhase.Lobby) return null
        val size = difficulty.size
        val total = size * size
        val goal = total - 1
        val forbidden = setOf(0, goal, 1, size, goal - 1, goal - size)
        val need = Math.round(total * difficulty.density).toInt()
        val bombs = LinkedHashSet<Int>()
        var guard = 0
        while (bombs.size < need && guard < total * 6) {
            guard++
            val idx = random.nextInt(total)
            if (idx !in forbidden) bombs.add(idx)
        }
        return state.copy(
            phase = BombPhase.Countdown,
            difficulty = difficulty,
            hintMode = hint,
            fogMode = fog || difficulty.moving,
            size = size,
            bombs = bombs.toList(),
            start = 0,
            goal = goal,
            pos = 0,
            moves = 0,
            guide = if (state.round > 1) state.guide.other else state.guide,
            countdownAt = now + COUNTDOWN_MS,
            startedAt = 0,
            timeLimit = difficulty.seconds,
            result = null,
            explodedAt = -1,
            finishMs = 0,
            chat = emptyList(),
            ping = null,
        )
    }

    /** Countdown finished: the clock starts. */
    fun beginPlaying(state: BombState, now: Long): BombState? {
        if (state.phase != BombPhase.Countdown) return null
        return state.copy(phase = BombPhase.Playing, startedAt = now)
    }

    fun secondsLeft(state: BombState, now: Long): Int =
        maxOf(0, Math.ceil(state.timeLimit - (now - state.startedAt) / 1000.0).toInt())

    /** The mover steps onto the next tile. A bomb ends the round, the goal wins it. */
    fun move(state: BombState, side: Side, idx: Int, now: Long): BombState? {
        if (state.phase != BombPhase.Playing || side != state.mover) return null
        if (idx !in 0 until state.size * state.size || !adjacent(state.size, state.pos, idx)) return null
        val moves = state.moves + 1
        if (idx in state.bombs) {
            return state.copy(
                pos = idx, moves = moves, phase = BombPhase.Result, result = BombResult.Exploded,
                explodedAt = idx, teamStreak = 0,
            )
        }
        if (idx == state.goal) {
            val ms = now - state.startedAt
            return state.copy(
                pos = idx, moves = moves, phase = BombPhase.Result, result = BombResult.Won, finishMs = ms,
                teamStreak = state.teamStreak + 1,
                bestTimeMs = if (state.bestTimeMs == 0L || ms < state.bestTimeMs) ms else state.bestTimeMs,
            )
        }
        return state.copy(pos = idx, moves = moves)
    }

    fun timeout(state: BombState): BombState? {
        if (state.phase != BombPhase.Playing) return null
        return state.copy(phase = BombPhase.Result, result = BombResult.Timeout)
    }

    fun ping(state: BombState, side: Side, idx: Int, nonce: Long): BombState? {
        if (state.phase != BombPhase.Playing || side != state.guide) return null
        if (idx !in 0 until state.size * state.size) return null
        return state.copy(ping = BombPing(idx, nonce))
    }

    /** Extreme mode: one bomb jumps to a free tile. */
    fun relocateBomb(state: BombState, random: Random = Random.Default): BombState? {
        if (state.phase != BombPhase.Playing || state.bombs.isEmpty()) return null
        val total = state.size * state.size
        val taken = state.bombs.toSet() + setOf(state.start, state.goal, state.pos)
        val free = (0 until total).filter { it !in taken }
        if (free.isEmpty()) return null
        val which = random.nextInt(state.bombs.size)
        val moved = state.bombs.toMutableList().also { it[which] = free[random.nextInt(free.size)] }
        return state.copy(bombs = moved)
    }

    fun chat(state: BombState, side: Side, text: String, now: Long): BombState? {
        val log = GameChat.add(state.chat, side, text, now) ?: return null
        return state.copy(chat = log)
    }

    /** Back to the lobby for another round; the streak and record stay. */
    fun nextRound(state: BombState): BombState? {
        if (state.phase != BombPhase.Result) return null
        return state.copy(phase = BombPhase.Lobby, round = state.round + 1, result = null, explodedAt = -1, ping = null, chat = emptyList())
    }

    /** "Change difficulty" wipes everything, like the web app. */
    fun reset(): BombState = BombState()

    /** Fog for the mover: tiles more than two steps away are hidden, except the goal. */
    fun isFogged(state: BombState, idx: Int): Boolean {
        if (!state.fogMode || idx == state.goal) return false
        val distance = Math.abs(idx / state.size - state.pos / state.size) + Math.abs(idx % state.size - state.pos % state.size)
        return distance > 2
    }

    /** Hint mode for the mover: a safe tile that touches a bomb glows. */
    fun isDanger(state: BombState, idx: Int): Boolean {
        if (!state.hintMode || idx in state.bombs) return false
        return listOf(idx - 1, idx + 1, idx - state.size, idx + state.size).any { it in state.bombs && adjacent(state.size, idx, it) }
    }
}
