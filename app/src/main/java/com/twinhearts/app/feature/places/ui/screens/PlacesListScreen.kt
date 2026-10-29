package com.twinhearts.app.feature.places.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.places.domain.formatVisitDate
import com.twinhearts.app.feature.places.domain.model.PlaceMemory
import com.twinhearts.app.feature.places.domain.placeStats
import com.twinhearts.app.feature.places.domain.visiblePlaces
import com.twinhearts.app.feature.places.ui.PlacesActions
import com.twinhearts.app.feature.places.ui.PlacesUiState
import com.twinhearts.app.feature.places.ui.components.FavoriteGold
import com.twinhearts.app.feature.places.ui.components.PlacesHeader
import com.twinhearts.app.feature.places.ui.components.StarButton
import com.twinhearts.app.feature.places.ui.components.StatTileRow
import java.time.LocalDate

/** Why this file exists: the Places Memory home: stats, search, Favorites filter, and the grid of place cards. */
@Composable
internal fun PlacesListScreen(state: PlacesUiState, actions: PlacesActions, onExit: () -> Unit) {
    val spacing = TwinTheme.spacing
    val colors = TwinTheme.colors
    Column(Modifier.fillMaxSize()) {
        PlacesHeader(stringResource(R.string.places_title), onExit)
        when (val places = state.places) {
            UiState.Loading -> LoadingSkeleton(Modifier.padding(spacing.screenEdge), lines = 5)
            is UiState.Error -> ErrorState(message = places.error.asText(), onRetry = actions::refresh)
            else -> {
                val list = visiblePlaces(state.all, state.query, state.favoritesOnly)
                val stats = placeStats(state.all)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = spacing.screenEdge, end = spacing.screenEdge, bottom = spacing.xxl),
                    horizontalArrangement = Arrangement.spacedBy(spacing.m),
                    verticalArrangement = Arrangement.spacedBy(spacing.m),
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    item(span = { GridItemSpan(2) }) {
                        StatTileRow(
                            listOf(
                                Triple(stats.places.toString(), stringResource(R.string.places_stat_places), colors.ink),
                                Triple(stats.favorites.toString(), stringResource(R.string.places_stat_favorites), FavoriteGold),
                                Triple(stats.photos.toString(), stringResource(R.string.places_stat_photos), colors.accent),
                                Triple(stats.memories.toString(), stringResource(R.string.places_stat_memories), colors.pinkSoft),
                            ),
                        )
                    }
                    item(span = { GridItemSpan(2) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                            TwinTextField(
                                value = state.query, onValueChange = actions::setQuery,
                                label = stringResource(R.string.places_search_label),
                                placeholder = stringResource(R.string.places_search_hint),
                                leadingIcon = Icons.Rounded.Search,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s), verticalAlignment = Alignment.CenterVertically) {
                                TwinFilterChip(stringResource(R.string.places_favorites), state.favoritesOnly, actions::toggleFavoritesOnly)
                                Box(Modifier.weight(1f))
                                GlassButton(stringResource(R.string.places_add), actions::openAdd, leadingIcon = Icons.Rounded.Add)
                            }
                        }
                    }
                    if (list.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            EmptyState(
                                title = stringResource(if (state.all.isEmpty()) R.string.places_empty_title else R.string.places_no_match_title),
                                message = if (state.all.isEmpty()) stringResource(R.string.places_empty_message) else null,
                            )
                        }
                    } else {
                        items(list, key = { it.id }) { place -> PlaceCard(place, actions) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceCard(place: PlaceMemory, actions: PlacesActions) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Column(
        Modifier.clip(shape).background(colors.glass).border(1.dp, colors.line, shape).clickable { actions.openDetail(place.id) },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.4f).background(colors.card), contentAlignment = Alignment.Center) {
            if (place.banner != null) {
                AsyncImage(place.banner, place.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text("🗺️", style = MaterialTheme.typography.headlineMedium)
            }
            StarButton(place.favorite, { actions.quickFavorite(place.id) }, Modifier.align(Alignment.TopEnd).padding(TwinTheme.spacing.s))
            if (place.photos.isNotEmpty()) {
                Text(
                    "📸 ${place.photos.size}", style = MaterialTheme.typography.labelSmall, color = colors.ink,
                    modifier = Modifier.align(Alignment.BottomStart).padding(TwinTheme.spacing.s).clip(TwinTheme.shapes.pill).background(colors.scrim).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Column(Modifier.padding(TwinTheme.spacing.m), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(place.name, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (place.location.isNotBlank()) Text("📍 ${place.location}", style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("🗓️ " + (formatVisitDate(place.visitDate).ifEmpty { "—" }), style = MaterialTheme.typography.labelMedium, color = colors.inkFaint)
        }
    }
}

internal val previewPlaces = listOf(
    PlaceMemory("p1", "Goa Beach Trip", "Goa, India", LocalDate.of(2026, 3, 14), "Sunsets and shacks", null, listOf("a", "b"), 15.2993, 74.124, true),
    PlaceMemory("p2", "Coffee in Coorg", "Coorg", LocalDate.of(2026, 1, 2)),
    PlaceMemory("p3", "A very long place name that wraps onto two lines for sure", "Somewhere far away", null),
)

private fun previewState(places: UiState<List<PlaceMemory>>, fav: Boolean = false) = PlacesUiState(places = places, favoritesOnly = fav)

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun PlacesContentPreview() = TwinTheme { TwinBackground { PlacesListScreen(previewState(UiState.Content(previewPlaces)), PlacesActions.None) {} } }

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun PlacesEmptyPreview() = TwinTheme { TwinBackground { PlacesListScreen(previewState(UiState.Empty), PlacesActions.None) {} } }

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun PlacesLoadingPreview() = TwinTheme { TwinBackground { PlacesListScreen(previewState(UiState.Loading), PlacesActions.None) {} } }

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun PlacesErrorPreview() = TwinTheme { TwinBackground { PlacesListScreen(previewState(UiState.Error(AppError.Network)), PlacesActions.None) {} } }

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun PlacesLargeFontPreview() = TwinTheme { TwinBackground { PlacesListScreen(previewState(UiState.Content(previewPlaces), fav = true), PlacesActions.None) {} } }
