package com.twinhearts.app.feature.places.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.places.domain.formatCoordinate
import com.twinhearts.app.feature.places.domain.formatVisitDate
import com.twinhearts.app.feature.places.domain.model.MemoryNote
import com.twinhearts.app.feature.places.domain.model.PlaceMemory
import com.twinhearts.app.feature.places.domain.newestFirst
import com.twinhearts.app.feature.places.ui.DetailTab
import com.twinhearts.app.feature.places.ui.PlacesActions
import com.twinhearts.app.feature.places.ui.PlacesUiState
import com.twinhearts.app.feature.places.ui.components.PlacesDateField
import com.twinhearts.app.feature.places.ui.components.StarButton
import com.twinhearts.app.core.common.UiState
import java.time.LocalDate

private const val GALLERY_COLUMNS = 3

/** Why this file exists: one place: banner, description, and the Gallery / Timeline & Notes / Map tabs, with Edit and Delete. */
@Composable
internal fun PlaceDetailScreen(state: PlacesUiState, actions: PlacesActions) {
    val colors = TwinTheme.colors
    val spacing = TwinTheme.spacing
    val place = state.detail
    if (place == null) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.statusBarsPadding().padding(spacing.screenEdge)) { GlassIconButton(Icons.Rounded.Close, stringResource(R.string.places_close), actions::closeDetail) }
            EmptyState(title = stringResource(R.string.places_not_available))
        }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(220.dp).background(colors.card)) {
            if (place.banner != null) AsyncImage(place.banner, place.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, colors.scrim))))
            GlassIconButton(Icons.Rounded.Close, stringResource(R.string.places_close), actions::closeDetail, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(spacing.m))
            StarButton(place.favorite, actions::toggleDetailFavorite, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(spacing.m), size = 40.dp)
            Column(Modifier.align(Alignment.BottomStart).padding(spacing.screenEdge)) {
                Text(place.name, style = MaterialTheme.typography.headlineMedium, fontFamily = SerifFamily, color = colors.ink)
                val meta = listOfNotNull(
                    place.location.takeIf { it.isNotBlank() }?.let { "📍 $it" },
                    place.visitDate?.let { "🗓️ " + formatVisitDate(it) },
                ).joinToString("  ·  ")
                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
        }
        Column(Modifier.padding(horizontal = spacing.screenEdge).padding(top = spacing.l, bottom = spacing.xxl), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            Text(place.description.ifBlank { stringResource(R.string.places_no_description) }, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
            val tabs = DetailTab.entries
            SegmentedTabs(
                options = tabs.map { stringResource(it.labelRes()) },
                selectedIndex = tabs.indexOf(state.tab),
                onSelect = { actions.setTab(tabs[it]) },
            )
            when (state.tab) {
                DetailTab.Gallery -> Gallery(place, state.galleryUploading, actions)
                DetailTab.Timeline -> Timeline(place, state, actions)
                DetailTab.Map -> MapTab(place, actions)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                GlassButton(stringResource(R.string.places_edit_place), { actions.openEdit(place.id) }, Modifier.weight(1f), style = GlassButtonStyle.Secondary)
                GlassButton(stringResource(R.string.places_delete_place), actions::requestDelete, Modifier.weight(1f), style = GlassButtonStyle.Ghost)
            }
        }
    }
}

private fun DetailTab.labelRes(): Int = when (this) {
    DetailTab.Gallery -> R.string.places_tab_gallery
    DetailTab.Timeline -> R.string.places_tab_timeline
    DetailTab.Map -> R.string.places_tab_map
}

