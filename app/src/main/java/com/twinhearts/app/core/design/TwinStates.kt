package com.twinhearts.app.core.design

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.rememberReduceMotion

/** "Nothing here yet" state. Use the web app's wording for [title] and [message] (R-41). */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Rounded.FavoriteBorder,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = TwinTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(TwinTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(TwinTheme.shapes.circle)
                .background(colors.glass)
                .border(1.dp, colors.line, TwinTheme.shapes.circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.pinkSoft, modifier = Modifier.size(32.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = colors.ink, textAlign = TextAlign.Center)
        if (message != null) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            GlassButton(text = actionLabel, onClick = onAction, style = GlassButtonStyle.Secondary)
        }
    }
}

/** Error state with a retry button. Pass text from `AppError.asText()`. */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(TwinTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m, Alignment.CenterVertically),
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(40.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = colors.ink, textAlign = TextAlign.Center)
        GlassButton(text = stringResource(R.string.core_retry), onClick = onRetry, style = GlassButtonStyle.Secondary)
    }
}

/** Placeholder bars while data loads. They pulse softly, or stay still when animations are off. */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    lines: Int = 3,
) {
    val colors = TwinTheme.colors
    val reduceMotion = rememberReduceMotion()
    val pulse = if (reduceMotion) {
        null
    } else {
        rememberInfiniteTransition(label = "skeleton").animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "skeletonAlpha",
        )
    }
    val loadingText = stringResource(R.string.core_loading)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = loadingText }
            .graphicsLayer { alpha = pulse?.value ?: 0.7f },
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        repeat(lines) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == lines - 1) 0.6f else 1f)
                    .height(18.dp)
                    .clip(TwinTheme.shapes.small)
                    .background(colors.glass),
            )
        }
    }
}
