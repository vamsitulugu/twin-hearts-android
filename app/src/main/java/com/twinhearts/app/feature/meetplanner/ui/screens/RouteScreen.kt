package com.twinhearts.app.feature.meetplanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.livemap.ui.map.MapPoint
import com.twinhearts.app.feature.livemap.ui.nav.ArrivalCard
import com.twinhearts.app.feature.livemap.ui.nav.KeepScreenOn
import com.twinhearts.app.feature.livemap.ui.nav.NavBottomBar
import com.twinhearts.app.feature.livemap.ui.nav.NavTopBanner
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
import com.twinhearts.app.feature.meetplanner.ui.components.MeetMap
import com.twinhearts.app.feature.meetplanner.ui.components.MeetStatsRow
import java.time.LocalDate

/**
 * Why this file exists: "Route Preview": the route on a real map with its numbered stops and totals.
 * "Start Navigation" runs real turn-by-turn inside the app (voice, next-turn banner, rerouting);
 * "Open in Maps app" is the old hand-off to Google Maps.
 */
@Composable
internal fun RouteScreen(state: MeetUiState, actions: MeetActions) {
    val spacing = TwinTheme.spacing
    val view = state.routeView
    Column(Modifier.fillMaxSize()) {
        val nav = state.nav
        if (view != null && state.navActive && nav != null) {
            NavigationLayer(state, view, actions)
            return@Column
        }
        MeetHeader(stringResource(R.string.meetplanner_route_title), onBack = actions::back, subtitle = view?.title)
        if (view == null) {
            EmptyState(title = stringResource(R.string.meetplanner_no_stops_saved))
            return@Column
        }
        Column(Modifier.weight(1f).padding(horizontal = spacing.screenEdge).padding(bottom = spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            GlassCard(Modifier.weight(1f).fillMaxWidth(), contentPadding = spacing.xs) {
                MeetMap(stops = view.stops, route = view.points)
            }
            MeetStatsRow(
                listOf(
                    (view.distanceKm?.takeIf { it > 0 }?.let { formatKm(it) } ?: "—") to stringResource(R.string.meetplanner_total_distance),
                    (view.durationMin?.takeIf { it > 0 }?.let { formatDuration(it) } ?: "—") to stringResource(R.string.meetplanner_est_time),
                    view.stops.size.toString() to stringResource(R.string.meetplanner_stops_label),
                ),
            )
            GlassButton(
                stringResource(R.string.meetplanner_start_navigation),
                actions::startNavigation,
                Modifier.fillMaxWidth(),
                leadingIcon = Icons.Rounded.Navigation,
                loading = state.navLoading,
            )
            GlassButton(
                stringResource(R.string.meetplanner_open_in_maps),
                actions::openInMaps,
                Modifier.fillMaxWidth(),
                style = GlassButtonStyle.Secondary,
            )
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

/** Full-screen turn-by-turn: the map follows you, with the same banner and bottom bar as Live Map. */
@Composable
private fun NavigationLayer(state: MeetUiState, view: RouteView, actions: MeetActions) {
    val nav = state.nav ?: return
    val spacing = TwinTheme.spacing
    val line = remember(nav.progress?.segment, nav.route) { nav.remainingLine().map { GeoPoint(it.lat, it.lng) } }
    // The stops still ahead stay numbered on the map; only the leg being driven is drawn as a line.
    val me = nav.progress?.snapped?.let { MapPoint(it.lat, it.lng) }
    val more = state.navStop + 1 < view.stops.size
    Box(Modifier.fillMaxSize()) {
        MeetMap(
            stops = view.stops,
            route = line,
            me = me,
            camera = state.navCamera,
            navigating = true,
            onUserGesture = actions::onUserMovedMap,
        )
        KeepScreenOn()
        if (nav.arrived) {
            if (more) {
                NextStopCard(
                    reached = view.stops[state.navStop].name,
                    next = view.stops[state.navStop + 1].name,
                    loading = state.navLoading,
                    onNext = actions::nextStop,
                    onEnd = actions::endNavigation,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(spacing.s),
                )
            } else {
                ArrivalCard(nav, actions::endNavigation, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(spacing.s))
            }
        } else {
            NavTopBanner(nav, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(spacing.s))
            NavBottomBar(
                nav,
                onEnd = actions::endNavigation,
                onMute = actions::toggleNavMute,
                onRecenter = actions::recenterNavigation,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(spacing.s),
            )
        }
    }
}

/** Shown after reaching a stop that is not the last one. */
@Composable
private fun NextStopCard(reached: String, next: String, loading: Boolean, onNext: () -> Unit, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    GlassCard(modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.l) {
        Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Text(stringResource(R.string.livemap_nav_arrived_title), style = MaterialTheme.typography.titleLarge, color = colors.ink)
            Text(stringResource(R.string.livemap_nav_arrived_body, reached), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
            GlassButton(
                stringResource(R.string.meetplanner_next_stop, next),
                onNext,
                Modifier.fillMaxWidth(),
                leadingIcon = Icons.Rounded.Navigation,
                loading = loading,
            )
            GlassButton(stringResource(R.string.livemap_nav_end), onEnd, Modifier.fillMaxWidth(), style = GlassButtonStyle.Secondary)
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
private fun RouteEmptyPreview() = TwinTheme {
    TwinBackground { RouteScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now())), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun RouteLargeFontPreview() = TwinTheme {
    val view = RouteView("Goa", previewStops, emptyList(), null, null, TravelMode.Walk)
    TwinBackground { RouteScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now()), routeView = view), MeetActions.None) }
}
