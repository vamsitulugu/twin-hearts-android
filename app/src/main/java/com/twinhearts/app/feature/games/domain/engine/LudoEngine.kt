package com.twinhearts.app.feature.games.domain.engine

import com.twinhearts.app.feature.games.domain.model.Outcome
import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.serialization.Serializable

enum class LudoEventKind { Moved, Captured, NoMove, TimedOut }

/** One line of the "what just happened" list. The screen words it for whoever is reading. */
@Serializable
data class LudoEvent(
    val side: Side,
    val kind: LudoEventKind,
    /** Token number 0..3 (shown as 1..4). */
    val token: Int = 0,
    val dice: Int = 0,
)

/**
 * A 2-player Ludo game. Token position: -1 = in the base yard, 0..50 = on the shared ring
 * (counted from that player's own start), 51..56 = home lane, 57 = home.
 */
@Serializable
data class LudoState(
    val one: List<Int> = List(TOKENS) { BASE },
    val two: List<Int> = List(TOKENS) { BASE },
    val turn: Side = Side.One,
    /** 0 = not rolled yet. */
    val dice: Int = 0,
    /** True when the roll has no legal move, so the turn is about to pass. */
    val noMove: Boolean = false,
    val outcome: Outcome = Outcome.None,
    /** Newest first, at most [HISTORY_LIMIT] lines. */
    val history: List<LudoEvent> = emptyList(),
) {
    fun tokens(side: Side): List<Int> = if (side == Side.One) one else two

    companion object {
        const val TOKENS = 4
        const val BASE = -1
        const val HOME = 57
        const val HISTORY_LIMIT = 8
    }
}

/** What one token move changed. */
data class LudoMove(
    val state: LudoState,
    /** Opponent token numbers that were sent back to their base. */
    val captured: List<Int>,
    val reachedHome: Boolean,
    val won: Boolean,
)

object LudoEngine {

    /** Seconds a player has to roll or move before the turn is taken automatically. */
    const val TURN_SECONDS = 30

    private const val RING = 52
    private const val LAST_RING_STEP = 50
    private const val FIRST_LANE_STEP = 51

    private val startOnRing = mapOf(Side.One to 0, Side.Two to 26)
    private val safeCells = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    /** The 52 shared cells of the ring as (row, column) on the 15 x 15 board. */
    val ringCells: List<Pair<Int, Int>> = listOf(
        6 to 1, 6 to 2, 6 to 3, 6 to 4, 6 to 5,
        5 to 6, 4 to 6, 3 to 6, 2 to 6, 1 to 6, 0 to 6,
        0 to 7,
        0 to 8, 1 to 8, 2 to 8, 3 to 8, 4 to 8, 5 to 8,
        6 to 9, 6 to 10, 6 to 11, 6 to 12, 6 to 13, 6 to 14,
        7 to 14,
        8 to 14, 8 to 13, 8 to 12, 8 to 11, 8 to 10, 8 to 9,
        9 to 8, 10 to 8, 11 to 8, 12 to 8, 13 to 8, 14 to 8,
        14 to 7,
        14 to 6, 13 to 6, 12 to 6, 11 to 6, 10 to 6, 9 to 6,
        8 to 5, 8 to 4, 8 to 3, 8 to 2, 8 to 1, 8 to 0,
        7 to 0,
        6 to 0,
    )

    /** The six home-lane cells for each player. */
    val homeLane: Map<Side, List<Pair<Int, Int>>> = mapOf(
        Side.One to listOf(7 to 1, 7 to 2, 7 to 3, 7 to 4, 7 to 5, 7 to 6),
        Side.Two to listOf(7 to 13, 7 to 12, 7 to 11, 7 to 10, 7 to 9, 7 to 8),
    )

    /** The two unused lanes, drawn dim so the board looks like a real Ludo board. */
    val decorativeLanes: List<List<Pair<Int, Int>>> = listOf(
        listOf(1 to 7, 2 to 7, 3 to 7, 4 to 7, 5 to 7, 6 to 7),
        listOf(13 to 7, 12 to 7, 11 to 7, 10 to 7, 9 to 7, 8 to 7),
    )

    val center: Pair<Int, Int> = 7 to 7

    /** Top-left corner (row, column) of each player's 6 x 6 base yard. */
    val baseOrigin: Map<Side, Pair<Int, Int>> = mapOf(Side.One to (0 to 0), Side.Two to (9 to 9))

    /** Top-left corners of the two unused yards. */
    val decorativeYards: List<Pair<Int, Int>> = listOf(0 to 9, 9 to 0)

