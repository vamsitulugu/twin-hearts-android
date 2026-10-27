package com.twinhearts.app.feature.twinai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.twinai.domain.model.AiAuthor
import com.twinhearts.app.feature.twinai.domain.model.AiMessage
import com.twinhearts.app.feature.twinai.domain.model.QuickPrompt

/**
 * Why this file exists: the YOU tab body. The hero card ("I'm Twin AI.") stays at the top; the
 * suggestion chips show only until the first question; then the conversation grows below.
 */
@Composable
fun YouContent(
    greetingName: String,
    messages: List<AiMessage>,
    speakingId: String?,
    onPrompt: (QuickPrompt) -> Unit,
    onRetry: (AiMessage) -> Unit,
    onRegenerate: (AiMessage) -> Unit,
    onListen: (AiMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val lastLength = messages.lastOrNull()?.text?.length ?: 0

    // New bubble: jump to it. Growing answer: follow it only while the reader is already at the bottom.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size)
    }
    LaunchedEffect(lastLength) {
        val total = listState.layoutInfo.totalItemsCount
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (messages.isNotEmpty() && lastVisible >= total - 2) listState.scrollToItem(total - 1, Int.MAX_VALUE)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s,
        ),
    ) {
        item(key = "hero") { TwinHero(greetingName, compact = messages.isNotEmpty()) }
        if (messages.isEmpty()) {
            item(key = "chips") { PromptChips(onPrompt) }
        }
        items(messages, key = { it.id }) { message ->
            when (message.author) {
                AiAuthor.Me -> MyBubble(message)
                AiAuthor.Twin -> TwinBubble(
                    message = message,
                    speakingId = speakingId,
                    onRetry = { onRetry(message) },
                    onRegenerate = { onRegenerate(message) },
                    onListen = { onListen(message) },
                )
            }
        }
    }
}

/** "Hey Vamsi, / I'm Twin AI." card with the robot on the right. */
@Composable
private fun TwinHero(name: String, compact: Boolean) {
    val colors = TwinTheme.colors
    GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.l) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                Text(
                    text = if (name.isBlank()) stringResource(R.string.twinai_hero_hi_anon)
                    else stringResource(R.string.twinai_hero_hi_named, name),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.accent,
                )
                Text(
                    text = buildAnnotatedString {
                        append(stringResource(R.string.twinai_hero_title_pre))
                        withStyle(SpanStyle(brush = colors.primaryBrush)) {
                            append(stringResource(R.string.twinai_hero_title_name))
                        }
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = if (compact) 26.sp else 34.sp, lineHeight = if (compact) 32.sp else 40.sp),
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                )
                if (!compact) {
                    Text(
                        stringResource(R.string.twinai_hero_copy),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted,
                    )
                }
            }
            TwinRobot(Modifier.width(if (compact) 96.dp else 150.dp))
        }
    }
}

/** Index of "Write something sweet": the chip the row rests on, like the web carousel. */
private const val CENTER_CHIP = 2
private const val RECENTER_DELAY_MS = 2_000L

@Composable
private fun PromptChips(onPrompt: (QuickPrompt) -> Unit) {
    val colors = TwinTheme.colors
    val listState = rememberLazyListState()

    // Rest with the middle chip centred; after 2 s without touch, glide back to it.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        delay(RECENTER_DELAY_MS)
        listState.centerChip(animate = true)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
        listState.centerChip(animate = false)
    }

    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        items(QuickPrompt.entries.toList(), key = { it.name }) { p ->
            Row(
                Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.glass)
                    .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
                    .clickable(role = Role.Button) { onPrompt(p) }
                    .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                Text(p.emoji, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(p.labelRes()), style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 1)
            }
        }
    }
}

private suspend fun LazyListState.centerChip(animate: Boolean) {
    if (layoutInfo.visibleItemsInfo.none { it.index == CENTER_CHIP }) {
        scrollToItem(CENTER_CHIP)
        snapshotFlow { layoutInfo.visibleItemsInfo.any { it.index == CENTER_CHIP } }.first { it }
    }
    val offset = centerOffset(this)
    if (animate) animateScrollToItem(CENTER_CHIP, offset) else scrollToItem(CENTER_CHIP, offset)
}

/** Scroll offset that puts the middle chip in the middle of the row (negative = move it right). */
private fun centerOffset(state: LazyListState): Int {
    val info = state.layoutInfo
    val chip = info.visibleItemsInfo.firstOrNull { it.index == CENTER_CHIP }?.size ?: return 0
    val viewport = info.viewportEndOffset - info.viewportStartOffset
    return -((viewport - chip) / 2)
}
