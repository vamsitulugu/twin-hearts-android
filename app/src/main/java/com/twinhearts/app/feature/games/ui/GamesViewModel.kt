package com.twinhearts.app.feature.games.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.games.data.GamesRepository
import com.twinhearts.app.feature.games.domain.model.GameId
import com.twinhearts.app.feature.games.domain.model.Players
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** The Games hub: both players' progress, who is playing what, and my favourites. */
@HiltViewModel
class GamesViewModel @Inject constructor(
    private val repository: GamesRepository,
    private val accessGate: com.twinhearts.app.feature.access.domain.AccessGate,
) : ViewModel() {

    private var gateOkAt = 0L

    /** Free games each day, then ONE small coin pass. Opening another game within 10 minutes is not counted again. */
    fun tryOpen(onAllowed: () -> Unit) {
        viewModelScope.launch {
            val recent = System.currentTimeMillis() - gateOkAt < 600_000L
            if (!recent) {
                if (!accessGate.check(com.twinhearts.app.feature.access.domain.AccessFeature.Games)) return@launch
                gateOkAt = System.currentTimeMillis()
            }
            onAllowed()
        }
    }

    private val _ui = MutableStateFlow<UiState<Players>>(UiState.Loading)
    val ui: StateFlow<UiState<Players>> = _ui.asStateFlow()

    /** Keeps the hub fresh while it is on screen. Cancel it to stop all network work. */
    suspend fun run() {
        try {
            coroutineScope {
                launch { repository.loadAvatars(); refreshNow() }
                launch {
                    while (isActive) {
                        repository.setPresence(null)
                        delay(HEARTBEAT_MS)
                    }
                }
                while (isActive) {
                    refreshNow()
                    delay(POLL_MS)
                }
            }
        } finally {
            withContext(NonCancellable) { repository.setPresence(null) }
        }
    }

    fun retry() {
        _ui.value = UiState.Loading
        viewModelScope.launch { refreshNow() }
    }

    private suspend fun refreshNow() {
        when (val result = repository.snapshot(null)) {
            is AppResult.Success -> _ui.value = UiState.Content(result.data.players)
            is AppResult.Failure -> if (_ui.value !is UiState.Content) _ui.value = UiState.Error(result.error)
        }
    }

    fun toggleFavorite(game: GameId) = updateMine { it.toggleFavorite(game) }

    /** Remembers the game was opened, so its button says "Continue" next time. */
    fun markOpened(game: GameId) = updateMine { it.withLastGame(game, System.currentTimeMillis()) }

    /** Sends my partner an invitation to play [game]. */
    fun sendRequest(game: GameId) {
        viewModelScope.launch {
            val result = repository.sendRequest(game)
            if (result is AppResult.Success) refreshNow()
        }
    }

    /** Accepts the pending invitation and opens that game. */
    fun acceptRequest() {
        val current = (_ui.value as? UiState.Content)?.data
        val request = current?.pendingRequest ?: return
        viewModelScope.launch {
            val cleared = repository.clearRequest()
            if (cleared is AppResult.Success) {
                refreshNow()
                GameId.fromKey(request.game)?.let { markOpened(it) }
            }
        }
    }

    /** Declines (cancels) the pending invitation. */
    fun declineRequest() {
        viewModelScope.launch {
            val cleared = repository.clearRequest()
            if (cleared is AppResult.Success) refreshNow()
        }
    }

    private fun updateMine(change: (com.twinhearts.app.feature.games.domain.model.PlayerProfile) -> com.twinhearts.app.feature.games.domain.model.PlayerProfile) {
        viewModelScope.launch {
            val result = repository.updateMyProfile(change)
            val current = (_ui.value as? UiState.Content)?.data
            if (result is AppResult.Success && current != null) {
                _ui.value = UiState.Content(current.copy(me = result.data))
            }
        }
    }

    private companion object {
        const val POLL_MS = 6_000L
        const val HEARTBEAT_MS = 10_000L
    }
}
