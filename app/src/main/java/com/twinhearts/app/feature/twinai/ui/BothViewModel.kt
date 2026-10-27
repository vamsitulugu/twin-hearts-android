package com.twinhearts.app.feature.twinai.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.twinai.data.TwinAiRepository
import com.twinhearts.app.feature.twinai.domain.model.AiIdentity
import com.twinhearts.app.feature.twinai.domain.model.BothDiscussion
import com.twinhearts.app.feature.twinai.domain.model.BothRound
import com.twinhearts.app.feature.twinai.domain.model.BothSession
import com.twinhearts.app.feature.twinai.domain.model.RoundStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The "Recent discussions" list in its four states (R-41). */
sealed interface BothHistory {
    data object Loading : BothHistory
    data object Empty : BothHistory
    data class Content(val sessions: List<BothSession>) : BothHistory
    data class Error(val error: AppError) : BothHistory
}

data class BothUiState(
    val partnerName: String = "",
    val history: BothHistory = BothHistory.Loading,
    /** Set while a discussion is open; null while the BOTH home page shows. */
    val open: BothDiscussion? = null,
    val starting: Boolean = false,
    val opening: Boolean = false,
    val draft: String = "",
    val submitting: Boolean = false,
    val editing: Boolean = false,
    val saving: Boolean = false,
    val reminded: Boolean = false,
    val deleteTarget: BothSession? = null,
    val deleting: Boolean = false,
)

/**
 * Why this file exists: runs the BOTH tab. The web page listens for live events; this version asks
 * the server for the round every 3 seconds, but only while the tab is on screen and a round is
 * still waiting (R-66), so nothing runs in the background.
 */
