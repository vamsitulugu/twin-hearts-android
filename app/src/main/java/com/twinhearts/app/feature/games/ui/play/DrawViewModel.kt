package com.twinhearts.app.feature.games.ui.play

import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.feature.games.data.GamesRepository
import com.twinhearts.app.feature.games.domain.engine.DrawEngine
import com.twinhearts.app.feature.games.domain.engine.DrawGuess
import com.twinhearts.app.feature.games.domain.engine.DrawState
import com.twinhearts.app.feature.games.domain.engine.DrawStroke
import com.twinhearts.app.feature.games.domain.engine.DrawTool
import com.twinhearts.app.feature.games.domain.model.GameId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Draw & Guess. The drawer's finished strokes are saved one by one and the guesser sees them on the
 * next refresh (about every two seconds). A right guess pays the guesser 20 XP and 8 coins.
 */
@HiltViewModel
class DrawViewModel @Inject constructor(
    private val games: GamesRepository,
    json: Json,
) : SyncedGameViewModel<DrawState>(games, json, GameId.Draw, DrawState.serializer()) {

    private val _checking = MutableStateFlow(false)

    /** True while a guess is being checked (the button says "Checking…"). */
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    private val redoStack = ArrayDeque<DrawStroke>()
    private var pendingGuess: DrawGuess? = null
    private var pendingGuessRound = -1

    /** The last round whose result this phone already announced. */
    private var announcedRound = 0

    override fun fresh() = DrawState()

    fun setWord(word: String): Boolean {
        val me = side
        val now = System.currentTimeMillis()
        val ok = mutate { DrawEngine.setWord(it, me, word, now) }
        if (ok) {
            redoStack.clear()
            cue(GameCue.Tap)
        }
        return ok
    }

    fun newWord() {
        val me = side
        if (mutate { DrawEngine.newWord(it, me) }) {
            redoStack.clear()
            cue(GameCue.Tap)
        }
    }

    fun addStroke(stroke: DrawStroke) {
        val me = side
        if (mutate { DrawEngine.addStroke(it, me, stroke) }) {
            redoStack.clear()
            if (stroke.tool == DrawTool.Fill) cue(GameCue.Tap)
        }
    }

    fun undo() {
        val me = side
        val last = state?.strokes?.lastOrNull() ?: return
        if (mutate { DrawEngine.undo(it, me) }) redoStack.addLast(last)
    }

    fun redo() {
        val me = side
        val stroke = redoStack.lastOrNull() ?: return
        if (mutate { DrawEngine.addStroke(it, me, stroke) }) redoStack.removeLastOrNull()
    }

    fun clear() {
        val me = side
        if (mutate { DrawEngine.clear(it, me) }) redoStack.clear()
    }

    fun react(emoji: String) {
        val me = side
        val now = System.currentTimeMillis()
        if (mutate { DrawEngine.react(it, me, emoji, now) }) cue(GameCue.Tap)
    }

    /** Checks the guess (exact, plural, typo, compound, then synonyms) and records it. */
    fun guess(text: String) {
        val clean = text.trim()
        val current = state ?: return
        if (clean.isEmpty() || _checking.value || current.drawer == side || !current.wordSet) return
        _checking.value = true
        val word = current.word
        val round = current.round
        viewModelScope.launch {
            val correct = isCorrect(clean, word)
            _checking.value = false
            // The round may have ended (time ran out) while the check was running.
            if (state?.round != round) return@launch
            val me = side
            val now = System.currentTimeMillis()
            if (!mutate { DrawEngine.guess(it, me, clean, correct, now) }) return@launch
            pendingGuess = DrawGuess(me, clean, correct, now)
            pendingGuessRound = round
            if (correct) {
                announcedRound = round
                cue(GameCue.Success)
                award(
                    xp = GUESS_XP,
                    coins = GUESS_COINS,
                    prize = Prize("🎉", R.string.games_dw_correct, listOf(PrizeLine.Formatted(R.string.games_dw_word_was, word), PrizeLine.Xp(GUESS_XP))),
                )
            } else {
                cue(GameCue.Fail)
                emit(GameEvent.Message(R.string.games_dw_not_quite))
            }
        }
    }

    private suspend fun isCorrect(guess: String, word: String): Boolean {
        if (DrawEngine.localMatch(guess, word)) return true
        return coroutineScope {
            val target = async { games.relatedWords(word) }
            val spoken = async { games.relatedWords(guess) }
            DrawEngine.semanticMatch(guess, word, target.await(), spoken.await())
        }
    }

    /** The drawer's phone ends a round when the 75 seconds are over (the guesser's phone backs it up). */
    suspend fun runClock() {
        while (true) {
            delay(1_000)
            val current = state ?: continue
            if (!current.wordSet) continue
            val now = System.currentTimeMillis()
            val overBy = now - current.roundStartedAt - DrawEngine.ROUND_SECONDS * 1_000L
            val grace = if (side == current.drawer) 0L else BACKUP_MS
            if (overBy < grace) continue
            val round = current.round
            if (mutate { if (it.wordSet && it.round == round) DrawEngine.nextRound(it, now) else null }) {
                emit(GameEvent.Message(R.string.games_dw_times_up))
            }
        }
    }

    override fun onRemoteState(previous: DrawState?, current: DrawState) {
        if (previous == null) {
            announcedRound = current.result?.round ?: 0
            return
        }
        // My guess was lost because the drawer saved at the same moment: send it again.
        val guess = pendingGuess
        if (guess != null) {
            val stillThere = current.guesses.any { it.ts == guess.ts && it.by == guess.by }
            if (current.round == pendingGuessRound && current.wordSet && !stillThere) {
                val me = side
                mutate { DrawEngine.guess(it, me, guess.text, guess.correct, guess.ts) }
            } else if (stillThere || current.round != pendingGuessRound) {
                pendingGuess = null
            }
        }
        // The other phone's right guess ended the round: tell the drawer once.
        val result = current.result
        if (result != null && result.round > announcedRound) {
            announcedRound = result.round
            if (result.by != side) {
                cue(GameCue.Success)
                emit(GameEvent.Message(R.string.games_dw_guessed_it, listOf(partnerName, result.word)))
            }
        }
    }

    private companion object {
        const val GUESS_XP = 20
        const val GUESS_COINS = 8
        const val BACKUP_MS = 5_000L
    }
}
