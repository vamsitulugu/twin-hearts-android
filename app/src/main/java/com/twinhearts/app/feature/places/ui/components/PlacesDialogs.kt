package com.twinhearts.app.feature.places.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.places.ui.PlaceForm
import com.twinhearts.app.feature.places.ui.PlacesActions
import java.time.LocalDate

/** Why this file exists: the pop-ups of Places: Add/Edit form, delete confirm, and the full-screen photo viewer. */

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaceFormDialog(form: PlaceForm, saving: Boolean, actions: PlacesActions) {
    FormContent(form, saving, actions)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormContent(form: PlaceForm, saving: Boolean, actions: PlacesActions) {
    val colors = TwinTheme.colors
    val spacing = TwinTheme.spacing
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { actions.pickBanner(it.toString()) } }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) actions.addFormPhotos(uris.map { it.toString() })
    }
    Dialog(onDismissRequest = actions::closeForm, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            Modifier.fillMaxSize().background(colors.card).statusBarsPadding().navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(spacing.screenEdge),
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (form.editId == null) R.string.places_new_title else R.string.places_edit_title),
                    style = MaterialTheme.typography.titleLarge, color = colors.ink, modifier = Modifier.weight(1f),
                )
                Icon(Icons.Rounded.Close, stringResource(R.string.places_close), tint = colors.ink, modifier = Modifier.size(28.dp).clickable(onClick = actions::closeForm))
            }
            TwinTextField(
                form.name, actions::setName, stringResource(R.string.places_field_name),
                placeholder = stringResource(R.string.places_field_name_hint),
                error = if (form.nameError) stringResource(R.string.places_toast_name_needed) else null,
            )
            TwinTextField(form.location, actions::setLocation, stringResource(R.string.places_field_location), placeholder = stringResource(R.string.places_field_location_hint))
            PlacesDateField(stringResource(R.string.places_field_date), form.date, actions::setDate, onClear = { actions.setDate(null) })
            PlacesTextArea(form.description, actions::setDescription, stringResource(R.string.places_field_description), stringResource(R.string.places_field_description_hint))

            Text(stringResource(R.string.places_field_banner), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
            Box(
                Modifier.fillMaxWidth().height(120.dp).clip(TwinTheme.shapes.field).background(colors.glass).border(1.dp, colors.line, TwinTheme.shapes.field),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    form.bannerUploading -> Text(stringResource(R.string.places_uploading), color = colors.inkMuted)
                    form.banner != null -> AsyncImage(form.banner, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else -> Text(stringResource(R.string.places_no_banner), color = colors.inkFaint)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                GlassButton(stringResource(R.string.places_upload_banner), { bannerPicker.launch("image/*") }, style = GlassButtonStyle.Secondary)
                if (form.banner != null) GlassButton(stringResource(R.string.places_remove), actions::removeBanner, style = GlassButtonStyle.Ghost)
            }

            Text(stringResource(R.string.places_field_photos), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.s), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                form.photos.forEachIndexed { i, url ->
                    Box(Modifier.size(72.dp).clip(TwinTheme.shapes.small)) {
                        AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Icon(
                            Icons.Rounded.Close, stringResource(R.string.places_remove_photo), tint = colors.ink,
                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(colors.scrim).clickable { actions.removeFormPhoto(i) },
                        )
                    }
                }
                repeat(form.photosUploading) {
                    Box(Modifier.size(72.dp).clip(TwinTheme.shapes.small).background(colors.glass), contentAlignment = Alignment.Center) { Text("⏳") }
                }
            }
            GlassButton(stringResource(R.string.places_add_photos), { photoPicker.launch("image/*") }, style = GlassButtonStyle.Secondary)

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                TwinTextField(form.lat, actions::setLat, stringResource(R.string.places_field_lat), Modifier.weight(1f), placeholder = "15.2993", keyboardType = KeyboardType.Decimal)
                TwinTextField(form.lng, actions::setLng, stringResource(R.string.places_field_lng), Modifier.weight(1f), placeholder = "74.1240", keyboardType = KeyboardType.Decimal)
            }
            GlassButton(stringResource(R.string.places_use_location), actions::useMyLocation, Modifier.fillMaxWidth(), style = GlassButtonStyle.Secondary, loading = form.locating)

            Row(
                Modifier.fillMaxWidth().clip(TwinTheme.shapes.field).background(if (form.favorite) FavoriteGold.copy(alpha = 0.18f) else colors.glass)
                    .border(1.dp, colors.line, TwinTheme.shapes.field).clickable { actions.setFormFavorite(!form.favorite) }.padding(spacing.l),
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
            ) {
                Text(if (form.favorite) "⭐" else "☆", style = MaterialTheme.typography.titleMedium, color = FavoriteGold)
                Text(stringResource(R.string.places_mark_favorite), style = MaterialTheme.typography.bodyLarge, color = colors.ink)
            }
            GlassButton(stringResource(R.string.places_save), actions::savePlace, Modifier.fillMaxWidth(), enabled = !form.busy, loading = saving)
        }
    }
}

@Composable
internal fun DeletePlaceDialog(actions: PlacesActions) {
    AlertDialog(
        onDismissRequest = actions::cancelDelete,
        title = { Text(stringResource(R.string.places_delete_title)) },
        text = { Text(stringResource(R.string.places_delete_message)) },
        confirmButton = { TextButton(onClick = actions::confirmDelete) { Text(stringResource(R.string.places_delete)) } },
        dismissButton = { TextButton(onClick = actions::cancelDelete) { Text(stringResource(R.string.core_cancel)) } },
    )
}

@Composable
internal fun PhotoViewer(photos: List<String>, startIndex: Int, actions: PlacesActions) {
    val colors = TwinTheme.colors
    if (photos.isEmpty()) return
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, photos.lastIndex)) { photos.size }
    LaunchedEffect(startIndex) { pager.scrollToPage(startIndex.coerceIn(0, photos.lastIndex)) }
    Dialog(onDismissRequest = actions::closeLightbox, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(colors.scrim)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                AsyncImage(photos[page], stringResource(R.string.places_photo_cd), Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Icon(
                Icons.Rounded.Close, stringResource(R.string.places_close), tint = colors.ink,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(TwinTheme.spacing.l).size(32.dp).clickable(onClick = actions::closeLightbox),
            )
            if (photos.size > 1) {
                Text(
                    "${pager.currentPage + 1} / ${photos.size}", style = MaterialTheme.typography.labelLarge, color = colors.ink,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(TwinTheme.spacing.l),
                )
            }
            GlassButton(
                stringResource(R.string.places_remove_photo_full), { actions.removeLightboxPhoto() },
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(TwinTheme.spacing.l), style = GlassButtonStyle.Ghost,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun FormPreview() = TwinTheme { FormContent(PlaceForm(name = "Goa Beach Trip", date = LocalDate.of(2026, 3, 14), favorite = true), false, PlacesActions.None) }

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun FormLargeFontPreview() = TwinTheme { FormContent(PlaceForm(nameError = true, bannerUploading = true, photosUploading = 2), true, PlacesActions.None) }