@Composable
private fun Gallery(place: PlaceMemory, uploading: Int, actions: PlacesActions) {
    val colors = TwinTheme.colors
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) actions.addGalleryPhotos(uris.map { it.toString() })
    }
    // Each cell is either a photo, a "uploading" tile, or the "+ Add" tile, laid out three to a row.
    val cells: List<Cell> = place.photos.mapIndexed { i, url -> Cell.Photo(i, url) } +
        List(uploading) { Cell.Busy } + Cell.Add
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        cells.chunked(GALLERY_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                row.forEach { cell ->
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(TwinTheme.shapes.small).background(colors.glass).border(1.dp, colors.line, TwinTheme.shapes.small)
                            .let { if (cell is Cell.Busy) it else it.clickable { if (cell is Cell.Photo) actions.openLightbox(cell.index) else picker.launch("image/*") } },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (cell) {
                            is Cell.Photo -> AsyncImage(cell.url, stringResource(R.string.places_photo_cd), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Cell.Busy -> Text("⏳", style = MaterialTheme.typography.titleLarge)
                            Cell.Add -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("+", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                                Text(stringResource(R.string.places_add_short), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                            }
                        }
                    }
                }
                repeat(GALLERY_COLUMNS - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

private sealed interface Cell {
    data class Photo(val index: Int, val url: String) : Cell
    data object Busy : Cell
    data object Add : Cell
}

@Composable
private fun Timeline(place: PlaceMemory, state: PlacesUiState, actions: PlacesActions) {
    val colors = TwinTheme.colors
    val spacing = TwinTheme.spacing
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            PlacesDateField(stringResource(R.string.places_note_date), state.note.date, actions::setNoteDate, onClear = { actions.setNoteDate(null) })
            TwinTextField(state.note.text, actions::setNoteText, stringResource(R.string.places_note_label), placeholder = stringResource(R.string.places_note_hint))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.note.favorite, actions::setNoteFavorite)
                Text(stringResource(R.string.places_note_favorite), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.weight(1f))
                GlassButton(stringResource(R.string.places_note_add), actions::addNote)
            }
        }
    }
    if (place.memories.isEmpty()) {
        EmptyState(title = stringResource(R.string.places_timeline_empty))
    } else {
        newestFirst(place.memories).forEach { NoteRow(it, actions) }
    }
}

@Composable
private fun NoteRow(note: MemoryNote, actions: PlacesActions) {
    val colors = TwinTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Text((if (note.favorite) "⭐ " else "") + formatVisitDate(note.date), style = MaterialTheme.typography.labelMedium, color = colors.accent)
        Text(note.text, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.padding(vertical = TwinTheme.spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Text(
                stringResource(if (note.favorite) R.string.places_note_unfavorite else R.string.places_note_make_favorite),
                style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.clickable { actions.toggleNoteFavorite(note.id) },
            )
            Text(stringResource(R.string.places_note_delete), style = MaterialTheme.typography.labelMedium, color = colors.error, modifier = Modifier.clickable { actions.deleteNote(note.id) })
        }
    }
}

@Composable
private fun MapTab(place: PlaceMemory, actions: PlacesActions) {
    val colors = TwinTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Text("📍", style = MaterialTheme.typography.headlineMedium)
            val lat = place.lat
            val lng = place.lng
            if (lat != null && lng != null) {
                Text(formatCoordinate(lat) + ", " + formatCoordinate(lng), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                GlassButton(stringResource(R.string.places_open_maps), actions::openMap)
            } else {
                Text(stringResource(R.string.places_no_location), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                GlassButton(stringResource(R.string.places_add_location), { actions.openEdit(place.id) })
            }
        }
    }
}

private val detailPlace = PlaceMemory(
    "p1", "Goa Beach Trip", "Goa, India", LocalDate.of(2026, 3, 14), "Sunsets and shacks", null, listOf("a", "b", "c", "d"), 15.2993, 74.124, true,
    listOf(MemoryNote("n1", LocalDate.of(2026, 3, 14), "Best sunset ever", true)),
)

private fun detailState(p: PlaceMemory?, tab: DetailTab) = PlacesUiState(
    places = if (p == null) UiState.Empty else UiState.Content(listOf(p)), detailId = p?.id, tab = tab,
)

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun DetailGalleryPreview() = TwinTheme { TwinBackground { PlaceDetailScreen(detailState(detailPlace, DetailTab.Gallery), PlacesActions.None) } }

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun DetailTimelinePreview() = TwinTheme { TwinBackground { PlaceDetailScreen(detailState(detailPlace, DetailTab.Timeline), PlacesActions.None) } }

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun DetailEmptyMapPreview() = TwinTheme { TwinBackground { PlaceDetailScreen(detailState(detailPlace.copy(lat = null, lng = null, memories = emptyList(), photos = emptyList()), DetailTab.Map), PlacesActions.None) } }

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun DetailMissingLargeFontPreview() = TwinTheme { TwinBackground { PlaceDetailScreen(detailState(null, DetailTab.Gallery), PlacesActions.None) } }
