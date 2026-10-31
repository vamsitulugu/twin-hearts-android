package com.twinhearts.app.feature.games.ui.play

import com.twinhearts.app.R
import com.twinhearts.app.feature.games.data.GamesRepository
import com.twinhearts.app.feature.games.domain.engine.MindReaderEngine
import com.twinhearts.app.feature.games.domain.engine.MindReaderState
import com.twinhearts.app.feature.games.domain.engine.MrMatch
import com.twinhearts.app.feature.games.domain.engine.MrMode
import com.twinhearts.app.feature.games.domain.engine.MrPhase
import com.twinhearts.app.feature.games.domain.model.GameId
import com.twinhearts.app.feature.games.domain.model.Side
import com.twinhearts.app.feature.games.ui.components.MindReaderCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Mind Reader: both answer in secret, then flip together. Each person earns XP and coins for every
 * revealed round (more for a closer match), exactly like the web app.
 */
@HiltViewModel
class MindReaderViewModel @Inject constructor(
    repository: GamesRepository,
    json: Json,
) : SyncedGameViewModel<MindReaderState>(repository, json, GameId.MindReader, MindReaderState.serializer()) {

    override fun fresh() = MindReaderState()

    /** My locked answer, kept until the saved game confirms it (two phones may save at once). */
    private var pendingRound = -1
    private var pendingAnswer = ""

    /** `totalRounds` of the last round I was paid for, so a round is never paid twice. */
    private var paidRounds = -1

    fun start(mode: MrMode, category: String) {
        val now = System.currentTimeMillis()
        val pool = MindReaderCatalog.pool(if (mode == MrMode.Category) category else MindReaderEngine.RANDOM)
        if (mutate { MindReaderEngine.start(it, mode, category, pool, now) }) cue(GameCue.Tap)
    }

    fun startDaily() {
        start(MrMode.BestOfTen, MindReaderEngine.RANDOM)
        emit(GameEvent.Message(R.string.games_mr_daily_started))
    }

    fun startWeekly() {
        start(MrMode.Endless, MindReaderEngine.RANDOM)
        emit(GameEvent.Message(R.string.games_mr_weekly_started))
    }

    /** Locks my answer. When it is the second lock the answers flip and the round is scored. */
    fun lock(answer: String) {
        val me = side
        val round = state?.roundIndex ?: return
        if (!mutate { MindReaderEngine.lock(it, me, answer, isChoice(it)) }) return
        pendingRound = round
        pendingAnswer = answer.trim()
        cue(GameCue.Tap)
        payIfRevealed()
    }

    fun next() {
        val now = System.currentTimeMillis()
        if (mutate { MindReaderEngine.next(it, poolOf(it), now) }) cue(GameCue.Tap)
    }

    fun endMatch() {
        if (mutate { MindReaderEngine.endMatch(it) }) cue(GameCue.Tap)
    }

    fun backToLobby() {
        if (mutate { MindReaderEngine.backToLobby(it) }) cue(GameCue.Tap)
    }

    fun toggleFavorite() {
        if (mutate { MindReaderEngine.toggleFavorite(it) }) cue(GameCue.Tap)
    }

    /** After the 3-2-1, the first phone to notice opens the answers; the other one waits a little. */
    suspend fun runClock() {
        while (true) {
            delay(CLOCK_MS)
            val current = state ?: continue
            if (current.phase != MrPhase.Countdown) continue
            val now = System.currentTimeMillis()
            val grace = if (side == Side.One) 0L else GRACE_MS
            if (now >= current.countdownAt + grace) mutate { MindReaderEngine.startAnswering(it, now) }
        }
    }

    override fun onRemoteState(previous: MindReaderState?, current: MindReaderState) {
        if (previous == null) {
            // Joined in the middle of a game: a round that is already revealed was paid before.
            if (current.phase == MrPhase.Reveal) paidRounds = current.totalRounds
            return
        }
        if (pendingRound == current.roundIndex && current.phase == MrPhase.Answering && !current.lockedOf(side)) {
            // The other phone saved at the same moment and my lock was lost: lock again.
            val me = side
            val answer = pendingAnswer
            mutate { MindReaderEngine.lock(it, me, answer, isChoice(it)) }
        } else if (pendingRound != current.roundIndex || current.lockedOf(side)) {
            pendingRound = -1
        }
        payIfRevealed()
    }

    private fun payIfRevealed() {
        val current = state ?: return
        val match = current.match ?: return
        if (current.phase != MrPhase.Reveal || paidRounds == current.totalRounds) return
        paidRounds = current.totalRounds
        award(xp = MindReaderEngine.xp(match), coins = MindReaderEngine.coins(match))
        cue(
            when (match) {
                MrMatch.Exact, MrMatch.Near -> GameCue.Success
                MrMatch.Funny -> GameCue.Tap
                MrMatch.Different -> GameCue.Fail
            },
        )
    }

    private fun isChoice(s: MindReaderState): Boolean = MindReaderCatalog.of(s.question)?.isChoice == true

    private fun poolOf(s: MindReaderState) =
        MindReaderCatalog.pool(if (s.mode == MrMode.Category) s.category else MindReaderEngine.RANDOM)

    private companion object {
        const val CLOCK_MS = 250L
        const val GRACE_MS = 3_000L
    }
}
