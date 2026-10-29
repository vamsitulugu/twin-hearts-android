package com.twinhearts.app.feature.chat.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.chat.domain.EmojiCategories
import com.twinhearts.app.feature.chat.domain.formatTimer
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.SendEffect
import com.twinhearts.app.feature.chat.domain.searchEmoji
import kotlin.math.roundToInt

/**
 * Why this file exists: the bottom of the Chat screen: the reply / edit bars, the message box
 * ( "+" | text | camera, then the round mic / send button, like the web), the emoji picker and the
 * celebration animation for "send with effect".
 */

private val ComposerMinHeight = 48.dp
private val EmojiCell = 44.dp
private val EffectFall = 700.dp
private const val EFFECT_MS = 2_200
private const val EFFECT_PIECES = 18
private const val EMOJI_PANEL_COLUMNS = 8
private const val RECENT_TAB = 0

@Composable
internal fun ReplyBar(message: ChatMessage, partnerName: String, onCancel: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatStyle.Bar)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.chat_replying_to, if (message.mine) stringResource(R.string.chat_you) else partnerName),
                style = MaterialTheme.typography.labelMedium,
                color = colors.pinkSoft,
            )
            Text(message.previewText(), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, stringResource(R.string.chat_cancel_reply), tint = colors.ink) }
    }
}

@Composable
internal fun EditBar(message: ChatMessage, onCancel: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatStyle.Bar)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Icon(Icons.Rounded.Edit, null, tint = colors.pinkSoft)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.chat_editing), style = MaterialTheme.typography.labelMedium, color = colors.pinkSoft)
            Text(message.previewText(), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, stringResource(R.string.chat_cancel_edit), tint = colors.ink) }
    }
}

/** What the composer asks the screen to do. */
internal class ComposerActions(
    val onTextChange: (String) -> Unit,
    val onSend: (SendEffect?) -> Unit,
    val onSchedule: () -> Unit,
    val onAttach: () -> Unit,
    val onCamera: () -> Unit,
    val onStartRecording: () -> Unit,
    val onFinishRecording: (Boolean) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Composer(text: String, recordingSeconds: Int?, editing: Boolean, actions: ComposerActions) {
    val colors = TwinTheme.colors
    var sendMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Same grey glass bar as the chat header on top; the background is drawn first so it
            // also covers the area behind the gesture bar.
            .background(ChatStyle.Bar)
            .border(1.dp, colors.lineSoft)
            .navigationBarsPadding()
            .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        if (recordingSeconds != null) {
            IconButton(onClick = { actions.onFinishRecording(false) }) {
                Icon(Icons.Rounded.Close, stringResource(R.string.chat_recording_cancel), tint = colors.error)
            }
            Text(
                text = "${stringResource(R.string.chat_recording_label)}  ${formatTimer(recordingSeconds)}",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            RoundAction(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.chat_recording_send), onClick = { actions.onFinishRecording(true) })
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = ComposerMinHeight)
                    .clip(TwinTheme.shapes.pill)
                    .background(ChatStyle.Field)
                    .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = actions.onAttach) { Icon(Icons.Rounded.Add, stringResource(R.string.chat_attach), tint = colors.inkMuted) }
                Box(Modifier.weight(1f).padding(vertical = TwinTheme.spacing.s), contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        Text(stringResource(R.string.chat_hint), style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = actions.onTextChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
                        cursorBrush = SolidColor(colors.pinkSoft),
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (text.isBlank()) {
                    IconButton(onClick = actions.onCamera) { Icon(Icons.Rounded.CameraAlt, stringResource(R.string.chat_camera), tint = colors.inkMuted) }
                }
            }
            if (text.isBlank()) {
                RoundAction(Icons.Rounded.Mic, stringResource(R.string.chat_record), onClick = actions.onStartRecording)
            } else {
                Box {
                    RoundAction(
                        icon = Icons.AutoMirrored.Rounded.Send,
                        description = stringResource(R.string.chat_send),
                        onClick = { actions.onSend(null) },
                        onLongClick = if (editing) null else ({ sendMenu = true }),
                    )
                    DropdownMenu(expanded = sendMenu, onDismissRequest = { sendMenu = false }) {
                        SendEffect.entries.forEach { effect ->
                            DropdownMenuItem(
                                text = { Text("${effect.symbols.first()}  ${stringResource(effect.label())}") },
                                onClick = { sendMenu = false; actions.onSend(effect) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("⏰  ${stringResource(R.string.chat_send_later)}") },
                            onClick = { sendMenu = false; actions.onSchedule() },
                        )
                    }
                }
            }
        }
    }
}

