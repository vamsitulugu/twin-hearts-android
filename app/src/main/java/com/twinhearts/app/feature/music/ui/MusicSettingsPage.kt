package com.twinhearts.app.feature.music.ui

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.PillSwitch
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.music.data.OfflineSummary
import com.twinhearts.app.feature.music.domain.Milestone
import com.twinhearts.app.feature.music.domain.model.FxPreset
import com.twinhearts.app.feature.music.domain.model.MusicSettings
import com.twinhearts.app.feature.music.domain.model.SoundFx
import com.twinhearts.app.feature.music.ui.components.MusicSheet

/*
 * Why this file exists: "Music Settings" (web `public/music/settings.js`): Playback (crossfade, lyrics,
 * normalise, sound effects), Karaoke (vocal reduction), Library, Storage and Milestones.
 */

internal data class MusicSettingsActions(
    val onCrossfade: (Int) -> Unit,
    val onAutoLyrics: (Boolean) -> Unit,
    val onNormalize: (Boolean) -> Unit,
    val onVocalReduce: (Boolean) -> Unit,
    val onPreset: (FxPreset) -> Unit,
    val onFx: (SoundFx) -> Unit,
    val onResetSound: () -> Unit,
    val onAddSongs: () -> Unit,
    val onSync: () -> Unit,
    val onClearDownloads: () -> Unit,
    val onDeleteRecordings: () -> Unit,
)

