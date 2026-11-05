package com.twinhearts.app.feature.games.ui.comms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.sound.TwinSound
import com.twinhearts.app.feature.games.data.GameChatRepository
import com.twinhearts.app.feature.games.domain.engine.GameChatRules
import com.twinhearts.app.feature.games.domain.model.GameChatIdentity
import com.twinhearts.app.feature.games.domain.model.GameChatKind
import com.twinhearts.app.feature.games.domain.model.GameChatMessage
import com.twinhearts.app.feature.games.domain.model.GameChatStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject

/** Everything the chat button, badge and sheet draw (R-42: one state object, events apart). */
data class GameChatUiState(
    val messages: List<GameChatMessage> = emptyList(),
    val partnerName: String = "",
    val unread: Int = 0,
    val open: Boolean = false,
    val recording: Boolean = false,
    val recordSeconds: Int = 0,
)

/** One-off things to tell the person (a toast), not part of the state. */
sealed interface GameChatEvent {
    /** My partner wrote while the sheet was closed. [text] is null for a voice message. */
    data class Incoming(val name: String, val text: String?) : GameChatEvent
    data class Notice(val res: Int) : GameChatEvent
}

/**
 * Why this file exists: the brain of the in-game chat (web: `GameChat`). It keeps my messages and my
 * partner's, polls the shared state while a game is on screen, saves mine under my own key, and
 * records / uploads voice messages. Games calls [end] when a game closes, so the next game starts with an empty chat.
 */
