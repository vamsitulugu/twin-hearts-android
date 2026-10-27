package com.twinhearts.app.feature.twinai.ui

import android.widget.Toast
import com.twinhearts.app.feature.twinai.ui.components.rememberVoiceController
import com.twinhearts.app.feature.twinai.ui.components.TwinVoiceOverlay
import com.twinhearts.app.feature.twinai.domain.model.AiMessageState
import com.twinhearts.app.feature.twinai.domain.model.AiAuthor
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.twinhearts.app.feature.twinai.ui.components.GlassTextArea
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

    // Voice mode: listen, send to Twin, read the answer, listen again.
    val voice = rememberVoiceController(
        speaker = speaker,
        onHeard = { viewModel.ask(it) },
        onUnavailable = {
            viewModel.closeVoice()
            toast(R.string.twinai_voice_unavailable)
        },
    )
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.openVoice()
            voice.start()
        } else {
            toast(R.string.twinai_voice_permission)
        }
    }
    val last = state.messages.lastOrNull()
    LaunchedEffect(last?.id, last?.state) {
        if (state.voiceOpen && last?.author == AiAuthor.Twin) {
            when (last.state) {
                AiMessageState.Done -> voice.speak(last.id, last.text)
                AiMessageState.Failed -> voice.failed()
                AiMessageState.Streaming -> Unit
            }
        }
    }
    val clipboard = LocalClipboardManager.current
    fun startVoice() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.openVoice()
            voice.start()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    fun endVoice() {
        voice.end()
        viewModel.closeVoice()
    }

    // BOTH only asks the server for updates while it is on screen (R-66).
    LifecycleStartEffect(state.tab) {
        bothViewModel.setVisible(state.tab == TwinTab.Both)
        onStopOrDispose {
            bothViewModel.setVisible(false)
            // Never keep the microphone open while the app is in the background.
            voice.end()
            viewModel.closeVoice()
        }
    }
    LaunchedEffect(Unit) {
        bothViewModel.messages.collect { toast(it) }
    }

    // Back closes an open discussion first; only then does it leave the page.
    BackHandler(enabled = state.voiceOpen) { endVoice() }
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

    Box(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().imePadding()) {
        TwinAiHeader(
            syncing = state.syncing,
            subtitle = stringResource(if (state.tab == TwinTab.Both) R.string.twinai_subtitle_both else R.string.twinai_subtitle),
            onBack = onBack,
            onMenu = { viewModel.setMenu(true) },
            onSearch = viewModel::toggleSearch,
            menuExpanded = state.menuOpen,
            onMenuDismiss = { viewModel.setMenu(false) },
            menuContent = {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.twinai_menu_new_chat), color = TwinTheme.colors.ink) },
                    onClick = { viewModel.newChat(); viewModel.selectTab(TwinTab.You) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.twinai_menu_voice), color = TwinTheme.colors.ink) },
                    onClick = { viewModel.setMenu(false); viewModel.selectTab(TwinTab.You); startVoice() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.twinai_menu_quick), color = TwinTheme.colors.ink) },
                    onClick = { viewModel.setMenu(false); viewModel.selectTab(TwinTab.You); viewModel.toggleQuick() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.twinai_menu_copy), color = TwinTheme.colors.ink) },
                    onClick = {
                        viewModel.setMenu(false)
                        if (state.messages.isEmpty()) {
                            toast(R.string.twinai_menu_empty)
                        } else {
                            clipboard.setText(AnnotatedString(state.messages.joinToString("\n\n") {
                                (if (it.author == AiAuthor.Me) "Me: " else "Twin: ") + it.text
                            }))
                            toast(R.string.twinai_menu_copied)
                        }
                    },
                )
            },
            onSync = {
                viewModel.refresh()
                if (state.tab == TwinTab.Both) bothViewModel.loadHistory()
            },
        )
        run {
            TwinModeTabs(
                options = listOf(stringResource(R.string.twinai_tab_you), stringResource(R.string.twinai_tab_both)),
                selectedIndex = state.tab.ordinal,
                onSelect = { viewModel.selectTab(TwinTab.entries[it]) },
                modifier = Modifier
                    .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s)
                    .fillMaxWidth(0.62f),
            )
        }
        if (state.searchOpen) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    GlassTextArea(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchChange,
                        hint = stringResource(R.string.twinai_search_hint),
                        minHeight = 44.dp,
                        maxLines = 1,
                        shape = TwinTheme.shapes.pill,
                    )
                }
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.twinai_search_close),
                    tint = TwinTheme.colors.ink,
                    modifier = Modifier.size(TwinTheme.shapes.minTouch).clickable(onClick = viewModel::toggleSearch).padding(12.dp),
                )
            }
        }
        val query = state.searchQuery.trim()
        val shown = if (query.isEmpty()) state.messages else state.messages.filter { it.text.contains(query, ignoreCase = true) }
        Box(Modifier.weight(1f)) {
            when (state.tab) {
                TwinTab.You -> YouContent(
                    greetingName = state.greetingName,
                    messages = shown,
                    showHero = query.isEmpty(),
                    emptyNote = if (query.isNotEmpty() && shown.isEmpty()) stringResource(R.string.twinai_search_empty) else null,
                    speakingId = speaker.speakingId,
                    onPrompt = { viewModel.ask(it.prompt) },
                    onRetry = viewModel::askAgain,
                    onRegenerate = viewModel::askAgain,
                    onListen = { speaker.toggle(it.id, it.text) },
                )
                TwinTab.Both -> BothContent(both, actions, query = query)
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
            )
        }
    }
    if (state.voiceOpen) {
        TwinVoiceOverlay(controller = voice, onEnd = ::endVoice)
    }
    }
}
