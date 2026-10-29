package com.twinhearts.app.feature.places.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.places.domain.formatVisitDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Why this file exists: small pieces only Places uses (header, stat tiles, star, date field, notes box). */

/** Gold used for the favourite star. The web app uses amber `--yellow`. (Change Request: promote to a core token.) */
internal val FavoriteGold = Color(0xFFFBBF24)

@Composable
internal fun PlacesHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.core_back), onBack)
        Text(title, style = MaterialTheme.typography.titleLarge, color = TwinTheme.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

@Composable
internal fun StatTileRow(items: List<Triple<String, String, Color>>, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        items.forEach { (value, label, tint) ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(TwinTheme.shapes.card)
                    .background(colors.glass)
                    .border(1.dp, colors.line, TwinTheme.shapes.card)
                    .padding(vertical = TwinTheme.spacing.m, horizontal = TwinTheme.spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(value, style = MaterialTheme.typography.titleLarge, color = tint)
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The ⭐ button on cards and the detail banner. Filled when on. */
@Composable
internal fun StarButton(on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Box(
        modifier
            .size(size)
            .clip(TwinTheme.shapes.circle)
            .background(TwinTheme.colors.scrim)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (on) "⭐" else "☆", color = if (on) FavoriteGold else TwinTheme.colors.ink, style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlacesDateField(label: String, date: LocalDate?, onPick: (LocalDate) -> Unit, modifier: Modifier = Modifier, onClear: (() -> Unit)? = null) {
    val colors = TwinTheme.colors
    val shapes = TwinTheme.shapes
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.padding(bottom = TwinTheme.spacing.xs))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = shapes.fieldHeight)
                .clip(shapes.field)
                .background(colors.glass)
                .border(1.dp, colors.line, shapes.field)
                .clickable(role = Role.Button) { open = true }
                .padding(horizontal = TwinTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (date == null) stringResource(R.string.places_pick_date) else formatVisitDate(date),
                style = MaterialTheme.typography.bodyLarge,
                color = if (date == null) colors.inkFaint else colors.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (onClear != null && date != null) {
                Icon(
                    Icons.Rounded.Close, stringResource(R.string.places_clear_date), tint = colors.inkFaint,
                    modifier = Modifier.size(22.dp).clickable(role = Role.Button, onClick = onClear),
                )
            }
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text(stringResource(R.string.places_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.core_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

/** Multi-line glass box (description). TwinTextField is one line, so this one lives here. */
@Composable
internal fun PlacesTextArea(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val shapes = TwinTheme.shapes
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.padding(bottom = TwinTheme.spacing.xs))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(shapes.field).background(colors.glass)
                        .border(1.dp, colors.line, shapes.field).padding(TwinTheme.spacing.l),
                ) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                    inner()
                }
            },
        )
    }
}
