package com.twinhearts.app.feature.games.ui.comms

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.engine.GameChatRules
import com.twinhearts.app.feature.games.domain.model.GameChatKind
import com.twinhearts.app.feature.games.domain.model.GameChatMessage
import com.twinhearts.app.feature.games.domain.model.GameChatStatus
import java.text.DateFormat
import java.util.Date

/**
 * Why this file exists: the chat bottom sheet that opens over any game (web: the 💬 sheet).
 * Normal text messages, plus ONE 🎙 button that records and sends a voice message.
 * It is stateless: [state] comes from [GameChatViewModel], every tap goes back through a callback (R-42).
 * Back closes the sheet before it can leave the game, because the sheet handles Back itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameChatSheet(
    state: GameChatUiState,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: (send: Boolean) -> Unit,
    onRetry: (String) -> Unit,
) {
    val colors = TwinTheme.colors
    val context = LocalContext.current
    val player = rememberGameVoicePlayer()
    val listState = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onStartRecording()
        else Toast.makeText(context, R.string.games_comms_mic_needed, Toast.LENGTH_SHORT).show()
    }

    // Always show the newest message, also when a new one arrives.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SHEET_MIN_HEIGHT, max = SHEET_MAX_HEIGHT)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "💬 ${state.partnerName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                RoundIcon(Icons.Rounded.Close, stringResource(R.string.games_comms_close), colors.glass, small = true, onClick = onDismiss)
            }

            if (state.messages.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(TwinTheme.spacing.xl),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.games_comms_empty_title), style = MaterialTheme.typography.bodyLarge, color = colors.ink, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.games_comms_empty_hint), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
                    verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        Bubble(
                            message = message,
                            playing = player.playing != null && player.playing == message.playSource(),
                            onPlay = {
                                message.playSource()?.let { source ->
                                    player.toggle(source) {
                                        Toast.makeText(context, R.string.games_comms_play_failed, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onRetry = { onRetry(message.id) },
                        )
                    }
                }
            }

            if (state.recording) {
                RecordingBar(seconds = state.recordSeconds, onCancel = { onStopRecording(false) }, onSend = { onStopRecording(true) })
            } else {
                Composer(
                    draft = draft,
                    onDraftChange = { draft = it.take(GameChatRules.MAX_TEXT) },
                    onSend = {
                        onSend(draft)
                        draft = ""
                    },
                    onMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                )
            }
        }
    }
}

/** Where a voice message plays from: its web address, or my own recording while it is still uploading. */
private fun GameChatMessage.playSource(): String? =
    if (kind != GameChatKind.Voice) null else url.ifBlank { localPath }

@Composable
private fun Bubble(message: GameChatMessage, playing: Boolean, onPlay: () -> Unit, onRetry: () -> Unit) {
    val colors = TwinTheme.colors
    val mine = message.mine
    val shape = RoundedCornerShape(
        topStart = BUBBLE_RADIUS, topEnd = BUBBLE_RADIUS,
        bottomStart = if (mine) BUBBLE_RADIUS else BUBBLE_TAIL, bottomEnd = if (mine) BUBBLE_TAIL else BUBBLE_RADIUS,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = BUBBLE_MAX_WIDTH)
                .clip(shape)
                .background(if (mine) colors.brand else colors.glass)
                .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
        ) {
            if (message.kind == GameChatKind.Voice) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    RoundIcon(
                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        stringResource(if (playing) R.string.games_comms_pause else R.string.games_comms_play),
                        colors.line, small = true, onClick = onPlay,
                    )
                    Box(Modifier.size(width = WAVE_WIDTH, height = 5.dp).clip(CircleShape).background(colors.inkFaint))
                    Text("%d:%02d".format(message.seconds / 60, message.seconds % 60), style = MaterialTheme.typography.labelSmall, color = colors.ink)
                }
            } else {
                Text(message.text, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = TwinTheme.spacing.xs, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.sentAt)), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
            if (mine) {
                when (message.status) {
                    GameChatStatus.Sending -> Text(stringResource(R.string.games_comms_sending), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
                    GameChatStatus.Sent -> Text("✓", style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
                    GameChatStatus.Failed -> Text(
                        stringResource(R.string.games_comms_retry),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.error,
                        modifier = Modifier.clickable(onClick = onRetry).semantics { role = Role.Button },
                    )
                }
            }
        }
    }
}

@Composable
private fun Composer(draft: String, onDraftChange: (String) -> Unit, onSend: () -> Unit, onMic: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = FIELD_HEIGHT)
                .clip(TwinTheme.shapes.pill)
                .background(colors.glass)
                .padding(horizontal = TwinTheme.spacing.l),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (draft.isEmpty()) {
                Text(stringResource(R.string.games_comms_input_hint), style = MaterialTheme.typography.bodyMedium, color = colors.inkFaint)
            }
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        RoundIcon(Icons.Rounded.Mic, stringResource(R.string.games_comms_mic), colors.glass, onClick = onMic)
        RoundIcon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.games_comms_send), colors.brand, onClick = onSend)
    }
}

/** Shown instead of the composer while recording: cancel (bin), time, send. */
@Composable
private fun RecordingBar(seconds: Int, onCancel: () -> Unit, onSend: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        RoundIcon(Icons.Rounded.Delete, stringResource(R.string.games_comms_cancel_rec), colors.glass, onClick = onCancel)
        Box(Modifier.size(11.dp).clip(CircleShape).background(colors.error))
        Text("%d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        Text(stringResource(R.string.games_comms_recording), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, modifier = Modifier.weight(1f))
        RoundIcon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.games_comms_send_voice), colors.brand, onClick = onSend)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, description: String, fill: Color, small: Boolean = false, onClick: () -> Unit) {
    val size = if (small) SMALL_BUTTON else TwinTheme.shapes.minTouch
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = TwinTheme.colors.ink, modifier = Modifier.size(if (small) 16.dp else 22.dp))
    }
}

private val SHEET_MIN_HEIGHT = 280.dp
private val SHEET_MAX_HEIGHT = 480.dp
private val SMALL_BUTTON = 40.dp
private val FIELD_HEIGHT = 44.dp
private val BUBBLE_MAX_WIDTH = 280.dp
private val BUBBLE_RADIUS = 17.dp
private val BUBBLE_TAIL = 5.dp
private val WAVE_WIDTH = 96.dp
