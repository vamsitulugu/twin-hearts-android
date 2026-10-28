package com.twinhearts.app.feature.meetplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.meetplanner.domain.PlanDraft
import com.twinhearts.app.feature.meetplanner.domain.countdownParts
import com.twinhearts.app.feature.meetplanner.domain.daysUntil
import com.twinhearts.app.feature.meetplanner.domain.formatDate
import com.twinhearts.app.feature.meetplanner.domain.formatDuration
import com.twinhearts.app.feature.meetplanner.domain.formatKm
import com.twinhearts.app.feature.meetplanner.domain.formatMoney
import com.twinhearts.app.feature.meetplanner.domain.formatRange
import com.twinhearts.app.feature.meetplanner.domain.model.ChecklistItem
import com.twinhearts.app.feature.meetplanner.domain.model.MeetStatus
import com.twinhearts.app.feature.meetplanner.domain.model.Meetup
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.domain.model.TravelMode
import com.twinhearts.app.feature.meetplanner.domain.nights
import com.twinhearts.app.feature.meetplanner.ui.MeetActions
import com.twinhearts.app.feature.meetplanner.ui.MeetUiState
import com.twinhearts.app.feature.meetplanner.ui.components.MeetHeader
import com.twinhearts.app.feature.meetplanner.ui.components.MeetPill
import com.twinhearts.app.feature.meetplanner.ui.components.MeetStatsRow
import com.twinhearts.app.feature.meetplanner.ui.components.label
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime

private const val TICK_MS = 20_000L

