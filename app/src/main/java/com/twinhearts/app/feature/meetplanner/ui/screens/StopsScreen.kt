package com.twinhearts.app.feature.meetplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.meetplanner.domain.PlanDraft
import com.twinhearts.app.feature.meetplanner.domain.formatDuration
import com.twinhearts.app.feature.meetplanner.domain.formatKm
import com.twinhearts.app.feature.meetplanner.domain.model.GeoPoint
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.domain.roughEstimate
import com.twinhearts.app.feature.meetplanner.domain.roughLegKm
import com.twinhearts.app.feature.meetplanner.ui.MeetActions
import com.twinhearts.app.feature.meetplanner.ui.MeetUiState
import com.twinhearts.app.feature.meetplanner.ui.components.MeetHeader
import com.twinhearts.app.feature.meetplanner.ui.components.MeetPalette
import com.twinhearts.app.feature.meetplanner.ui.components.MeetPill
import com.twinhearts.app.feature.meetplanner.ui.components.MeetStatsRow
import com.twinhearts.app.feature.meetplanner.ui.components.MeetMap
import com.twinhearts.app.feature.meetplanner.ui.components.label
import java.time.LocalDate

/** Why this file exists: "Your Stops": reorder or remove the picked places, see a quick estimate, then build the itinerary. */
@Composable
internal fun StopsScreen(state: MeetUiState, actions: MeetActions) {
    val colors = TwinTheme.colors
    val spacing = TwinTheme.spacing
    val d = state.draft
    val est = roughEstimate(d.stops, d.mode)
    Column(Modifier.fillMaxSize()) {
        MeetHeader(
            title = stringResource(R.string.meetplanner_stops_title),
            onBack = actions::back,
            trailing = { MeetPill(stringResource(R.string.meetplanner_selected_count, d.stops.size), background = colors.brand) },
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenEdge)
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.s),
        ) {
            if (d.stops.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.meetplanner_no_stops_title),
                    message = stringResource(R.string.meetplanner_no_stops_message),
                )
            }
            d.stops.forEachIndexed { i, s -> StopRow(i, s, d.stops, actions) }

            GlassCard(Modifier.fillMaxWidth().height(280.dp), contentPadding = 0.dp) {
                // Real map with the numbered stops; the thin line is a straight preview until the road route is built.
                MeetMap(
                    stops = d.stops,
                    route = d.stops.map { GeoPoint(it.lat, it.lng) },
                    fallbackCenter = d.city?.let { GeoPoint(it.lat, it.lng) },
                    insideScroll = true,
                )
            }
            MeetStatsRow(
                listOf(
                    (if (d.stops.size > 1) "~" + formatKm(est.km) else "—") to stringResource(R.string.meetplanner_total_distance),
                    (if (d.stops.size > 1) "~" + formatDuration(est.minutes) else "—") to stringResource(R.string.meetplanner_est_time),
                    d.stops.size.toString() to stringResource(R.string.meetplanner_total_stops),
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                GlassButton(stringResource(R.string.meetplanner_add_places), actions::back, Modifier.weight(1f), style = GlassButtonStyle.Secondary)
                GlassButton(
                    stringResource(R.string.meetplanner_build_itinerary),
                    actions::buildRoute,
                    Modifier.weight(1f),
                    enabled = d.stops.size >= 2,
                    loading = state.building,
                )
            }
            if (d.stops.size < 2) {
                Text(
                    stringResource(R.string.meetplanner_build_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.inkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StopRow(index: Int, stop: Place, all: List<Place>, actions: MeetActions) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.line, shape)
            .padding(TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(colors.primaryBrush), contentAlignment = Alignment.Center) {
            Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = colors.ink)
        }
        Column(Modifier.weight(1f).clickable { actions.openPlace(stop) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                Text(stop.name, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (stop.isAnchor) MeetPill(stringResource(R.string.meetplanner_start_end), color = MeetPalette.amber, background = MeetPalette.amber.copy(alpha = 0.14f))
            }
            val leg = roughLegKm(all, index)
            Text(
                stop.category.label() + (leg?.let { " · " + stringResource(R.string.meetplanner_km_from_previous, formatKm(it)) } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SmallIcon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.meetplanner_move_up)) { actions.moveStop(index, -1) }
        SmallIcon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.meetplanner_move_down)) { actions.moveStop(index, 1) }
        SmallIcon(Icons.Rounded.Close, stringResource(R.string.meetplanner_remove_stop)) { actions.removeStop(index) }
    }
}

@Composable
private fun SmallIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = description, tint = TwinTheme.colors.ink, modifier = Modifier.size(20.dp))
    }
}

private val previewStops = listOf(
    Place("h", "Hotel Sea View", PlaceCategory.Hotel, "🏨", 15.30, 74.10, isAnchor = true),
    Place("c", "Cafe Mondo", PlaceCategory.Cafe, "☕", 15.32, 74.12),
    Place("m", "Fort Aguada", PlaceCategory.Tourist, "🗿", 15.34, 74.05),
)

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun StopsContentPreview() = TwinTheme {
    TwinBackground { StopsScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now()).copy(stops = previewStops)), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun StopsEmptyPreview() = TwinTheme {
    TwinBackground { StopsScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now())), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun StopsLargeFontPreview() = TwinTheme {
    TwinBackground { StopsScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now()).copy(stops = previewStops), building = true), MeetActions.None) }
}
