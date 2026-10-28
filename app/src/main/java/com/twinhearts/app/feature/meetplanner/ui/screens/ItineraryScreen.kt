package com.twinhearts.app.feature.meetplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
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
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.meetplanner.domain.PlanDraft
import com.twinhearts.app.feature.meetplanner.domain.TimelineRow
import com.twinhearts.app.feature.meetplanner.domain.buildTimeline
import com.twinhearts.app.feature.meetplanner.domain.formatClock
import com.twinhearts.app.feature.meetplanner.domain.formatDuration
import com.twinhearts.app.feature.meetplanner.domain.formatKm
import com.twinhearts.app.feature.meetplanner.domain.model.ChecklistItem
import com.twinhearts.app.feature.meetplanner.domain.model.GeoPoint
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.domain.model.RouteLeg
import com.twinhearts.app.feature.meetplanner.domain.model.RoutePlan
import com.twinhearts.app.feature.meetplanner.ui.MeetActions
import com.twinhearts.app.feature.meetplanner.ui.MeetUiState
import com.twinhearts.app.feature.meetplanner.ui.components.MeetHeader
import com.twinhearts.app.feature.meetplanner.ui.components.MeetStatsRow
import com.twinhearts.app.feature.meetplanner.ui.components.MeetTimeDialog
import com.twinhearts.app.feature.meetplanner.ui.components.ModeRow
import com.twinhearts.app.feature.meetplanner.ui.components.PlaceThumb
import com.twinhearts.app.feature.meetplanner.ui.components.label
import java.time.LocalDate

/** Why this file exists: "Itinerary": travel mode, totals, the timed day plan, the travel checklist and "Save Itinerary". */
@Composable
internal fun ItineraryScreen(state: MeetUiState, actions: MeetActions) {
    val colors = TwinTheme.colors
    val spacing = TwinTheme.spacing
    val d = state.draft
    val route = d.route
    Column(Modifier.fillMaxSize()) {
        MeetHeader(
            title = stringResource(R.string.meetplanner_itinerary),
            subtitle = if (d.routeBuilt) (d.city?.name?.let { "$it · " } ?: "") + stringResource(R.string.meetplanner_n_stops, d.stops.size) else null,
            onBack = actions::back,
            trailing = { GlassIconButton(Icons.Rounded.Map, stringResource(R.string.meetplanner_route_on_map), actions::openRouteDraft) },
        )
        if (!d.routeBuilt || route == null) {
            if (d.stops.size >= 2) {
                EmptyState(
                    title = stringResource(R.string.meetplanner_route_stale),
                    actionLabel = stringResource(R.string.meetplanner_build_itinerary),
                    onAction = actions::buildRoute,
                )
            } else {
                EmptyState(
                    title = stringResource(R.string.meetplanner_no_itinerary_title),
                    message = stringResource(R.string.meetplanner_no_itinerary_message),
                    actionLabel = stringResource(R.string.meetplanner_plan_new),
                    onAction = actions::startNew,
                )
            }
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            ModeRow(d.mode, actions::setMode)
            MeetStatsRow(
                listOf(
                    formatKm(route.distanceKm) to stringResource(R.string.meetplanner_total_distance),
                    formatDuration(route.durationMin) to stringResource(R.string.meetplanner_est_time),
                    d.stops.size.toString() to stringResource(R.string.meetplanner_stops_label),
                ),
            )
            StartTimeRow(state.startMinutes, actions::setStartTime)
            val rows = buildTimeline(d.stops, route.legs, state.startMinutes)
            Column {
                rows.forEachIndexed { i, row -> TimelineItem(row, d.mode.emoji, isLast = i == rows.lastIndex, onOpen = { actions.openPlace(row.stop) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                GlassButton(stringResource(R.string.meetplanner_route_on_map), actions::openRouteDraft, Modifier.weight(1f), style = GlassButtonStyle.Secondary)
                GlassButton(stringResource(R.string.meetplanner_reorder), actions::goStops, Modifier.weight(1f), style = GlassButtonStyle.Secondary)
            }
            ChecklistPanel(d.checklist, actions)
        }
        GlassCard(Modifier.fillMaxWidth().padding(horizontal = spacing.screenEdge, vertical = spacing.s), contentPadding = spacing.m) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.meetplanner_looks_good), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                    Text(stringResource(R.string.meetplanner_looks_good_sub), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
                }
                GlassButton(stringResource(R.string.meetplanner_save_itinerary), actions::savePlan, loading = state.saving)
            }
        }
    }
}

