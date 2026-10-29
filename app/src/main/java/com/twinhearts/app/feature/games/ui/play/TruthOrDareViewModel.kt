package com.twinhearts.app.feature.games.ui.play

import com.twinhearts.app.R
import com.twinhearts.app.feature.games.data.GamesRepository
import com.twinhearts.app.feature.games.domain.engine.TodPhase
import com.twinhearts.app.feature.games.domain.engine.TodState
import com.twinhearts.app.feature.games.domain.engine.TodType
import com.twinhearts.app.feature.games.domain.engine.TruthOrDareEngine
import com.twinhearts.app.feature.games.domain.model.GameId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.random.Random

/**
 * Truth or Dare. Answering earns +10 XP and +4 coins for both of you; skipping costs the
 * skipper 5 XP and gives a random penalty (same numbers as the web app).
 */
@HiltViewModel
class TruthOrDareViewModel @Inject constructor(
    repository: GamesRepository,
    json: Json,
) : SyncedGameViewModel<TodState>(repository, json, GameId.TruthOrDare, TodState.serializer()) {

    private var clockRevision = -1L
    private var clockStartNanos = System.nanoTime()

    override fun fresh() = TodState()

    fun setCategory(category: String) {
        if (mutate { TruthOrDareEngine.setCategory(it, category).takeIf { next -> next != it } }) cue(GameCue.Tap)
    }

    fun spin() {
        val me = side
        if (mutate { TruthOrDareEngine.startSpin(it, me) }) cue(GameCue.Tap)
    }

    /** The bottle stopped turning (called by the person who spun it). */
    fun spinFinished() {
        if (state?.turn != side) return
        mutate { TruthOrDareEngine.finishSpin(it) }
    }

    fun choose(type: TodType) {
        val me = side
        if (mutate { TruthOrDareEngine.chooseType(it, me, type) }) cue(GameCue.Tap)
    }

    fun send(text: String) {
        val me = side
        if (mutate { TruthOrDareEngine.send(it, me, text) }) cue(GameCue.Success)
    }

    fun answer(text: String) {
        val me = side
        if (!mutate { TruthOrDareEngine.answer(it, me, text) }) return
        finishSoon()
    }

    fun dareDone(doneLabel: String) {
        val me = side
        if (!mutate { TruthOrDareEngine.dareDone(it, me, doneLabel) }) return
        finishSoon()
    }

    /** Skip. [skippedLabel] is the word saved in history. */
    fun skip(skippedLabel: String) {
        if (!mutate { TruthOrDareEngine.skip(it, skippedLabel) }) return
        cue(GameCue.Fail)
        award(
            xp = TruthOrDareEngine.SKIP_XP,
            prize = Prize(
                "😈",
                R.string.games_tod_prize_penalty,
                listOf(PrizeLine.Penalty(Random.nextInt(PENALTY_COUNT)), PrizeLine.Xp(TruthOrDareEngine.SKIP_XP)),
            ),
        )
    }

    fun react(emoji: String) {
        val me = side
        mutate { TruthOrDareEngine.react(it, me, emoji, System.currentTimeMillis()) }
    }

    /** The answerer shows the answer for a moment, then closes the card. */
    private fun finishSoon() {
        cue(GameCue.Success)
        afterDelay(RESULT_MS) {
            if (mutate { TruthOrDareEngine.complete(it) }) {
                award(xp = DONE_XP, coins = DONE_COINS, prize = donePrize())
            }
        }
    }

    /** The asker's phone sees the card close and gets the same reward. */
    override fun onRemoteState(previous: TodState?, current: TodState) {
        if (previous == null) return
        val closed = previous.current?.answer != null && current.current == null
        val answered = current.history.size > previous.history.size && current.streak > previous.streak
        if (closed && answered) {
            award(xp = DONE_XP, coins = DONE_COINS, prize = donePrize())
        }
    }

    private fun donePrize() = Prize("💞", R.string.games_tod_prize_done, listOf(PrizeLine.Xp(DONE_XP), PrizeLine.Coins(DONE_COINS)))

    /** Auto-skips when the person answering takes longer than 45 seconds. */
    suspend fun runAnswerClock(skippedLabel: String) {
        while (true) {
            val rev = revision
            if (rev != clockRevision) {
                clockRevision = rev
                clockStartNanos = System.nanoTime()
            }
            val current = state
            val waiting = current != null && current.phase == TodPhase.Reveal &&
                current.current?.answer == null && TruthOrDareEngine.answererOf(current) == side
            val elapsed = (System.nanoTime() - clockStartNanos) / 1_000_000_000L
            if (waiting && elapsed >= TruthOrDareEngine.ANSWER_SECONDS) skip(skippedLabel)
            delay(1_000)
        }
    }

    private companion object {
        const val RESULT_MS = 2_600L
        const val DONE_XP = 10
        const val DONE_COINS = 4
        const val PENALTY_COUNT = 4
    }
}
