package com.twinhearts.app.feature.meetplanner.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.meetplanner.domain.WhenLabel
import com.twinhearts.app.feature.meetplanner.domain.formatDate
import com.twinhearts.app.feature.meetplanner.domain.model.GeoPoint
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.domain.model.TravelMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/*
 * Why this file exists: the small pieces every Meet Planner screen shares (header, date and time
 * pickers, stat row, travel-mode row, place thumbnail, route drawing). Kept inside the feature
 * (R-31); the ones other features will want are listed as a Change Request in SHARED_CHANGES.md.
 */

/** Colours that only Meet Planner needs (the category gradients and amber from the web CSS). See Change Request in SHARED_CHANGES.md. */
internal object MeetPalette {
    val amber = Color(0xFFFBBF24)

    private fun pair(a: Long, b: Long) = Brush.linearGradient(listOf(Color(a), Color(b)))

    fun gradient(category: PlaceCategory): Brush = when (category) {
        PlaceCategory.Hotel -> pair(0xFFB0127A, 0xFF7C0A57)
        PlaceCategory.Tourist -> pair(0xFFF97316, 0xFF7C2D12)
        PlaceCategory.Cafe -> pair(0xFFB45309, 0xFF431407)
        PlaceCategory.Restaurant -> pair(0xFF7C0A57, 0xFF4C0519)
        PlaceCategory.Mall -> pair(0xFFB0127A, 0xFF500724)
        PlaceCategory.Park -> pair(0xFF16A34A, 0xFF052E16)
        PlaceCategory.Museum -> pair(0xFFB0127A, 0xFF49082A)
        PlaceCategory.Temple -> pair(0xFFF59E0B, 0xFF78350F)
        PlaceCategory.Entertainment -> pair(0xFFC21A8B, 0xFF7C0A57)
        PlaceCategory.Custom -> pair(0xFFB0127A, 0xFF5B0A1F)
    }
}

@DrawableRes
internal fun destinationImage(name: String): Int? = when (name) {
    "Bengaluru" -> R.drawable.meetplanner_dest_bengaluru
    "Hyderabad" -> R.drawable.meetplanner_dest_hyderabad
    "Visakhapatnam" -> R.drawable.meetplanner_dest_visakhapatnam
    "Goa" -> R.drawable.meetplanner_dest_goa
    "Mysuru" -> R.drawable.meetplanner_dest_mysuru
    "Ooty" -> R.drawable.meetplanner_dest_ooty
    "Coorg" -> R.drawable.meetplanner_dest_coorg
    "Chennai" -> R.drawable.meetplanner_dest_chennai
    "Munnar" -> R.drawable.meetplanner_dest_munnar
    "Pondicherry" -> R.drawable.meetplanner_dest_pondicherry
    else -> null
}

@Composable
internal fun WhenLabel.asText(): String = when (this) {
    WhenLabel.Today -> stringResource(R.string.meetplanner_when_today)
    WhenLabel.Tomorrow -> stringResource(R.string.meetplanner_when_tomorrow)
    is WhenLabel.InDays -> stringResource(R.string.meetplanner_when_in_days, days)
    is WhenLabel.DaysAgo -> stringResource(R.string.meetplanner_when_days_ago, days)
}

@Composable
internal fun TravelMode.label(): String = stringResource(
    when (this) {
        TravelMode.Car -> R.string.meetplanner_mode_car
        TravelMode.Bike -> R.string.meetplanner_mode_bike
        TravelMode.Cycle -> R.string.meetplanner_mode_cycle
        TravelMode.Walk -> R.string.meetplanner_mode_walk
        TravelMode.Transit -> R.string.meetplanner_mode_transit
    },
)

@Composable
internal fun PlaceCategory.label(): String = stringResource(
    when (this) {
        PlaceCategory.Hotel -> R.string.meetplanner_cat_hotel
        PlaceCategory.Tourist -> R.string.meetplanner_cat_tourist
        PlaceCategory.Cafe -> R.string.meetplanner_cat_cafe
        PlaceCategory.Restaurant -> R.string.meetplanner_cat_restaurant
        PlaceCategory.Mall -> R.string.meetplanner_cat_mall
        PlaceCategory.Park -> R.string.meetplanner_cat_park
        PlaceCategory.Museum -> R.string.meetplanner_cat_museum
        PlaceCategory.Temple -> R.string.meetplanner_cat_temple
        PlaceCategory.Entertainment -> R.string.meetplanner_cat_entertainment
        PlaceCategory.Custom -> R.string.meetplanner_cat_custom
    },
)

@Composable
internal fun PlaceCategory.chipLabel(): String = stringResource(
    when (this) {
        PlaceCategory.Hotel -> R.string.meetplanner_chip_hotel
        PlaceCategory.Tourist -> R.string.meetplanner_chip_tourist
        PlaceCategory.Cafe -> R.string.meetplanner_chip_cafe
        PlaceCategory.Restaurant -> R.string.meetplanner_chip_restaurant
        PlaceCategory.Mall -> R.string.meetplanner_chip_mall
        PlaceCategory.Park -> R.string.meetplanner_chip_park
        PlaceCategory.Museum -> R.string.meetplanner_chip_museum
        PlaceCategory.Temple -> R.string.meetplanner_chip_temple
        PlaceCategory.Entertainment -> R.string.meetplanner_chip_entertainment
        PlaceCategory.Custom -> R.string.meetplanner_cat_custom
    },
)

/** Back button, title and optional subtitle, with an optional action on the right. Pads for the status bar. */
@Composable
internal fun MeetHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = TwinTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.core_back), onBack)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing?.invoke(this)
    }
}

