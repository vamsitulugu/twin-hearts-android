package com.twinhearts.app.feature.chat.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallMissed
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.chat.domain.AgoAmount
import com.twinhearts.app.feature.chat.domain.ChatItem
import com.twinhearts.app.feature.chat.domain.DayLabel
import com.twinhearts.app.feature.chat.domain.GroupPosition
import com.twinhearts.app.feature.chat.domain.RichPiece
import com.twinhearts.app.feature.chat.domain.fileEmoji
import com.twinhearts.app.feature.chat.domain.fileExtension
import com.twinhearts.app.feature.chat.domain.firstLink
import com.twinhearts.app.feature.chat.domain.formatClock
import com.twinhearts.app.feature.chat.domain.formatDuration
import com.twinhearts.app.feature.chat.domain.formatFileSize
import com.twinhearts.app.feature.chat.domain.linkHost
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.DeliveryState
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import com.twinhearts.app.feature.chat.domain.parseRichText
import com.twinhearts.app.feature.chat.domain.pollVotes
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/*
 * Why this file exists: draws the conversation: day pills, grouped bubbles and every kind of message.
 */

private val BubbleMaxWidth = 300.dp
private val BubbleCorner = 18.dp
private val BubbleFlat = 6.dp
private val BubbleTail = 4.dp
private val TickSize = 18.dp
private val PlaySize = 36.dp
private val WaveBarWidth = 3.dp
private val WaveBarMax = 22.dp
private val WaveBarMin = 5.dp
private val ImageMinWidth = 220.dp
private val QuoteBar = 3.dp
private val SwipeTrigger = 64.dp
private val SwipeMax = 96.dp
private val StickerSize = 56.dp
private val PillShadowGap = 2.dp
private const val IMAGE_RATIO = 1.3f
private const val WAVE_BARS = 26
private const val GROUP_GRID_MAX = 4
private const val BUBBLE_FILL_ALPHA = 0.45f
private const val QUOTE_FILL_ALPHA = 0.35f
private const val JUMP_DISTANCE = 3

