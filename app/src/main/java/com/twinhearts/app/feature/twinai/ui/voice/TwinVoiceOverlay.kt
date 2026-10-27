package com.twinhearts.app.feature.twinai.ui.voice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.twinai.ui.components.TwinOrb

/**
 * Why this file exists: the full-screen "voice mode" of the Twin page: big orb, what Twin is doing
 * ("Listening...", "Thinking...", "Speaking..."), the words, and the mic / end buttons.
 * Back, the end button and leaving the app all close it through the controller.
 */
@Composable
fun TwinVoiceOverlay(controller: TwinVoiceController, modifier: Modifier = Modifier) {
    BackHandler(enabled = controller.isOpen) { controller.exit() }
    AnimatedVisibility(visible = controller.isOpen, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        val colors = TwinTheme.colors
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background.copy(alpha = 0.97f))
                // Swallow touches so nothing under the voice screen can be tapped.
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(TwinTheme.spacing.l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(0.6f))
                TwinOrb(size = 160.dp)
                Spacer(Modifier.height(TwinTheme.spacing.xl))
                Text(
                    stringResource(controller.statusRes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                )
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = TwinTheme.spacing.m), contentAlignment = Alignment.TopCenter) {
                    Text(
                        text = buildAnnotatedString {
                            append(controller.finalText)
                            if (controller.finalText.isNotEmpty() && controller.interimText.isNotEmpty()) append(' ')
                            withStyle(SpanStyle(color = colors.inkFaint)) { append(controller.interimText) }
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xl), verticalAlignment = Alignment.CenterVertically) {
                    VoiceButton(
                        description = stringResource(if (controller.muted) R.string.twinai_voice_unmute else R.string.twinai_voice_mute),
                        highlighted = controller.muted,
                        onClick = controller::toggleMute,
                    ) {
                        Icon(
                            if (controller.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            contentDescription = null, tint = colors.ink, modifier = Modifier.size(28.dp),
                        )
                    }
                    VoiceButton(
                        description = stringResource(R.string.twinai_voice_end),
                        highlighted = true,
                        onClick = controller::exit,
                    ) { Icon(Icons.Rounded.Close, contentDescription = null, tint = colors.ink, modifier = Modifier.size(28.dp)) }
                }
                Text(
                    text = controller.hintRes?.let { stringResource(it) }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = TwinTheme.spacing.m).height(20.dp),
                )
            }
        }
    }
}

@Composable
private fun VoiceButton(
    description: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val colors = TwinTheme.colors
    Box(
        Modifier
            .size(64.dp)
            .clip(TwinTheme.shapes.circle)
            .background(if (highlighted) colors.primaryBrush else androidx.compose.ui.graphics.SolidColor(colors.glass))
            .border(1.dp, colors.line, TwinTheme.shapes.circle)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { icon() }
}
