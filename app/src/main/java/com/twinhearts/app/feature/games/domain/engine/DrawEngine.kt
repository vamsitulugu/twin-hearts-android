package com.twinhearts.app.feature.games.domain.engine

import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DrawTool {
    @SerialName("pen") Pen,
    @SerialName("highlighter") Highlighter,
    @SerialName("eraser") Eraser,
    @SerialName("fill") Fill,
}

/**
 * One finished stroke. [pts] is a flat list x0,y0,x1,y1,... with every value between 0 and 1, so it
 * looks the same on any screen size. A Fill stroke has a single point: where the bucket was tapped.
 */
@Serializable
data class DrawStroke(
    val id: Long,
    val tool: DrawTool,
    val color: Int,
    val size: Int,
    val pts: List<Float>,
)

@Serializable
data class DrawGuess(val by: Side, val text: String, val correct: Boolean, val ts: Long)

@Serializable
data class DrawReaction(val emoji: String, val by: Side, val nonce: Long)

/** The round that just ended with a right guess. Both phones show it once. */
@Serializable
data class DrawResult(val word: String, val by: Side, val round: Int)

/** Draw & Guess. The drawer types their own word and draws it; the other one guesses. */
@Serializable
data class DrawState(
    val drawer: Side = Side.One,
    val word: String = "",
    val wordSet: Boolean = false,
    val strokes: List<DrawStroke> = emptyList(),
    val guesses: List<DrawGuess> = emptyList(),
    val scoreOne: Int = 0,
    val scoreTwo: Int = 0,
    val round: Int = 1,
    val roundStartedAt: Long = 0,
    val result: DrawResult? = null,
    val reaction: DrawReaction? = null,
) {
    fun scoreOf(side: Side): Int = if (side == Side.One) scoreOne else scoreTwo
}

object DrawEngine {

    const val ROUND_SECONDS = 75
    const val MAX_POINTS = 8_000
    const val GUESS_KEEP = 30

    val colors: List<Int> = listOf(
        0xFF6B093A, 0xFFC21A8B, 0xFFF59E0B, 0xFF22C55E, 0xFF3B82F6, 0xFFFFFFFF, 0xFF000000,
    ).map { it.toInt() }

    val sizes = listOf(3, 6, 11)

    val suggestions = listOf(
        "cat", "pizza", "rainbow", "guitar", "heart", "tree", "sun", "umbrella", "robot", "castle",
        "fish", "flower", "moon", "car", "house", "butterfly", "ice cream", "star", "crown", "balloon",
        "shoe", "clock", "book", "key", "cloud", "boat", "apple", "kite", "snowman", "rocket",
    )

    /** The drawer picks what to draw. Starts the clock and clears the board. */
    fun setWord(state: DrawState, side: Side, word: String, now: Long): DrawState? {
        val clean = word.trim()
        if (side != state.drawer || clean.isEmpty() || state.wordSet) return null
        return state.copy(word = clean, wordSet = true, roundStartedAt = now, strokes = emptyList(), guesses = emptyList(), result = null)
    }

    /** "Pick a different word": back to the word screen with an empty board. */
    fun newWord(state: DrawState, side: Side): DrawState? {
        if (side != state.drawer || !state.wordSet) return null
        return state.copy(word = "", wordSet = false, strokes = emptyList(), guesses = emptyList())
    }

    private fun pointCount(strokes: List<DrawStroke>): Int = strokes.sumOf { it.pts.size }

    fun addStroke(state: DrawState, side: Side, stroke: DrawStroke): DrawState? {
        if (side != state.drawer || !state.wordSet || stroke.pts.size < 2) return null
        if (pointCount(state.strokes) + stroke.pts.size > MAX_POINTS) return null
        return state.copy(strokes = state.strokes + stroke)
    }

    fun undo(state: DrawState, side: Side): DrawState? {
        if (side != state.drawer || state.strokes.isEmpty()) return null
        return state.copy(strokes = state.strokes.dropLast(1))
    }

    fun clear(state: DrawState, side: Side): DrawState? {
        if (side != state.drawer || state.strokes.isEmpty()) return null
        return state.copy(strokes = emptyList())
    }

    fun react(state: DrawState, side: Side, emoji: String, nonce: Long): DrawState =
        state.copy(reaction = DrawReaction(emoji, side, nonce))

    /**
     * The guesser's try. A right guess scores a point and moves on to the next round with the
     * roles swapped. [correct] comes from [localMatch] / [semanticMatch].
     */
    fun guess(state: DrawState, side: Side, text: String, correct: Boolean, now: Long): DrawState? {
        val clean = text.trim()
        if (side == state.drawer || !state.wordSet || clean.isEmpty()) return null
        val log = (state.guesses + DrawGuess(side, clean, correct, now)).takeLast(GUESS_KEEP)
        if (!correct) return state.copy(guesses = log)
        val scored = state.copy(
            guesses = log,
            scoreOne = state.scoreOne + if (side == Side.One) 1 else 0,
            scoreTwo = state.scoreTwo + if (side == Side.Two) 1 else 0,
        )
        return nextRound(scored, now).copy(result = DrawResult(state.word, side, state.round))
    }

    /** Swaps the roles and goes back to the word screen. */
    fun nextRound(state: DrawState, now: Long): DrawState = state.copy(
        drawer = state.drawer.other,
        word = "",
        wordSet = false,
        strokes = emptyList(),
        guesses = emptyList(),
        round = state.round + 1,
        roundStartedAt = now,
    )

    fun secondsLeft(state: DrawState, now: Long): Int =
        maxOf(0, ROUND_SECONDS - ((now - state.roundStartedAt) / 1000).toInt())

    /** What the guesser sees: blanks, then the first letter after 30s, then the letter count after 45s. */
    fun hint(word: String, elapsedSeconds: Int): String {
        if (word.isEmpty()) return ""
        var blanks = word.map { if (it == ' ') " " else "_" }.joinToString(" ")
        if (elapsedSeconds >= 30) blanks = word[0].uppercase() + blanks.drop(1)
        return if (elapsedSeconds >= 45) "$blanks  (${word.length} letters)" else blanks
    }

    /** Exact, plural, typo and "one contains the other" checks. No network needed. */
    fun localMatch(guess: String, target: String): Boolean {
        val ng = TextMatch.tight(guess)
        val nt = TextMatch.tight(target)
        if (ng.isEmpty() || nt.isEmpty()) return false
        if (ng == nt) return true
        if (TextMatch.stem(ng) == TextMatch.stem(nt)) return true
        val maxLen = maxOf(ng.length, nt.length)
        if (maxLen >= 4 && TextMatch.levenshtein(ng, nt) <= maxOf(1, (maxLen * 0.25).toInt())) return true
        if (maxLen >= 5 && (ng.contains(nt) || nt.contains(ng))) return true
        return false
    }

    /** Synonym check: the guess is a related word of the target (or the other way round). */
    fun semanticMatch(guess: String, target: String, targetRelated: Set<String>, guessRelated: Set<String>): Boolean {
        val ng = TextMatch.tight(guess)
        val nt = TextMatch.tight(target)
        return ng in targetRelated || nt in guessRelated
    }
}