/** Short text for a message in the reply quote, the reply bar, search lists and the pinned bar. */
@Composable
internal fun ChatMessage.previewText(): String = when {
    deleted -> stringResource(R.string.chat_deleted)
    viewOncePending || (viewOnce && mediaUrl == null) -> stringResource(R.string.chat_view_once)
    !text.isNullOrBlank() && kind != MessageKind.Poll -> text
    else -> when (kind) {
        MessageKind.Image, MessageKind.ImageGroup -> stringResource(R.string.chat_preview_photo)
        MessageKind.Video -> stringResource(R.string.chat_preview_video)
        MessageKind.Voice, MessageKind.Audio -> stringResource(R.string.chat_preview_voice)
        MessageKind.File -> fileName ?: stringResource(R.string.chat_preview_file)
        MessageKind.Gif -> stringResource(R.string.chat_preview_gif)
        MessageKind.Sticker -> stringResource(R.string.chat_preview_sticker)
        MessageKind.Gift -> stringResource(R.string.chat_preview_gift)
        MessageKind.Location -> stringResource(R.string.chat_preview_location)
        MessageKind.Contact -> stringResource(R.string.chat_preview_contact, label.orEmpty())
        MessageKind.Poll -> stringResource(R.string.chat_preview_poll, text.orEmpty())
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

/** Everything a bubble can ask the screen to do. One object keeps the long lists of lambdas short. */
internal class BubbleActions(
    val onTap: (ChatMessage) -> Unit,
    val onLongPress: (ChatMessage) -> Unit,
    val onReply: (ChatMessage) -> Unit,
    val onRetry: (ChatMessage) -> Unit,
    val onReact: (ChatMessage, String) -> Unit,
    val onOpenImages: (List<String>, Int) -> Unit,
    val onOpenViewOnce: (ChatMessage) -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onOpenLocation: (ChatMessage) -> Unit,
    val onVote: (ChatMessage, String) -> Unit,
    val onJump: (Long) -> Unit,
    val onVoiceFailed: () -> Unit,
) {
    companion object {
        val None = BubbleActions({}, {}, {}, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}, { _, _ -> }, {}, {})
    }
}

/** The conversation. Newest message at the bottom; it jumps down when a new one arrives. */
@Composable
internal fun MessageList(
    items: List<ChatItem>,
    allMessages: List<ChatMessage>,
    partnerName: String,
    voice: VoicePlayerState,
    selected: Set<String>,
    actions: BubbleActions,
    scrollTo: Long?,
    onScrolled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val byId = remember(allMessages) { allMessages.filter { it.id != ChatMessage.LOCAL_ID }.associateBy { it.id } }
    val listState = rememberLazyListState()
    val reversed = remember(items) { items.asReversed() }
    val newest = items.lastOrNull()?.key
    val newestMine = (items.lastOrNull() as? ChatItem.Msg)?.message?.mine == true
    // reverseLayout: index 0 is the newest item, so "scroll to 0" means "show the latest".
    LaunchedEffect(newest) {
        if (newestMine || listState.firstVisibleItemIndex <= JUMP_DISTANCE) listState.animateScrollToItem(0)
    }
    LaunchedEffect(scrollTo) {
        val target = scrollTo ?: return@LaunchedEffect
        val index = reversed.indexOfFirst { it is ChatItem.Msg && it.message.id == target }
        if (index >= 0) listState.animateScrollToItem(index)
        onScrolled()
    }
    val scope = rememberCoroutineScope()
    val away by remember { derivedStateOf { listState.firstVisibleItemIndex > JUMP_DISTANCE } }
    var seenCount by remember { mutableStateOf(allMessages.size) }
    if (!away) seenCount = allMessages.size
    val unseen = if (away) (allMessages.size - seenCount).coerceAtLeast(0) else 0
    Box(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            state = listState,
            reverseLayout = true,
            // Few messages start at the top (like WhatsApp); the newest message is still index 0.
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
        ) {
            items(reversed, key = { it.key }) { item ->
                when (item) {
                    is ChatItem.Day -> DayPill(item.label)
                    is ChatItem.Msg -> MessageBubble(
                        message = item.message,
                        position = item.position,
                        quoted = item.message.replyToId?.let { byId[it] },
                        votes = if (item.message.kind == MessageKind.Poll) pollVotes(item.message, allMessages) else emptyMap(),
                        partnerName = partnerName,
                        voice = voice,
                        selected = item.message.clientId in selected,
                        selecting = selected.isNotEmpty(),
                        actions = actions,
                    )
                }
            }
        }
        if (away) {
            JumpToLatest(unseen, Modifier.align(Alignment.BottomEnd)) { scope.launch { listState.animateScrollToItem(0) } }
        }
    }
}

@Composable
private fun JumpToLatest(unseen: Int, modifier: Modifier, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Box(
        modifier = modifier
            .padding(TwinTheme.spacing.m)
            .size(TwinTheme.shapes.minTouch)
            .clip(TwinTheme.shapes.circle)
            .background(ChatStyle.Glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.circle)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.chat_jump_latest), tint = colors.ink)
        if (unseen > 0) Text(unseen.toString(), style = MaterialTheme.typography.labelSmall, color = colors.pinkSoft)
    }
}

@Composable
internal fun DayPill(label: DayLabel) {
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
                .background(ChatStyle.Glass)
                .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
                .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
        )
    }
}

