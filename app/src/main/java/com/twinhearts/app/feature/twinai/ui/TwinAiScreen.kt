package com.twinhearts.app.feature.twinai.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.twinai.domain.model.AiAuthor
import com.twinhearts.app.feature.twinai.domain.model.AiMessageState
import com.twinhearts.app.feature.twinai.ui.components.BothActions
import com.twinhearts.app.feature.twinai.ui.components.BothContent
import com.twinhearts.app.feature.twinai.ui.components.TwinAiDock
import com.twinhearts.app.feature.twinai.ui.components.TwinAiHeader
import com.twinhearts.app.feature.twinai.ui.components.TwinModeTabs
import com.twinhearts.app.feature.twinai.ui.components.YouContent
import com.twinhearts.app.feature.twinai.ui.components.rememberSpeaker
import com.twinhearts.app.feature.twinai.ui.voice.TwinVoiceOverlay
import com.twinhearts.app.feature.twinai.ui.voice.rememberTwinVoice

/**
 * Why this file exists: the Twin AI page (bottom-bar orb). Header, YOU / BOTH switch, then either
 * the chat with Twin (with its input dock) or the "Together" discussions.
 */
@Composable
fun TwinAiScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TwinAiViewModel = hiltViewModel(),
    bothViewModel: BothViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val both by bothViewModel.state.collectAsStateWithLifecycle()

    fun toast(resId: Int) = Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()

    val speaker = rememberSpeaker { toast(R.string.twinai_tts_unavailable) }

    //  voice mode (the full screen with the big orb) 
    val voice = rememberTwinVoice { spoken ->
        if (viewModel.state.value.streaming) {
            false
        } else {
            viewModel.ask(spoken)
            true
        }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voice.open() else voice.openDenied()
    }
    fun startVoice() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) voice.open() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }
    // When Twin finishes answering a spoken question, voice mode reads the answer aloud.
    LaunchedEffect(state.streaming) {
        if (!state.streaming) {
            val last = state.messages.lastOrNull { it.author == AiAuthor.Twin }
            voice.onReplyFinished(failed = last?.state == AiMessageState.Failed, reply = last?.text)
        }
    }
    // Leaving the app closes voice mode so the microphone is never left on (same as the web).
    LifecycleStartEffect(Unit) { onStopOrDispose { voice.exit() } }

    //  dictation into the BOTH answer box (one-shot, like the web mic) 
    val speechPrompt = stringResource(R.string.twinai_speech_prompt)
    val dictate = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !heard.isNullOrBlank()) bothViewModel.appendToDraft(heard)
    }
    fun dictateAnswer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, speechPrompt)
        try {
            dictate.launch(intent)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.twinai_mic_unavailable)
        }
    }

    // BOTH only asks the server for updates while it is on screen (R-66).
    LifecycleStartEffect(state.tab) {
        bothViewModel.setVisible(state.tab == TwinTab.Both)
        onStopOrDispose { bothViewModel.setVisible(false) }
    }
    LaunchedEffect(Unit) {
        bothViewModel.messages.collect { t ->
            val text = t.text ?: t.resId?.let { context.getString(it, *t.args.toTypedArray()) }
            if (text != null) Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) { viewModel.messages.collect { toast(it) } }

    // Back, like the web: delete card, then an open discussion, then BOTH home goes to YOU, then leave.
    BackHandler(enabled = state.tab == TwinTab.Both) {
        when {
            both.deleteTarget != null -> bothViewModel.cancelDelete()
            both.open != null -> bothViewModel.close()
            else -> viewModel.backToYou()
        }
    }
    BackHandler(enabled = state.tab == TwinTab.You && state.quickOpen) { viewModel.closeQuick() }

    val actions = remember(bothViewModel) {
        BothActions(
            onStart = bothViewModel::start, onOpen = bothViewModel::open, onClose = bothViewModel::close,
            onReload = bothViewModel::loadHistory, onDraft = bothViewModel::onDraftChange,
            onSubmit = bothViewModel::submit, onBeginEdit = bothViewModel::beginEdit,
            onCancelEdit = bothViewModel::cancelEdit, onSaveEdit = bothViewModel::saveEdit,
            onNudge = bothViewModel::nudge, onContinue = bothViewModel::continueTogether,
            onRetry = bothViewModel::retry, onAskDelete = bothViewModel::askDelete,
            onConfirmDelete = bothViewModel::confirmDelete, onCancelDelete = bothViewModel::cancelDelete,
            onMic = ::dictateAnswer,
        )
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            TwinAiHeader(
                subtitle = stringResource(if (state.tab == TwinTab.Both) R.string.twinai_subtitle_both else R.string.twinai_subtitle),
                syncing = state.syncing,
                onBack = { if (state.tab == TwinTab.Both) viewModel.backToYou() else onBack() },
                onMenu = { toast(R.string.twinai_pending_menu) },
                onSearch = { toast(R.string.twinai_pending_search) },
                onSync = {
                    viewModel.refresh()
                    if (state.tab == TwinTab.Both) bothViewModel.loadHistory()
                },
            )
            TwinModeTabs(
                options = listOf(stringResource(R.string.twinai_tab_you), stringResource(R.string.twinai_tab_both)),
                selectedIndex = state.tab.ordinal,
                onSelect = { viewModel.selectTab(TwinTab.entries[it]) },
                modifier = Modifier
                    .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s)
                    .fillMaxWidth(0.62f),
            )
            Box(
                Modifier
                    .weight(1f)
                    // Tapping the page outside the "+" sheet closes it (the web does the same). Buttons inside still get their own taps.
                    .then(
                        if (state.tab == TwinTab.You && state.quickOpen) {
                            Modifier.pointerInput(Unit) { detectTapGestures(onTap = { viewModel.closeQuick() }) }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                when (state.tab) {
                    TwinTab.You -> YouContent(
                        greetingName = state.greetingName,
                        messages = state.messages,
                        speakingId = speaker.speakingId,
                        onPrompt = { viewModel.ask(it.prompt) },
                        onRetry = viewModel::askAgain,
                        onRegenerate = viewModel::askAgain,
                        onListen = { speaker.toggle(it.id, it.text) },
                    )
                    TwinTab.Both -> BothContent(both, actions)
                }
            }
            if (state.tab == TwinTab.You) {
                TwinAiDock(
                    input = state.input,
                    onInputChange = viewModel::onInputChange,
                    onSend = viewModel::sendInput,
                    onMic = ::startVoice,
                    onTogglePlus = viewModel::toggleQuick,
                    quickOpen = state.quickOpen,
                    onPrompt = { viewModel.ask(it.prompt) },
                    sendEnabled = state.input.isNotBlank() && !state.streaming,
                    orb = if (voice.isOpen) voice.orb else state.orb,
                    sending = state.streaming,
                )
            }
        }
        TwinVoiceOverlay(voice)
    }

    if (state.proPrompt) {
        AlertDialog(
            onDismissRequest = viewModel::dismissProPrompt,
            title = { Text(stringResource(R.string.twinai_pro_title)) },
            text = { Text(stringResource(R.string.twinai_pro_body), color = TwinTheme.colors.inkMuted) },
            confirmButton = { TextButton(onClick = viewModel::dismissProPrompt) { Text(stringResource(R.string.twinai_pro_ok)) } },
            containerColor = TwinTheme.colors.card,
            titleContentColor = TwinTheme.colors.ink,
            textContentColor = TwinTheme.colors.inkMuted,
        )
    }
}
