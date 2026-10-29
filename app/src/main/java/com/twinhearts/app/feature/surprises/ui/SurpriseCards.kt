package com.twinhearts.app.feature.surprises.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.surprises.domain.SurpriseView
import com.twinhearts.app.feature.surprises.domain.isLocked
import com.twinhearts.app.feature.surprises.domain.model.Capsule
import com.twinhearts.app.feature.surprises.domain.model.Surprise
import com.twinhearts.app.feature.surprises.domain.viewOf
import java.time.LocalDate

/** One row of the Surprises list. Mirrors `renderSurprises()` in the web app. */
@Composable
fun SurpriseCard(
    surprise: Surprise,
    today: LocalDate,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val view = viewOf(surprise, today)
    ItemBox(modifier) {
        when (view) {
            // The partner's locked surprise: no title, no content, and no delete (the web hides them too).
            SurpriseView.PartnerLocked -> {
                StatusLine(Icons.Rounded.Lock, stringResource(R.string.surprises_status_locked), null)
                Text(
                    text = stringResource(R.string.surprises_partner_locked, formatDate(surprise.unlock)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                )
            }
            SurpriseView.MineLocked, SurpriseView.Unlocked -> {
                val unlocked = view == SurpriseView.Unlocked
                StatusLine(
                    icon = if (unlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                    text = stringResource(
                        when {
                            unlocked -> R.string.surprises_status_unlocked
                            else -> R.string.surprises_status_locked_yours
                        },
                    ),
                    onDelete = onDelete,
                )
                Text(
                    text = "${surprise.emoji} ${surprise.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                )
                if (unlocked) {
                    if (surprise.message.isNotBlank()) {
                        Text(surprise.message, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                    }
                    val kind = surprise.mediaKind
                    if (kind != null) SurpriseMedia(surprise.mediaUrl, kind)
                } else {
                    Text(
                        text = stringResource(R.string.surprises_opens, formatDate(surprise.unlock)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted,
                    )
                }
            }
        }
    }
}

/** One row of the Love Capsule list. Mirrors `renderCapsules()` in the web app. */
@Composable
fun CapsuleCard(
    capsule: Capsule,
    today: LocalDate,
    busy: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val locked = isLocked(capsule.unlock, today)
    ItemBox(modifier) {
        StatusLine(
            icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            text = stringResource(if (locked) R.string.surprises_status_locked else R.string.surprises_status_unlockable),
            onDelete = onDelete,
        )
        Text(
            text = stringResource(R.string.surprises_opens, formatDate(capsule.unlock)),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkFaint,
        )
        when {
            locked -> Text(
                stringResource(R.string.surprises_capsule_sealed_text),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
            )
            capsule.opened -> Reveal {
                Text(capsule.message, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            }
            else -> {
                Text(
                    stringResource(R.string.surprises_capsule_tap_to_open),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                )
                GlassButton(
                    text = "💌 " + stringResource(R.string.surprises_capsule_open),
                    onClick = onOpen,
                    enabled = !busy,
                )
            }
        }
    }
}

/** The glass box around one list item. */
@Composable
private fun ItemBox(modifier: Modifier, content: @Composable () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, shape)
            .padding(TwinTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        content()
    }
}

/** "🔒 Locked" line, with the delete button at the end when [onDelete] is given. */
@Composable
private fun StatusLine(icon: ImageVector, text: String, onDelete: (() -> Unit)?) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = colors.accent, modifier = Modifier.weight(1f))
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, stringResource(R.string.surprises_a11y_delete), tint = colors.inkMuted)
            }
        }
    }
}

/** Fades and grows a just-opened capsule message into place. Skipped when the phone asks for less motion. */
@Composable
private fun Reveal(content: @Composable () -> Unit) {
    val reduce = rememberReduceMotion()
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) progress.animateTo(1f, tween(durationMillis = 450))
    }
    Column(
        modifier = Modifier.graphicsLayer {
            alpha = progress.value
            val scale = 0.94f + 0.06f * progress.value
            scaleX = scale
            scaleY = scale
        },
    ) { content() }
}
