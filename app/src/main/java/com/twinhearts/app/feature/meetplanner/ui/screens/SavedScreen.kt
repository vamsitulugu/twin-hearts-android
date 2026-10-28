package com.twinhearts.app.feature.meetplanner.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.meetplanner.domain.PlanDraft
import com.twinhearts.app.feature.meetplanner.domain.SavedFilter
import com.twinhearts.app.feature.meetplanner.domain.formatKm
import com.twinhearts.app.feature.meetplanner.domain.formatMoney
import com.twinhearts.app.feature.meetplanner.domain.formatRange
import com.twinhearts.app.feature.meetplanner.domain.model.MeetStatus
import com.twinhearts.app.feature.meetplanner.domain.model.Meetup
import com.twinhearts.app.feature.meetplanner.domain.model.TravelMode
import com.twinhearts.app.feature.meetplanner.domain.monthShort
import com.twinhearts.app.feature.meetplanner.domain.splitPlans
import com.twinhearts.app.feature.meetplanner.domain.whenLabel
import com.twinhearts.app.feature.meetplanner.ui.MeetActions
import com.twinhearts.app.feature.meetplanner.ui.MeetUiState
import com.twinhearts.app.feature.meetplanner.ui.components.MeetHeader
import com.twinhearts.app.feature.meetplanner.ui.components.MeetPalette
import com.twinhearts.app.feature.meetplanner.ui.components.MeetPill
import com.twinhearts.app.feature.meetplanner.ui.components.asText
import java.time.LocalDate

/** Why this file exists: "Saved Meetups": All / Upcoming / Completed tabs, cards, long-press to select and delete. */
@Composable
internal fun SavedScreen(state: MeetUiState, actions: MeetActions, today: LocalDate = LocalDate.now()) {
    val colors = TwinTheme.colors
    val spacing = TwinTheme.spacing
    val split = splitPlans(state.meetups)
    val filters = SavedFilter.entries
    Column(Modifier.fillMaxSize()) {
        if (state.selectMode) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = spacing.screenEdge, vertical = spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
            ) {
                GlassIconButton(Icons.Rounded.Close, stringResource(R.string.meetplanner_exit_selection), actions::exitSelect)
                Text(stringResource(R.string.meetplanner_selected_count, state.selected.size), style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.Delete, stringResource(R.string.meetplanner_delete_selected), actions::deleteSelected)
            }
        } else {
            MeetHeader(stringResource(R.string.meetplanner_saved_title), onBack = actions::back, subtitle = stringResource(R.string.meetplanner_saved_subtitle))
        }
        Box(Modifier.padding(horizontal = spacing.screenEdge)) {
            SegmentedTabs(
                options = filters.map { f ->
                    val n = when (f) {
                        SavedFilter.All -> split.planned.size + split.done.size
                        SavedFilter.Upcoming -> split.planned.size
                        SavedFilter.Completed -> split.done.size
                    }
                    stringResource(
                        when (f) {
                            SavedFilter.All -> R.string.meetplanner_tab_all
                            SavedFilter.Upcoming -> R.string.meetplanner_tab_upcoming
                            SavedFilter.Completed -> R.string.meetplanner_tab_completed
                        },
                        n,
                    )
                },
                selectedIndex = filters.indexOf(state.savedFilter),
                onSelect = { actions.setFilter(filters[it]) },
            )
        }
        val plans = state.plans
        val list = split.forFilter(state.savedFilter)
        when {
            plans is UiState.Loading -> LoadingSkeleton(Modifier.padding(spacing.screenEdge), lines = 5)
            plans is UiState.Error -> ErrorState(message = plans.error.asText(), onRetry = actions::refresh)
            list.isEmpty() -> EmptyState(
                title = stringResource(if (state.savedFilter == SavedFilter.Completed) R.string.meetplanner_no_completed_title else R.string.meetplanner_no_saved_title),
                message = if (state.savedFilter == SavedFilter.Completed) stringResource(R.string.meetplanner_no_completed_message) else null,
                actionLabel = stringResource(R.string.meetplanner_plan_a_meetup),
                onAction = actions::startNew,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = spacing.screenEdge, vertical = spacing.m),
                verticalArrangement = Arrangement.spacedBy(spacing.m),
            ) {
                items(list, key = { it.id }) { plan ->
                    PlanCard(plan, today, state.selectMode, plan.id in state.selected, actions)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlanCard(plan: Meetup, today: LocalDate, selectMode: Boolean, selected: Boolean, actions: MeetActions) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    val date = plan.meetDate
    val dateBrush = when {
        plan.isCompleted -> Brush.linearGradient(listOf(colors.success, colors.success.copy(alpha = 0.55f)))
        date == null -> Brush.linearGradient(listOf(colors.neutralGlass, colors.neutralGlass.copy(alpha = 0.55f)))
        else -> colors.primaryBrush
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.glass)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.accent else colors.line, shape)
            .combinedClickable(
                onClick = { if (selectMode) actions.toggleSelect(plan.id) else actions.openPlan(plan.id) },
                onLongClick = { if (selectMode) actions.toggleSelect(plan.id) else actions.enterSelect(plan.id) },
            ),
    ) {
        Column(
            Modifier.width(74.dp).background(dateBrush).padding(vertical = TwinTheme.spacing.m).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(date?.dayOfMonth?.toString() ?: "—", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
            Text(date?.let { monthShort(it).uppercase() } ?: stringResource(R.string.meetplanner_tbd), style = MaterialTheme.typography.labelSmall, color = colors.ink)
        }
        Column(Modifier.weight(1f).padding(TwinTheme.spacing.m), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.title, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (selectMode) {
                    SelectDot(selected)
                } else {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.meetplanner_options),
                        tint = colors.inkMuted,
                        modifier = Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape).combinedClickable(onClick = { actions.openOptions(plan.id) }),
                    )
                }
            }
            Text(
                (formatRange(plan.meetDate, plan.endDate) ?: stringResource(R.string.meetplanner_date_not_set)) + (plan.cityName?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(pluralStringResource(R.plurals.meetplanner_stops, plan.stops.size, plan.stops.size))
                    plan.distanceKm?.takeIf { it > 0 }?.let { append("  ·  ").append(formatKm(it)) }
                    plan.budget?.takeIf { it > 0 }?.let { append("  ·  ₹").append(formatMoney(it)) }
                },
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkFaint,
            )
            when {
                plan.isCompleted -> MeetPill(stringResource(R.string.meetplanner_pill_completed), color = colors.success, background = colors.success.copy(alpha = 0.14f))
                date != null -> MeetPill(whenLabel(date, today).asText(), color = colors.ink, background = colors.brand)
                else -> MeetPill(stringResource(R.string.meetplanner_date_not_set), color = MeetPalette.amber, background = MeetPalette.amber.copy(alpha = 0.14f))
            }
        }
    }
}

