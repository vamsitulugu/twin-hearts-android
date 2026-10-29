package com.twinhearts.app.feature.games.domain.engine

import com.twinhearts.app.feature.games.domain.model.Outcome
import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.serialization.Serializable

/** Everything about one Tic Tac Toe match. [board] holds "", "X" or "O" for the 9 cells. */
@Serializable
data class TicTacToeState(
    val board: List<String> = List(CELLS) { "" },
    val turn: Side = Side.One,
    val outcome: Outcome = Outcome.None,
    val winningLine: List<Int> = emptyList(),
    val scoreOne: Int = 0,
    val scoreTwo: Int = 0,
) {
    fun scoreOf(side: Side): Int = if (side == Side.One) scoreOne else scoreTwo

    companion object {
        const val CELLS = 9
    }
}

/** The rules of Tic Tac Toe. No Android in here, so it can be unit tested. */
object TicTacToeEngine {

    private val lines = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
        listOf(0, 4, 8), listOf(2, 4, 6),
    )

    /** The first player is X, the second is O (same as the web app). */
    fun symbolOf(side: Side): String = if (side == Side.One) "X" else "O"

    /**
     * [side] marks cell [index]. Returns the new state, or null when the move is not allowed
     * (not your turn, cell taken, round over).
     */
    fun play(state: TicTacToeState, side: Side, index: Int): TicTacToeState? {
        if (index !in 0 until TicTacToeState.CELLS) return null
        if (state.outcome != Outcome.None || state.turn != side || state.board[index].isNotEmpty()) return null
        val board = state.board.toMutableList().also { it[index] = symbolOf(side) }
        val line = winningLine(board)
        return when {
            line.isNotEmpty() -> state.copy(
                board = board,
                outcome = Outcome.of(side),
                winningLine = line,
                scoreOne = state.scoreOne + if (side == Side.One) 1 else 0,
                scoreTwo = state.scoreTwo + if (side == Side.Two) 1 else 0,
            )
            board.all { it.isNotEmpty() } -> state.copy(board = board, outcome = Outcome.Draw)
            else -> state.copy(board = board, turn = side.other)
        }
    }

    /** Another round: empty board, scores are kept, first player starts. */
    fun rematch(state: TicTacToeState): TicTacToeState =
        TicTacToeState(scoreOne = state.scoreOne, scoreTwo = state.scoreTwo)

    fun winningLine(board: List<String>): List<Int> =
        lines.firstOrNull { (a, b, c) -> board[a].isNotEmpty() && board[a] == board[b] && board[a] == board[c] }
            ?: emptyList()
}
