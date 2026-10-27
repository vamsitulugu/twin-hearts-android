package com.twinhearts.app.feature.chat.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.chat.domain.AgoAmount
import com.twinhearts.app.feature.chat.domain.ChatItem
import com.twinhearts.app.feature.chat.domain.DayLabel
import com.twinhearts.app.feature.chat.domain.formatClock
import com.twinhearts.app.feature.chat.domain.formatDuration
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.DeliveryState
import com.twinhearts.app.feature.chat.domain.model.MessageKind

private val BubbleMaxWidth = 300.dp
private val BubbleCorner = 18.dp
private val BubbleTail = 6.dp
private val TickSize = 15.dp
private val PlaySize = 36.dp
private val VoiceBarHeight = 3.dp
private val VoiceBarWidth = 110.dp
private val ImageMinWidth = 200.dp
private val QuoteBar = 3.dp

/** Short text for a message in the reply quote, the reply bar and search lists. */
@Composable
internal fun ChatMessage.previewText(): String = when {
    deleted -> stringResource(R.string.chat_deleted)
    viewOncePending -> stringResource(R.string.chat_view_once)
    !text.isNullOrBlank() -> text
    else -> when (kind) {
        MessageKind.Image, MessageKind.ImageGroup -> stringResource(R.string.chat_preview_photo)
        MessageKind.Video -> stringResource(R.string.chat_preview_video)
        MessageKind.Voice, MessageKind.Audio -> stringResource(R.string.chat_preview_voice)
        MessageKind.File -> fileName ?: stringResource(R.string.chat_preview_file)
        MessageKind.Gif -> stringResource(R.string.chat_preview_gif)
        MessageKind.Sticker -> stringResource(R.string.chat_preview_sticker)
        else -> stringResource(R.string.chat_preview_other)
    }
}

@Composable
internal fun AgoAmount.asText(): String = when (this) {
    AgoAmount.JustNow -> stringResource(R.string.chat_last_seen_now)
    is AgoAmount.Minutes -> stringResource(R.string.chat_last_seen, stringResource(R.string.chat_ago_minutes, value))
    is AgoAmount.Hours -> stringResource(R.string.chat_last_seen, stringResource(R.string.chat_ago_hours, value))
    is AgoAmount.Days -> stringResource(R.string.chat_last_seen, stringResource(R.string.chat_ago_days, value))
}

/** The conversation. Newest message at the bottom; it jumps down when a new one arrives. */
@Composable
internal fun MessageList(
    items: List<ChatItem>,
    allMessages: List<ChatMessage>,
    partnerName: String,
    voice: VoicePlayerState,
    onReply: (ChatMessage) -> Unit,
    onCopy: (ChatMessage) -> Unit,
    onStar: (ChatMessage) -> Unit,
    onRetry: (ChatMessage) -> Unit,
    onVoiceFailed: () -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit,
    onReact: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byId = remember(allMessages) { allMessages.filter { it.id != ChatMessage.LOCAL_ID }.associateBy { it.id } }
    val listState = rememberLazyListState()
    val newest = items.lastOrNull()?.key
    // reverseLayout: index 0 is the newest item, so "scroll to 0" means "show the latest".
    LaunchedEffect(newest) { if (listState.firstVisibleItemIndex <= 2) listState.animateScrollToItem(0) }
    LazyColumn(
        modifier = modifier,
        state = listState,
        reverseLayout = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s,
        ),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
    ) {
        items(items.asReversed(), key = { it.key }) { item ->
            when (item) {
                is ChatItem.Day -> DayPill(item.label)
                is ChatItem.Msg -> MessageBubble(
                    message = item.message,
                    quoted = item.message.replyToId?.let { byId[it] },
                    partnerName = partnerName,
                    voice = voice,
                    onReply = { onReply(item.message) },
                    onCopy = { onCopy(item.message) },
                    onStar = { onStar(item.message) },
                    onRetry = { onRetry(item.message) },
                    onVoiceFailed = onVoiceFailed,
                    onEdit = { onEdit(item.message) },
                    onDelete = { onDelete(item.message) },
                    onReact = { onReact(item.message) },
                )
            }
        }
    }
}