@Composable
private fun SelectDot(selected: Boolean) {
    val colors = TwinTheme.colors
    val shape = androidx.compose.foundation.shape.CircleShape
    Box(
        Modifier.size(24.dp).clip(shape).background(if (selected) colors.accent else colors.glass).border(1.5.dp, colors.ink.copy(alpha = 0.6f), shape),
        contentAlignment = Alignment.Center,
    ) { if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.ink, modifier = Modifier.size(14.dp)) }
}

private val previewPlans = listOf(
    Meetup("1", "Weekend in Goa", LocalDate.now().plusDays(3), LocalDate.now().plusDays(5), null, "", 5000.0, "Goa", emptyList(), emptyList(), 12.3, 40.0, TravelMode.Car, emptyList(), MeetStatus.Planned, null),
    Meetup("2", "Mysuru day out", null, null, null, "", null, "Mysuru", emptyList(), emptyList(), null, null, TravelMode.Bike, emptyList(), MeetStatus.Planned, null),
    Meetup("3", "Ooty trip", LocalDate.now().minusDays(20), null, null, "", null, "Ooty", emptyList(), emptyList(), 80.0, 120.0, TravelMode.Car, emptyList(), MeetStatus.Completed, "9"),
)

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun SavedContentPreview() = TwinTheme {
    TwinBackground { SavedScreen(MeetUiState(plans = UiState.Content(previewPlans), draft = PlanDraft.fresh(LocalDate.now())), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun SavedEmptyPreview() = TwinTheme {
    TwinBackground { SavedScreen(MeetUiState(plans = UiState.Empty, draft = PlanDraft.fresh(LocalDate.now())), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun SavedErrorPreview() = TwinTheme {
    TwinBackground { SavedScreen(MeetUiState(plans = UiState.Error(AppError.Network), draft = PlanDraft.fresh(LocalDate.now())), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun SavedSelectLargeFontPreview() = TwinTheme {
    TwinBackground { SavedScreen(MeetUiState(plans = UiState.Content(previewPlans), draft = PlanDraft.fresh(LocalDate.now()), selectMode = true, selected = setOf("1")), MeetActions.None) }
}
