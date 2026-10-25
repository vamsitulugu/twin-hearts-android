package com.twinhearts.app.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** Number + caption tile, e.g. "12 / Days together". */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = TwinTheme.colors
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.pinkSoft, modifier = Modifier.size(22.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, color = colors.ink)
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
    }
}

/** Section title ("Us", "Explore Together" ...) with an optional action on the right. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = TwinTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink)
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.titleSmall,
                color = colors.accent,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

/** Round avatar: the photo when [imageUrl] is given, otherwise the first letter of [name]. */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    imageUrl: String? = null,
) {
    val colors = TwinTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.primaryBrush)
            .border(2.dp, colors.background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
        )
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Two overlapping avatars: you and your partner. */
@Composable
fun CoupleAvatars(
    firstName: String,
    secondName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    firstImageUrl: String? = null,
    secondImageUrl: String? = null,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(-(size * 0.28f))) {
        Avatar(firstName, size = size, imageUrl = firstImageUrl)
        Avatar(secondName, size = size, imageUrl = secondImageUrl)
    }
}
