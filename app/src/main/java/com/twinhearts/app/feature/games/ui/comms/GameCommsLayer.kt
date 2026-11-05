package com.twinhearts.app.feature.games.ui.comms

import android.widget.Toast
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.ui.GameCallHooks
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Why this file exists: everything "talking while you play" in one place, so the game frame
 * ([com.twinhearts.app.feature.games.ui.components.GameHost]) only adds one line and all 8 games get it.
 *
 *  - Header buttons, top-right: 💬 chat (with an unread badge), 📞 voice call, 📹 face-to-face video.
 *  - 💬 opens the chat sheet; 📞 turns the voice call on/off and shows the tiny pill.
 *  - While a game is on screen and the app is visible, it listens for my partner's messages and calls.
 *
 * Leaving the game ends the voice call and clears the chat (same as the web page).
 */
@Composable
fun BoxScope.GameCommsLayer(gameKey: String, hooks: GameCallHooks) {
    val chat = hiltViewModel<GameChatViewModel>()
    val chatState by chat.ui.collectAsStateWithLifecycle()
    val voice = hooks.voice
    val voiceState = voice.state()
    val inCall = hooks.inCall()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Listen only while the screen is visible; the voice call itself keeps going while it runs.
    LaunchedEffect(gameKey) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            coroutineScope {
                launch { chat.run() }
                launch { voice.watch(gameKey) }
            }
        }
    }
    DisposableEffect(gameKey) {
        onDispose {
            chat.end()
            voice.leave()
        }
    }
    LaunchedEffect(chat) {
        chat.events.collect { event ->
            val text = when (event) {
                is GameChatEvent.Incoming ->
                    if (event.text != null) context.getString(R.string.games_comms_toast_text, event.name, event.text.take(PREVIEW_CHARS))
                    else context.getString(R.string.games_comms_toast_voice, event.name)
                is GameChatEvent.Notice -> context.getString(event.res)
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
    ) {
        HeaderButton(
            onClick = chat::toggleSheet,
            description = stringResource(R.string.games_comms_chat),
            tint = if (chatState.open) TwinTheme.colors.accent else TwinTheme.colors.ink,
            icon = Icons.Rounded.ChatBubble,
            badge = chatState.unread,
        )
        HeaderButton(
            onClick = { if (voiceState.running) voice.hangUp() else if (voiceState.phase == GameVoiceUiPhase.Idle) voice.start() },
            description = stringResource(if (voiceState.running) R.string.games_voice_end else R.string.games_voice_start),
            tint = if (voiceState.running) TwinTheme.colors.success else TwinTheme.colors.ink,
            icon = if (voiceState.running) Icons.Rounded.CallEnd else Icons.Rounded.Call,
        )
        HeaderButton(
            onClick = { if (inCall) hooks.hangUp() else hooks.startVideo() },
            description = stringResource(if (inCall) R.string.games_call_end else R.string.games_call_start),
            tint = if (inCall) TwinTheme.colors.accent else TwinTheme.colors.ink,
            icon = if (inCall) Icons.Rounded.CallEnd else Icons.Rounded.Videocam,
        )
    }

    // The small draggable video window, the voice pill / incoming card, then the chat sheet on top.
    hooks.overlay(this)
    GameVoiceOverlay(voiceState, voice)
    if (chatState.open) {
        GameChatSheet(
            state = chatState,
            onDismiss = chat::closeSheet,
            onSend = chat::sendText,
            onStartRecording = chat::startRecording,
            onStopRecording = chat::stopRecording,
            onRetry = chat::retry,
        )
    }
}

@Composable
private fun HeaderButton(onClick: () -> Unit, description: String, tint: Color, icon: ImageVector, badge: Int = 0) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (badge > 0) {
                    Badge(containerColor = TwinTheme.colors.error) {
                        Text(if (badge > MAX_BADGE) "$MAX_BADGE+" else badge.toString())
                    }
                }
            },
        ) {
            Icon(imageVector = icon, contentDescription = description, tint = tint)
        }
    }
}

private const val PREVIEW_CHARS = 60
private const val MAX_BADGE = 9
