package com.twinhearts.app.feature.home.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.feature.home.data.HomeRepository
import com.twinhearts.app.feature.home.domain.model.LoveCardState
import com.twinhearts.app.feature.home.domain.model.LoveKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Short message under the Stay Connected card: green for news, red for problems. */
sealed interface HomeMessage {
    data class Info(@StringRes val id: Int, val arg: String? = null) : HomeMessage
    data class Failure(val error: AppError) : HomeMessage
}

@Composable
fun HomeMessage.asText(): String = when (this) {
    is HomeMessage.Info -> if (arg == null) stringResource(id) else stringResource(id, arg)
    is HomeMessage.Failure -> error.asText()
}

data class HomeUiState(
    val session: Session? = null,
    val loveCard: LoveCardState = LoveCardState(),
    val isSending: Boolean = false,
    val message: HomeMessage? = null,
    /** A hug the user tapped "Later" on, so it is not shown again until a new one arrives. */
    val dismissedHugId: Long? = null,
)

/**
 * Logic of the Home screen. Hug / Touch / Miss You are saved into the couple's shared state on the
 * server, the same place the web app uses, so both apps see each other's signals.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.session.collect { session -> _state.update { it.copy(session = session) } }
        }
        refresh()
    }

    /** Silent refresh: a failed check never replaces what is already on screen. */
    fun refresh() {
        viewModelScope.launch {
            (repository.loveCard() as? AppResult.Success)?.let { result ->
                _state.update { it.copy(loveCard = result.data) }
            }
        }
    }

    fun send(kind: LoveKind) {
        val current = _state.value
        val session = current.session ?: return
        if (current.isSending || !session.paired) return
        _state.update { it.copy(isSending = true, message = null) }
        viewModelScope.launch {
            when (val result = repository.send(kind)) {
                is AppResult.Success -> {
                    val info = when (kind) {
                        LoveKind.Touch -> R.string.home_info_touch_sent
                        LoveKind.MissYou -> R.string.home_info_miss_sent
                        LoveKind.Hug -> R.string.home_info_hug_sent
                    }
                    _state.update { it.copy(isSending = false, message = HomeMessage.Info(info, session.partnerName)) }
                    refresh()
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isSending = false, message = HomeMessage.Failure(result.error))
                }
            }
        }
    }

    fun acceptHug() {
        val current = _state.value
        if (current.isSending) return
        _state.update { it.copy(isSending = true, message = null) }
        viewModelScope.launch {
            when (val result = repository.acceptHug()) {
                is AppResult.Success -> {
                    val partner = current.session?.partnerName
                    _state.update { it.copy(isSending = false, message = HomeMessage.Info(R.string.home_info_hug_accepted, partner)) }
                    refresh()
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isSending = false, message = HomeMessage.Failure(result.error))
                }
            }
        }
    }

    fun dismissHug() = _state.update { it.copy(dismissedHugId = it.loveCard.pendingIncomingHugId) }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