@Composable
internal fun MusicSettingsPage(
    settings: MusicSettings,
    offline: OfflineSummary,
    milestones: Set<Milestone>,
    actions: MusicSettingsActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var crossfadeOpen by remember { mutableStateOf(false) }
    var fxOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmRecordings by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = TwinTheme.spacing.screenEdge, end = TwinTheme.spacing.screenEdge, bottom = TwinTheme.spacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
    ) {
        item(key = "playback") {
            SettingGroup(stringResource(R.string.music_settings_playback)) {
                SettingRow(
                    Icons.Rounded.Tune, stringResource(R.string.music_set_crossfade), stringResource(R.string.music_set_crossfade_sub),
                    onClick = { crossfadeOpen = true },
                    trailing = { ValueText(if (settings.crossfadeSec == 0) stringResource(R.string.music_set_off) else stringResource(R.string.music_set_seconds, settings.crossfadeSec)) },
                )
                SettingRow(
                    Icons.Rounded.Lyrics, stringResource(R.string.music_set_auto_lyrics), stringResource(R.string.music_set_auto_lyrics_sub),
                    onClick = { actions.onAutoLyrics(!settings.autoLyrics) },
                    trailing = { PillSwitch(settings.autoLyrics, actions.onAutoLyrics) },
                )
                SettingRow(
                    Icons.Rounded.VolumeUp, stringResource(R.string.music_set_normalize), stringResource(R.string.music_set_normalize_sub),
                    onClick = { actions.onNormalize(!settings.normalize) },
                    trailing = { PillSwitch(settings.normalize, actions.onNormalize) },
                )
                SettingRow(
                    Icons.Rounded.AutoAwesome, stringResource(R.string.music_set_fx), stringResource(R.string.music_set_fx_sub),
                    onClick = { fxOpen = true },
                    trailing = { ValueText(stringResource(presetLabel(settings.preset))) },
                )
            }
        }
        item(key = "karaoke") {
            SettingGroup(stringResource(R.string.music_settings_karaoke)) {
                SettingRow(
                    Icons.Rounded.Mic, stringResource(R.string.music_set_vocal), stringResource(R.string.music_set_vocal_sub),
                    onClick = { actions.onVocalReduce(!settings.vocalReduce) },
                    trailing = { PillSwitch(settings.vocalReduce, actions.onVocalReduce) },
                )
            }
        }
        item(key = "library") {
            SettingGroup(stringResource(R.string.music_settings_library)) {
                SettingRow(Icons.Rounded.Add, stringResource(R.string.music_add_songs), stringResource(R.string.music_set_add_songs_sub), onClick = actions.onAddSongs)
                SettingRow(Icons.Rounded.Sync, stringResource(R.string.music_set_sync), stringResource(R.string.music_set_sync_sub), onClick = actions.onSync)
            }
        }
        item(key = "storage") {
            SettingGroup(stringResource(R.string.music_settings_storage)) {
                SettingRow(
                    Icons.Rounded.Download, stringResource(R.string.music_set_downloads),
                    stringResource(R.string.music_set_downloads_sub, offline.count),
                    onClick = null,
                    trailing = { ValueText(Formatter.formatShortFileSize(context, offline.bytes)) },
                )
                SettingRow(
                    Icons.Rounded.Delete, stringResource(R.string.music_set_clear_downloads), stringResource(R.string.music_set_clear_downloads_sub),
                    onClick = { confirmClear = true }, danger = true,
                )
                SettingRow(
                    Icons.Rounded.Delete, stringResource(R.string.music_set_delete_recs), stringResource(R.string.music_set_delete_recs_sub),
                    onClick = { confirmRecordings = true }, danger = true,
                )
            }
        }
        item(key = "milestones") {
            SettingGroup(stringResource(R.string.music_settings_milestones, milestones.size, Milestone.entries.size)) {
                Column(Modifier.padding(TwinTheme.spacing.m), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    Milestone.entries.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                            pair.forEach { m -> MilestoneTile(m, m in milestones, Modifier.weight(1f)) }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (crossfadeOpen) CrossfadeDialog(settings.crossfadeSec, actions.onCrossfade) { crossfadeOpen = false }
    if (fxOpen) SoundEffectsSheet(settings, actions) { fxOpen = false }
    if (confirmClear) {
        ConfirmText(
            title = stringResource(R.string.music_confirm_clear_title),
            message = stringResource(R.string.music_confirm_clear_body, offline.count, Formatter.formatShortFileSize(context, offline.bytes)),
            confirm = stringResource(R.string.music_set_clear_downloads),
            onConfirm = actions.onClearDownloads,
            onDismiss = { confirmClear = false },
        )
    }
    if (confirmRecordings) {
        ConfirmText(
            title = stringResource(R.string.music_confirm_recs_title),
            message = stringResource(R.string.music_confirm_recs_body),
            confirm = stringResource(R.string.music_set_delete_recs),
            onConfirm = actions.onDeleteRecordings,
            onDismiss = { confirmRecordings = false },
        )
    }
}

// ── building blocks ──

@Composable
private fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = TwinTheme.colors.inkMuted,
            modifier = Modifier.padding(start = TwinTheme.spacing.xs, bottom = TwinTheme.spacing.s),
        )
        GlassCard(contentPadding = 0.dp) { Column { content() } }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    danger: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) colors.error else colors.pinkSoft, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(TwinTheme.spacing.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (danger) colors.error else colors.ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
        trailing()
    }
}

@Composable
private fun ValueText(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = TwinTheme.colors.accent)
}

@Composable
private fun MilestoneTile(m: Milestone, earned: Boolean, modifier: Modifier) {
    val colors = TwinTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (earned) colors.glass else colors.card)
            .padding(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (earned) m.emoji else "🔒", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(TwinTheme.spacing.s))
        Text(
            stringResource(milestoneName(m)),
            style = MaterialTheme.typography.bodySmall,
            color = if (earned) colors.ink else colors.inkFaint,
            maxLines = 2,
        )
    }
}

private fun milestoneName(m: Milestone): Int = when (m) {
    Milestone.FirstNote -> R.string.music_ms_first_note
    Milestone.GrowingCollection -> R.string.music_ms_growing
    Milestone.MusicLover -> R.string.music_ms_lover
    Milestone.Fan -> R.string.music_ms_fan
    Milestone.TrueFan -> R.string.music_ms_true_fan
    Milestone.OurSoundtrack -> R.string.music_ms_soundtrack
    Milestone.SameTaste -> R.string.music_ms_same_taste
    Milestone.Poet -> R.string.music_ms_poet
    Milestone.OnAir -> R.string.music_ms_on_air
    Milestone.StageStar -> R.string.music_ms_stage
    Milestone.OnRepeat -> R.string.music_ms_repeat
}

