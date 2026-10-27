package com.twinhearts.app.feature.movie.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.movie.domain.model.MovieReaction

/** Why this file exists: the small extras of the player (⋮ menu, reaction bar, floating emoji) kept out of PlayerScreen. */

/** How the picture fills the screen. Same three modes as the web page. */
enum class ScreenFit(val label: Int) {
    Fit(R.string.movie_fit_fit), Fill(R.string.movie_fit_fill), Zoom(R.string.movie_fit_zoom);

    fun next(): ScreenFit = entries[(ordinal + 1) % entries.size]
}

/** Sleep timer steps in minutes (0 = off), same as the web page. */
val sleepSteps: List<Int> = listOf(0, 15, 30, 45, 60)

fun nextSleepStep(current: Int): Int = sleepSteps[(sleepSteps.indexOf(current) + 1) % sleepSteps.size]

/** What the Movie Info row shows. */
data class MovieInfo(val title: String, val lengthText: String, val cloud: Boolean, val sizeText: String?, val subtitles: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMoreSheet(
    info: MovieInfo,
    fit: ScreenFit,
    sleepMinutes: Int,
    keepScreenOn: Boolean,
    pipAvailable: Boolean,
    onSubtitles: () -> Unit,
    onFit: () -> Unit,
    onPip: () -> Unit,
    onSleep: () -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onEnd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    var showInfo by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.card) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = TwinTheme.spacing.l)) {
            Text(
                stringResource(R.string.movie_more_title), color = colors.ink, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
            )
            MoreRow(Icons.Rounded.Info, R.string.movie_more_info, null, { showInfo = !showInfo })
            if (showInfo) InfoBox(info)
            MoreRow(
                Icons.Rounded.ClosedCaption, R.string.movie_more_subtitles,
                stringResource(if (info.subtitles != null) R.string.movie_value_on else R.string.movie_value_off), onSubtitles,
            )
            MoreRow(Icons.Rounded.Fullscreen, R.string.movie_more_fit, stringResource(fit.label), onFit)
            if (pipAvailable) MoreRow(Icons.Rounded.PictureInPictureAlt, R.string.movie_more_pip, null, onPip)
            MoreRow(
                Icons.Rounded.Bedtime, R.string.movie_more_sleep,
                if (sleepMinutes == 0) stringResource(R.string.movie_value_off) else stringResource(R.string.movie_sleep_minutes, sleepMinutes), onSleep,
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.LightMode, null, tint = colors.inkMuted)
                Text(
                    stringResource(R.string.movie_more_awake), color = colors.ink, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).padding(start = TwinTheme.spacing.m),
                )
                Switch(
                    checked = keepScreenOn, onCheckedChange = onKeepScreenOn,
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.accent),
                )
            }
            MoreRow(Icons.Rounded.PowerSettingsNew, R.string.movie_more_end, null, onEnd, danger = true)
        }
    }
}

@Composable
private fun MoreRow(icon: ImageVector, label: Int, value: String?, onClick: () -> Unit, danger: Boolean = false) {
    val colors = TwinTheme.colors
    val tint = if (danger) colors.error else colors.ink
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (danger) colors.error else colors.inkMuted)
        Text(
            stringResource(label), color = tint, style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(start = TwinTheme.spacing.m),
        )
        if (value != null) Text(value, color = colors.inkMuted, style = MaterialTheme.typography.bodyMedium)
        if (!danger) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.inkFaint)
    }
}

@Composable
private fun InfoBox(info: MovieInfo) {
    val colors = TwinTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.screenEdge).background(colors.glass, TwinTheme.shapes.small)
            .padding(TwinTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
    ) {
        InfoLine(R.string.movie_info_title, info.title)
        InfoLine(R.string.movie_info_length, info.lengthText)
        InfoLine(R.string.movie_info_source, stringResource(if (info.cloud) R.string.movie_info_source_cloud else R.string.movie_info_source_device))
        info.sizeText?.let { InfoLine(R.string.movie_info_size, it) }
        InfoLine(R.string.movie_info_subtitles, info.subtitles ?: stringResource(R.string.movie_value_off))
    }
}

@Composable
private fun InfoLine(label: Int, value: String) {
    Row {
        Text(stringResource(label) + ": ", color = TwinTheme.colors.inkMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = TwinTheme.colors.ink, style = MaterialTheme.typography.bodyMedium)
    }
}

/** The five quick emoji of the web page, shown as a row when the reaction button is tapped. */
@Composable
fun ReactionBar(onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.background(TwinTheme.colors.scrim, TwinTheme.shapes.pill).padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
    ) {
        reactionEmoji.forEach { emoji ->
            Text(
                emoji, fontSize = 26.sp,
                modifier = Modifier.clickable { onPick(emoji) }.padding(TwinTheme.spacing.s),
            )
        }
    }
}

val reactionEmoji: List<String> = listOf("❤️", "😂", "🥺", "😱", "😘")

/** Emoji rise from the bottom and fade out. Each one lives only as long as the list keeps it. */
@Composable
fun ReactionsLayer(reactions: List<MovieReaction>, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        reactions.forEach { r ->
            androidx.compose.runtime.key(r.id) { FloatingEmoji(r) }
        }
    }
}

@Composable
private fun BoxScope.FloatingEmoji(reaction: MovieReaction) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = RISE_MS, easing = LinearEasing)) }
    // The sideways spot comes from the id, so it is steady while the emoji rises.
    val slot = ((reaction.id % SLOTS).toInt() + SLOTS) % SLOTS
    Text(
        reaction.emoji, fontSize = 36.sp,
        modifier = Modifier.align(Alignment.BottomEnd)
            .padding(end = (SIDE_START_DP + slot * SIDE_STEP_DP).dp, bottom = BOTTOM_DP.dp)
            .graphicsLayer {
                translationY = -progress.value * RISE_PX
                alpha = 1f - progress.value
            }
            .size(44.dp),
    )
}

private const val RISE_MS = 2500
private const val RISE_PX = 700f
private const val SLOTS = 5
private const val SIDE_START_DP = 16
private const val SIDE_STEP_DP = 18
private const val BOTTOM_DP = 160
