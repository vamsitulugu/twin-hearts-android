package com.twinhearts.app.feature.games.ui.play

import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.feature.games.data.GamesRepository
import com.twinhearts.app.feature.games.domain.engine.MemoryEngine
import com.twinhearts.app.feature.games.domain.engine.MemoryState
import com.twinhearts.app.feature.games.domain.model.GameId
import com.twinhearts.app.feature.games.domain.model.Outcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Memory Match: +6 XP and +3 coins for every pair you find. The player who flipped the second
 * card checks the pair after a short look, exactly like the web app.
 */
@HiltViewModel
class MemoryViewModel @Inject constructor(
    repository: GamesRepository,
    json: Json,
) : SyncedGameViewModel<MemoryState>(repository, json, GameId.Memory, MemoryState.serializer()) {

    override fun fresh() = MemoryEngine.fresh()

    fun flip(index: Int) {
        val me = side
        if (!mutate { MemoryEngine.flip(it, me, index) }) return
        cue(GameCue.Tap)
        if (state?.let { MemoryEngine.hasPendingPair(it) } == true) {
            val atRevision = revision
            viewModelScope.launch {
                delay(LOOK_MS)
                if (revision == atRevision) resolvePair()
            }
        }
    }

    private fun resolvePair() {
        val me = side
        var matched = false
        if (!mutate { current -> MemoryEngine.resolve(current, me)?.also { matched = it.matched }?.state }) return
        if (matched) {
            cue(GameCue.Success)
            award(xp = PAIR_XP, coins = PAIR_COINS)
        } else {
            cue(GameCue.Fail)
        }
        state?.takeIf { it.outcome != Outcome.None }?.let { announceFinish(it) }
    }

    /** The other phone finished the game: show the result here too, and credit a win. */
    override fun onRemoteState(previous: MemoryState?, current: MemoryState) {
        if (previous != null && previous.outcome == Outcome.None && current.outcome != Outcome.None) {
            announceFinish(current)
        }
    }

    private fun announceFinish(finished: MemoryState) {
        award(
            win = finished.outcome.isWonBy(side),
            prize = Prize(
                "🧠",
                R.string.games_memory_prize_done,
                listOf(PrizeLine.Final(finished.scoreOne, finished.scoreTwo)),
            ),
        )
    }

    fun rematch() {
        if (mutate { MemoryEngine.fresh() }) cue(GameCue.Tap)
    }

    private companion object {
        const val LOOK_MS = 850L
        const val PAIR_XP = 6
        const val PAIR_COINS = 3
    }
}
