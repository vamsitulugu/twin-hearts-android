package com.twinhearts.app.feature.twinai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.twinai.domain.model.OrbState
import com.twinhearts.app.feature.twinai.domain.model.QuickPrompt

/** The name shown on a chip or in the "+" sheet. Kept here so the screen and the dock share one list. */
fun QuickPrompt.labelRes(): Int = when (this) {
    QuickPrompt.PlanDate -> R.string.twinai_chip_date
    QuickPrompt.Argued -> R.string.twinai_chip_argued
    QuickPrompt.Sweet -> R.string.twinai_chip_sweet
    QuickPrompt.DeepTalk -> R.string.twinai_chip_deep
    QuickPrompt.Surprise -> R.string.twinai_chip_surprise
}

/**
 * Why this file exists: the bottom input bar of the YOU tab: "+" (quick prompts), the text pill with
 * the mic, the send button, and the Twin orb that sits in a notch on top of it.
 */
@Composable
fun TwinAiDock(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit,
    onTogglePlus: () -> Unit,
    quickOpen: Boolean,
    onPrompt: (QuickPrompt) -> Unit,
    sendEnabled: Boolean,
    orb: OrbState,
    modifier: Modifier = Modifier,
    sending: Boolean = false,
) {
    val colors = TwinTheme.colors
    Column(modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.m)) {
        AnimatedVisibility(visible = quickOpen, enter = fadeIn(), exit = fadeOut()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = TwinTheme.spacing.s)
                    .clip(TwinTheme.shapes.card)
                    .background(colors.card)
                    .border(1.dp, colors.line, TwinTheme.shapes.card)
                    .padding(TwinTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
            ) {
                Text(
                    stringResource(R.string.twinai_quick_head),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkFaint,
                    modifier = Modifier.padding(bottom = TwinTheme.spacing.xs),
                )
                QuickPrompt.entries.forEach { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(TwinTheme.shapes.small)
                            .clickable(role = Role.Button) { onPrompt(p) }
                            .padding(vertical = TwinTheme.spacing.s, horizontal = TwinTheme.spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                    ) {
                        Text(p.emoji, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(p.labelRes()), style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = TwinTheme.spacing.s)
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.glass)
                    .border(1.dp, colors.line, TwinTheme.shapes.pill)
                    .padding(TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                RoundAction(
                    description = stringResource(R.string.twinai_plus),
                    onClick = onTogglePlus,
                    modifier = Modifier.graphicsLayer { rotationZ = if (quickOpen) 45f else 0f },
                ) { Icon(Icons.Rounded.Add, null, tint = colors.ink, modifier = Modifier.size(26.dp)) }
                Box(Modifier.weight(1f)) {
                    GlassTextArea(
                        value = input,
                        onValueChange = onInputChange,
                        hint = stringResource(R.string.twinai_input_hint),
                        minHeight = 48.dp,
                        shape = TwinTheme.shapes.pill,
                        trailing = {
                            Icon(
                                Icons.Rounded.Mic,
                                contentDescription = stringResource(R.string.twinai_mic),
                                tint = colors.inkMuted,
                                modifier = Modifier
                                    .size(TwinTheme.shapes.minTouch)
                                    .clip(TwinTheme.shapes.circle)
                                    .clickable(role = Role.Button, onClick = onMic)
                                    .padding(12.dp),
                            )
                        },
                    )
                }
                // Web states: resting 92 %, ready (text typed) 100 % and slightly bigger, sending 60 % with a spinning icon.
                RoundAction(
                    description = stringResource(R.string.twinai_send),
                    onClick = onSend,
                    enabled = sendEnabled,
                    modifier = Modifier.graphicsLayer {
                        val scale = if (sendEnabled) 1.04f else 1f
                        scaleX = scale; scaleY = scale
                    },
                    visualAlpha = when {
                        sending -> 0.6f
                        sendEnabled -> 1f
                        else -> 0.92f
                    },
                ) { SendIcon(sending) }
            }
            TwinOrb(Modifier.offset(y = 0.dp), size = 52.dp, state = orb)
        }
    }
}

@Composable
private fun RoundAction(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    visualAlpha: Float = if (enabled) 1f else 0.5f,
    icon: @Composable () -> Unit,
) {
    val colors = TwinTheme.colors
    Box(
        modifier
            .size(TwinTheme.shapes.minTouch)
            .alpha(visualAlpha)
            .clip(TwinTheme.shapes.circle)
            .background(colors.brand)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { icon() }
}

/** The paper-plane; it spins while Twin is answering, like the web send button. */
@Composable
private fun SendIcon(sending: Boolean) {
    val reduce = rememberReduceMotion()
    val turn = if (sending && !reduce) {
        val t = rememberInfiniteTransition(label = "send")
        val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "sendTurn")
        a
    } else {
        0f
    }
    Icon(
        Icons.AutoMirrored.Rounded.Send, null,
        tint = TwinTheme.colors.ink,
        modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = turn },
    )
}
