package com.twinhearts.app.feature.games.domain.engine

import com.twinhearts.app.feature.games.domain.model.Outcome
import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class MemoryCard(val emoji: String, val matched: Boolean = false)

/** One Memory Match game: 16 cards, players take turns flipping two. */
@Serializable
data class MemoryState(
    val cards: List<MemoryCard>,
    /** Indexes of the cards face-up right now (0, 1 or 2 of them). */
    val flipped: List<Int> = emptyList(),
    val turn: Side = Side.One,
    val scoreOne: Int = 0,
    val scoreTwo: Int = 0,
    val outcome: Outcome = Outcome.None,
) {
    fun scoreOf(side: Side): Int = if (side == Side.One) scoreOne else scoreTwo
}

/** What happened when the two flipped cards were checked. */
data class MemoryResolution(val state: MemoryState, val matched: Boolean, val finished: Boolean)

object MemoryEngine {

    val emojis = listOf("❤️", "🌟", "🎈", "🍕", "🌙", "🐶", "🎵", "🌈")

    fun fresh(random: Random = Random.Default): MemoryState {
        val deck = (emojis + emojis).map { MemoryCard(it) }.shuffled(random)
        return MemoryState(cards = deck)
    }

    /** [side] turns card [index] face-up. Null when it is not allowed. */
    fun flip(state: MemoryState, side: Side, index: Int): MemoryState? {
        if (state.outcome != Outcome.None || state.turn != side) return null
        if (index !in state.cards.indices) return null
        if (state.cards[index].matched || index in state.flipped || state.flipped.size >= 2) return null
        return state.copy(flipped = state.flipped + index)
    }

    fun hasPendingPair(state: MemoryState): Boolean = state.flipped.size == 2

    /**
     * Checks the two face-up cards for [side]. A match scores a point and keeps the turn;
     * a miss turns them back and passes the turn.
     */
    fun resolve(state: MemoryState, side: Side): MemoryResolution? {
        if (!hasPendingPair(state)) return null
        val (a, b) = state.flipped
        val matched = state.cards[a].emoji == state.cards[b].emoji
        if (!matched) {
            return MemoryResolution(state.copy(flipped = emptyList(), turn = state.turn.other), matched = false, finished = false)
        }
        val cards = state.cards.toMutableList().also {
            it[a] = it[a].copy(matched = true)
            it[b] = it[b].copy(matched = true)
        }
        var next = state.copy(
            cards = cards,
            flipped = emptyList(),
            scoreOne = state.scoreOne + if (side == Side.One) 1 else 0,
            scoreTwo = state.scoreTwo + if (side == Side.Two) 1 else 0,
        )
        val finished = cards.all { it.matched }
        if (finished) {
            next = next.copy(
                outcome = when {
                    next.scoreOne == next.scoreTwo -> Outcome.Draw
                    next.scoreOne > next.scoreTwo -> Outcome.One
                    else -> Outcome.Two
                },
            )
        }
        return MemoryResolution(next, matched = true, finished = finished)
    }
}