    /** Where the four tokens sit inside a base yard, relative to its corner. */
    val pockets: List<Pair<Int, Int>> = listOf(1 to 1, 1 to 4, 4 to 1, 4 to 4)

    fun isSafeRingCell(ringIndex: Int): Boolean = ringIndex in safeCells

    fun ringIndexOf(side: Side, position: Int): Int = (startOnRing.getValue(side) + position) % RING

    /** The (row, column) on the board where a token stands. */
    fun cellOf(side: Side, tokenNumber: Int, position: Int): Pair<Int, Int> = when {
        position == LudoState.BASE -> {
            val (row, col) = baseOrigin.getValue(side)
            val (dr, dc) = pockets[tokenNumber]
            (row + dr) to (col + dc)
        }
        position <= LAST_RING_STEP -> ringCells[ringIndexOf(side, position)]
        position < LudoState.HOME -> homeLane.getValue(side)[position - FIRST_LANE_STEP]
        else -> center
    }

    /** Token numbers [side] may move with roll [dice]. */
    fun movable(tokens: List<Int>, dice: Int): List<Int> =
        tokens.indices.filter { i ->
            val pos = tokens[i]
            when {
                pos == LudoState.HOME -> false
                pos == LudoState.BASE -> dice == 6
                else -> pos + dice <= LudoState.HOME
            }
        }

    /** [side] rolls [dice]. Null when it is not their turn to roll. */
    fun roll(state: LudoState, side: Side, dice: Int): LudoState? {
        if (state.outcome != Outcome.None || state.turn != side || state.dice != 0) return null
        if (dice !in 1..6) return null
        return state.copy(dice = dice, noMove = movable(state.tokens(side), dice).isEmpty())
    }

    /** After a roll with no legal move: clear the dice and pass the turn (a 6 keeps it). */
    fun passAfterNoMove(state: LudoState, side: Side): LudoState {
        val keepTurn = state.dice == 6
        return state.copy(
            dice = 0,
            noMove = false,
            turn = if (keepTurn) side else side.other,
            history = state.history.prepend(LudoEvent(side, LudoEventKind.NoMove, dice = state.dice)),
        )
    }

    /** [side] moves token [token] by the rolled dice. Null when that move is not allowed. */
    fun move(state: LudoState, side: Side, token: Int): LudoMove? {
        if (state.outcome != Outcome.None || state.turn != side || state.dice == 0) return null
        val dice = state.dice
        if (token !in movable(state.tokens(side), dice)) return null

        val mine = state.tokens(side).toMutableList()
        val newPos = if (mine[token] == LudoState.BASE) 0 else mine[token] + dice
        mine[token] = newPos

        var theirs = state.tokens(side.other)
        val captured = mutableListOf<Int>()
        if (newPos in 0..LAST_RING_STEP) {
            val ring = ringIndexOf(side, newPos)
            if (!isSafeRingCell(ring)) {
                theirs = theirs.mapIndexed { i, pos ->
                    if (pos in 0..LAST_RING_STEP && ringIndexOf(side.other, pos) == ring) {
                        captured += i
                        LudoState.BASE
                    } else {
                        pos
                    }
                }
            }
        }

        var history = state.history.prepend(LudoEvent(side, LudoEventKind.Moved, token, dice))
        captured.forEach { history = history.prepend(LudoEvent(side, LudoEventKind.Captured, it)) }

        val won = mine.all { it == LudoState.HOME }
        val next = state.copy(
            one = if (side == Side.One) mine else theirs,
            two = if (side == Side.Two) mine else theirs,
            dice = 0,
            noMove = false,
            turn = if (won) side else if (dice == 6) side else side.other,
            outcome = if (won) Outcome.of(side) else Outcome.None,
            history = history,
        )
        return LudoMove(next, captured, reachedHome = newPos == LudoState.HOME, won = won)
    }

    /**
     * The turn clock ran out for [side]. With no roll yet the turn is skipped; after a roll the
     * first legal move is played for them.
     */
    fun timeOut(state: LudoState, side: Side): LudoState? {
        if (state.outcome != Outcome.None || state.turn != side) return null
        if (state.dice == 0) {
            return state.copy(
                turn = side.other,
                history = state.history.prepend(LudoEvent(side, LudoEventKind.TimedOut)),
            )
        }
        val first = movable(state.tokens(side), state.dice).firstOrNull()
            ?: return passAfterNoMove(state, side)
        return move(state, side, first)?.state
    }

    fun rematch(): LudoState = LudoState()

    private fun List<LudoEvent>.prepend(event: LudoEvent): List<LudoEvent> =
        (listOf(event) + this).take(LudoState.HISTORY_LIMIT)
}
