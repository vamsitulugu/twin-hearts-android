package com.twinhearts.app.feature.help.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.help.domain.formatClock
import com.twinhearts.app.feature.help.domain.model.SenderKind
import com.twinhearts.app.feature.help.domain.model.SupportMessage
import com.twinhearts.app.feature.help.domain.model.SupportMode
import com.twinhearts.app.feature.help.ui.ChatLink
import com.twinhearts.app.feature.help.ui.ChatState
import java.time.ZoneId
import java.util.Locale

private val AVATAR_SIZE = 32.dp

/**
 * "Live Support & AI Chat" tab (web `#viewChat`): the AI / Live Agent toggle, the welcome card
 * with quick questions, the thread, the typing line and the input bar. The thread shows a
 * skeleton while connecting and an error with retry when it could not connect (R-41).
 */
@Composable
internal fun HelpChatView(
    chat: ChatState,
    myName: String,
    onModeSelect: (SupportMode) -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val showSkeleton = chat.link == ChatLink.Connecting && chat.messages.isEmpty()
    val showError = chat.link is ChatLink.Failed

    // Keep the newest line in view: welcome card + (skeleton or error) + messages + typing line.
    val itemCount = 1 + (if (showSkeleton || showError) 1 else 0) + chat.messages.size + (if (chat.sending) 1 else 0)
    LaunchedEffect(chat.messages.size, chat.sending) {
        if (chat.messages.isNotEmpty() || chat.sending) listState.animateScrollToItem(itemCount - 1)
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        ChatHeader(mode = chat.mode, myName = myName, onModeSelect = onModeSelect)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(TwinTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            item { WelcomeCard(onQuick = onQuick) }
            if (showSkeleton) item { LoadingSkeleton(lines = 3) }
            (chat.link as? ChatLink.Failed)?.let { failed ->
                item { ErrorState(message = failed.error.asText(), onRetry = onRetry) }
            }
            items(chat.messages) { message -> MessageRow(message = message, myName = myName) }
            if (chat.sending) item { TypingLine(mode = chat.mode) }
        }
        InputBar(
            draft = chat.draft,
            mode = chat.mode,
            sending = chat.sending,
            onDraftChange = onDraftChange,
            onSend = onSend,
        )
    }
}

@Composable
private fun ChatHeader(mode: SupportMode, myName: String, onModeSelect: (SupportMode) -> Unit) {
    val colors = TwinTheme.colors
    val options = listOf(stringResource(R.string.help_mode_ai), stringResource(R.string.help_mode_human))
    val name = myName.ifBlank { stringResource(R.string.help_default_user) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.glass)
            .bottomBorder(colors.line)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HelpPillTabs(
            options = options,
            selectedIndex = if (mode == SupportMode.Ai) 0 else 1,
            onSelect = { index -> onModeSelect(if (index == 0) SupportMode.Ai else SupportMode.Human) },
            modifier = Modifier.weight(1f),
            compact = true,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(name = name, size = 24.dp)
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 88.dp),
            )
        }
    }
}

/** The first card of the thread (web `.chat-welcome`) with the four quick questions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeCard(onQuick: (String) -> Unit) {
    val colors = TwinTheme.colors
    val quick = listOf(
        stringResource(R.string.help_chip_pairing) to stringResource(R.string.help_q_pairing),
        stringResource(R.string.help_chip_vault) to stringResource(R.string.help_q_vault),
        stringResource(R.string.help_chip_notifications) to stringResource(R.string.help_q_notifications),
        stringResource(R.string.help_chip_human) to stringResource(R.string.help_q_human),
    )
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            Text(text = "💌", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.help_welcome_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.help_welcome_text),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                quick.forEach { (label, message) ->
                    TwinFilterChip(label = label, selected = false, onClick = { onQuick(message) })
                }
            }
        }
    }
}

/** One message: avatar, sender name with a badge, the bubble and the time (web `.msg-row`). */
@Composable
private fun MessageRow(message: SupportMessage, myName: String) {
    val isUser = message.sender == SenderKind.User
    val senderName = when (message.sender) {
        SenderKind.Ai -> stringResource(R.string.help_sender_ai)
        SenderKind.Admin -> message.senderName.ifBlank { stringResource(R.string.help_sender_admin) }
        SenderKind.User -> message.senderName.ifBlank { stringResource(R.string.help_sender_user) }
    }
    val time = remember(message.createdAt) {
        message.createdAt?.let { formatClock(it, ZoneId.systemDefault(), Locale.getDefault()) }.orEmpty()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.86f),
            horizontalArrangement = Arrangement.spacedBy(10.dp, if (isUser) Alignment.End else Alignment.Start),
            verticalAlignment = Alignment.Top,
        ) {
            if (!isUser) SenderAvatar(message.sender, myName)
            Column(
                modifier = Modifier.weight(1f, fill = false),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = senderName, style = MaterialTheme.typography.labelMedium, color = TwinTheme.colors.inkMuted)
                    SenderBadge(message.sender)
                }
                Bubble(message)
                if (time.isNotEmpty()) {
                    Text(text = time, style = MaterialTheme.typography.bodySmall, color = TwinTheme.colors.inkFaint)
                }
            }
            if (isUser) SenderAvatar(message.sender, myName)
        }
    }
}

