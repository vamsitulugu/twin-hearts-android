package com.twinhearts.app.feature.games.ui.play

import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.games.data.GamesRepository
import com.twinhearts.app.feature.games.domain.engine.ChatLine
import com.twinhearts.app.feature.games.domain.engine.GameChat
import com.twinhearts.app.feature.games.domain.engine.PuzzleEngine
import com.twinhearts.app.feature.games.domain.engine.PuzzlePhase
import com.twinhearts.app.feature.games.domain.engine.PuzzleState
import com.twinhearts.app.feature.games.domain.model.GameId
import com.twinhearts.app.feature.games.domain.model.Side
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** What I picked on the setup page. It is only shared with the partner once the puzzle starts. */
data class PuzzleSetup(
    val gridN: Int = 3,
    val photoUrl: String? = null,
    val uploading: Boolean = false,
    /** The custom size field is open. */
    val custom: Boolean = false,
)

/**
 * Picture Puzzle. The photo goes to the server (same upload as chat photos) so both phones can show
 * it. Each person drops only their own pieces; a piece sticks only in its own spot and upright.
 */
@HiltViewModel
class PuzzleViewModel @Inject constructor(
    private val games: GamesRepository,
    json: Json,
) : SyncedGameViewModel<PuzzleState>(games, json, GameId.Puzzle, PuzzleState.serializer()) {

    private val _setup = MutableStateFlow(PuzzleSetup())
    val setup: StateFlow<PuzzleSetup> = _setup.asStateFlow()

    private val _held = MutableStateFlow<Int?>(null)

    /** The piece I tapped and am about to place. */
    val held: StateFlow<Int?> = _held.asStateFlow()

    private var paidRound = -1
    private val placedByMe = mutableSetOf<Int>()
    private val sentChat = ArrayDeque<ChatLine>()

    override fun fresh() = PuzzleState()

    // ── setup ──

    fun pickSize(n: Int) {
        _setup.update { it.copy(gridN = PuzzleEngine.clampGrid(n), custom = n !in PuzzleEngine.gridChoices) }
        cue(GameCue.Tap)
    }

    fun openCustom() {
        _setup.update { it.copy(custom = true, gridN = if (it.gridN in PuzzleEngine.gridChoices) CUSTOM_START else it.gridN) }
        cue(GameCue.Tap)
    }

    fun photoPicked(uri: String) {
        _setup.update { it.copy(uploading = true) }
        emit(GameEvent.Message(R.string.games_pz_uploading))
        viewModelScope.launch {
            when (val result = games.uploadPhoto(uri)) {
                is AppResult.Success -> {
                    _setup.update { it.copy(uploading = false, photoUrl = result.data) }
                    cue(GameCue.Success)
                }
                is AppResult.Failure -> {
                    _setup.update { it.copy(uploading = false) }
                    emit(GameEvent.Message(R.string.games_pz_upload_failed))
                }
            }
        }
    }

    fun start() {
        val chosen = _setup.value
        val url = chosen.photoUrl
        if (url.isNullOrBlank()) {
            emit(GameEvent.Message(R.string.games_pz_photo_first))
            return
        }
        val now = System.currentTimeMillis()
        if (mutate { PuzzleEngine.start(it, chosen.gridN, url, now) }) {
            placedByMe.clear()
            _held.value = null
            cue(GameCue.Tap)
        }
    }

    // ── playing ──

    fun select(pieceId: Int) {
        if (state?.phase != PuzzlePhase.Playing) return
        _held.value = if (_held.value == pieceId) null else pieceId
        cue(GameCue.Tap)
    }

    fun rotate(pieceId: Int) {
        val me = side
        if (mutate { PuzzleEngine.rotate(it, me, pieceId) }) cue(GameCue.Tap)
    }

    /** Drops the piece I am holding on the slot at [row],[col]. */
    fun placeHeld(row: Int, col: Int) {
        val pieceId = _held.value ?: return
        val me = side
        val now = System.currentTimeMillis()
        var correct = false
        if (!mutate { current -> PuzzleEngine.place(current, me, pieceId, row, col, now)?.also { correct = it.correct }?.state }) return
        if (correct) {
            placedByMe.add(pieceId)
            _held.value = null
            cue(GameCue.Success)
            payIfFinished()
        } else {
            cue(GameCue.Fail)
        }
    }

    fun send(text: String) {
        val me = side
        val now = System.currentTimeMillis()
        if (!mutate { PuzzleEngine.chat(it, me, text, now) }) return
        sentChat.addLast(ChatLine(me, text.trim(), now))
        while (sentChat.size > KEEP_SENT) sentChat.removeFirstOrNull()
        cue(GameCue.Tap)
    }

    fun newPuzzle() {
        if (mutate { PuzzleEngine.nextRound(it) }) {
            _setup.update { it.copy(photoUrl = null) }
            placedByMe.clear()
            cue(GameCue.Tap)
        }
    }

    /** "Change photo" keeps the grid size but asks for a new picture. */
    fun changePhoto() {
        _setup.update { it.copy(photoUrl = null) }
        newPuzzle()
    }

    /** After the countdown the first phone to notice starts the clock (Side.One first, Side.Two as backup). */
    suspend fun runClock() {
        while (true) {
            delay(CLOCK_MS)
            val current = state ?: continue
            if (current.phase != PuzzlePhase.Countdown) continue
            val now = System.currentTimeMillis()
            val grace = if (side == Side.One) 0L else GRACE_MS
            if (now >= current.countdownAt + grace) mutate { PuzzleEngine.beginPlaying(it, now) }
        }
    }

    override fun onRemoteState(previous: PuzzleState?, current: PuzzleState) {
        if (previous == null) {
            if (current.phase == PuzzlePhase.Result) paidRound = current.round
            return
        }
        if (current.phase != PuzzlePhase.Playing) {
            placedByMe.clear()
            _held.value = null
        } else {
            val now = System.currentTimeMillis()
            // Pieces I placed that the other phone's save overwrote are put back.
            placedByMe.toList().forEach { id ->
                val piece = current.pieces.firstOrNull { it.id == id }
                if (piece != null && !piece.placed) mutate { PuzzleEngine.restorePlaced(it, id, now) }
            }
            val mine = sentChat.toList()
            if (mine.isNotEmpty() && GameChat.restore(current.chat, mine) != current.chat) {
                mutate { it.copy(chat = GameChat.restore(it.chat, mine)) }
            }
            if (_held.value?.let { id -> current.pieces.firstOrNull { it.id == id }?.placed } == true) _held.value = null
        }
        payIfFinished()
    }

    private fun payIfFinished() {
        val current = state ?: return
        if (current.phase != PuzzlePhase.Result || paidRound == current.round) return
        paidRound = current.round
        val owned = current.pieces.count { it.owner == side }
        val xp = PuzzleEngine.xpFor(owned)
        val coins = PuzzleEngine.coinsFor(owned)
        award(
            xp = xp,
            coins = coins,
            win = true,
            prize = Prize("🧩", R.string.games_pz_prize, listOf(PrizeLine.Xp(xp), PrizeLine.Coins(coins), PrizeLine.Streak(current.teamStreak))),
        )
    }

    private companion object {
        const val CLOCK_MS = 250L
        const val GRACE_MS = 3_000L
        const val CUSTOM_START = 7
        const val KEEP_SENT = 5
    }
}
