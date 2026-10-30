package com.twinhearts.app.feature.meetplanner.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.SectionHeader
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.meetplanner.domain.PlanDraft
import com.twinhearts.app.feature.meetplanner.domain.PopularDestinations
import com.twinhearts.app.feature.meetplanner.domain.formatKm
import com.twinhearts.app.feature.meetplanner.domain.formatRange
import com.twinhearts.app.feature.meetplanner.domain.model.MeetIdentity
import com.twinhearts.app.feature.meetplanner.domain.model.MeetStatus
import com.twinhearts.app.feature.meetplanner.domain.model.Meetup
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.domain.monthShort
import com.twinhearts.app.feature.meetplanner.domain.nights
import com.twinhearts.app.feature.meetplanner.domain.splitPlans
import com.twinhearts.app.feature.meetplanner.domain.upcomingPlan
import com.twinhearts.app.feature.meetplanner.domain.whenLabel
import com.twinhearts.app.feature.meetplanner.ui.MeetActions
import com.twinhearts.app.feature.meetplanner.ui.MeetUiState
import com.twinhearts.app.feature.meetplanner.ui.components.MeetPalette
import com.twinhearts.app.feature.meetplanner.ui.components.MeetPill
import com.twinhearts.app.feature.meetplanner.ui.components.asText
import com.twinhearts.app.feature.meetplanner.ui.components.destinationImage
import java.time.LocalDate

/** Why this file exists: the Meet Planner home page (reference screenshot 21): couple card, quick actions, stats, next meetup, destinations. */
@Composable
internal fun MeetHomeScreen(state: MeetUiState, actions: MeetActions, onExit: () -> Unit, today: LocalDate = LocalDate.now()) {
    val colors = TwinTheme.colors
    val spacing = TwinTheme.spacing
    val me = state.identity?.myName?.ifBlank { null } ?: stringResource(R.string.meetplanner_you)
    val partner = state.identity?.partnerName?.ifBlank { null } ?: stringResource(R.string.meetplanner_partner)
    val split = splitPlans(state.meetups)
    val draft = state.draft

    Column(Modifier.fillMaxSize()) {
        TwinTopBar(title = stringResource(R.string.meetplanner_title), onBackClick = onExit, onSyncClick = actions::refresh)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenEdge)
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            GlassCard(Modifier.fillMaxWidth(), contentPadding = spacing.xl) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(me, size = 84.dp)
                        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = colors.pinkSoft, modifier = Modifier.padding(horizontal = spacing.m).size(28.dp))
                        Avatar(partner, size = 84.dp)
                    }
                    Spacer(Modifier.height(spacing.m))
                    Text(stringResource(R.string.meetplanner_couple_names, me, partner), style = MaterialTheme.typography.titleLarge, color = colors.ink, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.meetplanner_tagline), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                QuickTile(Icons.Rounded.Add, stringResource(R.string.meetplanner_plan_new), stringResource(R.string.meetplanner_plan_new_sub), true, actions::startNew, Modifier.weight(1f))
                QuickTile(Icons.Rounded.Timeline, stringResource(R.string.meetplanner_itinerary), stringResource(R.string.meetplanner_itinerary_sub), false, actions::openItinerary, Modifier.weight(1f))
                QuickTile(Icons.Rounded.Favorite, stringResource(R.string.meetplanner_saved), stringResource(R.string.meetplanner_saved_sub), false, actions::openSaved, Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                HomeStat((split.planned.size + split.done.size).toString(), stringResource(R.string.meetplanner_stat_total), Modifier.weight(1f))
                HomeStat(split.planned.size.toString(), stringResource(R.string.meetplanner_stat_upcoming), Modifier.weight(1f))
                HomeStat(split.done.size.toString(), stringResource(R.string.meetplanner_stat_completed), Modifier.weight(1f))
            }

            if (draft.stops.isNotEmpty()) DraftBanner(draft, onClick = { if (draft.routeBuilt) actions.openItinerary() else actions.goStops() })

            SectionHeader(
                title = stringResource(R.string.meetplanner_upcoming_title),
                actionLabel = stringResource(R.string.meetplanner_view_all),
                onAction = actions::openSaved,
            )
            val plans = state.plans
            val next = upcomingPlan(state.meetups, today)
            when {
                plans is UiState.Error -> ErrorState(message = plans.error.asText(), onRetry = actions::refresh)
                next == null -> EmptyUpcoming(actions::startNew)
                else -> UpcomingCard(next, today) { actions.openPlan(next.id) }
            }

            Text(stringResource(R.string.meetplanner_popular_title), style = MaterialTheme.typography.titleLarge, color = colors.ink)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
                items(PopularDestinations, key = { it.name }) { d ->
                    val image = destinationImage(d.name)
                    Box(
                        Modifier
                            .size(width = 150.dp, height = 150.dp)
                            .clip(TwinTheme.shapes.card)
                            .border(1.dp, colors.line, TwinTheme.shapes.card)
                            .background(colors.glass)
                            .clickable { actions.pickPopular(d) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (image != null) {
                            Image(painterResource(image), contentDescription = d.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Text("${d.emoji}\n${d.name}", textAlign = TextAlign.Center, color = colors.ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTile(icon: ImageVector, title: String, subtitle: String, main: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = TwinTheme.colors
    GlassCard(modifier, onClick = onClick, contentPadding = TwinTheme.spacing.m) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(TwinTheme.shapes.small)
                    .background(if (main) colors.brand else colors.glass),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = colors.ink) }
            Spacer(Modifier.height(TwinTheme.spacing.s))
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.inkFaint, maxLines = 1)
        }
    }
}

@Composable
private fun HomeStat(value: String, label: String, modifier: Modifier) {
    val colors = TwinTheme.colors
    GlassCard(modifier, contentPadding = TwinTheme.spacing.m) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint, maxLines = 1)
        }
    }
}