/** Telegram-style corners: inside a run of messages the side next to the edge is flatter. */
private fun bubbleShape(mine: Boolean, position: GroupPosition): RoundedCornerShape {
    val joinsAbove = position == GroupPosition.Middle || position == GroupPosition.Last
    val joinsBelow = position == GroupPosition.First || position == GroupPosition.Middle
    val edgeTop = if (joinsAbove) BubbleFlat else BubbleCorner
    val edgeBottom = if (joinsBelow) BubbleFlat else BubbleTail
    return if (mine) {
        RoundedCornerShape(topStart = BubbleCorner, topEnd = edgeTop, bottomEnd = edgeBottom, bottomStart = BubbleCorner)
    } else {
        RoundedCornerShape(topStart = edgeTop, topEnd = BubbleCorner, bottomEnd = BubbleCorner, bottomStart = edgeBottom)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    position: GroupPosition,
    quoted: ChatMessage?,
    votes: Map<String, Int>,
    partnerName: String,
    voice: VoicePlayerState,
    selected: Boolean,
    selecting: Boolean,
    actions: BubbleActions,
) {
    val colors = TwinTheme.colors
    val mine = message.mine
    if (message.kind == MessageKind.CallLog) {
        CallLogRow(message)
        return
    }
    val failed = message.delivery == DeliveryState.Failed
    val shape = bubbleShape(mine, position)
    val topGap = if (position == GroupPosition.First || position == GroupPosition.Single) TwinTheme.spacing.s else PillShadowGap
    val triggerPx = with(LocalDensity.current) { SwipeTrigger.toPx() }
    val maxPx = with(LocalDensity.current) { SwipeMax.toPx() }
    var dragX by remember { mutableFloatStateOf(0f) }
    val dragState = rememberDraggableState { delta -> dragX = (dragX + delta).coerceIn(0f, maxPx) }
    val canSwipe = !selecting && !message.deleted && message.id != ChatMessage.LOCAL_ID
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = topGap)
            .then(if (selected) Modifier.background(ChatStyle.Selection) else Modifier)
            .then(
                if (canSwipe) {
                    Modifier.draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        onDragStopped = {
                            if (dragX >= triggerPx) actions.onReply(message)
                            dragX = 0f
                        },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().offset { IntOffset(dragX.roundToInt(), 0) },
            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        ) {
            Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                Column(
                    modifier = Modifier
                        .widthIn(max = BubbleMaxWidth)
                        .clip(shape)
                        .then(
                            if (mine) Modifier.background(ChatStyle.SentBubble)
                            else Modifier.background(colors.neutralGlass.copy(alpha = BUBBLE_FILL_ALPHA)),
                        )
                        .border(1.dp, if (failed) colors.error else colors.lineSoft, shape)
                        .combinedClickable(
                            onClick = { if (selecting) actions.onTap(message) else if (failed) actions.onRetry(message) },
                            onLongClick = { if (!message.deleted) actions.onLongPress(message) },
                        )
                        .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
                ) {
                    if (message.forwarded && !message.deleted) {
                        Text(
                            stringResource(R.string.chat_forwarded),
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = colors.inkMuted,
                        )
                    }
                    if (message.replyToId != null && !message.deleted) {
                        ReplyQuote(quoted = quoted, partnerName = partnerName, onClick = { actions.onJump(message.replyToId) })
                    }
                    BubbleBody(message, votes, voice, actions)
                    MetaRow(message, Modifier.align(if (mine) Alignment.End else Alignment.Start))
                    if (failed) {
                        Text(stringResource(R.string.chat_failed_retry), style = MaterialTheme.typography.labelSmall, color = colors.error)
                    }
                }
                if (message.reactions.isNotEmpty() && !message.deleted) {
                    ReactionPills(message, actions.onReact)
                }
            }
        }
    }
}

/** What the server wrote for a call ("Voice call · 2:31", "Missed voice call" ...), read into parts. */
private enum class CallOutcome { Answered, Missed, NoAnswer, Declined }

private data class CallLogInfo(val video: Boolean, val outcome: CallOutcome, val duration: String?)

private val DurationRegex = Regex("""\d{1,2}:\d{2}(?::\d{2})?""")

private fun parseCallLog(text: String?, mine: Boolean): CallLogInfo {
    val t = text.orEmpty().lowercase()
    val duration = DurationRegex.find(t)?.value?.takeIf { it != "0:00" && it != "00:00" }
    val outcome = when {
        t.contains("declin") -> CallOutcome.Declined
        t.contains("missed") || t.contains("no answer") || t.contains("cancel") || (duration == null && !t.contains("ended")) ->
            // I called and nobody picked up = "No answer"; they called and I did not = "Missed".
            if (mine) CallOutcome.NoAnswer else CallOutcome.Missed
        else -> CallOutcome.Answered
    }
    return CallLogInfo(video = t.contains("video"), outcome = outcome, duration = duration)
}

