package com.twinhearts.app.feature.livemap.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.livemap.domain.GeoMath
import com.twinhearts.app.feature.livemap.domain.LiveMapFormat
import com.twinhearts.app.feature.livemap.domain.model.SavedPlace
import com.twinhearts.app.feature.livemap.ui.LiveMapUiState
import com.twinhearts.app.feature.livemap.ui.LiveMapViewModel
import com.twinhearts.app.feature.livemap.ui.map.MapPoint

private val CATEGORIES = listOf("Home", "College", "Office", "Hostel", "Cafe", "Restaurant", "Gym", "Custom")

@Composable
private fun categoryLabel(cat: String): String = stringResource(
    when (cat) {
        "Home" -> R.string.livemap_cat_home
        "College" -> R.string.livemap_cat_college
        "Office" -> R.string.livemap_cat_office
        "Hostel" -> R.string.livemap_cat_hostel
        "Cafe" -> R.string.livemap_cat_cafe
        "Restaurant" -> R.string.livemap_cat_restaurant
        "Gym" -> R.string.livemap_cat_gym
        else -> R.string.livemap_cat_custom
    },
)

/** The "Places" tab: my places, my partner's places and favorites. */
@Composable
fun PlacesPage(state: LiveMapUiState, vm: LiveMapViewModel, modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var adding by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(TwinTheme.spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        SegmentedTabs(
            listOf(stringResource(R.string.livemap_places_mine), stringResource(R.string.livemap_places_partner, state.partnerName), stringResource(R.string.livemap_places_favorites)),
            tab, { tab = it },
        )
        when (tab) {
            2 -> FavoritesList(state, vm)
            else -> {
                val mine = tab == 0
                val list = state.shared.places.filter { (it.ownerRole == state.myRole) == mine }
                if (mine) GlassButton(stringResource(R.string.livemap_add_place), { adding = true }, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Add)
                if (list.isEmpty()) {
                    EmptyState(stringResource(R.string.livemap_places_empty_title), message = stringResource(R.string.livemap_places_empty_message), icon = Icons.Rounded.Place)
                } else {
                    list.forEach { PlaceCard(it, mine, state, vm) }
                }
            }
        }
    }
    if (adding) AddPlaceDialog(state, vm, onDismiss = { adding = false })
}

@Composable
private fun PlaceCard(place: SavedPlace, mine: Boolean, state: LiveMapUiState, vm: LiveMapViewModel) {
    val colors = TwinTheme.colors
    val me = state.myFix?.let { it.lat to it.lng } ?: state.positions.me?.let { it.lat to it.lng }
    GlassCard(Modifier.fillMaxWidth(), onClick = { vm.flyTo(MapPoint(place.lat, place.lng)) }, contentPadding = TwinTheme.spacing.m) {
        Text(place.name.ifBlank { categoryLabel(place.category) }, style = MaterialTheme.typography.titleSmall, color = colors.ink)
        val sub = listOfNotNull(
            place.address.takeIf { it.isNotBlank() },
            me?.let { LiveMapFormat.distance(GeoMath.distanceM(it.first, it.second, place.lat, place.lng)) },
        ).joinToString(" · ")
        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        Row(Modifier.padding(top = TwinTheme.spacing.s), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            GlassButton(stringResource(R.string.livemap_directions), { vm.directionsToPlace(place) }, Modifier.weight(1f), style = GlassButtonStyle.Secondary, leadingIcon = Icons.Rounded.Directions)
            if (mine) GlassButton(stringResource(R.string.livemap_delete), { vm.deletePlace(place.id) }, Modifier.weight(1f), style = GlassButtonStyle.Ghost, leadingIcon = Icons.Rounded.Delete)
        }
    }
}

@Composable
private fun FavoritesList(state: LiveMapUiState, vm: LiveMapViewModel) {
    val colors = TwinTheme.colors
    var renaming by rememberSaveable { mutableStateOf(-1) }
    var newName by rememberSaveable { mutableStateOf("") }
    val favorites = state.shared.favorites
    if (favorites.isEmpty()) {
        EmptyState(stringResource(R.string.livemap_fav_empty_title), message = stringResource(R.string.livemap_fav_empty_message))
        return
    }
    favorites.forEachIndexed { i, f ->
        GlassCard(Modifier.fillMaxWidth(), onClick = { vm.openFavorite(f) }, contentPadding = TwinTheme.spacing.m) {
            Text(f.name, style = MaterialTheme.typography.titleSmall, color = colors.ink)
            if (f.address.isNotBlank()) Text(f.address, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            Row(Modifier.padding(top = TwinTheme.spacing.s), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                TextButton(onClick = { renaming = i; newName = f.name }) { Text(stringResource(R.string.livemap_rename)) }
                TextButton(onClick = { vm.moveFavorite(i, -1) }, enabled = i > 0) { Text(stringResource(R.string.livemap_move_up)) }
                TextButton(onClick = { vm.moveFavorite(i, 1) }, enabled = i < favorites.lastIndex) { Text(stringResource(R.string.livemap_move_down)) }
                TextButton(onClick = { vm.removeFavorite(i) }) { Text(stringResource(R.string.livemap_delete)) }
            }
        }
    }
    if (renaming >= 0) {
        AlertDialog(
            onDismissRequest = { renaming = -1 },
            containerColor = colors.card,
            title = { Text(stringResource(R.string.livemap_rename), color = colors.ink) },
            text = { TwinTextField(newName, { newName = it }, stringResource(R.string.livemap_place_name), maxLength = 60) },
            confirmButton = { TextButton(onClick = { vm.renameFavorite(renaming, newName); renaming = -1 }) { Text(stringResource(R.string.livemap_save)) } },
            dismissButton = { TextButton(onClick = { renaming = -1 }) { Text(stringResource(R.string.livemap_close)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPlaceDialog(state: LiveMapUiState, vm: LiveMapViewModel, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    var category by rememberSaveable { mutableStateOf("Home") }
    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    val here = state.myFix?.let { MapPoint(it.lat, it.lng) } ?: state.positions.me?.let { MapPoint(it.lat, it.lng) }
    val needsName = category == "Custom" && name.isBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(stringResource(R.string.livemap_add_place), color = colors.ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    CATEGORIES.forEach { c -> TwinFilterChip(categoryLabel(c), category == c, { category = c }) }
                }
                if (category == "Custom") TwinTextField(name, { name = it }, stringResource(R.string.livemap_place_name), maxLength = 60)
                TwinTextField(address, { address = it }, stringResource(R.string.livemap_place_address), maxLength = 120)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    androidx.compose.material3.Icon(Icons.Rounded.MyLocation, null, tint = colors.accent)
                    Text(
                        if (here != null) stringResource(R.string.livemap_place_uses_current) else stringResource(R.string.livemap_msg_no_my_location),
                        style = MaterialTheme.typography.bodySmall, color = colors.inkMuted,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (here != null) { vm.addPlace(category, name, here.lat, here.lng, address); onDismiss() } },
                enabled = here != null && !needsName && !state.savingPlace,
            ) { Text(stringResource(R.string.livemap_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.livemap_close)) } },
    )
}