/** Why this file exists: one saved meetup: countdown, details, notes, route, checklist, and the Mark Complete / Edit buttons. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailScreen(state: MeetUiState, actions: MeetActions, nowOverride: LocalDateTime? = null) {
    val colors = TwinTheme.colors
    val spacing = TwinTheme.spacing
    val plan = state.detail
    // The countdown refreshes every 20 seconds, like the web app.
    var ticked by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(TICK_MS); ticked = LocalDateTime.now() } }
    val now = nowOverride ?: ticked

    Column(Modifier.fillMaxSize()) {
        MeetHeader(
            title = plan?.title ?: stringResource(R.string.meetplanner_meetup),
            subtitle = plan?.cityName,
            onBack = actions::back,
            trailing = { if (plan != null) GlassIconButton(Icons.Rounded.MoreVert, stringResource(R.string.meetplanner_options), { actions.openOptions(plan.id) }) },
        )
        if (plan == null) {
            EmptyState(title = stringResource(R.string.meetplanner_not_available))
            return@Column
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenEdge).padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            Hero(plan, now, actions)
            val nightCount = nights(plan.meetDate, plan.endDate)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.s), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                plan.cityName?.let { Chip("📍 $it") }
                Chip("📅 " + (formatRange(plan.meetDate, plan.endDate) ?: stringResource(R.string.meetplanner_date_not_set)))
                if (nightCount != null && nightCount > 0) Chip("🕒 " + pluralStringResource(R.plurals.meetplanner_nights, nightCount.toInt(), nightCount.toInt()))
                Chip("${plan.travelMode.emoji} ${plan.travelMode.label()}")
                plan.budget?.takeIf { it > 0 }?.let { Chip("💰 ₹" + formatMoney(it)) }
            }
            MeetStatsRow(
                listOf(
                    plan.stops.size.toString() to stringResource(R.string.meetplanner_stops_label),
                    (plan.distanceKm?.takeIf { it > 0 }?.let { formatKm(it) } ?: "—") to stringResource(R.string.meetplanner_distance),
                    (plan.durationMin?.takeIf { it > 0 }?.let { formatDuration(it) } ?: "—") to stringResource(R.string.meetplanner_travel_time),
                ),
            )
            GlassCard(Modifier.fillMaxWidth()) {
                PanelTitle(stringResource(R.string.meetplanner_notes), stringResource(R.string.meetplanner_edit)) { actions.openEdit(plan.id) }
                if (plan.notes.isNotBlank()) {
                    Text(plan.notes, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                } else {
                    Text(stringResource(R.string.meetplanner_notes_empty), style = MaterialTheme.typography.bodyMedium, color = colors.inkFaint)
                }
            }
            if (plan.stops.isNotEmpty()) {
                GlassCard(Modifier.fillMaxWidth()) {
                    PanelTitle(stringResource(R.string.meetplanner_route), stringResource(R.string.meetplanner_view_map)) { actions.openRouteSaved(plan.id) }
                    plan.stops.forEachIndexed { i, s ->
                        Row(Modifier.padding(vertical = spacing.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = colors.inkFaint, modifier = Modifier.size(20.dp), textAlign = TextAlign.Center)
                            Text("${s.emoji}  ${s.name}", style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                        }
                    }
                }
            }
            if (plan.checklist.isNotEmpty()) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.meetplanner_checklist), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                    plan.checklist.forEachIndexed { i, c ->
                        Row(Modifier.padding(vertical = spacing.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
                            CheckDot(c.done) { actions.toggleSavedCheck(i) }
                            Text(
                                c.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (c.done) colors.inkFaint else colors.ink,
                                textDecoration = if (c.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                if (!plan.isCompleted) {
                    GlassButton(stringResource(R.string.meetplanner_mark_complete), { actions.openComplete(plan.id) }, Modifier.weight(1f))
                } else if (plan.globeMemoryId != null) {
                    GlassButton(stringResource(R.string.meetplanner_view_photos), { actions.viewPhotos(plan.id) }, Modifier.weight(1f))
                }
                GlassButton(stringResource(R.string.meetplanner_edit_details), { actions.openEdit(plan.id) }, Modifier.weight(1f), style = GlassButtonStyle.Secondary)
            }
        }
    }
}

@Composable
private fun Hero(plan: Meetup, now: LocalDateTime, actions: MeetActions) {
    val colors = TwinTheme.colors
    val spacing = TwinTheme.spacing
    val date = plan.meetDate
    val tint = if (plan.isCompleted) colors.success.copy(alpha = 0.16f) else colors.brand.copy(alpha = 0.45f)
    GlassCard(Modifier.fillMaxWidth().background(tint, TwinTheme.shapes.card), contentPadding = spacing.xl) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            when {
                plan.isCompleted -> {
                    HeroTitle(stringResource(R.string.meetplanner_hero_memory))
                    Text(
                        stringResource(R.string.meetplanner_hero_completed, date?.let { " · " + formatDate(it) }.orEmpty()),
                        style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, textAlign = TextAlign.Center,
                    )
                }
                date == null -> {
                    HeroTitle(stringResource(R.string.meetplanner_hero_cant_wait_plain))
                    Text(stringResource(R.string.meetplanner_hero_pick_date), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                    GlassButton(stringResource(R.string.meetplanner_set_date), { actions.openEdit(plan.id) })
                }
                else -> {
                    HeroTitle(stringResource(R.string.meetplanner_hero_cant_wait))
                    Text(formatRange(plan.meetDate, plan.endDate).orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                    val parts = countdownParts(date, now)
                    if (parts != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                            CountBox(parts.days.toString(), stringResource(R.string.meetplanner_days))
                            CountBox(parts.hours.toString(), stringResource(R.string.meetplanner_hours))
                            CountBox(parts.minutes.toString(), stringResource(R.string.meetplanner_minutes))
                        }
                    } else {
                        Text(
                            stringResource(if (daysUntil(date, now.toLocalDate()) == 0L) R.string.meetplanner_hero_today else R.string.meetplanner_hero_passed),
                            style = MaterialTheme.typography.titleMedium, color = colors.ink, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroTitle(text: String) =
    Text(text, style = MaterialTheme.typography.headlineMedium, fontFamily = SerifFamily, fontStyle = FontStyle.Italic, color = TwinTheme.colors.ink, textAlign = TextAlign.Center)

@Composable
private fun CountBox(value: String, label: String) {
    val colors = TwinTheme.colors
    Column(
        Modifier
            .size(width = 84.dp, height = 78.dp)
            .clip(TwinTheme.shapes.small)
            .background(colors.scrim)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
    }
}

@Composable
private fun Chip(text: String) {
    val colors = TwinTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = colors.inkMuted,
        modifier = Modifier
            .clip(TwinTheme.shapes.pill)
            .background(colors.glass)
            .border(1.dp, colors.line, TwinTheme.shapes.pill)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
    )
}

@Composable
private fun PanelTitle(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = TwinTheme.spacing.s), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = TwinTheme.colors.ink)
        MeetPill(action, modifier = Modifier.clickable(onClick = onAction))
    }
}

private val previewPlan = Meetup(
    "1", "Weekend in Goa", LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 7), null, "Surprise plan!", 5000.0, "Goa",
    listOf(Place("a", "Hotel Sea", PlaceCategory.Hotel, "🏨", 15.0, 74.0), Place("b", "Cafe Mondo", PlaceCategory.Cafe, "☕", 15.1, 74.1)),
    emptyList(), 12.3, 40.0, TravelMode.Car, listOf(ChecklistItem(1, "Passport", true), ChecklistItem(2, "Sunscreen")), MeetStatus.Planned, null,
)

private fun detailState(plan: Meetup?) = MeetUiState(
    plans = if (plan == null) com.twinhearts.app.core.common.UiState.Empty else com.twinhearts.app.core.common.UiState.Content(listOf(plan)),
    draft = PlanDraft.fresh(LocalDate.now()),
    detailId = plan?.id,
)

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun DetailCountdownPreview() = TwinTheme {
    TwinBackground { DetailScreen(detailState(previewPlan), MeetActions.None, LocalDateTime.of(2026, 10, 28, 10, 0)) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun DetailCompletedPreview() = TwinTheme {
    TwinBackground { DetailScreen(detailState(previewPlan.copy(status = MeetStatus.Completed, globeMemoryId = "9")), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun DetailMissingLargeFontPreview() = TwinTheme {
    TwinBackground { DetailScreen(detailState(null), MeetActions.None) }
}
