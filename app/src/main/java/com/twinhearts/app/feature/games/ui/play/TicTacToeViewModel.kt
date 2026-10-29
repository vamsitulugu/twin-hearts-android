package com.twinhearts.app.feature.games.ui.play

import com.twinhearts.app.R
import com.twinhearts.app.feature.games.data.GamesRepository
import com.twinhearts.app.feature.games.domain.engine.TicTacToeEngine
import com.twinhearts.app.feature.games.domain.engine.TicTacToeState
import com.twinhearts.app.feature.games.domain.model.GameId
import com.twinhearts.app.feature.games.domain.model.Outcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Tic Tac Toe: +8 XP and +5 coins for each round you win (same as the web app). */
@HiltViewModel
class TicTacToeViewModel @Inject constructor(
    repository: GamesRepository,
    json: Json,
) : SyncedGameViewModel<TicTacToeState>(repository, json, GameId.TicTacToe, TicTacToeState.serializer()) {

    override fun fresh() = TicTacToeState()

    fun play(index: Int) {
        val me = side
        val accepted = mutate { TicTacToeEngine.play(it, me, index) }
        if (!accepted) return
        when (state?.outcome) {
            Outcome.None, null -> cue(GameCue.Tap)
            Outcome.Draw -> cue(GameCue.Tap)
            else -> {
                cue(GameCue.Success)
                award(
                    xp = WIN_XP,
                    coins = WIN_COINS,
                    win = true,
                    prize = Prize("🏆", R.string.games_ttt_prize_won, listOf(PrizeLine.Xp(WIN_XP), PrizeLine.Coins(WIN_COINS))),
                )
            }
        }
    }

    fun rematch() {
        if (mutate { TicTacToeEngine.rematch(it) }) cue(GameCue.Tap)
    }

    private companion object {
        const val WIN_XP = 8
        const val WIN_COINS = 5
    }
}
