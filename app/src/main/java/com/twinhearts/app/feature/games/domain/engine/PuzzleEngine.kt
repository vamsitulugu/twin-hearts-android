package com.twinhearts.app.feature.games.domain.engine

import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * One piece of the photo. [row] and [col] are where it belongs; [rot] is how it is turned now
 * (0, 90, 180 or 270). It only fits when it is placed in its own spot with [rot] at 0.
 */
@Serializable
data class PuzzlePiece(
    val id: Int,
    val row: Int,
    val col: Int,
    val rot: Int,
    val owner: Side,
    val placed: Boolean = false,
)

@Serializable
enum class PuzzlePhase { Setup, Countdown, Playing, Result }

/** Picture Puzzle: each person holds half the pieces and only they can place them. */
@Serializable
data class PuzzleState(
    val phase: PuzzlePhase = PuzzlePhase.Setup,
    val gridN: Int = 3,
    val imageUrl: String = "",
    val pieces: List<PuzzlePiece> = emptyList(),
    val round: Int = 1,
    val countdownAt: Long = 0,
    val startedAt: Long = 0,
    val moves: Int = 0,
    val teamStreak: Int = 0,
    /** 0 means "no record yet". */
    val bestMs: Long = 0,
    val finishMs: Long = 0,
    val chat: List<ChatLine> = emptyList(),
) {
    val placedCount: Int get() = pieces.count { it.placed }
    fun mine(side: Side): List<PuzzlePiece> = pieces.filter { it.owner == side && !it.placed }
    fun leftFor(side: Side): Int = pieces.count { it.owner == side && !it.placed }
}

/** What happened when a piece was dropped on a slot. */
data class PuzzlePlacement(val state: PuzzleState, val correct: Boolean, val finished: Boolean)

object PuzzleEngine {

    const val MIN_GRID = 2
    const val MAX_GRID = 8
    const val COUNTDOWN_MS = 3_000L
    val gridChoices = listOf(2, 3, 4, 5, 6)
    val emojis = listOf("⬆️", "⬇️", "⬅️", "➡️", "↻", "✅", "👍", "🎯")

    fun clampGrid(n: Int): Int = n.coerceIn(MIN_GRID, MAX_GRID)

    /** Shuffles the pieces, turns each one at random and deals them out one by one. */
    fun start(state: PuzzleState, gridN: Int, imageUrl: String, now: Long, random: Random = Random.Default): PuzzleState? {
        if (state.phase != PuzzlePhase.Setup || imageUrl.isBlank()) return null
        val n = clampGrid(gridN)
        val ids = (0 until n * n).toList().shuffled(random)
        val pieces = ids.mapIndexed { i, pid ->
            PuzzlePiece(
                id = pid,
                row = pid / n,
                col = pid % n,
                rot = listOf(0, 90, 180, 270)[random.nextInt(4)],
                owner = if (i % 2 == 0) Side.One else Side.Two,
            )
        }
        return state.copy(
            phase = PuzzlePhase.Countdown,
            gridN = n,
            imageUrl = imageUrl,
            pieces = pieces,
            countdownAt = now + COUNTDOWN_MS,
            startedAt = 0,
            moves = 0,
            finishMs = 0,
            chat = emptyList(),
        )
    }

    fun beginPlaying(state: PuzzleState, now: Long): PuzzleState? {
        if (state.phase != PuzzlePhase.Countdown) return null
        return state.copy(phase = PuzzlePhase.Playing, startedAt = now)
    }

    /** Turns one of my own pieces a quarter turn. */
    fun rotate(state: PuzzleState, side: Side, pieceId: Int): PuzzleState? {
        if (state.phase != PuzzlePhase.Playing) return null
        val piece = state.pieces.firstOrNull { it.id == pieceId } ?: return null
        if (piece.owner != side || piece.placed) return null
        return state.copy(pieces = state.pieces.map { if (it.id == pieceId) it.copy(rot = (it.rot + 90) % 360) else it })
    }

    /**
     * Drops one of my pieces on the slot at [row],[col]. It sticks only when it is the right piece
     * for that slot and is turned the right way up. Every try counts as a move.
     */
    fun place(state: PuzzleState, side: Side, pieceId: Int, row: Int, col: Int, now: Long): PuzzlePlacement? {
        if (state.phase != PuzzlePhase.Playing) return null
        val piece = state.pieces.firstOrNull { it.id == pieceId } ?: return null
        if (piece.owner != side || piece.placed) return null
        val slotTaken = state.pieces.any { it.placed && it.row == row && it.col == col }
        val fits = !slotTaken && piece.row == row && piece.col == col && piece.rot == 0
        val moves = state.moves + 1
        if (!fits) return PuzzlePlacement(state.copy(moves = moves), correct = false, finished = false)
        val placed = finish(markPlaced(state.copy(moves = moves), pieceId), now)
        return PuzzlePlacement(placed, correct = true, finished = placed.phase == PuzzlePhase.Result)
    }

    /** Re-applies a placement that the other phone's save overwrote. No extra move is counted. */
    fun restorePlaced(state: PuzzleState, pieceId: Int, now: Long): PuzzleState? {
        if (state.phase != PuzzlePhase.Playing) return null
        val piece = state.pieces.firstOrNull { it.id == pieceId } ?: return null
        if (piece.placed) return null
        return finish(markPlaced(state, pieceId), now)
    }

    private fun markPlaced(state: PuzzleState, pieceId: Int): PuzzleState =
        state.copy(pieces = state.pieces.map { if (it.id == pieceId) it.copy(placed = true, rot = 0) else it })

    private fun finish(state: PuzzleState, now: Long): PuzzleState {
        if (state.pieces.isEmpty() || !state.pieces.all { it.placed }) return state
        val ms = now - state.startedAt
        return state.copy(
            phase = PuzzlePhase.Result,
            finishMs = ms,
            teamStreak = state.teamStreak + 1,
            bestMs = if (state.bestMs == 0L || ms < state.bestMs) ms else state.bestMs,
        )
    }

    fun chat(state: PuzzleState, side: Side, text: String, now: Long): PuzzleState? {
        val log = GameChat.add(state.chat, side, text, now) ?: return null
        return state.copy(chat = log)
    }

    /** "New Puzzle": pick again, keep the streak and the record. */
    fun nextRound(state: PuzzleState): PuzzleState? {
        if (state.phase != PuzzlePhase.Result) return null
        return PuzzleState(round = state.round + 1, teamStreak = state.teamStreak, bestMs = state.bestMs)
    }

    fun xpFor(owned: Int): Int = 15 + owned * 2

    fun coinsFor(owned: Int): Int = 6 + Math.round(owned * 0.8f)

    fun elapsedText(ms: Long): String {
        val total = (ms / 1000).toInt()
        return "%d:%02d".format(total / 60, total % 60)
    }
}