@Composable
private fun SenderAvatar(sender: SenderKind, myName: String) {
    val colors = TwinTheme.colors
    when (sender) {
        SenderKind.User -> Avatar(name = myName.ifBlank { stringResource(R.string.help_default_user) }, size = AVATAR_SIZE)
        SenderKind.Ai -> AvatarCircle(
            emoji = "🤖",
            fill = SolidColor(colors.success.copy(alpha = 0.30f)),
        )
        SenderKind.Admin -> AvatarCircle(emoji = "🛡️", fill = colors.primaryBrush)
    }
}

@Composable
private fun AvatarCircle(emoji: String, fill: Brush) {
    Box(
        modifier = Modifier
            .size(AVATAR_SIZE)
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
    }
}

/** "AI Response" / "Care Agent" tag next to the sender name. My own messages have none. */
@Composable
private fun SenderBadge(sender: SenderKind) {
    val colors = TwinTheme.colors
    val label = when (sender) {
        SenderKind.Ai -> R.string.help_badge_ai
        SenderKind.Admin -> R.string.help_badge_admin
        SenderKind.User -> return
    }
    val isAi = sender == SenderKind.Ai
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelSmall,
        color = if (isAi) colors.success else colors.pinkSoft,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isAi) colors.success.copy(alpha = 0.25f) else colors.primaryStart.copy(alpha = 0.35f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

/** The bubble: pink for me, glass for the AI, card colour with an accent border for an agent. */
@Composable
private fun Bubble(message: SupportMessage) {
    val colors = TwinTheme.colors
    val isUser = message.sender == SenderKind.User
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
    )
    val base = Modifier.clip(shape)
    val styled = when (message.sender) {
        SenderKind.User -> base.background(colors.primaryBrush)
        SenderKind.Ai -> base.background(colors.glass).border(1.dp, colors.line, shape)
        SenderKind.Admin -> base.background(colors.card).border(1.5.dp, colors.accent, shape)
    }
    SelectionContainer {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
            modifier = styled.padding(horizontal = 15.dp, vertical = TwinTheme.spacing.m),
        )
    }
}

/** "Twin Care AI is drafting an answer..." with three pulsing dots (web `.typing-indicator`). */
@Composable
private fun TypingLine(mode: SupportMode) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    val textRes = if (mode == SupportMode.Ai) R.string.help_typing_ai else R.string.help_typing_human
    Row(
        modifier = Modifier
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 14.dp, vertical = TwinTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypingDots()
        Text(text = stringResource(textRes), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
    }
}

@Composable
private fun TypingDots() {
    val colors = TwinTheme.colors
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "helpTypingDots")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val pulse = if (reduceMotion) {
                null
            } else {
                transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(600, delayMillis = index * 150), RepeatMode.Reverse),
                    label = "helpTypingDot$index",
                )
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { alpha = pulse?.value ?: 0.7f }
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
    }
}

/** The text box and the round send button (web `.chat-bar`). */
@Composable
private fun InputBar(
    draft: String,
    mode: SupportMode,
    sending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    val placeholder = stringResource(if (mode == SupportMode.Ai) R.string.help_input_ai else R.string.help_input_human)
    val canSend = draft.isNotBlank() && !sending
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.glass)
            .topBorder(colors.line)
            .padding(horizontal = TwinTheme.spacing.m, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            maxLines = 4,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clip(shape)
                        .background(colors.card)
                        .border(1.dp, colors.line, shape)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (draft.isEmpty()) {
                        Text(text = placeholder, style = MaterialTheme.typography.bodyMedium, color = colors.inkFaint)
                    }
                    inner()
                }
            },
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .alpha(if (canSend) 1f else 0.5f)
                .clip(CircleShape)
                .background(colors.primaryBrush)
                .clickable(enabled = canSend, role = Role.Button, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = stringResource(R.string.help_send),
                tint = colors.ink,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
