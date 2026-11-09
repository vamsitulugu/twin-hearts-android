package com.twinhearts.app.feature.home.ui.signal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.notifications.TwinNotifier
import com.twinhearts.app.feature.home.data.HomeRepository
import com.twinhearts.app.feature.home.domain.model.LoveCardState
import com.twinhearts.app.feature.home.domain.model.LoveKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the popup shows: who sent which signal. [stamp] is the hug id or the Touch / Miss You time. */
data class LoveSignalPopup(val kind: LoveKind, val partnerName: String, val stamp: Long)

/**
 * Why this file exists: decides WHEN the "your partner sent you a Hug / Touch / Miss You" Accept
 * card opens, on any screen. Hugs use the server's real accept (`hug.status`). Touch and Miss You
 * have no accept on the server, so the answer is remembered on this phone (nothing changes for the web).
 */
@HiltViewModel
class LoveSignalViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val sessionStore: SessionStore,
    private val notifier: TwinNotifier,
) : ViewModel() {

    private val _popup = MutableStateFlow<LoveSignalPopup?>(null)
    val popup: StateFlow<LoveSignalPopup?> = _popup.asStateFlow()

    private var busy = false
    private var dismissedHugId: Long? = null

    /** Looks for something waiting for me. [hugsOnHome] is true on Home, where the hug already has its own inline card. */
    fun check(hugsOnHome: Boolean) {
        if (busy || _popup.value != null) return
        busy = true
        viewModelScope.launch {
            try {
                val result = repository.loveCard()
                if (result is AppResult.Success) _popup.value = next(result.data, hugsOnHome)
            } finally {
                busy = false
            }
        }
    }

    private suspend fun next(card: LoveCardState, hugsOnHome: Boolean): LoveSignalPopup? {
        val name = sessionStore.session.first()?.partnerName?.takeIf { it.isNotBlank() }.orEmpty()
        val hugId = card.pendingIncomingHugId
        if (hugId != null && !hugsOnHome && hugId != dismissedHugId) {
            return LoveSignalPopup(LoveKind.Hug, name, hugId)
        }
        // Oldest unanswered first, so two signals in a row are both seen, one after the other.
        return card.incoming.firstNotNullOfOrNull { signal ->
            val handled = repository.handledSignal(signal.kind)
            if (handled == null) {
                // First time on this phone: start from now, never pop up an old touch.
                repository.markSignalHandled(signal.kind, signal.timestampMillis)
                null
            } else if (signal.timestampMillis > handled) {
                LoveSignalPopup(signal.kind, name, signal.timestampMillis)
            } else {
                null
            }
        }
    }

    fun accept() {
        val current = _popup.value ?: return
        _popup.value = null
        viewModelScope.launch {
            if (current.kind == LoveKind.Hug) repository.acceptHug() else repository.markSignalHandled(current.kind, current.stamp)
            notifier.clearLoveSignals()
        }
    }

    fun later() {
        val current = _popup.value ?: return
        _popup.value = null
        if (current.kind == LoveKind.Hug) {
            dismissedHugId = current.stamp // the hug stays waiting on Home; the popup does not nag
        } else {
            viewModelScope.launch { repository.markSignalHandled(current.kind, current.stamp) }
        }
    }
}
