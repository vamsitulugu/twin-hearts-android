package com.twinhearts.app.feature.games.ui.play

import com.twinhearts.app.R
import com.twinhearts.app.feature.games.data.GamesRepository
import com.twinhearts.app.feature.games.domain.engine.BombDifficulty
import com.twinhearts.app.feature.games.domain.engine.BombEngine
import com.twinhearts.app.feature.games.domain.engine.BombPhase
import com.twinhearts.app.feature.games.domain.engine.BombResult
import com.twinhearts.app.feature.games.domain.engine.BombState
import com.twinhearts.app.feature.games.domain.engine.ChatLine
import com.twinhearts.app.feature.games.domain.engine.GameChat
import com.twinhearts.app.feature.games.domain.model.GameId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Don't Touch The Bomb. The mover's phone starts and stops the clock (the guide's phone backs it
 * up a few seconds later), and the guide's phone moves the bombs in Extreme mode.
 * The mover earns the full reward for a win, the guide 65%.
 */
@HiltViewModel
class BombViewModel @Inject constructor(
    repository: GamesRepository,
    json: Json,
) : SyncedGameViewModel<BombState>(repository, json, GameId.Bomb, BombState.serializer()) {

    override fun fresh() = BombState()

    private var paidRound = -1
    private var lastRelocateAt = 0L
    private val sentChat = ArrayDeque<ChatLine>()

    fun start(difficulty: BombDifficulty, hint: Boolean, fog: Boolean) {
        val now = System.currentTimeMillis()
        if (mutate { BombEngine.start(it, difficulty, hint, fog, now) }) {
            lastRelocateAt = 0L
            cue(GameCue.Tap)
        }
    }

    /** The mover steps onto a tile. */
    fun move(idx: Int) {
        val me = side
        val now = System.currentTimeMillis()
        if (!mutate { BombEngine.move(it, me, idx, now) }) return
        if (state?.phase == BombPhase.Result) payIfFinished() else cue(GameCue.Tap)
    }

    /** The guide points at a tile; both phones show a 📍 on it. */
    fun ping(idx: Int) {
        val me = side
        val nonce = System.currentTimeMillis()
        if (mutate { BombEngine.ping(it, me, idx, nonce) }) cue(GameCue.Tap)
    }

    fun send(text: String) {
        val me = side
        val now = System.currentTimeMillis()
        if (!mutate { BombEngine.chat(it, me, text, now) }) return
        keepSent(ChatLine(me, text.trim(), now))
        cue(GameCue.Tap)
    }

    fun nextRound() {
        if (mutate { BombEngine.nextRound(it) }) cue(GameCue.Tap)
    }

    fun changeDifficulty() {
        if (mutate { BombEngine.reset() }) cue(GameCue.Tap)
    }

    /** Countdown, round timer and (Extreme) moving bombs. */
    suspend fun runClock() {
        while (true) {
            delay(CLOCK_MS)
            val current = state ?: continue
            val now = System.currentTimeMillis()
            val isMover = side == current.mover
            when (current.phase) {
                BombPhase.Countdown -> {
                    val grace = if (isMover) 0L else GRACE_MS
                    if (now >= current.countdownAt + grace) mutate { BombEngine.beginPlaying(it, now) }
                }
                BombPhase.Playing -> {
                    val grace = if (isMover) 0L else GRACE_MS
                    if (now - current.startedAt >= current.timeLimit * 1_000L + grace) {
                        mutate { BombEngine.timeout(it) }
                        payIfFinished()
                    } else if (current.difficulty.moving && side == current.guide) {
                        if (lastRelocateAt == 0L) lastRelocateAt = now
                        if (now - lastRelocateAt >= BombEngine.RELOCATE_MS) {
                            lastRelocateAt = now
                            mutate { BombEngine.relocateBomb(it) }
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    override fun onRemoteState(previous: BombState?, current: BombState) {
        if (previous == null) {
            if (current.phase == BombPhase.Result) paidRound = current.round
            return
        }
        // Chat lines of mine that the other phone's save overwrote are put back.
        if (sentChat.isNotEmpty() && GameChat.restore(current.chat, sentChat.toList()) != current.chat) {
            val mine = sentChat.toList()
            mutate { it.copy(chat = GameChat.restore(it.chat, mine)) }
        }
        if (current.phase != BombPhase.Result && current.phase != BombPhase.Playing) sentChat.clear()
        payIfFinished()
    }

    private fun keepSent(line: ChatLine) {
        sentChat.addLast(line)
        while (sentChat.size > KEEP_SENT) sentChat.removeFirstOrNull()
    }

    private fun payIfFinished() {
        val current = state ?: return
        if (current.phase != BombPhase.Result || paidRound == current.round) return
        paidRound = current.round
        if (current.result != BombResult.Won) {
            cue(GameCue.Fail)
            return
        }
        val cfg = current.difficulty
        val mover = side == current.mover
        val xp = if (mover) cfg.xp else Math.round(cfg.xp * GUIDE_SHARE)
        val coins = if (mover) cfg.coins else Math.round(cfg.coins * GUIDE_SHARE)
        cue(GameCue.Success)
        award(
            xp = xp,
            coins = coins,
            win = true,
            prize = Prize("💚", R.string.games_bm_prize_won, listOf(PrizeLine.Xp(xp), PrizeLine.Coins(coins), PrizeLine.Streak(current.teamStreak))),
        )
    }

    private companion object {
        const val CLOCK_MS = 250L
        const val GRACE_MS = 4_000L
        const val GUIDE_SHARE = 0.65f
        const val KEEP_SENT = 5
    }
}
