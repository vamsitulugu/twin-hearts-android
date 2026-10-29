package com.twinhearts.app.feature.games.ui.play

import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.feature.games.data.GamesRepository
import com.twinhearts.app.feature.games.domain.engine.LudoEngine
import com.twinhearts.app.feature.games.domain.engine.LudoState
import com.twinhearts.app.feature.games.domain.model.GameId
import com.twinhearts.app.feature.games.domain.model.Outcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.random.Random

/** Things only this phone cares about: the dice animation and the turn clock. */
data class LudoLocal(val rolling: Boolean = false, val secondsLeft: Int = LudoEngine.TURN_SECONDS)

/**
 * Ludo. Rewards like the web app: +5 coins for each capture, +15 XP and +10 coins for winning.
 * The 30 second turn clock restarts whenever a new position is seen, on both phones, so the two
 * phones' clocks never have to agree with each other.
 */
@HiltViewModel
class LudoViewModel @Inject constructor(
    repository: GamesRepository,
    json: Json,
) : SyncedGameViewModel<LudoState>(repository, json, GameId.Ludo, LudoState.serializer()) {

    private val _local = MutableStateFlow(LudoLocal())
    val local: StateFlow<LudoLocal> = _local.asStateFlow()

    private var clockRevision = -1L
    private var clockStartNanos = System.nanoTime()

    override fun fresh() = LudoEngine.rematch()

    fun roll() {
        val me = side
        val current = state ?: return
        if (current.turn != me || current.dice != 0 || current.outcome != Outcome.None || _local.value.rolling) return
        cue(GameCue.Dice)
        _local.value = _local.value.copy(rolling = true)
        viewModelScope.launch {
            delay(ROLL_MS)
            _local.value = _local.value.copy(rolling = false)
            val dice = Random.nextInt(1, 7)
            if (mutate { LudoEngine.roll(it, me, dice) } && state?.noMove == true) {
                afterDelay(NO_MOVE_MS) { mutate { LudoEngine.passAfterNoMove(it, me) } }
            }
        }
    }

    fun moveToken(token: Int) {
        val me = side
        var outcome: com.twinhearts.app.feature.games.domain.engine.LudoMove? = null
        if (!mutate { s -> LudoEngine.move(s, me, token)?.also { outcome = it }?.state }) return
        val move = outcome ?: return
        cue(GameCue.Place)
        if (move.captured.isNotEmpty()) {
            cue(GameCue.Success)
            emit(GameEvent.Message(R.string.games_ludo_captured, listOf(partnerName)))
            award(coins = CAPTURE_COINS * move.captured.size)
        }
        if (move.won) {
            award(
                xp = WIN_XP,
                coins = WIN_COINS,
                win = true,
                prize = Prize("🏆", R.string.games_ludo_prize_won, listOf(PrizeLine.Xp(WIN_XP), PrizeLine.Coins(WIN_COINS))),
            )
        }
    }

    fun rematch() {
        if (mutate { LudoEngine.rematch() }) cue(GameCue.Tap)
    }

    /** Runs the visible 30 second clock. Keep it alive next to [run] while the screen is on. */
    suspend fun runClock() {
        while (true) {
            val rev = revision
            if (rev != clockRevision) {
                clockRevision = rev
                clockStartNanos = System.nanoTime()
            }
            val current = state
            val elapsed = ((System.nanoTime() - clockStartNanos) / 1_000_000_000L).toInt()
            val left = (LudoEngine.TURN_SECONDS - elapsed).coerceAtLeast(0)
            _local.value = _local.value.copy(secondsLeft = left)
            if (left == 0 && current != null && current.outcome == Outcome.None && current.turn == side) {
                val me = side
                if (mutate { LudoEngine.timeOut(it, me) }) {
                    emit(GameEvent.Message(R.string.games_ludo_time_up))
                }
            }
            delay(500)
        }
    }

    private companion object {
        const val ROLL_MS = 600L
        const val NO_MOVE_MS = 1_000L
        const val CAPTURE_COINS = 5
        const val WIN_XP = 15
        const val WIN_COINS = 10
    }
}
