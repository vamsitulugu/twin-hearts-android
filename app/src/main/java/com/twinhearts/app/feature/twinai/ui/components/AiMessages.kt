package com.twinhearts.app.feature.twinai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.twinai.domain.model.AiMessage
import com.twinhearts.app.feature.twinai.domain.model.AiMessageState
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private fun AiMessage.timeText(): String = timeFormat.format(time.atZone(ZoneId.systemDefault()))

/** My question: a burgundy bubble on the right with the time and double tick, as on the web. */
@Composable
fun MyBubble(message: AiMessage, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Row(modifier.popIn(message.time).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                .background(colors.brand)
                .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
            horizontalAlignment = Alignment.End,
        ) {
            Text(message.text, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                Text(message.timeText(), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
                Icon(Icons.Rounded.DoneAll, contentDescription = null, tint = colors.pinkSoft, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** Twin's answer: round avatar, a glass bubble, and the Copy / Regenerate / Listen row once finished. */
@Composable
fun TwinBubble(
    message: AiMessage,
    speakingId: String?,
    onRetry: () -> Unit,
    onRegenerate: () -> Unit,
    onListen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1_500); copied = false } }

    Row(modifier.popIn(message.time).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        TwinAvatar(size = 32.dp)
        Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
            val failed = message.state == AiMessageState.Failed
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
                    .background(if (failed) colors.error.copy(alpha = 0.14f) else colors.glass)
                    .border(1.dp, if (failed) colors.error.copy(alpha = 0.4f) else colors.lineSoft, RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
                    .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
            ) {
                when {
                    message.state == AiMessageState.Streaming && message.text.isEmpty() -> ThinkingDots()
                    failed && message.text.isEmpty() -> Text(
                        (message.error ?: com.twinhearts.app.core.common.AppError.Unknown).asText(),
                        style = MaterialTheme.typography.bodyMedium, color = colors.error,
                    )
                    else -> {
                        AiMarkdownText(message.text)
                        if (message.state == AiMessageState.Streaming) StreamCaret()
                    }
                }
            }
            if (failed) {
                ToolbarLink(stringResource(R.string.twinai_retry), onRetry)
            }
            if (message.state == AiMessageState.Done) {
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                    ToolbarLink(stringResource(if (copied) R.string.twinai_copied else R.string.twinai_copy)) {
                        clipboard.setText(AnnotatedString(message.text)); copied = true
                    }
                    ToolbarLink(stringResource(R.string.twinai_regenerate), onRegenerate)
                    ToolbarLink(stringResource(if (speakingId == message.id) R.string.twinai_stop else R.string.twinai_listen), onListen)
                }
                Text(message.timeText(), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
            }
        }
    }
}

@Composable
private fun ToolbarLink(text: String, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
        color = colors.inkMuted,
        modifier = Modifier
            .clip(TwinTheme.shapes.pill)
            .background(colors.glass)
            .clickable(onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
    )
}

/** "Thinking..." with three bouncing dots; the dots stand still when animations are off (R-55). */
@Composable
private fun ThinkingDots() {
    val colors = TwinTheme.colors
    val reduce = rememberReduceMotion()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { index ->
                val lift = if (reduce) {
                    0f
                } else {
                    val t = rememberInfiniteTransition(label = "dot$index")
                    val v by t.animateFloat(
                        0f, 1f,
                        infiniteRepeatable(keyframes { durationMillis = 900; 0f at (index * 150); 1f at (index * 150 + 300); 0f at (index * 150 + 600) }, RepeatMode.Restart),
                        label = "dotLift",
                    )
                    v
                }
                Row(
                    Modifier
                        .size(8.dp)
                        .graphicsLayer { translationY = -6f * lift }
                        .alpha(0.5f + 0.5f * lift)
                        .clip(CircleShape)
                        .background(colors.accent),
                ) {}
            }
        }
        Text(stringResource(R.string.twinai_thinking), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
    }
}
