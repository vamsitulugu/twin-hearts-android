package com.twinhearts.app.feature.livemap.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.PillSwitch
import com.twinhearts.app.core.design.SectionHeader
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.StatTile
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.feature.livemap.domain.LiveMapFormat
import com.twinhearts.app.feature.livemap.ui.DayState
import com.twinhearts.app.feature.livemap.ui.RangeState
import com.twinhearts.app.feature.livemap.ui.TrackRange
import com.twinhearts.app.feature.livemap.ui.TrackUiState
import com.twinhearts.app.feature.livemap.ui.TrackViewModel
import com.twinhearts.app.feature.livemap.ui.map.LiveMapView
import com.twinhearts.app.feature.livemap.ui.map.MapColors
import com.twinhearts.app.feature.livemap.ui.map.MapOverlay
import com.twinhearts.app.feature.livemap.ui.map.MapPoint
import com.twinhearts.app.feature.livemap.domain.model.MapStyleKind
import com.twinhearts.app.feature.livemap.ui.map.MapStyles
import java.text.DateFormat
import java.util.Date

/** The "Track" tab: one day's route on a map with playback, timeline and stops, plus Week/Month totals. */
@Composable
fun TrackPage(
    state: TrackUiState,
    vm: TrackViewModel,
    myName: String,
    partnerName: String,
    mapStyle: MapStyleKind,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val context = LocalContext.current
    val timeFormat = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(TwinTheme.spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        SegmentedTabs(listOf(stringResource(R.string.livemap_track_me), partnerName), if (state.ofPartner) 1 else 0, { vm.showFor(it == 1) })
        SegmentedTabs(
            listOf(stringResource(R.string.livemap_range_today), stringResource(R.string.livemap_range_week), stringResource(R.string.livemap_range_month)),
            state.range.ordinal,
            { vm.setRange(TrackRange.entries[it]) },
        )

        if (!state.ofPartner) HistoryPrivacy(state, vm)

        when (state.range) {
            TrackRange.Today -> {
                if (state.dates.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                        items(state.dates.sortedDescending().take(14)) { d ->
                            TwinFilterChip(d.takeLast(5), d == state.selectedDate, { vm.selectDate(d) })
                        }
                    }
                }
                when (val day = state.day) {
                    DayState.Loading -> LoadingSkeleton(lines = 5)
                    DayState.Empty -> EmptyState(stringResource(R.string.livemap_track_empty_title), message = stringResource(R.string.livemap_track_empty_message))
                    is DayState.Error -> ErrorState(day.error.asText(), vm::retry)
                    is DayState.Content -> DayContent(state, day, vm, if (state.ofPartner) partnerName else myName, mapStyle, timeFormat) { gpx ->
                        context.startActivity(
                            Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/gpx+xml").putExtra(Intent.EXTRA_TEXT, gpx).putExtra(Intent.EXTRA_SUBJECT, "route-${day.day.date}.gpx"), null),
                        )
                    }
                }
            }
            else -> RangeContent(state, vm)
        }
    }
}

@Composable
private fun HistoryPrivacy(state: TrackUiState, vm: TrackViewModel) {
    val colors = TwinTheme.colors
    val settings = state.settings
    GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.livemap_history_title), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                Text(stringResource(R.string.livemap_history_hint), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
            PillSwitch(checked = settings?.enabled == true, onCheckedChange = vm::setHistoryEnabled, enabled = settings != null)
        }
        if (settings?.enabled == true) {
            Row(Modifier.padding(top = TwinTheme.spacing.s), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                TwinFilterChip(stringResource(R.string.livemap_keep_7), settings.retentionDays == 7, { vm.setRetention(7) })
                TwinFilterChip(stringResource(R.string.livemap_keep_30), settings.retentionDays == 30, { vm.setRetention(30) })
                TwinFilterChip(stringResource(R.string.livemap_keep_forever), settings.retentionDays == null, { vm.setRetention(null) })
            }
        }
    }
}

