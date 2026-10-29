package com.twinhearts.app.feature.games.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.games.data.GamesRepository
import com.twinhearts.app.feature.games.domain.model.GameEnvelope
import com.twinhearts.app.feature.games.domain.model.GameId
import com.twinhearts.app.feature.games.domain.model.PlayerProfile
import com.twinhearts.app.feature.games.domain.model.Players
import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.time.LocalDate

/** What a game screen draws: the game's state plus both players. */
data class GameContent<S>(val state: S, val players: Players)

/**
 * The shared engine behind every two-phone game.
 *
 * How it stays in sync (the old app used Supabase realtime, which the Android app may not call,
 * so this follows the same "poll while open" idea the rest of the app uses, R-66):
 * - the whole game lives in ONE saved document, wrapped in a [GameEnvelope] with a revision number;
 * - every move bumps the revision and saves the document;
 * - while the screen is on, [run] reads the document every couple of seconds and takes the
 *   other phone's version when it is newer.
 * Polling stops when the screen leaves the foreground ([run] is cancelled).
 */
abstract class SyncedGameViewModel<S : Any>(
    private val repository: GamesRepository,
    private val json: Json,
    val game: GameId,
    serializer: KSerializer<S>,
) : ViewModel() {

    private val envelopeSerializer = GameEnvelope.serializer(serializer)

    private val _ui = MutableStateFlow<UiState<GameContent<S>>>(UiState.Loading)
    val ui: StateFlow<UiState<GameContent<S>>> = _ui.asStateFlow()

    private val _events = Channel<GameEvent>(Channel.BUFFERED)
    val events: Flow<GameEvent> = _events.receiveAsFlow()

    private var envelope: GameEnvelope<S>? = null
    private var players: Players? = null
    private var dirty = false
    private var offlineShown = false
    private val saveLock = Mutex()

    /** A brand-new game, used when nothing has been saved yet. */
    protected abstract fun fresh(): S

    /** Called after the other phone's newer state was taken (timers can restart here). */
    protected open fun onRemoteState(previous: S?, current: S) = Unit

    /** The current game state, or null before the first load. */
    protected val state: S? get() = envelope?.data

    protected val side: Side get() = players?.side ?: Side.One

    protected val partnerName: String get() = players?.partnerName.orEmpty()

    /** Changes with every saved move; effects use it to check "is this still the same position?". */
    protected val revision: Long get() = envelope?.rev ?: 0L

    /**
     * Keeps this game alive: first load, then polling, plus a "I am playing this" heartbeat.
     * Call it from a lifecycle-aware effect; cancelling it stops all network work.
     */
    suspend fun run() {
        try {
            coroutineScope {
                launch { repository.loadAvatars(); publish() }
                launch {
                    while (isActive) {
                        repository.setPresence(game)
                        delay(HEARTBEAT_MS)
                    }
                }
                while (isActive) {
                    pollOnce()
                    delay(POLL_MS)
                }
            }
        } finally {
            withContext(NonCancellable) { repository.setPresence(null) }
        }
    }

    /** Pull-to-retry / the Retry button. */
    fun retry() {
        viewModelScope.launch {
            if (envelope == null) _ui.value = UiState.Loading
            pollOnce()
        }
    }

    private suspend fun pollOnce() {
        when (val result = repository.snapshot(game)) {
            is AppResult.Failure -> {
                if (envelope == null) {
                    _ui.value = UiState.Error(result.error)
                } else {
                    notifyOffline()
                }
            }
            is AppResult.Success -> {
                offlineShown = false
                players = result.data.players
                adopt(result.data.doc)
                if (dirty) flush()
                publish()
            }
        }
    }

    private fun adopt(doc: JsonElement?) {
        val remote = doc?.let { decode(it) }
        val local = envelope
        when {
            remote == null && local == null -> {
                // First phone to open this game starts it.
                envelope = GameEnvelope(rev = 1, by = side, data = fresh())
                dirty = true
            }
            remote == null -> Unit
            local == null -> takeRemote(remote)
            dirty -> Unit // my own newer move is still on its way to the server
            remote.rev > local.rev || (remote.rev == local.rev && remote.by != local.by) -> takeRemote(remote)
        }
    }

    private fun takeRemote(remote: GameEnvelope<S>) {
        val previous = envelope?.data
        envelope = remote
        onRemoteState(previous, remote.data)
    }

    private fun decode(doc: JsonElement): GameEnvelope<S>? =
        runCatching { json.decodeFromJsonElement(envelopeSerializer, doc) }.getOrNull()

    private fun publish() {
        val current = envelope
        val people = players
        if (current != null && people != null) {
            _ui.value = UiState.Content(GameContent(current.data, people))
        }
    }

    /**
     * Applies one move. [block] returns the new state, or null when the move is not allowed
     * (then nothing changes). Returns true when the move was accepted.
     */
    protected fun mutate(block: (S) -> S?): Boolean {
        val current = envelope ?: return false
        val next = block(current.data) ?: return false
        envelope = GameEnvelope(rev = current.rev + 1, by = side, data = next)
        dirty = true
        publish()
        viewModelScope.launch { flush() }
        return true
    }

    private suspend fun flush() {
        saveLock.withLock {
            val current = envelope ?: return
            if (!dirty) return
            val element = json.encodeToJsonElement(envelopeSerializer, current)
            when (repository.saveDoc(game, element)) {
                is AppResult.Success -> if (envelope?.rev == current.rev) dirty = false
                is AppResult.Failure -> notifyOffline()
            }
        }
    }

    private fun notifyOffline() {
        if (offlineShown) return
        offlineShown = true
        emit(GameEvent.Message(R.string.games_sync_problem))
    }

    protected fun emit(event: GameEvent) {
        _events.trySend(event)
    }

    protected fun cue(cue: GameCue) = emit(GameEvent.Cue(cue))

    /** Runs [action] after [millis], but only if nobody moved in between (same revision). */
    protected fun afterDelay(millis: Long, action: () -> Unit) {
        val startRevision = revision
        viewModelScope.launch {
            delay(millis)
            if (revision == startRevision) action()
        }
    }

    /** Gives the current player XP / coins / a win, and announces a level-up. */
    protected fun award(
        xp: Int = 0,
        coins: Int = 0,
        win: Boolean = false,
        prize: Prize? = null,
    ) {
        viewModelScope.launch {
            val levelBefore = players?.me?.level ?: 1
            val result = repository.updateMyProfile { profile -> applyReward(profile, xp, coins, win) }
            if (result is AppResult.Success) {
                players = players?.copy(me = result.data)
                publish()
                if (coins > 0) cue(GameCue.Coin)
            }
            prize?.let { emit(GameEvent.ShowPrize(it)) }
            if (result is AppResult.Success && result.data.level > levelBefore) {
                emit(GameEvent.ShowPrize(Prize("🏆", R.string.games_prize_level_up, listOf(PrizeLine.Level(result.data.level)))))
                cue(GameCue.LevelUp)
            }
        }
    }

    private fun applyReward(profile: PlayerProfile, xp: Int, coins: Int, win: Boolean): PlayerProfile {
        var next = profile
        if (xp != 0) next = next.withXp(xp, LocalDate.now())
        if (coins != 0) next = next.withCoins(coins)
        if (win) next = next.withWin()
        return next
    }

    private companion object {
        const val POLL_MS = 2_000L
        const val HEARTBEAT_MS = 10_000L
    }
}