/** A call in the chat history, like WhatsApp: icon, "Voice/Video call", then picked up (time) or missed. */
@Composable
private fun CallLogRow(message: ChatMessage) {
    val colors = TwinTheme.colors
    val info = remember(message.text, message.mine) { parseCallLog(message.text, message.mine) }
    val accent = when (info.outcome) {
        CallOutcome.Answered -> ChatStyle.CallOk
        CallOutcome.Missed -> ChatStyle.CallMissed
        else -> colors.inkMuted
    }
    val statusText = when (info.outcome) {
        CallOutcome.Answered -> info.duration ?: stringResource(R.string.calls_log_answered)
        CallOutcome.Missed -> stringResource(R.string.calls_log_missed)
        CallOutcome.NoAnswer -> stringResource(R.string.calls_no_answer)
        CallOutcome.Declined -> stringResource(R.string.calls_log_declined)
    }
    val arrow = when {
        message.mine -> Icons.Rounded.CallMade
        info.outcome == CallOutcome.Missed -> Icons.Rounded.CallMissed
        else -> Icons.Rounded.CallReceived
    }
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = TwinTheme.spacing.xs),
        horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start,
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = 200.dp, max = BubbleMaxWidth)
                .clip(shape)
                .background(ChatStyle.Glass)
                .border(1.dp, colors.lineSoft, shape)
                .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Box(
                Modifier.size(40.dp).clip(TwinTheme.shapes.circle).background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (info.video) Icons.Rounded.Videocam else Icons.Rounded.Call, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (info.video) R.string.calls_log_video else R.string.calls_log_voice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(arrow, null, tint = accent, modifier = Modifier.size(14.dp))
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (info.outcome == CallOutcome.Missed) ChatStyle.CallMissed else colors.inkMuted,
                    )
                }
            }
            Text(
                formatClock(message.createdAt),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                color = ChatStyle.MetaText,
                modifier = Modifier.align(Alignment.Bottom),
            )
        }
    }
}

@Composable
private fun ReactionPills(message: ChatMessage, onReact: (ChatMessage, String) -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier.padding(top = TwinTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
    ) {
        message.reactions.forEach { group ->
            Text(
                text = if (group.count > 1) "${group.emoji} ${group.count}" else group.emoji,
                style = MaterialTheme.typography.labelMedium,
                color = colors.ink,
                modifier = Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(if (group.mine) ChatStyle.Selection else ChatStyle.Glass)
                    .border(1.dp, if (group.mine) ChatStyle.MineOutline else colors.lineSoft, TwinTheme.shapes.pill)
                    .clickable { onReact(message, group.emoji) }
                    .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
            )
        }
    }
}