@Composable
private fun StartTimeRow(minutes: Int, onPick: (Int) -> Unit) {
    val colors = TwinTheme.colors
    var open by rememberSaveable { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        Text(stringResource(R.string.meetplanner_start_day_at), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        Text(
            formatClock(minutes),
            style = MaterialTheme.typography.titleSmall,
            color = colors.ink,
            modifier = Modifier
                .clip(TwinTheme.shapes.small)
                .background(colors.glass)
                .border(1.dp, colors.line, TwinTheme.shapes.small)
                .clickable { open = true }
                .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
        )
    }
    if (open) MeetTimeDialog(minutes, onConfirm = { onPick(it); open = false }, onDismiss = { open = false })
}

@Composable
private fun TimelineItem(row: TimelineRow, modeEmoji: String, isLast: Boolean, onOpen: () -> Unit) {
    val colors = TwinTheme.colors
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        Text(
            formatClock(row.startMinutes),
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkMuted,
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp).padding(top = 14.dp),
            maxLines = 1,
        )
        Box(
            Modifier
                .width(16.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    if (!isLast) {
                        drawLine(
                            colors.accent.copy(alpha = 0.55f),
                            Offset(x, 28.dp.toPx()),
                            Offset(x, size.height),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                        )
                    }
                    drawCircle(colors.accent.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = Offset(x, 21.dp.toPx()))
                    drawCircle(colors.accent, radius = 6.dp.toPx(), center = Offset(x, 21.dp.toPx()))
                },
        )
        Column(Modifier.weight(1f)) {
            GlassCard(Modifier.fillMaxWidth(), onClick = onOpen, contentPadding = TwinTheme.spacing.s) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                    PlaceThumb(row.stop, size = 50.dp)
                    Column(Modifier.weight(1f)) {
                        Text(row.stop.name, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(R.string.meetplanner_cat_stay, row.stop.category.label(), row.stayMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkFaint,
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.inkFaint)
                }
            }
            row.leg?.let {
                Text(
                    stringResource(R.string.meetplanner_leg, modeEmoji, formatDuration(it.durationMin), formatKm(it.distanceKm)),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkFaint,
                    modifier = Modifier.padding(vertical = TwinTheme.spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun ChecklistPanel(items: List<ChecklistItem>, actions: MeetActions) {
    val colors = TwinTheme.colors
    var text by rememberSaveable { mutableStateOf("") }
    GlassCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.meetplanner_checklist_title), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        Row(
            Modifier.padding(vertical = TwinTheme.spacing.s),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            TwinTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.meetplanner_checklist_label),
                placeholder = stringResource(R.string.meetplanner_checklist_hint),
                imeAction = ImeActionDone,
                onImeAction = { actions.addChecklistItem(text); text = "" },
                modifier = Modifier.weight(1f),
            )
            GlassButton(stringResource(R.string.meetplanner_add), { actions.addChecklistItem(text); text = "" })
        }
        if (items.isEmpty()) {
            Text(stringResource(R.string.meetplanner_checklist_empty), style = MaterialTheme.typography.bodySmall, color = colors.inkFaint, modifier = Modifier.padding(TwinTheme.spacing.s))
        }
        items.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                CheckDot(item.done) { actions.toggleChecklist(item.id) }
                Text(
                    item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.done) colors.inkFaint else colors.ink,
                    textDecoration = if (item.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.meetplanner_remove_item),
                    tint = colors.inkFaint,
                    modifier = Modifier.size(22.dp).clickable { actions.removeChecklist(item.id) },
                )
            }
        }
    }
}

private val ImeActionDone = androidx.compose.ui.text.input.ImeAction.Done

/** The round tick box used by every checklist. */
@Composable
internal fun CheckDot(done: Boolean, onClick: (() -> Unit)?) {
    val colors = TwinTheme.colors
    val base = Modifier.size(24.dp).clip(CircleShape)
    Box(
        (if (done) base.background(colors.primaryBrush) else base.border(2.dp, colors.lineSoft, CircleShape))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.ink, modifier = Modifier.size(14.dp))
    }
}

private val previewStops = listOf(
    Place("h", "Hotel Sea View", PlaceCategory.Hotel, "🏨", 15.30, 74.10, isAnchor = true),
    Place("c", "Cafe Mondo", PlaceCategory.Cafe, "☕", 15.32, 74.12),
    Place("h", "Hotel Sea View", PlaceCategory.Hotel, "🏨", 15.30, 74.10, isAnchor = true),
)
private val previewRoute = RoutePlan(
    previewStops.map { GeoPoint(it.lat, it.lng) }, 12.4, 42.0, listOf(RouteLeg(6.0, 20.0), RouteLeg(6.4, 22.0)),
)

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun ItineraryContentPreview() = TwinTheme {
    val draft = PlanDraft.fresh(LocalDate.now()).copy(stops = previewStops, route = previewRoute, checklist = listOf(ChecklistItem(1, "Passport", true), ChecklistItem(2, "Sunscreen")))
    TwinBackground { ItineraryScreen(MeetUiState(draft = draft), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun ItineraryEmptyPreview() = TwinTheme {
    TwinBackground { ItineraryScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now())), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun ItineraryLargeFontPreview() = TwinTheme {
    val draft = PlanDraft.fresh(LocalDate.now()).copy(stops = previewStops, route = previewRoute)
    TwinBackground { ItineraryScreen(MeetUiState(draft = draft, saving = true), MeetActions.None) }
}
