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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SectionHeader
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.meetplanner.domain.PlanDraft
import com.twinhearts.app.feature.meetplanner.domain.formatKm
import com.twinhearts.app.feature.meetplanner.domain.isSelected
import com.twinhearts.app.feature.meetplanner.domain.model.City
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.ui.MeetActions
import com.twinhearts.app.feature.meetplanner.ui.MeetUiState
import com.twinhearts.app.feature.meetplanner.ui.PoiState
import com.twinhearts.app.feature.meetplanner.ui.components.MeetHeader
import com.twinhearts.app.feature.meetplanner.ui.components.MeetPalette
import com.twinhearts.app.feature.meetplanner.ui.components.MeetSearchField
import com.twinhearts.app.feature.meetplanner.ui.components.PlaceThumb
import com.twinhearts.app.feature.meetplanner.ui.components.chipLabel
import com.twinhearts.app.feature.meetplanner.ui.components.label
import java.time.LocalDate

private const val TOP_TILES = 6

/** Why this file exists: "Find Places": category chips, a search box, popular tiles and rows, and the "N stops selected" dock. */
@Composable
internal fun SearchScreen(state: MeetUiState, actions: MeetActions) {
    val spacing = TwinTheme.spacing
    val draft = state.draft
    Column(Modifier.fillMaxSize()) {
        MeetHeader(
            title = stringResource(R.string.meetplanner_find_title),
            subtitle = draft.city?.let { stringResource(R.string.meetplanner_in_city, it.name) } ?: stringResource(R.string.meetplanner_find_subtitle),
            onBack = actions::back,
        )
        Column(Modifier.padding(horizontal = spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            MeetSearchField(state.poiQuery, actions::setQuery, stringResource(R.string.meetplanner_search_hint), actions::submitSearch)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                items(PlaceCategory.chips, key = { it.key }) { c ->
                    TwinFilterChip(label = "${c.emoji} ${c.chipLabel()}", selected = state.poiCategory == c, onClick = { actions.setCategory(c) })
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when (val poi = state.poi) {
                PoiState.Idle, PoiState.Loading -> LoadingSkeleton(Modifier.padding(spacing.screenEdge), lines = 5)
                is PoiState.Failed -> ErrorState(message = poi.error.asText(), onRetry = actions::retryPlaces)
                is PoiState.Results -> if (poi.places.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.meetplanner_no_results_title),
                        message = stringResource(R.string.meetplanner_no_results_message),
                    )
                } else {
                    PlaceList(state, poi.places, actions)
                }
            }
        }
        if (draft.stops.isNotEmpty()) {
            GlassCard(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenEdge, vertical = spacing.s),
                contentPadding = spacing.m,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            pluralStringResource(R.plurals.meetplanner_stops_selected, draft.stops.size, draft.stops.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = TwinTheme.colors.ink,
                        )
                        Text(stringResource(R.string.meetplanner_dock_hint), style = MaterialTheme.typography.labelSmall, color = TwinTheme.colors.inkFaint)
                    }
                    GlassButton(stringResource(R.string.meetplanner_view_stops), actions::goStops)
                }
            }
        }
    }
}

@Composable
private fun PlaceList(state: MeetUiState, places: List<Place>, actions: MeetActions) {
    val spacing = TwinTheme.spacing
    val draft = state.draft
    val byCategory = state.poiCategory != null
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = spacing.screenEdge, vertical = spacing.s),
        verticalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        if (byCategory) {
            item { SectionHeader(stringResource(R.string.meetplanner_nearby_popular)) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
                    items(places.take(TOP_TILES), key = { it.id }) { p ->
                        PlaceTile(p, draft.isSelected(p), { actions.openPlace(p) }, { actions.toggleStop(p) })
                    }
                }
            }
            if (places.size > TOP_TILES) {
                item { SectionHeader(stringResource(R.string.meetplanner_trending_in, draft.city?.name.orEmpty())) }
                items(places.drop(TOP_TILES), key = { it.id }) { p -> PlaceRow(p, draft.isSelected(p), { actions.openPlace(p) }, { actions.toggleStop(p) }) }
            }
        } else {
            item { SectionHeader(stringResource(R.string.meetplanner_results_for, state.poiQuery.trim())) }
            items(places, key = { it.id }) { p -> PlaceRow(p, draft.isSelected(p), { actions.openPlace(p) }, { actions.toggleStop(p) }) }
        }
    }
}

@Composable
private fun PlaceTile(place: Place, selected: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Box(
        Modifier
            .size(width = 138.dp, height = 168.dp)
            .clip(shape)
            .background(MeetPalette.gradient(place.category))
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.accent else colors.line, shape)
            .clickable(onClick = onOpen),
    ) {
        Text(place.emoji, style = MaterialTheme.typography.displaySmall, modifier = Modifier.align(Alignment.TopEnd).padding(TwinTheme.spacing.m))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, colors.background.copy(alpha = 0.72f)))))
        AddDot(selected, onToggle, Modifier.align(Alignment.TopStart).padding(TwinTheme.spacing.s))
        Column(Modifier.align(Alignment.BottomStart).padding(TwinTheme.spacing.m)) {
            Text(place.name, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(place.distKm?.let { formatKm(it) } ?: place.category.label(), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted, maxLines = 1)
        }
    }
}

@Composable
private fun AddDot(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primaryBrush else Brush.linearGradient(listOf(colors.scrim, colors.scrim)))
            .border(1.5.dp, colors.ink.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (selected) Icons.Rounded.Check else Icons.Rounded.Add,
            contentDescription = stringResource(if (selected) R.string.meetplanner_remove_from_plan else R.string.meetplanner_add_to_plan),
            tint = colors.ink,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PlaceRow(place: Place, selected: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.brand.copy(alpha = 0.35f) else colors.glass)
            .border(1.dp, if (selected) colors.accent else colors.line, shape)
            .clickable(onClick = onOpen)
            .padding(TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        PlaceThumb(place)
        Column(Modifier.weight(1f)) {
            Text(place.name, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(place.distKm?.let { formatKm(it) }, place.address?.take(44)).joinToString(" · ")
            Text(sub.ifEmpty { place.category.label() }, style = MaterialTheme.typography.bodySmall, color = colors.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AddDot(selected, onToggle)
    }
}

private val previewCity = City("Goa", "Goa, India", 15.3, 74.1)
private val previewPlaces = (1..8).map {
    Place("p$it", "Cafe Number $it", if (it % 2 == 0) PlaceCategory.Cafe else PlaceCategory.Hotel, "☕", 15.3, 74.1, "MG Road, Panaji", it * 0.8)
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun SearchContentPreview() = TwinTheme {
    val draft = PlanDraft.fresh(LocalDate.now()).copy(city = previewCity, stops = listOf(previewPlaces[0]))
    TwinBackground { SearchScreen(MeetUiState(draft = draft, poi = PoiState.Results(previewPlaces)), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun SearchEmptyPreview() = TwinTheme {
    TwinBackground { SearchScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now()).copy(city = previewCity), poi = PoiState.Results(emptyList())), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun SearchErrorPreview() = TwinTheme {
    TwinBackground { SearchScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now()).copy(city = previewCity), poi = PoiState.Failed(AppError.Network)), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun SearchLoadingLargeFontPreview() = TwinTheme {
    TwinBackground { SearchScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now()).copy(city = previewCity), poi = PoiState.Loading), MeetActions.None) }
}
