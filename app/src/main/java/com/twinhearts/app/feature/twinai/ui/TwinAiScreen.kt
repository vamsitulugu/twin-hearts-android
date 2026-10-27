package com.twinhearts.app.feature.twinai.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.twinai.ui.components.BothActions
import com.twinhearts.app.feature.twinai.ui.components.BothContent
import com.twinhearts.app.feature.twinai.ui.components.TwinAiDock
import com.twinhearts.app.feature.twinai.ui.components.TwinAiHeader
import com.twinhearts.app.feature.twinai.ui.components.TwinModeTabs
import com.twinhearts.app.feature.twinai.ui.components.YouContent
import com.twinhearts.app.feature.twinai.ui.components.rememberSpeaker

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

    val speechPrompt = stringResource(R.string.twinai_speech_prompt)
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !heard.isNullOrBlank()) viewModel.ask(heard)
    }

    // BOTH only asks the server for updates while it is on screen (R-66).
    LifecycleStartEffect(state.tab) {
        bothViewModel.setVisible(state.tab == TwinTab.Both)
        onStopOrDispose { bothViewModel.setVisible(false) }
    }
    LaunchedEffect(Unit) {
        bothViewModel.messages.collect { toast(it) }
    }

    // Back closes an open discussion first; only then does it leave the page.
    BackHandler(enabled = state.tab == TwinTab.Both && both.open != null) { bothViewModel.close() }

    val actions = remember(bothViewModel) {
        BothActions(
            onStart = bothViewModel::start, onOpen = bothViewModel::open, onClose = bothViewModel::close,
            onReload = bothViewModel::loadHistory, onDraft = bothViewModel::onDraftChange,
            onSubmit = bothViewModel::submit, onBeginEdit = bothViewModel::beginEdit,
            onCancelEdit = bothViewModel::cancelEdit, onSaveEdit = bothViewModel::saveEdit,
            onNudge = bothViewModel::nudge, onContinue = bothViewModel::continueTogether,
            onRetry = bothViewModel::retry, onAskDelete = bothViewModel::askDelete,
            onConfirmDelete = bothViewModel::confirmDelete, onCancelDelete = bothViewModel::cancelDelete,
        )
    }

    Column(modifier.fillMaxSize().imePadding()) {
        TwinAiHeader(
            syncing = state.syncing,
            onBack = onBack,
            onMenu = { toast(R.string.twinai_pending_menu) },
            onSearch = { toast(R.string.twinai_pending_search) },
            onSync = {
                viewModel.refresh()
                if (state.tab == TwinTab.Both) bothViewModel.loadHistory()
            },
        )
        if (state.bothEnabled) {
            TwinModeTabs(
                options = listOf(stringResource(R.string.twinai_tab_you), stringResource(R.string.twinai_tab_both)),
                selectedIndex = state.tab.ordinal,
                onSelect = { viewModel.selectTab(TwinTab.entries[it]) },
                modifier = Modifier
                    .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s)
                    .fillMaxWidth(0.62f),
            )
        }
        Box(Modifier.weight(1f)) {
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
                onMic = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, speechPrompt)
                    try {
                        speech.launch(intent)
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.twinai_mic_unavailable)
                    }
                },
                onTogglePlus = viewModel::toggleQuick,
                quickOpen = state.quickOpen,
                onPrompt = { viewModel.ask(it.prompt) },
                sendEnabled = state.input.isNotBlank() && !state.streaming,
            )
        }
    }
}
