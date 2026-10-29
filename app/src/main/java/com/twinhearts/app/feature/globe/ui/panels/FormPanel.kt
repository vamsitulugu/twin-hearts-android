@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.twinhearts.app.feature.globe.ui.panels

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.globe.domain.CURRENCY_CHOICES
import com.twinhearts.app.feature.globe.domain.MOOD_CHOICES
import com.twinhearts.app.feature.globe.domain.WEATHER_CHOICES
import com.twinhearts.app.feature.globe.domain.formatDate
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.MemoryDraft
import com.twinhearts.app.feature.globe.domain.model.PlaceHit
import com.twinhearts.app.feature.globe.domain.model.StagedMedia
import com.twinhearts.app.feature.globe.ui.MemoryFormState
import com.twinhearts.app.feature.globe.ui.components.ChipListInput
import com.twinhearts.app.feature.globe.ui.components.GlobeTextArea
import com.twinhearts.app.feature.globe.ui.components.MediaPickers
import com.twinhearts.app.feature.globe.ui.components.SheetFrame
import com.twinhearts.app.feature.globe.ui.components.SmallPill
import com.twinhearts.app.feature.globe.ui.components.VoiceControl
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** What the form can ask its ViewModel and the page to do. */
class FormActions(
    val onClose: () -> Unit,
    val onLocationText: (String) -> Unit,
    val onPickPlace: (PlaceHit) -> Unit,
    val onEdit: ((MemoryDraft) -> MemoryDraft) -> Unit,
    val onSave: () -> Unit,
)

@Composable
fun BoxScope.FormPanel(
    wide: Boolean,
    state: MemoryFormState,
    cityTitle: String?,
    pickers: MediaPickers,
    voice: VoiceControl,
    actions: FormActions,
) {
    val draft = state.draft
    val colors = TwinTheme.colors
    var restaurantText by remember { mutableStateOf("") }
    var hotelText by remember { mutableStateOf("") }
    var giftText by remember { mutableStateOf("") }
    val title = when {
        state.isEditing -> stringResource(R.string.globe_form_title_edit)
        cityTitle != null -> stringResource(R.string.globe_form_title_city, cityTitle)
        else -> stringResource(R.string.globe_form_title_new)
    }

    SheetFrame(wide = wide, title = title, onClose = actions.onClose) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TwinTheme.spacing.l)
                .padding(bottom = TwinTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            TwinTextField(
                value = draft.locationText,
                onValueChange = actions.onLocationText,
                label = stringResource(R.string.globe_form_location),
                placeholder = stringResource(R.string.globe_form_location_hint),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.results.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TwinTheme.shapes.field)
                        .background(colors.glass)
                        .border(1.dp, colors.line, TwinTheme.shapes.field),
                ) {
                    state.results.forEach { hit ->
                        Text(
                            text = hit.displayName.ifBlank { hit.city },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { actions.onPickPlace(hit) }
                                .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
                        )
                    }
                }
            }

            TwinTextField(
                value = draft.tripName,
                onValueChange = { v -> actions.onEdit { it.copy(tripName = v) } },
                label = stringResource(R.string.globe_form_trip_name),
                placeholder = stringResource(R.string.globe_form_trip_name_hint),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                DateField(
                    label = stringResource(R.string.globe_form_from),
                    value = draft.dateFrom,
                    onChange = { v -> actions.onEdit { it.copy(dateFrom = v) } },
                    modifier = Modifier.weight(1f),
                )
                DateField(
                    label = stringResource(R.string.globe_form_to),
                    value = draft.dateTo,
                    onChange = { v -> actions.onEdit { it.copy(dateTo = v) } },
                    modifier = Modifier.weight(1f),
                )
            }

            ChoiceChips(
                label = stringResource(R.string.globe_form_mood),
                choices = MOOD_CHOICES,
                selected = draft.mood,
                onSelect = { v -> actions.onEdit { it.copy(mood = v) } },
            )
            ChoiceChips(
                label = stringResource(R.string.globe_form_weather),
                choices = WEATHER_CHOICES,
                selected = draft.weather,
                onSelect = { v -> actions.onEdit { it.copy(weather = v) } },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                TwinTextField(
                    value = draft.temperature,
                    onValueChange = { v -> actions.onEdit { it.copy(temperature = v.filter { c -> c.isDigit() || c == '-' || c == '.' }) } },
                    label = stringResource(R.string.globe_form_temp),
                    placeholder = stringResource(R.string.globe_form_temp_hint),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                TwinTextField(
                    value = draft.cost,
                    onValueChange = { v -> actions.onEdit { it.copy(cost = v.filter { c -> c.isDigit() || c == '.' }) } },
                    label = stringResource(R.string.globe_form_cost),
                    placeholder = stringResource(R.string.globe_form_cost_hint),
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
            }
            ChoiceChips(
                label = stringResource(R.string.globe_form_currency),
                choices = CURRENCY_CHOICES,
                selected = draft.currency,
                onSelect = { v -> actions.onEdit { it.copy(currency = v.ifBlank { "INR" }) } },
                allowClear = false,
            )

            GlobeTextArea(
                value = draft.notes,
                onValueChange = { v -> actions.onEdit { it.copy(notes = v) } },
                label = stringResource(R.string.globe_form_notes),
                hint = stringResource(R.string.globe_form_notes_hint),
            )
            GlobeTextArea(
                value = draft.favoriteMoment,
                onValueChange = { v -> actions.onEdit { it.copy(favoriteMoment = v) } },
                label = stringResource(R.string.globe_form_favorite),
                hint = stringResource(R.string.globe_form_favorite_hint),
                minLines = 2,
            )

            ChipListInput(
                label = stringResource(R.string.globe_form_restaurants),
                hint = stringResource(R.string.globe_form_restaurants_hint),
                items = draft.restaurants,
                draft = restaurantText,
                onDraftChange = { restaurantText = it },
                onChange = { list -> actions.onEdit { it.copy(restaurants = list) } },
            )
            ChipListInput(
                label = stringResource(R.string.globe_form_hotels),
                hint = stringResource(R.string.globe_form_hotels_hint),
                items = draft.hotels,
                draft = hotelText,
                onDraftChange = { hotelText = it },
                onChange = { list -> actions.onEdit { it.copy(hotels = list) } },
            )
            ChipListInput(
                label = stringResource(R.string.globe_form_gifts),
                hint = stringResource(R.string.globe_form_gifts_hint),
                items = draft.gifts,
                draft = giftText,
                onDraftChange = { giftText = it },
                onChange = { list -> actions.onEdit { it.copy(gifts = list) } },
            )

            Text(
                stringResource(R.string.globe_form_media),
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkMuted,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                SmallPill(text = stringResource(R.string.globe_form_photos), onClick = pickers.pickPhotos)
                SmallPill(text = stringResource(R.string.globe_form_video), onClick = pickers.pickVideo)
                SmallPill(
                    text = stringResource(if (voice.recording) R.string.globe_form_stop else R.string.globe_form_voice),
                    onClick = voice.toggle,
                    primary = voice.recording,
                )
            }
            if (state.staged.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.staged.forEach { StagedChip(it) }
                }
            }

            GlobeTextArea(
                value = draft.journal,
                onValueChange = { v -> actions.onEdit { it.copy(journal = v) } },
                label = stringResource(R.string.globe_form_journal),
                hint = stringResource(R.string.globe_form_journal_hint),
            )

            GlassButton(
                text = stringResource(if (state.isEditing) R.string.globe_form_update else R.string.globe_form_pin),
                onClick = actions.onSave,
                loading = state.saving,
                enabled = !state.saving && !state.uploading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A row of single-choice chips. Tapping the chosen one again clears it (unless [allowClear] is false). */
@Composable
private fun ChoiceChips(label: String, choices: List<String>, selected: String, onSelect: (String) -> Unit, allowClear: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TwinTheme.colors.inkMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            choices.forEach { choice ->
                TwinFilterChip(
                    label = choice,
                    selected = choice == selected,
                    onClick = { onSelect(if (choice == selected && allowClear) "" else choice) },
                )
            }
        }
    }
}