/** Small rounded label, e.g. "Today", "3 selected". */
@Composable
internal fun MeetPill(text: String, modifier: Modifier = Modifier, color: Color = TwinTheme.colors.ink, background: Color = TwinTheme.colors.brand) {
    Box(
        modifier = modifier
            .clip(TwinTheme.shapes.pill)
            .background(background)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

@Composable
internal fun MeetStatsRow(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        items.forEach { (value, label) ->
            GlassCard(Modifier.weight(1f), contentPadding = TwinTheme.spacing.m) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 1)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = colors.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun ModeRow(selected: TravelMode, onSelect: (TravelMode) -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        TravelMode.entries.forEach { mode ->
            val on = mode == selected
            val shape = TwinTheme.shapes.small
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (on) colors.brand.copy(alpha = 0.55f) else colors.glass)
                    .border(1.dp, if (on) colors.accent else colors.line, shape)
                    .clickable(role = Role.RadioButton) { onSelect(mode) }
                    .padding(vertical = TwinTheme.spacing.m),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(mode.emoji, style = MaterialTheme.typography.titleLarge)
                Text(mode.label(), style = MaterialTheme.typography.labelSmall, color = if (on) colors.ink else colors.inkMuted, maxLines = 1)
            }
        }
    }
}

/** Gradient square with the place's emoji (stands in for a photo, like the web app). */
@Composable
internal fun PlaceThumb(place: Place, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 56.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(TwinTheme.shapes.small)
            .background(MeetPalette.gradient(place.category)),
        contentAlignment = Alignment.Center,
    ) {
        Text(place.emoji, style = MaterialTheme.typography.titleLarge)
    }
}

/** A date shown as a field; tapping opens the system-style calendar. [onClear] adds a small x. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeetDateField(
    label: String,
    date: LocalDate?,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    val colors = TwinTheme.colors
    val shapes = TwinTheme.shapes
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.padding(bottom = TwinTheme.spacing.xs))
        Row(
            modifier = Modifier
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
                text = date?.let { formatDate(it) } ?: stringResource(R.string.meetplanner_pick_date),
                style = MaterialTheme.typography.bodyLarge,
                color = if (date == null) colors.inkFaint else colors.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (onClear != null && date != null) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.meetplanner_clear_date),
                    tint = colors.inkFaint,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(role = Role.Button, onClick = onClear),
                )
            }
        }
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text(stringResource(R.string.meetplanner_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.core_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeetTimeDialog(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinutes / 60, initialMinute = initialMinutes % 60, is24Hour = false)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.meetplanner_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) } },
        text = { TimePicker(state = state) },
    )
}

/** A multi-line glass field (notes). TwinTextField is single-line, so this one lives here. */
@Composable
internal fun MeetTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
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
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .clip(shapes.field)
                        .background(colors.glass)
                        .border(1.dp, colors.line, shapes.field)
                        .padding(TwinTheme.spacing.l),
                ) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                    inner()
                }
            },
        )
    }
}

/** The "Search places, cafes, hotels..." box: one line, search key on the keyboard. */
@Composable
internal fun MeetSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val shapes = TwinTheme.shapes
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = shapes.fieldHeight)
                    .clip(shapes.field)
                    .background(colors.glass)
                    .border(1.dp, colors.line, shapes.field)
                    .padding(horizontal = TwinTheme.spacing.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.inkFaint, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f).padding(start = TwinTheme.spacing.s)) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint, maxLines = 1)
                    inner()
                }
            }
        },
    )
}

/**
 * Draws the route as a line with numbered stops. It is a plain drawing, not a tile map: the version
 * catalog has no map library yet (R-11 / R-15), so "Start Navigation" opens the phone's maps app.
 */
@Composable
internal fun RouteCanvas(points: List<GeoPoint>, stops: List<Place>, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = colors.ink)
    Canvas(modifier) {
        val all = points + stops.map { GeoPoint(it.lat, it.lng) }
        if (all.isEmpty()) return@Canvas
        val minLat = all.minOf { it.lat }
        val maxLat = all.maxOf { it.lat }
        val minLng = all.minOf { it.lng }
        val maxLng = all.maxOf { it.lng }
        // Longitude degrees are narrower away from the equator; this keeps the shape honest.
        val kx = cos(Math.toRadians((minLat + maxLat) / 2))
        val w = (maxLng - minLng) * kx
        val h = maxLat - minLat
        val pad = 28.dp.toPx()
        val availW = (size.width - 2 * pad).toDouble()
        val availH = (size.height - 2 * pad).toDouble()
        val scale = min(availW / max(w, TINY), availH / max(h, TINY))
        fun place(p: GeoPoint) = Offset(
            x = (pad + (p.lng - minLng) * kx * scale + (availW - w * scale) / 2).toFloat(),
            y = (pad + (maxLat - p.lat) * scale + (availH - h * scale) / 2).toFloat(),
        )
        if (points.size >= 2) {
            val path = Path()
            points.forEachIndexed { i, p -> place(p).let { o -> if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) } }
            drawPath(path, colors.accent, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        stops.forEachIndexed { i, s ->
            val o = place(GeoPoint(s.lat, s.lng))
            drawCircle(colors.brand, radius = 13.dp.toPx(), center = o)
            drawCircle(colors.ink, radius = 13.dp.toPx(), center = o, style = Stroke(width = 2.dp.toPx()))
            val text = measurer.measure("${i + 1}", labelStyle)
            drawText(text, topLeft = Offset(o.x - text.size.width / 2f, o.y - text.size.height / 2f))
        }
    }
}

private const val TINY = 1e-9