private fun presetLabel(p: FxPreset): Int = when (p) {
    FxPreset.Flat -> R.string.music_fx_flat
    FxPreset.Studio -> R.string.music_fx_studio
    FxPreset.Bathroom -> R.string.music_fx_bathroom
    FxPreset.Stadium -> R.string.music_fx_stadium
    FxPreset.BassBoost -> R.string.music_fx_bass_boost
    FxPreset.Bright -> R.string.music_fx_bright
    FxPreset.Custom -> R.string.music_fx_custom
}

@Composable
private fun ConfirmText(title: String, message: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = colors.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.music_cancel), color = colors.inkMuted) } },
    )
}

// ── crossfade ──

@Composable
private fun CrossfadeDialog(current: Int, onChange: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    var value by remember { mutableFloatStateOf(current.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(stringResource(R.string.music_set_crossfade), style = MaterialTheme.typography.titleLarge, color = colors.ink) },
        text = {
            Column {
                Text(stringResource(R.string.music_crossfade_note), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                Spacer(Modifier.height(TwinTheme.spacing.m))
                Text(
                    if (value.toInt() == 0) stringResource(R.string.music_set_off) else stringResource(R.string.music_set_seconds, value.toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    onValueChangeFinished = { onChange(value.toInt()) },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onChange(value.toInt()); onDismiss() }) { Text(stringResource(R.string.music_done), color = colors.accent) } },
    )
}

// ── sound effects ──

@Composable
private fun SoundEffectsSheet(settings: MusicSettings, actions: MusicSettingsActions, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    MusicSheet(onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Text(stringResource(R.string.music_fx_title), style = MaterialTheme.typography.titleLarge, color = colors.ink)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                FxPreset.entries.filter { it != FxPreset.Custom }.forEach { preset ->
                    TwinFilterChip(stringResource(presetLabel(preset)), settings.preset == preset, { actions.onPreset(preset) })
                }
            }
            FxSlider(stringResource(R.string.music_fx_reverb), settings.fx.reverb, 0f..1f, 19, { "${(it * 100).toInt()}%" }) { actions.onFx(settings.fx.copy(reverb = it)) }
            FxSlider(stringResource(R.string.music_fx_echo), settings.fx.echo, 0f..1f, 19, { "${(it * 100).toInt()}%" }) { actions.onFx(settings.fx.copy(echo = it)) }
            FxSlider(stringResource(R.string.music_fx_bass), settings.fx.bass.toFloat(), -10f..10f, 19, { "${it.toInt()} dB" }) { actions.onFx(settings.fx.copy(bass = it.toInt())) }
            FxSlider(stringResource(R.string.music_fx_treble), settings.fx.treble.toFloat(), -10f..10f, 19, { "${it.toInt()} dB" }) { actions.onFx(settings.fx.copy(treble = it.toInt())) }
            SettingRow(
                Icons.Rounded.Mic, stringResource(R.string.music_set_vocal), stringResource(R.string.music_set_vocal_sub),
                onClick = { actions.onVocalReduce(!settings.vocalReduce) },
                trailing = { PillSwitch(settings.vocalReduce, actions.onVocalReduce) },
            )
            SettingRow(
                Icons.Rounded.VolumeUp, stringResource(R.string.music_set_normalize), stringResource(R.string.music_set_normalize_sub),
                onClick = { actions.onNormalize(!settings.normalize) },
                trailing = { PillSwitch(settings.normalize, actions.onNormalize) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m), modifier = Modifier.padding(top = TwinTheme.spacing.s)) {
                GlassButton(stringResource(R.string.music_reset), actions.onResetSound, Modifier.weight(1f), GlassButtonStyle.Ghost)
                GlassButton(stringResource(R.string.music_done), onDismiss, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FxSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Float) -> String,
    onCommit: (Float) -> Unit,
) {
    val colors = TwinTheme.colors
    var dragging by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
            Text(format(dragging), style = MaterialTheme.typography.labelLarge, color = colors.accent)
        }
        Slider(
            value = dragging,
            onValueChange = { dragging = it },
            onValueChangeFinished = { onCommit(dragging) },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent),
        )
    }
}