@Composable
private fun DayPill(label: DayLabel) {
    val colors = TwinTheme.colors
    val text = when (label) {
        DayLabel.Today -> stringResource(R.string.chat_day_today)
        DayLabel.Yesterday -> stringResource(R.string.chat_day_yesterday)
        is DayLabel.Date -> label.text
    }
    Box(Modifier.fillMaxWidth().padding(vertical = TwinTheme.spacing.s), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colors.inkMuted,
            modifier = Modifier
                .clip(TwinTheme.shapes.pill)
                .background(colors.glass)
                .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
                .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    quoted: ChatMessage?,
    partnerName: String,
    voice: VoicePlayerState,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onStar: () -> Unit,
    onRetry: () -> Unit,
    onVoiceFailed: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReact: () -> Unit,
) {
    val colors = TwinTheme.colors
    var menu by remember { mutableStateOf(false) }
    val mine = message.mine
    val shape = RoundedCornerShape(
        topStart = BubbleCorner, topEnd = BubbleCorner,
        bottomStart = if (mine) BubbleCorner else BubbleTail,
        bottomEnd = if (mine) BubbleTail else BubbleCorner,
    )
    val failed = message.delivery == DeliveryState.Failed
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Box {
            Column(
                modifier = Modifier
                    .widthIn(max = BubbleMaxWidth)
                    .clip(shape)
                    .then(if (mine) Modifier.background(colors.primaryBrush) else Modifier.background(colors.neutralGlass.copy(alpha = 0.45f)))
                    .border(1.dp, if (failed) colors.error else colors.lineSoft, shape)
                    .combinedClickable(
                        onClick = { if (failed) onRetry() },
                        onLongClick = { if (!message.deleted) menu = true },
                    )
                    .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
            ) {
                if (message.replyToId != null) {
                    ReplyQuote(quoted = quoted, partnerName = partnerName)
                }
                BubbleBody(message, voice, onVoiceFailed)
                if (message.reactions.isNotEmpty()) {
                    ReactionsRow(message.reactions)
                }
                MetaRow(message)
                if (failed) {
                    Text(
                        stringResource(R.string.chat_failed_retry),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.error,
                    )
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.chat_menu_reply)) }, onClick = { menu = false; onReply() })
                if (!message.text.isNullOrBlank()) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.chat_menu_copy)) }, onClick = { menu = false; onCopy() })
                }
                if (message.mine && !message.deleted) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_menu_edit)) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp)) },
                        onClick = { menu = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_menu_delete)) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = colors.error, modifier = Modifier.size(18.dp)) },
                        onClick = { menu = false; onDelete() },
                    )
                }
                if (message.id != ChatMessage.LOCAL_ID) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (message.starred) R.string.chat_menu_unstar else R.string.chat_menu_star)) },
                        onClick = { menu = false; onStar() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_menu_react)) },
                        onClick = { menu = false; onReact() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplyQuote(quoted: ChatMessage?, partnerName: String) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    Row(
        modifier = Modifier
            .padding(bottom = TwinTheme.spacing.xs)
            .clip(shape)
            .background(colors.background.copy(alpha = 0.35f))
            .height(IntrinsicSize.Min),
    ) {
        Box(Modifier.width(QuoteBar).fillMaxHeight().background(colors.pinkSoft))
        Column(Modifier.padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs)) {
            Text(
                text = if (quoted == null) "" else if (quoted.mine) stringResource(R.string.chat_you) else partnerName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.ink,
            )
            Text(
                text = quoted?.previewText().orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BubbleBody(message: ChatMessage, voice: VoicePlayerState, onVoiceFailed: () -> Unit) {
    val colors = TwinTheme.colors
    when {
        message.deleted -> Text(
            stringResource(R.string.chat_deleted),
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = colors.inkMuted,
        )
        message.viewOncePending -> Text(stringResource(R.string.chat_view_once), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
        message.kind == MessageKind.Voice || message.kind == MessageKind.Audio -> VoiceRow(message, voice, onVoiceFailed)
        message.kind in setOf(MessageKind.Image, MessageKind.ImageGroup, MessageKind.Gif, MessageKind.Sticker) &&
            (message.mediaUrl != null || message.groupUrls.isNotEmpty()) -> {
            AsyncImage(
                model = message.groupUrls.firstOrNull() ?: message.mediaUrl,
                contentDescription = stringResource(R.string.chat_preview_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .widthIn(min = ImageMinWidth)
                    .aspectRatio(IMAGE_RATIO)
                    .clip(TwinTheme.shapes.small),
            )
            if (!message.text.isNullOrBlank()) BodyText(message.text)
        }
        !message.text.isNullOrBlank() -> BodyText(message.text)
        else -> Text(message.previewText(), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
    }
}

private const val IMAGE_RATIO = 1.3f

@Composable
private fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = TwinTheme.colors.ink)
}

@Composable
private fun VoiceRow(message: ChatMessage, voice: VoicePlayerState, onFailed: () -> Unit) {
    val colors = TwinTheme.colors
    val url = message.mediaUrl
    val playing = url != null && voice.playing == url
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        Icon(
            imageVector = if (playing) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
            contentDescription = stringResource(if (playing) R.string.chat_voice_pause else R.string.chat_voice_play),
            tint = colors.ink,
            modifier = Modifier
                .size(PlaySize)
                .clip(TwinTheme.shapes.circle)
                .clickable(enabled = url != null) { url?.let { voice.toggle(it, onFailed) } },
        )
        Box(
            Modifier
                .width(VoiceBarWidth)
                .height(VoiceBarHeight)
                .clip(TwinTheme.shapes.pill)
                .background(if (playing) colors.pinkSoft else colors.inkFaint),
        )
        Text(formatDuration(message.durationSec), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
    }
}

/** Reaction emojis shown under a message bubble. */
@Composable
private fun ReactionsRow(reactions: List<String>) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        reactions.forEach { emoji ->
            Box(
                modifier = Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.glass)
                    .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
                    .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink,
                )
            }
        }
    }
}

/** Time, "edited", star and (my messages only) the ticks. */
@Composable
private fun MetaRow(message: ChatMessage) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (message.starred) Icon(Icons.Rounded.Star, null, tint = colors.pinkSoft, modifier = Modifier.size(TickSize - 3.dp))
        if (message.edited) Text(stringResource(R.string.chat_edited), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
        Text(formatClock(message.createdAt), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted, textAlign = TextAlign.End)
        if (message.mine) Ticks(message.delivery)
    }
}

@Composable
private fun Ticks(state: DeliveryState) {
    val colors = TwinTheme.colors
    val (icon, tint) = when (state) {
        DeliveryState.Sending -> Icons.Rounded.AccessTime to colors.inkFaint
        DeliveryState.Failed -> Icons.Rounded.ErrorOutline to colors.error
        DeliveryState.Sent -> Icons.Rounded.Done to colors.inkMuted
        DeliveryState.Delivered -> Icons.Rounded.DoneAll to colors.inkMuted
        DeliveryState.Read -> Icons.Rounded.DoneAll to colors.ink
    }
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(TickSize))
}