@HiltViewModel
class BothViewModel @Inject constructor(
    private val repository: TwinAiRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BothUiState())
    val state: StateFlow<BothUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    /** One-off texts (string ids) the screen shows as a short toast. */
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    private var identity: AiIdentity? = null
    private var visible = false
    private var pollJob: Job? = null

    /** The screen calls this when the BOTH tab appears or disappears (or the app goes to the background). */
    fun setVisible(isVisible: Boolean) {
        visible = isVisible
        if (isVisible) {
            if (_state.value.open == null && _state.value.history is BothHistory.Loading) loadHistory()
            ensurePolling()
        } else {
            pollJob?.cancel()
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(history = BothHistory.Loading) }
            val who = who() ?: return@launch
            _state.update { it.copy(partnerName = who.partnerName) }
            _state.update {
                it.copy(
                    history = when (val r = repository.bothSessions(who)) {
                        is AppResult.Success ->
                            if (r.data.isEmpty()) BothHistory.Empty else BothHistory.Content(r.data)
                        is AppResult.Failure -> BothHistory.Error(r.error)
                    },
                )
            }
        }
    }

    fun start() {
        if (_state.value.starting) return
        viewModelScope.launch {
            _state.update { it.copy(starting = true) }
            val who = who()
            val result = who?.let { repository.startDiscussion(it) }
            _state.update { it.copy(starting = false) }
            when (result) {
                is AppResult.Success -> {
                    _state.update { it.copy(open = result.data, draft = "", editing = false, reminded = false) }
                    ensurePolling()
                }
                else -> say(R.string.twinai_both_start_failed)
            }
        }
    }

    fun open(sessionId: String) {
        if (_state.value.opening) return
        viewModelScope.launch {
            _state.update { it.copy(opening = true) }
            val who = who()
            val result = who?.let { repository.discussion(it, sessionId) }
            _state.update { it.copy(opening = false) }
            when (result) {
                is AppResult.Success -> {
                    _state.update { it.copy(open = result.data, draft = "", editing = false, reminded = false) }
                    ensurePolling()
                }
                else -> say(R.string.twinai_both_open_failed)
            }
        }
    }

    /** Leaves the discussion and shows the list again, refreshed. */
    fun close() {
        pollJob?.cancel()
        _state.update { it.copy(open = null, draft = "", editing = false, reminded = false) }
        loadHistory()
    }

    fun onDraftChange(text: String) = _state.update { it.copy(draft = text) }

    fun submit() {
        val round = _state.value.open?.current ?: return
        val text = _state.value.draft.trim()
        if (text.isEmpty() || _state.value.submitting) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            val who = who()
            val result = who?.let { repository.submit(it, round.id, text) }
            _state.update { it.copy(submitting = false) }
            when (result) {
                is AppResult.Success -> {
                    updateRound(round.id) {
                        it.copy(
                            youSubmitted = true,
                            yourContent = text,
                            status = if (result.data == RoundStatus.Analyzing) RoundStatus.Analyzing else it.status,
                        )
                    }
                    _state.update { it.copy(draft = "") }
                    ensurePolling()
                }
                else -> say(R.string.twinai_both_submit_failed)
            }
        }
    }

    fun beginEdit() {
        val mine = _state.value.open?.current?.yourContent.orEmpty()
        _state.update { it.copy(editing = true, draft = mine) }
    }

    fun cancelEdit() = _state.update { it.copy(editing = false, draft = "") }

    fun saveEdit() {
        val round = _state.value.open?.current ?: return
        val text = _state.value.draft.trim()
        if (text.isEmpty() || _state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val who = who()
            val result = who?.let { repository.edit(it, round.id, text) }
            _state.update { it.copy(saving = false) }
            when (result) {
                is AppResult.Success -> {
                    updateRound(round.id) { it.copy(yourContent = text) }
                    _state.update { it.copy(editing = false, draft = "") }
                }
                is AppResult.Failure -> say(R.string.twinai_both_save_failed)
                null -> say(R.string.twinai_both_save_failed)
            }
        }
    }

    fun nudge() {
        val round = _state.value.open?.current ?: return
        viewModelScope.launch {
            val who = who() ?: return@launch
            when (val r = repository.nudge(who, round.id)) {
                is AppResult.Success -> _state.update { it.copy(reminded = true) }
                is AppResult.Failure -> say(R.string.twinai_both_nudge_failed)
            }
        }
    }

    /** "Continue Together": starts the next round of the same discussion. */
    fun continueTogether() {
        val open = _state.value.open ?: return
        viewModelScope.launch {
            when (val r = repository.nextRound(open.session.id)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            open = it.open?.let { o -> o.copy(rounds = o.rounds + r.data) },
                            draft = "", editing = false, reminded = false,
                        )
                    }
                    ensurePolling()
                }
                is AppResult.Failure -> say(R.string.twinai_both_next_failed)
            }
        }
    }

    fun retry() {
        val round = _state.value.open?.current ?: return
        viewModelScope.launch {
            val who = who() ?: return@launch
            when (repository.retry(who, round.id)) {
                is AppResult.Success -> {
                    updateRound(round.id) { it.copy(status = RoundStatus.Analyzing) }
                    ensurePolling()
                }
                is AppResult.Failure -> say(R.string.twinai_both_retry_failed)
            }
        }
    }

    fun askDelete(session: BothSession) = _state.update { it.copy(deleteTarget = session) }

    fun cancelDelete() = _state.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        if (_state.value.deleting) return
        viewModelScope.launch {
            _state.update { it.copy(deleting = true) }
            val who = who()
            val result = who?.let { repository.delete(it, target.id) }
            _state.update { it.copy(deleting = false, deleteTarget = null) }
            when (result) {
                is AppResult.Success -> _state.update { s ->
                    val rest = (s.history as? BothHistory.Content)?.sessions.orEmpty().filter { it.id != target.id }
                    s.copy(history = if (rest.isEmpty()) BothHistory.Empty else BothHistory.Content(rest))
                }
                else -> say(R.string.twinai_both_delete_failed)
            }
        }
    }

    // ── helpers ──

    private suspend fun who(): AiIdentity? =
        identity ?: repository.identity().also { identity = it }

    private fun updateRound(roundId: String, change: (BothRound) -> BothRound) {
        _state.update { s ->
            val open = s.open ?: return@update s
            s.copy(open = open.copy(rounds = open.rounds.map { if (it.id == roundId) change(it) else it }))
        }
    }

    /** Asks the server about the newest round every 3 s while it is waiting and the tab is visible. */
    private fun ensurePolling() {
        if (!visible || pollJob?.isActive == true) return
        if (_state.value.open?.current?.status?.inProgress != true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_MS)
                val round = _state.value.open?.current ?: break
                if (!round.status.inProgress) break
                val who = who() ?: break
                val r = repository.round(who, round.id)
                if (r is AppResult.Success) {
                    updateRound(round.id) {
                        it.copy(
                            status = r.data.status,
                            youSubmitted = r.data.youSubmitted,
                            partnerSubmitted = r.data.partnerSubmitted,
                            yourContent = r.data.yourContent ?: it.yourContent,
                            result = r.data.result,
                        )
                    }
                }
            }
        }
    }

    private fun say(@StringRes id: Int) {
        _messages.tryEmit(id)
    }

    private companion object {
        const val POLL_MS = 3_000L
    }
}
