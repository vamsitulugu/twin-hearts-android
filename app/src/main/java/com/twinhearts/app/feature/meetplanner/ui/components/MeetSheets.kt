package com.twinhearts.app.feature.meetplanner.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.meetplanner.domain.model.MeetMoods
import com.twinhearts.app.feature.meetplanner.domain.model.Meetup
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.ui.MeetActions
import com.twinhearts.app.feature.meetplanner.ui.MeetUiState

/** Why this file exists: all pop-ups of Meet Planner (place sheet, options, complete, edit, delete, photo viewer) in one place. */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaceSheet(place: Place, selected: Boolean, actions: MeetActions, onOpenUrl: (String) -> Unit) {
    val colors = TwinTheme.colors
    ModalBottomSheet(onDismissRequest = actions::closePlace, containerColor = colors.card) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(TwinTheme.spacing.xl), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Text("${place.emoji}  ${place.name}", style = MaterialTheme.typography.titleLarge, color = colors.ink)
            place.address?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted) }
            GlassButton(
                stringResource(if (selected) R.string.meetplanner_remove_from_plan else R.string.meetplanner_add_to_plan),
                { actions.toggleStop(place); actions.closePlace() },
                Modifier.fillMaxWidth(),
            )
            GlassButton(stringResource(R.string.meetplanner_get_directions), { onOpenUrl(com.twinhearts.app.feature.meetplanner.domain.placeDirectionsUrl(place)) }, Modifier.fillMaxWidth(), style = GlassButtonStyle.Secondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OptionsSheet(plan: Meetup, actions: MeetActions) {
    val colors = TwinTheme.colors
    ModalBottomSheet(onDismissRequest = actions::closeOptions, containerColor = colors.card) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = TwinTheme.spacing.xl)) {
            Text(stringResource(R.string.meetplanner_options_title), style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.padding(horizontal = TwinTheme.spacing.xl, vertical = TwinTheme.spacing.s))
            val rows = buildList {
                add(R.string.meetplanner_opt_edit to { actions.closeOptions(); actions.openEdit(plan.id) })
                add(R.string.meetplanner_opt_route to { actions.closeOptions(); actions.openRouteSaved(plan.id) })
                if (!plan.isCompleted) add(R.string.meetplanner_opt_complete to { actions.closeOptions(); actions.openComplete(plan.id) })
                if (plan.isCompleted && plan.globeMemoryId != null) add(R.string.meetplanner_opt_photos to { actions.closeOptions(); actions.viewPhotos(plan.id) })
                add(R.string.meetplanner_opt_share to { actions.closeOptions(); actions.share(plan.id) })
                add(R.string.meetplanner_opt_copy to { actions.closeOptions(); actions.copy(plan.id) })
                add(R.string.meetplanner_opt_delete to { actions.closeOptions(); actions.requestDelete(plan.id) })
            }
            rows.forEach { (label, tap) ->
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (label == R.string.meetplanner_opt_delete) colors.error else colors.ink,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = tap).padding(horizontal = TwinTheme.spacing.xl, vertical = TwinTheme.spacing.l),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CompleteDialog(state: MeetUiState, actions: MeetActions) {
    val colors = TwinTheme.colors
    var mood by rememberSaveable { mutableStateOf<String?>(null) }
    var extra by rememberSaveable { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) actions.addPhotos(uris.map { it.toString() })
    }
    Dialog(onDismissRequest = actions::closeComplete) {
        Column(
            Modifier.clip(TwinTheme.shapes.card).background(colors.card).verticalScroll(rememberScrollState()).padding(TwinTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(stringResource(R.string.meetplanner_complete_title), style = MaterialTheme.typography.titleLarge, color = colors.ink)
            Text(stringResource(R.string.meetplanner_complete_sub), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            Text(stringResource(R.string.meetplanner_complete_mood), style = MaterialTheme.typography.labelLarge, color = colors.inkMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                MeetMoods.forEach { m -> TwinFilterChip(m, mood == m, { mood = m }) }
            }
            Text(stringResource(R.string.meetplanner_complete_photos), style = MaterialTheme.typography.labelLarge, color = colors.inkMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                state.completePhotos.forEachIndexed { i, p ->
                    Box(Modifier.size(64.dp).clip(TwinTheme.shapes.small)) {
                        AsyncImage(p.uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Icon(
                            Icons.Rounded.Close, stringResource(R.string.meetplanner_remove_photo), tint = colors.ink,
                            modifier = Modifier.align(Alignment.TopEnd).size(22.dp).background(colors.scrim).clickable { actions.removePhoto(i) },
                        )
                    }
                }
                Box(
                    Modifier.size(64.dp).clip(TwinTheme.shapes.small).background(colors.glass).clickable { picker.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) { Text("＋", color = colors.ink, style = MaterialTheme.typography.titleLarge) }
            }
            if (state.uploadingCount > 0) Text(pluralStringResource(R.plurals.meetplanner_uploading, state.uploadingCount, state.uploadingCount), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            MeetTextArea(extra, { extra = it }, stringResource(R.string.meetplanner_complete_extra), stringResource(R.string.meetplanner_complete_extra_hint))
            GlassButton(
                stringResource(R.string.meetplanner_complete_save), { actions.confirmComplete(mood, extra) }, Modifier.fillMaxWidth(),
                enabled = state.uploadingCount == 0, loading = state.completing,
            )
            GlassButton(stringResource(R.string.core_cancel), actions::closeComplete, Modifier.fillMaxWidth(), style = GlassButtonStyle.Ghost)
        }
    }
}

@Composable
internal fun EditDialog(plan: Meetup, actions: MeetActions) {
    val colors = TwinTheme.colors
    var title by rememberSaveable(plan.id) { mutableStateOf(plan.title) }
    var from by rememberSaveable(plan.id) { mutableStateOf(plan.meetDate) }
    var to by rememberSaveable(plan.id) { mutableStateOf(plan.endDate) }
    var budget by rememberSaveable(plan.id) { mutableStateOf(plan.budget?.takeIf { it > 0 }?.toLong()?.toString().orEmpty()) }
    var notes by rememberSaveable(plan.id) { mutableStateOf(plan.notes) }
    Dialog(onDismissRequest = actions::closeEdit) {
        Column(
            Modifier.clip(TwinTheme.shapes.card).background(colors.card).verticalScroll(rememberScrollState()).padding(TwinTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(stringResource(R.string.meetplanner_edit_title), style = MaterialTheme.typography.titleLarge, color = colors.ink)
            TwinTextField(title, { title = it }, label = stringResource(R.string.meetplanner_field_title))
            MeetDateField(stringResource(R.string.meetplanner_from), from, { from = it }, onClear = { from = null })
            MeetDateField(stringResource(R.string.meetplanner_to), to, { to = it }, onClear = { to = null })
            TwinTextField(budget, { budget = it.filter(Char::isDigit) }, label = stringResource(R.string.meetplanner_field_budget))
            MeetTextArea(notes, { notes = it }, stringResource(R.string.meetplanner_notes), stringResource(R.string.meetplanner_notes_hint))
            GlassButton(stringResource(R.string.meetplanner_edit_save), { actions.saveEdit(plan.id, title, from, to, budget, notes) }, Modifier.fillMaxWidth())
            GlassButton(stringResource(R.string.core_cancel), actions::closeEdit, Modifier.fillMaxWidth(), style = GlassButtonStyle.Ghost)
        }
    }
}

@Composable
internal fun DeleteDialog(count: Int, actions: MeetActions) {
    AlertDialog(
        onDismissRequest = actions::cancelDelete,
        title = { Text(stringResource(R.string.meetplanner_delete_title)) },
        text = { Text(pluralStringResource(R.plurals.meetplanner_delete_message, count)) },
        confirmButton = { TextButton(onClick = actions::confirmDelete) { Text(stringResource(R.string.meetplanner_delete)) } },
        dismissButton = { TextButton(onClick = actions::cancelDelete) { Text(stringResource(R.string.core_cancel)) } },
    )
}

@Composable
internal fun PhotoViewer(urls: List<String>, onClose: () -> Unit) {
    val pager = rememberPagerState { urls.size }
    Dialog(onDismissRequest = onClose) {
        Box(Modifier.fillMaxSize().background(TwinTheme.colors.scrim)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                AsyncImage(urls[page], null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Icon(
                Icons.Rounded.Close, stringResource(R.string.meetplanner_close), tint = TwinTheme.colors.ink,
                modifier = Modifier.align(Alignment.TopEnd).padding(TwinTheme.spacing.l).size(32.dp).clickable(onClick = onClose),
            )
        }
    }
}
