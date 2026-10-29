package com.twinhearts.app.feature.help.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.help.domain.model.SupportMode
import com.twinhearts.app.feature.help.ui.components.HelpChatView
import com.twinhearts.app.feature.help.ui.components.HelpFaqView
import com.twinhearts.app.feature.help.ui.components.HelpPillTabs
import com.twinhearts.app.feature.help.ui.components.bottomBorder

/**
 * Help & Customer Care page (web: `public/help.html`). Two tabs: "Questions & QA" (search, topics,
 * answers) and "Live Support & AI Chat" (Twin Care AI, or a live agent on the admin dashboard).
 * Like the web page it has its own top bar and no bottom bar.
 */
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    viewModel: HelpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val chatOpen = state.tab == HelpTab.Chat

    // One-off events become a toast. The text is resolved here because error texts need resources.
    var event by remember { mutableStateOf<HelpEvent?>(null) }
    LaunchedEffect(viewModel) { viewModel.events.collect { event = it } }
    val toastText = event?.let { helpEventText(it) }
    LaunchedEffect(event) {
        if (toastText != null) {
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            event = null
        }
    }

    // New replies are fetched only while the chat tab is open and the app is on screen (R-66).
    LaunchedEffect(chatOpen, lifecycleOwner) {
        if (chatOpen) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pollWhileVisible()
            }
        }
    }

    val status = when {
        !chatOpen -> stringResource(R.string.help_status_idle)
        state.chat.mode == SupportMode.Ai -> stringResource(R.string.help_status_ai)
        else -> stringResource(R.string.help_status_human)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HelpTopBar(status = status, onBack = onBack)
        HelpPillTabs(
            options = listOf(
                stringResource(R.string.help_tab_questions),
                stringResource(R.string.help_tab_chat),
            ),
            selectedIndex = if (chatOpen) 1 else 0,
            onSelect = { index -> viewModel.selectTab(if (index == 0) HelpTab.Faq else HelpTab.Chat) },
            modifier = Modifier
                .fillMaxWidth()
                .bottomBorder(TwinTheme.colors.line)
                .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
        )
        if (chatOpen) {
            HelpChatView(
                chat = state.chat,
                myName = state.myName,
                onModeSelect = viewModel::selectMode,
                onDraftChange = viewModel::onDraftChange,
                onSend = viewModel::send,
                onQuick = viewModel::sendQuick,
                onRetry = viewModel::retryConnect,
                modifier = Modifier.weight(1f),
            )
        } else {
            HelpFaqView(
                state = state,
                onQueryChange = viewModel::onQueryChange,
                onCategory = viewModel::onCategory,
                onToggle = viewModel::toggleFaq,
                onAskAi = { question -> viewModel.startChat(SupportMode.Ai, question) },
                onMessageSupport = { about -> viewModel.startChat(SupportMode.Human, about) },
                onStartChat = { viewModel.startChat(SupportMode.Human) },
                onRetry = viewModel::loadFaqs,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Back arrow, "Help & Customer Care" and the green status line (web `.appbar`). */
@Composable
private fun HelpTopBar(status: String, onBack: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.glass)
            .bottomBorder(colors.line)
            .statusBarsPadding()
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.core_back),
            onClick = onBack,
        )
        Column {
            Text(
                text = stringResource(R.string.help_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.success),
                )
                Text(text = status, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
            }
        }
    }
}

/** The toast wording for each one-off event. Most texts are the web's own. */
@Composable
private fun helpEventText(event: HelpEvent): String = when (event) {
    HelpEvent.MessageSent -> stringResource(R.string.help_toast_sent)
    HelpEvent.ConnectFailed -> stringResource(R.string.help_toast_connect_failed)
    HelpEvent.SendFailed -> stringResource(R.string.help_toast_send_failed)
    HelpEvent.AiUnavailable -> stringResource(R.string.help_toast_ai_unavailable)
    is HelpEvent.Failed -> event.error.asText()
}
