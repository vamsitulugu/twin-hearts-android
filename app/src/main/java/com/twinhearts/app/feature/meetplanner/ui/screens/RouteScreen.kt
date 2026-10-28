package com.twinhearts.app.feature.meetplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.meetplanner.domain.PlanDraft
import com.twinhearts.app.feature.meetplanner.domain.formatDuration
import com.twinhearts.app.feature.meetplanner.domain.formatKm
import com.twinhearts.app.feature.meetplanner.domain.model.GeoPoint
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.domain.model.TravelMode
import com.twinhearts.app.feature.meetplanner.ui.MeetActions
import com.twinhearts.app.feature.meetplanner.ui.MeetUiState
import com.twinhearts.app.feature.meetplanner.ui.RouteView
import com.twinhearts.app.feature.meetplanner.ui.components.MeetHeader
import com.twinhearts.app.feature.meetplanner.ui.components.MeetStatsRow
import com.twinhearts.app.feature.meetplanner.ui.components.RouteCanvas
import java.time.LocalDate

/** Why this file exists: "Route Preview": the route drawn with its stops, totals, and "Start Navigation" (opens the maps app). */
@Composable
internal fun RouteScreen(state: MeetUiState, actions: MeetActions) {
    val spacing = TwinTheme.spacing
    val view = state.routeView
    Column(Modifier.fillMaxSize()) {
        MeetHeader(stringResource(R.string.meetplanner_route_title), onBack = actions::back, subtitle = view?.title)
        if (view == null) {
            EmptyState(title = stringResource(R.string.meetplanner_no_stops_saved))
            return@Column
        }
        Column(Modifier.weight(1f).padding(horizontal = spacing.screenEdge).padding(bottom = spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            GlassCard(Modifier.weight(1f).fillMaxWidth(), contentPadding = spacing.xs) {
                RouteCanvas(points = view.points, stops = view.stops, modifier = Modifier.fillMaxSize())
            }
            MeetStatsRow(
                listOf(
                    (view.distanceKm?.takeIf { it > 0 }?.let { formatKm(it) } ?: "—") to stringResource(R.string.meetplanner_total_distance),
                    (view.durationMin?.takeIf { it > 0 }?.let { formatDuration(it) } ?: "—") to stringResource(R.string.meetplanner_est_time),
                    view.stops.size.toString() to stringResource(R.string.meetplanner_stops_label),
                ),
            )
            GlassButton(stringResource(R.string.meetplanner_start_navigation), actions::startNavigation, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Navigation)
            Text(
                stringResource(R.string.meetplanner_navigation_note),
                style = MaterialTheme.typography.labelSmall,
                color = TwinTheme.colors.inkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val previewStops = listOf(
    Place("h", "Hotel", PlaceCategory.Hotel, "🏨", 15.30, 74.10, isAnchor = true),
    Place("c", "Cafe", PlaceCategory.Cafe, "☕", 15.33, 74.13),
    Place("m", "Fort", PlaceCategory.Tourist, "🗿", 15.36, 74.06),
)

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun RouteContentPreview() = TwinTheme {
    val view = RouteView("Goa", previewStops, previewStops.map { GeoPoint(it.lat, it.lng) }, 14.2, 48.0, TravelMode.Car)
    TwinBackground { RouteScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now()), routeView = view), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun RouteEmptyPreview() = TwinTheme {
    TwinBackground { RouteScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now())), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun RouteLargeFontPreview() = TwinTheme {
    val view = RouteView("Goa", previewStops, emptyList(), null, null, TravelMode.Walk)
    TwinBackground { RouteScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now()), routeView = view), MeetActions.None) }
}