@Composable
private fun DayContent(
    state: TrackUiState,
    day: DayState.Content,
    vm: TrackViewModel,
    who: String,
    mapStyle: MapStyleKind,
    timeFormat: DateFormat,
    onExport: (String) -> Unit,
) {
    val colors = TwinTheme.colors
    val mapColors = remember(colors) {
        MapColors(colors.accent.toArgb(), colors.pinkSoft.toArgb(), colors.orchid.toArgb(), colors.success.toArgb(), colors.inkMuted.toArgb(),
            colors.error.toArgb(), colors.pinkSoft.toArgb(), colors.ink.toArgb(), colors.background.toArgb(), Color.White.toArgb())
    }
    val points = remember(day) { day.day.points.map { MapPoint(it.lat, it.lng) } }
    val index = state.playback.index.coerceIn(0, (points.size - 1).coerceAtLeast(0))
    val overlay = remember(day, index, state.playback.playing) {
        MapOverlay(
            route = points,
            stops = day.day.stops.map { MapPoint(it.lat, it.lng) },
            playback = if (index > 0 || state.playback.playing) points.getOrNull(index) else null,
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        StatTile("%.1f km".format(java.util.Locale.US, day.day.distanceKm), stringResource(R.string.livemap_stat_distance), Modifier.weight(1f))
        StatTile(LiveMapFormat.minutes(day.day.durationMin), stringResource(R.string.livemap_stat_duration), Modifier.weight(1f))
        StatTile(day.day.points.size.toString(), stringResource(R.string.livemap_stat_points), Modifier.weight(1f))
    }
    LiveMapView(
        style = MapStyles.sourceFor(mapStyle, false),
        overlay = overlay,
        colors = mapColors,
        modifier = Modifier.fillMaxWidth().height(260.dp),
        camera = state.camera,
        initialCenter = points.firstOrNull(),
    )
    // Playback: the dot walks along the route; the line below shows how far along it is.
    GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
        val cum = day.cumulativeKm.getOrNull(index) ?: 0.0
        val elapsedMin = ((day.day.points.getOrNull(index)?.atMs ?: 0L) - (day.day.points.firstOrNull()?.atMs ?: 0L)) / 60_000
        Text(
            stringResource(R.string.livemap_playback_line, "%.2f".format(java.util.Locale.US, cum), LiveMapFormat.minutes(elapsedMin.toInt())),
            style = MaterialTheme.typography.bodyMedium, color = colors.ink,
        )
        Row(Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            GlassButton(
                stringResource(if (state.playback.playing) R.string.livemap_pause else R.string.livemap_play),
                { if (state.playback.playing) vm.pause() else vm.play() },
                Modifier.weight(1f),
                leadingIcon = if (state.playback.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            )
            listOf(1f, 2f, 4f).forEach { sp ->
                TwinFilterChip("${sp.toInt()}x", state.playback.speed == sp, { vm.setSpeed(sp) })
            }
        }
    }
    if (state.timeline.isNotEmpty()) {
        SectionHeader(stringResource(R.string.livemap_timeline))
        state.timeline.forEach { e ->
            GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
                Text(e.label, style = MaterialTheme.typography.titleSmall, color = colors.ink)
                val arrived = timeFormat.format(Date(e.arrivedAtMs))
                val left = e.leftAtMs?.let { timeFormat.format(Date(it)) }
                Text(
                    if (e.ongoing || left == null) stringResource(R.string.livemap_timeline_here, arrived) else "$arrived – $left",
                    style = MaterialTheme.typography.bodySmall, color = colors.inkMuted,
                )
            }
        }
    }
    if (day.day.stops.isNotEmpty()) {
        SectionHeader(stringResource(R.string.livemap_stops))
        day.day.stops.forEach { s ->
            Text(
                stringResource(R.string.livemap_stop_line, timeFormat.format(Date(s.arrivedAtMs)), LiveMapFormat.minutes(s.minutes)),
                style = MaterialTheme.typography.bodyMedium, color = colors.ink,
            )
        }
    }
    GlassButton(
        stringResource(R.string.livemap_export_route),
        { vm.gpx(who)?.let(onExport) },
        Modifier.fillMaxWidth(),
        style = GlassButtonStyle.Secondary,
        leadingIcon = Icons.Rounded.Download,
    )
}

@Composable
private fun RangeContent(state: TrackUiState, vm: TrackViewModel) {
    val colors = TwinTheme.colors
    when (val r = state.rangeState) {
        RangeState.Idle, RangeState.Loading -> LoadingSkeleton(lines = 5)
        RangeState.Empty -> EmptyState(stringResource(R.string.livemap_track_empty_title), message = stringResource(R.string.livemap_range_empty_message))
        is RangeState.Error -> ErrorState(r.error.asText(), vm::retry)
        is RangeState.Content -> {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                StatTile("%.1f km".format(java.util.Locale.US, r.days.sumOf { it.distanceKm }), stringResource(R.string.livemap_stat_distance), Modifier.weight(1f))
                StatTile(LiveMapFormat.minutes(r.days.sumOf { it.durationMin }), stringResource(R.string.livemap_stat_duration), Modifier.weight(1f))
                StatTile(r.days.size.toString(), stringResource(R.string.livemap_stat_days), Modifier.weight(1f))
            }
            r.days.forEach { d ->
                GlassCard(Modifier.fillMaxWidth(), onClick = { vm.selectDate(d.date) }, contentPadding = TwinTheme.spacing.m) {
                    Text(d.date, style = MaterialTheme.typography.titleSmall, color = colors.ink)
                    Text(
                        stringResource(R.string.livemap_day_line, "%.1f".format(java.util.Locale.US, d.distanceKm), LiveMapFormat.minutes(d.durationMin), "%.0f".format(java.util.Locale.US, d.avgKmh), d.placesVisited),
                        style = MaterialTheme.typography.bodySmall, color = colors.inkMuted,
                    )
                }
            }
        }
    }
}
