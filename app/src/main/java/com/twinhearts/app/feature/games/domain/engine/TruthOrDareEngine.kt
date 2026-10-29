package com.twinhearts.app.feature.games.domain.engine

import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class TodPhase { Idle, Spinning, Choose, Compose, Reveal }

@Serializable
enum class TodType {
    @SerialName("truth") Truth,
    @SerialName("dare") Dare,
}

/** The question or dare currently on the table. [answer] stays null until it is answered. */
@Serializable
data class TodCard(
    val type: TodType,
    val text: String,
    val askedBy: Side,
    val answer: String? = null,
)

@Serializable
data class TodHistoryItem(val type: TodType, val text: String, val answer: String)

/** A reaction emoji one partner sent. [nonce] changes each time so the same emoji can repeat. */
@Serializable
data class TodReaction(val emoji: String, val by: Side, val nonce: Long)

/**
 * Truth or Dare. The person whose [turn] it is spins the bottle, picks truth or dare, and
 * writes it for the other one, who answers.
 */
@Serializable
data class TodState(
    val turn: Side = Side.One,
    val streak: Int = 0,
    val phase: TodPhase = TodPhase.Idle,
    val category: String = "romantic",
    val pendingType: TodType = TodType.Truth,
    val current: TodCard? = null,
    val history: List<TodHistoryItem> = emptyList(),
    val reaction: TodReaction? = null,
)

object TruthOrDareEngine {

    const val HISTORY_LIMIT = 10
    const val ANSWER_SECONDS = 45
    const val SKIP_XP = -5

    /** Category ids, same order as the web app. */
    val categories = listOf("romantic", "cute", "funny", "deep", "hot", "adventure")

    fun setCategory(state: TodState, category: String): TodState =
        if (category in categories) state.copy(category = category) else state

    /** The asker starts the spin. */
    fun startSpin(state: TodState, side: Side): TodState? =
        if (state.turn == side && state.phase == TodPhase.Idle) state.copy(phase = TodPhase.Spinning) else null

    /** The bottle stopped. */
    fun finishSpin(state: TodState): TodState? =
        if (state.phase == TodPhase.Spinning) state.copy(phase = TodPhase.Choose) else null

    fun chooseType(state: TodState, side: Side, type: TodType): TodState? =
        if (state.turn == side && state.phase == TodPhase.Choose) {
            state.copy(phase = TodPhase.Compose, pendingType = type)
        } else {
            null
        }

    /** The asker sends the written question or dare. Blank text is refused. */
    fun send(state: TodState, side: Side, text: String): TodState? {
        val clean = text.trim()
        if (clean.isEmpty() || state.turn != side || state.phase != TodPhase.Compose) return null
        return state.copy(
            phase = TodPhase.Reveal,
            current = TodCard(state.pendingType, clean, side),
        )
    }

    /** The other person answers a truth. */
    fun answer(state: TodState, side: Side, text: String): TodState? {
        val card = state.current ?: return null
        val clean = text.trim()
        if (clean.isEmpty() || card.askedBy == side || card.answer != null || state.phase != TodPhase.Reveal) return null
        return state.copy(current = card.copy(answer = clean))
    }

    /** The other person says the dare is done. [doneText] is the saved label ("Completed"). */
    fun dareDone(state: TodState, side: Side, doneText: String): TodState? {
        val card = state.current ?: return null
        if (card.askedBy == side || card.answer != null || state.phase != TodPhase.Reveal) return null
        return state.copy(current = card.copy(answer = doneText))
    }

    /** After the answer was shown for a moment: save it to history, +1 streak, swap roles. */
    fun complete(state: TodState): TodState? {
        val card = state.current ?: return null
        val answer = card.answer ?: return null
        return state.copy(
            history = (listOf(TodHistoryItem(card.type, card.text, answer)) + state.history).take(HISTORY_LIMIT),
            streak = state.streak + 1,
            current = null,
            phase = TodPhase.Idle,
            turn = state.turn.other,
        )
    }

    /** The answerer skipped (or the clock ran out): streak resets and roles swap. */
    fun skip(state: TodState, skippedText: String): TodState? {
        val card = state.current ?: return null
        return state.copy(
            history = (listOf(TodHistoryItem(card.type, card.text, skippedText)) + state.history).take(HISTORY_LIMIT),
            streak = 0,
            current = null,
            phase = TodPhase.Idle,
            turn = state.turn.other,
        )
    }

    fun react(state: TodState, side: Side, emoji: String, nonce: Long): TodState =
        state.copy(reaction = TodReaction(emoji, side, nonce))

    /** Who has to answer the card on the table. */
    fun answererOf(state: TodState): Side? = state.current?.askedBy?.other
}