@Composable
private fun DraftBanner(draft: PlanDraft, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MeetPalette.amber.copy(alpha = 0.14f))
            .border(1.dp, MeetPalette.amber.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Column(Modifier.weight(1f)) {
            val city = draft.city?.name
            Text(
                if (city != null) stringResource(R.string.meetplanner_continue_city, city) else stringResource(R.string.meetplanner_continue),
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
            )
            Text(
                pluralStringResource(R.plurals.meetplanner_stops_picked, draft.stops.size, draft.stops.size) +
                    if (draft.routeBuilt) stringResource(R.string.meetplanner_route_ready) else "",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
            )
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MeetPalette.amber)
    }
}

@Composable
private fun EmptyUpcoming(onClick: () -> Unit) {
    val colors = TwinTheme.colors
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick, contentPadding = TwinTheme.spacing.xl) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💌", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(TwinTheme.spacing.s))
            Text(stringResource(R.string.meetplanner_no_upcoming), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
            Text(stringResource(R.string.meetplanner_tap_to_plan), style = MaterialTheme.typography.titleSmall, color = colors.ink, textAlign = TextAlign.Center)
        }
    }
}

/** The big "ticket" card for the next meetup: date block on the left, details on the right. */
@Composable
internal fun UpcomingCard(plan: Meetup, today: LocalDate, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    val date = plan.meetDate
    val nightCount = nights(plan.meetDate, plan.endDate)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier
                .width(86.dp)
                .background(colors.primaryBrush)
                .padding(vertical = TwinTheme.spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(date?.dayOfMonth?.toString() ?: "—", style = MaterialTheme.typography.headlineMedium, color = colors.ink)
            Text(date?.let { monthShort(it).uppercase() } ?: stringResource(R.string.meetplanner_no_date), style = MaterialTheme.typography.labelMedium, color = colors.ink)
        }
        Column(Modifier.weight(1f).padding(TwinTheme.spacing.m), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(plan.title, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 1, modifier = Modifier.weight(1f))
                if (date != null) MeetPill(whenLabel(date, today).asText(), background = colors.brand)
            }
            Text(
                (formatRange(plan.meetDate, plan.endDate) ?: "") + (plan.cityName?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
            )
            Text(
                buildString {
                    append(pluralStringResource(R.plurals.meetplanner_stops, plan.stops.size, plan.stops.size))
                    if (nightCount != null) append("  ·  ").append(pluralStringResource(R.plurals.meetplanner_nights, nightCount.toInt(), nightCount.toInt()))
                    plan.distanceKm?.takeIf { it > 0 }?.let { append("  ·  ").append(formatKm(it)) }
                },
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkFaint,
            )
        }
    }
}

private val previewIdentity = MeetIdentity("c", "user1", "Vamsi", "Likitha")
private val previewPlan = Meetup(
    "1", "Weekend in Goa", LocalDate.now().plusDays(3), LocalDate.now().plusDays(5), null, "", 5000.0, "Goa",
    listOf(Place("a", "Hotel Sea", PlaceCategory.Hotel, "🏨", 15.0, 74.0)), emptyList(), 12.3, 40.0,
    com.twinhearts.app.feature.meetplanner.domain.model.TravelMode.Car, emptyList(), MeetStatus.Planned, null,
)

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun HomeContentPreview() = TwinTheme {
    TwinBackground { MeetHomeScreen(MeetUiState(identity = previewIdentity, plans = UiState.Content(listOf(previewPlan)), draft = PlanDraft.fresh(LocalDate.now())), MeetActions.None, {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun HomeEmptyPreview() = TwinTheme {
    TwinBackground { MeetHomeScreen(MeetUiState(identity = previewIdentity, plans = UiState.Empty, draft = PlanDraft.fresh(LocalDate.now())), MeetActions.None, {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun HomeErrorPreview() = TwinTheme {
    TwinBackground { MeetHomeScreen(MeetUiState(plans = UiState.Error(AppError.Network), draft = PlanDraft.fresh(LocalDate.now())), MeetActions.None, {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun HomeLargeFontPreview() = TwinTheme {
    TwinBackground { MeetHomeScreen(MeetUiState(identity = previewIdentity, plans = UiState.Content(listOf(previewPlan)), draft = PlanDraft.fresh(LocalDate.now())), MeetActions.None, {}) }
}