@HiltViewModel
class GameChatViewModel @Inject constructor(
    private val repository: GameChatRepository,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val _ui = MutableStateFlow(GameChatUiState())
    val ui: StateFlow<GameChatUiState> = _ui.asStateFlow()
    private val _events = MutableSharedFlow<GameChatEvent>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<GameChatEvent> = _events.asSharedFlow()

    private val recorder = GameVoiceRecorder(context)
    private val saveLock = Mutex()
    private var who: GameChatIdentity? = null
    private var mine = emptyList<GameChatMessage>()
    private var theirs = emptyList<GameChatMessage>()
    private var baseline = false
    private var recordJob: Job? = null

    /** The game closed: forget everything and stop any recording. */
    fun end() {
        discardRecording()
        mine = emptyList()
        theirs = emptyList()
        baseline = false
        _ui.value = GameChatUiState()
    }

    /**
     * Runs while a game is on screen and the app is visible (the caller cancels it otherwise).
     * Looks every few seconds; faster while the sheet is open, like the web app.
     */
    suspend fun run() {
        val me = who ?: repository.identity()?.also { who = it } ?: return
        _ui.update { it.copy(partnerName = me.partnerName) }
        while (true) {
            pull(me)
            delay(if (_ui.value.open) OPEN_POLL_MS else CLOSED_POLL_MS)
        }
    }

    private suspend fun pull(me: GameChatIdentity) {
        val result = repository.pull(me, includeMine = !baseline) as? AppResult.Success ?: return
        val data = result.data
        if (!baseline) {
            // First look: whatever is already there is old news, not "new messages".
            mine = GameChatRules.restoreMine(mine, data.mine)
            theirs = data.theirs
            baseline = true
            publish()
            return
        }
        val fresh = GameChatRules.fresh(theirs, data.theirs)
        theirs = data.theirs
        if (fresh.isEmpty()) {
            publish()
            return
        }
        TwinSound.msgReceived()
        if (_ui.value.open) {
            publish()
        } else {
            _ui.update { it.copy(unread = it.unread + fresh.size) }
            publish()
            val last = fresh.last()
            _events.tryEmit(GameChatEvent.Incoming(me.partnerName, last.text.takeIf { last.kind == GameChatKind.Text }))
        }
    }

    // ───────────── the sheet ─────────────

    /** Opens or closes the sheet. Not paired yet: says so instead of opening. */
    fun toggleSheet() {
        if (_ui.value.open) {
            setOpen(false)
            return
        }
        viewModelScope.launch {
            val me = who ?: repository.identity()?.also { who = it }
            if (me == null) {
                _events.tryEmit(GameChatEvent.Notice(R.string.games_comms_connect_first))
                return@launch
            }
            _ui.update { it.copy(partnerName = me.partnerName) }
            setOpen(true)
        }
    }

    fun closeSheet() = setOpen(false)

    private fun setOpen(open: Boolean) {
        if (!open) discardRecording()
        _ui.update { it.copy(open = open, unread = if (open) 0 else it.unread) }
        if (open) publish()
    }

    // ───────────── sending ─────────────

    fun sendText(raw: String) {
        val text = GameChatRules.cleanText(raw)
        if (text.isEmpty()) return
        TwinSound.msgSent()
        add(GameChatMessage(newId(), GameChatKind.Text, text = text, sentAt = System.currentTimeMillis(), mine = true, status = GameChatStatus.Sending))
        save()
    }

    fun retry(id: String) {
        val m = mine.firstOrNull { it.id == id } ?: return
        if (m.kind == GameChatKind.Voice && m.url.isBlank() && m.localPath != null) {
            upload(id)
        } else {
            setStatus(id, GameChatStatus.Sending)
            save()
        }
    }

    private fun add(message: GameChatMessage) {
        mine = (mine + message).takeLast(GameChatRules.KEPT_IN_MEMORY)
        publish()
    }

    private fun setStatus(id: String, status: GameChatStatus) {
        mine = mine.map { if (it.id == id) it.copy(status = status) else it }
        publish()
    }

    /** Saves my last messages. One save at a time, so two quick messages cannot overwrite each other. */
    private fun save() {
        val me = who ?: return
        viewModelScope.launch {
            saveLock.withLock {
                val sent = GameChatRules.toSave(mine)
                val ids = sent.mapTo(HashSet()) { it.id }
                val ok = repository.saveMine(me, mine) is AppResult.Success
                mine = mine.map { m ->
                    when {
                        m.id !in ids -> m
                        ok && m.status != GameChatStatus.Sent -> m.copy(status = GameChatStatus.Sent)
                        !ok && m.status == GameChatStatus.Sending -> m.copy(status = GameChatStatus.Failed)
                        else -> m
                    }
                }
                publish()
            }
        }
    }

    // ───────────── voice messages ─────────────

    /** The screen has already asked for the microphone permission. */
    fun startRecording() {
        if (_ui.value.recording) return
        if (!recorder.start()) {
            _events.tryEmit(GameChatEvent.Notice(R.string.games_comms_mic_failed))
            return
        }
        TwinSound.voiceStart()
        val startedAt = System.currentTimeMillis()
        _ui.update { it.copy(recording = true, recordSeconds = 0) }
        recordJob = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                val seconds = ((System.currentTimeMillis() - startedAt) / MS_PER_SECOND).toInt()
                _ui.update { it.copy(recordSeconds = seconds) }
                if (seconds >= GameChatRules.MAX_RECORD_SECONDS) {
                    stopRecording(send = true)
                    return@launch
                }
            }
        }
    }

    fun stopRecording(send: Boolean) {
        if (!_ui.value.recording) return
        recordJob?.cancel()
        recordJob = null
        _ui.update { it.copy(recording = false, recordSeconds = 0) }
        if (!send) {
            recorder.cancel()
            TwinSound.voiceCancel()
            return
        }
        val rec = recorder.finish()
        if (rec == null || rec.millis < GameChatRules.MIN_RECORD_MS) {
            rec?.file?.delete()
            _events.tryEmit(GameChatEvent.Notice(R.string.games_comms_too_short))
            return
        }
        TwinSound.voiceStop()
        val message = GameChatMessage(
            id = newId(), kind = GameChatKind.Voice,
            seconds = (rec.millis / MS_PER_SECOND).toInt().coerceAtLeast(1),
            sentAt = System.currentTimeMillis(), mine = true,
            status = GameChatStatus.Sending, localPath = rec.file.absolutePath,
        )
        add(message)
        upload(message.id)
    }

    private fun discardRecording() {
        recordJob?.cancel()
        recordJob = null
        if (_ui.value.recording) {
            recorder.cancel()
            _ui.update { it.copy(recording = false, recordSeconds = 0) }
        }
    }

    private fun upload(id: String) {
        val me = who ?: return
        val path = mine.firstOrNull { it.id == id }?.localPath ?: return
        setStatus(id, GameChatStatus.Sending)
        viewModelScope.launch {
            when (val r = repository.uploadVoice(me, path)) {
                is AppResult.Success -> {
                    mine = mine.map { if (it.id == id) it.copy(url = r.data) else it }
                    save()
                }
                is AppResult.Failure -> {
                    setStatus(id, GameChatStatus.Failed)
                    _events.tryEmit(GameChatEvent.Notice(R.string.games_comms_voice_failed))
                }
            }
        }
    }

    private fun publish() {
        _ui.update { it.copy(messages = GameChatRules.visible(mine, theirs, System.currentTimeMillis())) }
    }

    private fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(ID_LENGTH)

    override fun onCleared() {
        recorder.cancel()
        super.onCleared()
    }

    private companion object {
        const val OPEN_POLL_MS = 2_000L
        const val CLOSED_POLL_MS = 4_000L
        const val TICK_MS = 250L
        const val MS_PER_SECOND = 1_000L
        const val EVENT_BUFFER = 8
        const val ID_LENGTH = 12
    }
}