/** A tappable date box that opens the calendar. A small cross clears the date. */
@Composable
private fun DateField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    var open by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.padding(bottom = TwinTheme.spacing.xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TwinTheme.shapes.fieldHeight)
                .clip(shape)
                .background(colors.glass)
                .border(1.dp, colors.line, shape)
                .clickable(role = Role.Button) { open = true }
                .padding(horizontal = TwinTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (value.isBlank()) stringResource(R.string.globe_form_pick_date) else formatDate(value),
                style = MaterialTheme.typography.bodyMedium,
                color = if (value.isBlank()) colors.inkFaint else colors.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (value.isNotBlank()) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.globe_form_remove_chip),
                    tint = colors.inkMuted,
                    modifier = Modifier.size(20.dp).clickable { onChange("") },
                )
            }
        }
    }
    if (open) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = isoToMillis(value))
        DatePickerDialog(
            onDismissRequest = { open = false },
            containerColor = colors.card,
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onChange(millisToIso(it)) }
                    open = false
                }) { Text(stringResource(R.string.globe_save), color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.globe_cancel), color = colors.inkMuted) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun isoToMillis(iso: String): Long? =
    runCatching { LocalDate.parse(iso.take(ISO_DATE_LENGTH)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()

private fun millisToIso(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

private const val ISO_DATE_LENGTH = 10

/** A file that was picked or recorded: a spinner while it uploads, then a small label. */
@Composable
private fun StagedChip(item: StagedMedia) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .clip(TwinTheme.shapes.pill)
            .background(colors.glass)
            .border(1.dp, colors.line, TwinTheme.shapes.pill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (item.uploading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = colors.accent,
            )
        }
        val icon = when (item.kind) {
            MediaKind.Photo -> "📸"
            MediaKind.Video -> "🎬"
            MediaKind.Voice -> "🎙️"
            MediaKind.Journal -> "📖"
        }
        Text(
            text = if (item.duration.isNotBlank()) "$icon ${item.duration}" else icon,
            style = MaterialTheme.typography.labelLarge,
            color = colors.ink,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