private fun SendEffect.label(): Int = when (this) {
    SendEffect.Confetti -> R.string.chat_effect_confetti
    SendEffect.Hearts -> R.string.chat_effect_hearts
    SendEffect.Balloons -> R.string.chat_effect_balloons
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoundAction(icon: ImageVector, description: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(TwinTheme.shapes.minTouch)
            .clip(TwinTheme.shapes.circle)
            .background(ChatStyle.SentBubble)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = TwinTheme.colors.ink)
    }
}

// ───────────── emoji picker ─────────────

/** Same tabs, search and "Recent" row as the web picker. [closeOnPick] is true for reactions. */
@Composable
internal fun EmojiPickerSheet(recent: List<String>, closeOnPick: Boolean, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(if (recent.isEmpty()) 1 else RECENT_TAB) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = when {
        query.isNotBlank() -> searchEmoji(query)
        tab == RECENT_TAB -> recent
        else -> EmojiCategories[tab].second
    }
    ChatSheet(onDismiss) {
        val colors = TwinTheme.colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TwinTheme.shapes.pill)
                .background(ChatStyle.Glass)
                .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            Icon(Icons.Rounded.Search, null, tint = colors.inkMuted)
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text(stringResource(R.string.chat_emoji_search), color = colors.inkFaint, style = MaterialTheme.typography.bodyMedium)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.pinkSoft),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            EmojiCategories.forEachIndexed { index, (name, _) ->
                TwinFilterChip(label = emojiTabLabel(name), selected = index == tab && query.isBlank(), onClick = { tab = index; query = "" })
            }
        }
        if (shown.isEmpty()) {
            Text(stringResource(R.string.chat_emoji_empty), color = colors.inkMuted, style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(EmojiCell), modifier = Modifier.heightIn(max = EmojiGridMax)) {
                items(shown) { emoji ->
                    Text(
                        emoji,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .clip(TwinTheme.shapes.small)
                            .clickable { if (closeOnPick) onDismiss(); onPick(emoji) }
                            .padding(TwinTheme.spacing.s),
                    )
                }
            }
        }
    }
}

private val EmojiGridMax = 260.dp

@Composable
private fun emojiTabLabel(name: String): String = when (name) {
    "Recent" -> stringResource(R.string.chat_emoji_recent)
    "Smileys" -> stringResource(R.string.chat_emoji_smileys)
    "Emotions" -> stringResource(R.string.chat_emoji_emotions)
    "Love" -> stringResource(R.string.chat_emoji_love)
    "Gestures" -> stringResource(R.string.chat_emoji_gestures)
    else -> stringResource(R.string.chat_emoji_celebration)
}

// ───────────── send effects ─────────────

/** Emoji that float up for about two seconds. Skipped when the phone has animations turned off (R-55). */
@Composable
internal fun EffectOverlay(effect: SendEffect, onDone: () -> Unit) {
    val reduce = rememberReduceMotion()
    val progress = remember(effect) { Animatable(0f) }
    LaunchedEffect(effect, reduce) {
        if (reduce) {
            onDone()
            return@LaunchedEffect
        }
        progress.animateTo(1f, tween(EFFECT_MS, easing = LinearEasing))
        onDone()
    }
    val rise = with(LocalDensity.current) { EffectFall.toPx() }
    Box(Modifier.fillMaxSize()) {
        repeat(EFFECT_PIECES) { i ->
            val symbol = effect.symbols[i % effect.symbols.size]
            val column = (i * PRIME_X) % PERCENT
            val delay = (i * PRIME_D) % DELAY_STEPS / DELAY_STEPS.toFloat() * HALF
            val t = ((progress.value - delay) / (1f - HALF)).coerceIn(0f, 1f)
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = (column * COLUMN_STEP).dp)
                    .offset { IntOffset(0, -(rise * t).roundToInt()) },
            )
        }
    }
}

private const val PRIME_X = 37
private const val PRIME_D = 13
private const val PERCENT = 100
private const val DELAY_STEPS = 10
private const val HALF = 0.5f
private const val COLUMN_STEP = 3.4f