@Composable
private fun ReplyQuote(quoted: ChatMessage?, partnerName: String, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .padding(bottom = TwinTheme.spacing.xs)
            .clip(TwinTheme.shapes.small)
            .background(colors.background.copy(alpha = QUOTE_FILL_ALPHA))
            .clickable(onClick = onClick)
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
                text = quoted?.previewText() ?: stringResource(R.string.chat_quote_missing),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BubbleBody(message: ChatMessage, votes: Map<String, Int>, voice: VoicePlayerState, actions: BubbleActions) {
    val colors = TwinTheme.colors
    when {
        message.deleted -> Text(
            stringResource(R.string.chat_deleted),
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = colors.inkMuted,
        )
        message.viewOnce && message.mediaUrl == null -> ViewOnceTile(message, actions.onOpenViewOnce)
        message.kind == MessageKind.Voice || message.kind == MessageKind.Audio -> VoiceRow(message, voice, actions.onVoiceFailed)
        message.kind == MessageKind.Image || message.kind == MessageKind.ImageGroup || message.kind == MessageKind.Gif ->
            PictureBody(message, actions)
        message.kind == MessageKind.Video -> VideoBody(message, actions.onOpenUrl)
        message.kind == MessageKind.File -> FileCard(message, actions.onOpenUrl)
        message.kind == MessageKind.Sticker || message.kind == MessageKind.Gift -> EmojiCard(message)
        message.kind == MessageKind.Location -> InfoCard(
            icon = "📍",
            title = stringResource(R.string.chat_location_title),
            subtitle = stringResource(R.string.chat_location_open),
            onClick = { actions.onOpenLocation(message) },
        )
        message.kind == MessageKind.Contact -> InfoCard(
            icon = "👤",
            title = message.label.orEmpty(),
            subtitle = stringResource(R.string.chat_contact_title),
            onClick = null,
        )
        message.kind == MessageKind.Poll -> PollCard(message, votes, actions.onVote)
        !message.text.isNullOrBlank() -> {
            BodyText(message.text)
            firstLink(message.text)?.let { LinkCard(it, actions.onOpenUrl) }
        }
        else -> Text(message.previewText(), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
    }
}

/** Text with the web's formatting: *bold*, _italic_, ~strike~, ```mono```, ||spoiler|| and links. */
@Composable
private fun BodyText(text: String) {
    val colors = TwinTheme.colors
    var revealed by remember { mutableStateOf(false) }
    val annotated = remember(text, revealed, colors) { richAnnotated(parseRichText(text), colors.pinkSoft, colors.ink, ChatStyle.Glass, revealed) }
    val hasSpoiler = remember(text) { parseRichText(text).any { it.spoiler } }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
        color = colors.ink,
        modifier = if (hasSpoiler) Modifier.clickable { revealed = !revealed } else Modifier,
    )
}

private fun richAnnotated(pieces: List<RichPiece>, linkColor: Color, ink: Color, monoBg: Color, revealed: Boolean): AnnotatedString =
    buildAnnotatedString {
        pieces.forEach { p ->
            val style = SpanStyle(
                fontWeight = if (p.bold) FontWeight.Bold else null,
                fontStyle = if (p.italic) FontStyle.Italic else null,
                textDecoration = when {
                    p.strike -> TextDecoration.LineThrough
                    p.link != null -> TextDecoration.Underline
                    else -> null
                },
                fontFamily = if (p.mono) FontFamily.Monospace else null,
                background = if (p.mono) monoBg else if (p.spoiler && !revealed) ink else Color.Unspecified,
                color = when {
                    p.spoiler && !revealed -> ink
                    p.link != null -> linkColor
                    else -> Color.Unspecified
                },
            )
            withStyle(style) { append(p.text) }
        }
    }

@Composable
private fun LinkCard(url: String, onOpen: (String) -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .padding(top = TwinTheme.spacing.xs)
            .clip(TwinTheme.shapes.small)
            .background(colors.background.copy(alpha = QUOTE_FILL_ALPHA))
            .clickable { onOpen(url) }
            .padding(TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Text("🔗", style = MaterialTheme.typography.titleMedium)
        Column {
            Text(linkHost(url) ?: url, style = MaterialTheme.typography.labelLarge, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(url, style = MaterialTheme.typography.labelSmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PictureBody(message: ChatMessage, actions: BubbleActions) {
    val urls = message.groupUrls.ifEmpty { listOfNotNull(message.mediaUrl) }
    when {
        urls.isEmpty() -> Text(message.previewText(), style = MaterialTheme.typography.bodyMedium, color = TwinTheme.colors.ink)
        urls.size == 1 -> {
            Box {
                AsyncImage(
                    model = urls.first(),
                    contentDescription = stringResource(R.string.chat_preview_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .widthIn(min = ImageMinWidth)
                        .aspectRatio(IMAGE_RATIO)
                        .clip(TwinTheme.shapes.small)
                        .clickable { actions.onOpenImages(urls, 0) },
                )
                if (message.kind == MessageKind.Gif) Badge(stringResource(R.string.chat_preview_gif), Modifier.align(Alignment.TopStart))
            }
        }
        else -> PhotoGrid(urls) { index -> actions.onOpenImages(urls, index) }
    }
    if (!message.text.isNullOrBlank()) BodyText(message.text)
}

@Composable
private fun Badge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TwinTheme.colors.ink,
        modifier = modifier
            .padding(TwinTheme.spacing.xs)
            .clip(TwinTheme.shapes.pill)
            .background(TwinTheme.colors.scrim)
            .padding(horizontal = TwinTheme.spacing.s, vertical = PillShadowGap),
    )
}

/** Up to four tiles in two columns; the last shows "+N" when there are more pictures. */
@Composable
private fun PhotoGrid(urls: List<String>, onOpen: (Int) -> Unit) {
    val shown = urls.take(GROUP_GRID_MAX)
    val extra = urls.size - shown.size
    Column(Modifier.widthIn(min = ImageMinWidth), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        shown.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                pair.forEachIndexed { col, url ->
                    val index = row * 2 + col
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(TwinTheme.shapes.small).clickable { onOpen(index) }) {
                        AsyncImage(
                            model = url,
                            contentDescription = stringResource(R.string.chat_preview_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        )
                        if (extra > 0 && index == shown.lastIndex) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight().background(TwinTheme.colors.scrim), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.chat_image_more, extra), style = MaterialTheme.typography.titleLarge, color = TwinTheme.colors.ink)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoBody(message: ChatMessage, onOpen: (String) -> Unit) {
    val colors = TwinTheme.colors
    val url = message.mediaUrl
    Box(
        modifier = Modifier
            .widthIn(min = ImageMinWidth)
            .aspectRatio(IMAGE_RATIO)
            .clip(TwinTheme.shapes.small)
            .background(colors.scrim)
            .clickable(enabled = url != null) { url?.let(onOpen) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.PlayCircle, stringResource(R.string.chat_play_video), tint = colors.ink, modifier = Modifier.size(PlaySize + PlaySize))
    }
    if (!message.text.isNullOrBlank()) BodyText(message.text)
}

@Composable
private fun ViewOnceTile(message: ChatMessage, onOpen: (ChatMessage) -> Unit) {
    val colors = TwinTheme.colors
    val opened = message.viewed
    val label = if (message.kind == MessageKind.Video) R.string.chat_view_once_video else R.string.chat_view_once_photo
    Row(
        modifier = Modifier
            .clip(TwinTheme.shapes.small)
            .clickable(enabled = !opened && !message.mine) { onOpen(message) }
            .padding(vertical = TwinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Text("🔒", style = MaterialTheme.typography.titleLarge)
        Column {
            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            Text(
                stringResource(if (opened) R.string.chat_view_once_opened else R.string.chat_view_once_tap),
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkMuted,
            )
        }
    }
}

@Composable
private fun FileCard(message: ChatMessage, onOpen: (String) -> Unit) {
    val colors = TwinTheme.colors
    val url = message.mediaUrl
    Row(
        modifier = Modifier
            .clip(TwinTheme.shapes.small)
            .background(colors.background.copy(alpha = QUOTE_FILL_ALPHA))
            .clickable(enabled = url != null) { url?.let(onOpen) }
            .padding(TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Text(fileEmoji(message.fileName), style = MaterialTheme.typography.headlineSmall)
        Column(Modifier.widthIn(max = BubbleMaxWidth - BubbleMaxWidth / 3)) {
            Text(
                message.fileName ?: stringResource(R.string.chat_preview_file),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOf(fileExtension(message.fileName), formatFileSize(message.fileSize)).filter { it.isNotEmpty() }.joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        }
    }
}

@Composable
private fun EmojiCard(message: ChatMessage) {
    val colors = TwinTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            message.emoji.orEmpty(),
            style = MaterialTheme.typography.displayMedium.copy(fontSize = with(LocalDensity.current) { StickerSize.toSp() }),
        )
        val name = message.label.orEmpty()
        if (name.isNotEmpty()) {
            Text(
                if (message.kind == MessageKind.Gift) stringResource(R.string.chat_gift_label, name) else name,
                style = MaterialTheme.typography.labelLarge,
                color = colors.inkMuted,
            )
        }
    }
}

@Composable
private fun InfoCard(icon: String, title: String, subtitle: String, onClick: (() -> Unit)?) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .clip(TwinTheme.shapes.small)
            .background(colors.background.copy(alpha = QUOTE_FILL_ALPHA))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Text(icon, style = MaterialTheme.typography.headlineSmall)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = colors.pinkSoft)
        }
    }
}

@Composable
private fun PollCard(message: ChatMessage, votes: Map<String, Int>, onVote: (ChatMessage, String) -> Unit) {
    val colors = TwinTheme.colors
    val total = votes.values.sum()
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        Text("📊 ${message.text.orEmpty()}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.ink)
        message.pollOptions.forEach { option ->
            val count = votes[option] ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TwinTheme.shapes.small)
                    .background(colors.background.copy(alpha = QUOTE_FILL_ALPHA))
                    .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
                    .clickable(enabled = message.id != ChatMessage.LOCAL_ID) { onVote(message, option) }
                    .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(option, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
                if (count > 0) Text(count.toString(), style = MaterialTheme.typography.labelLarge, color = colors.pinkSoft)
            }
        }
        Text(stringResource(R.string.chat_poll_total, total), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
    }
}

@Composable
private fun VoiceRow(message: ChatMessage, voice: VoicePlayerState, onFailed: () -> Unit) {
    val colors = TwinTheme.colors
    val url = message.mediaUrl
    val playing = url != null && voice.playing == url
    val bars = remember(message.clientId) { waveBars(message.clientId) }
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PillShadowGap)) {
            bars.forEach { h ->
                Box(
                    Modifier
                        .width(WaveBarWidth)
                        .height(WaveBarMin + (WaveBarMax - WaveBarMin) * h)
                        .clip(TwinTheme.shapes.pill)
                        .background(if (playing) colors.pinkSoft else colors.inkFaint),
                )
            }
        }
        Text(formatDuration(message.durationSec), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        if (message.kind == MessageKind.Voice && playing) {
            Text(
                text = stringResource(R.string.chat_voice_speed, voice.speedLabel),
                style = MaterialTheme.typography.labelMedium,
                color = colors.ink,
                modifier = Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(ChatStyle.Glass)
                    .clickable { voice.cycleSpeed() }
                    .padding(horizontal = TwinTheme.spacing.s, vertical = PillShadowGap),
            )
        }
    }
}

/** A steady, message-specific bar pattern (0..1): the same message always looks the same. */
private fun waveBars(seed: String): List<Float> {
    var h = seed.hashCode()
    return List(WAVE_BARS) {
        h = h * 1_103_515_245 + 12_345
        ((h ushr 16) and 0xFF) / 255f
    }
}

/** Time, "edited", pin, star and (my messages only) the ticks. Plain small text, no extra spacing. */
@Composable
private fun MetaRow(message: ChatMessage, modifier: Modifier = Modifier) {
    val metaStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    )
    Row(
        // No fillMaxWidth: the bubble must be as wide as its text, not always the maximum.
        modifier = modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (message.pinned) Icon(Icons.Rounded.PushPin, null, tint = ChatStyle.MetaText, modifier = Modifier.size(11.dp))
        if (message.starred) Icon(Icons.Rounded.Star, null, tint = ChatStyle.MetaText, modifier = Modifier.size(11.dp))
        if (message.edited) Text(stringResource(R.string.chat_edited), style = metaStyle, color = ChatStyle.MetaText)
        Text(formatClock(message.createdAt), style = metaStyle, color = ChatStyle.MetaText)
        if (message.mine) Ticks(message.delivery)
    }
}

@Composable
private fun Ticks(state: DeliveryState) {
    val colors = TwinTheme.colors
    val (icon, tint) = when (state) {
        DeliveryState.Sending -> Icons.Rounded.AccessTime to colors.inkFaint
        DeliveryState.Failed -> Icons.Rounded.ErrorOutline to colors.error
        // WhatsApp: one white tick = sent (partner offline / no net), two white = delivered (partner
        // online, not seen yet), two blue = seen.
        DeliveryState.Sent -> Icons.Rounded.Done to ChatStyle.TickSent
        DeliveryState.Delivered -> Icons.Rounded.DoneAll to ChatStyle.TickDelivered
        DeliveryState.Read -> Icons.Rounded.DoneAll to ChatStyle.TickSeen
    }
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(TickSize))
}
