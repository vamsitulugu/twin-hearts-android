package com.twinhearts.app.feature.livemap.ui.nav

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.livemap.data.nav.NavPhrases
import com.twinhearts.app.feature.livemap.domain.LiveMapFormat
import com.twinhearts.app.feature.livemap.domain.nav.NavRoute
import com.twinhearts.app.feature.livemap.domain.nav.TravelMode
import com.twinhearts.app.feature.livemap.ui.Destination
import java.util.Date
import kotlin.math.roundToInt

/**
 * The "directions" part of the destination card: Drive / Walk / Bike chips, up to three route
 * options with time and arrival, a Start button and an expandable list of steps.
 */
@Composable
fun DirectionsSection(
    destination: Destination,
    onMode: (TravelMode) -> Unit,
    onSelect: (Int) -> Unit,
    onStart: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    var stepsOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            TravelMode.entries.forEach { mode ->
                GlassButton(
                    text = stringResource(
                        when (mode) {
                            TravelMode.Drive -> R.string.livemap_dir_mode_drive
                            TravelMode.Walk -> R.string.livemap_dir_mode_walk
                            TravelMode.Bike -> R.string.livemap_dir_mode_bike
                        },
                    ),
                    onClick = { onMode(mode) },
                    modifier = Modifier.weight(1f),
                    style = if (destination.mode == mode) GlassButtonStyle.Primary else GlassButtonStyle.Secondary,
                    leadingIcon = mode.icon(),
                )
            }
        }
        when {
            destination.routeLoading -> Text(
                stringResource(R.string.livemap_route_loading),
                style = MaterialTheme.typography.bodyMedium, color = colors.accent,
            )
            destination.routes.isEmpty() -> {
                Text(
                    stringResource(if (destination.routeFailed) R.string.livemap_dir_no_route else R.string.livemap_route_failed),
                    style = MaterialTheme.typography.bodyMedium, color = colors.error,
                )
                GlassButton(stringResource(R.string.livemap_dir_retry), onRetry, Modifier.fillMaxWidth(), style = GlassButtonStyle.Secondary)
            }
            else -> {
                destination.routes.forEachIndexed { index, route ->
                    RouteOption(route, index, selected = index == destination.selected, onClick = { onSelect(index) })
                }
                GlassButton(
                    text = stringResource(R.string.livemap_dir_start),
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Rounded.Navigation,
                )
                GlassButton(
                    text = stringResource(if (stepsOpen) R.string.livemap_dir_hide_steps else R.string.livemap_dir_steps),
                    onClick = { stepsOpen = !stepsOpen },
                    modifier = Modifier.fillMaxWidth(),
                    style = GlassButtonStyle.Ghost,
                )
                if (stepsOpen) destination.route?.let { StepList(it) }
                Text(
                    stringResource(R.string.livemap_dir_attribution),
                    style = MaterialTheme.typography.labelSmall, color = colors.inkFaint,
                )
            }
        }
    }
}

@Composable
private fun RouteOption(route: NavRoute, index: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val context = LocalContext.current
    val arrive = remember(route) { DateFormat.getTimeFormat(context).format(Date(System.currentTimeMillis() + (route.durationS * 1000).toLong())) }
    val shape = TwinTheme.shapes.card
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) colors.accent.copy(alpha = 0.12f) else colors.glass)
            .border(1.dp, if (selected) colors.accent else colors.line, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                LiveMapFormat.minutes(maxOf(1, (route.durationS / 60).roundToInt())),
                style = MaterialTheme.typography.titleMedium, color = if (selected) colors.accent else colors.ink,
            )
            val via = if (route.via.isBlank()) "" else stringResource(R.string.livemap_dir_via, route.via)
            val tag = stringResource(if (index == 0) R.string.livemap_dir_fastest else R.string.livemap_dir_alternative)
            Text(
                listOf(tag, via).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(LiveMapFormat.distance(route.distanceM), style = MaterialTheme.typography.titleSmall, color = colors.ink)
            Text(stringResource(R.string.livemap_dir_arrive_at, arrive), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
    }
}

@Composable
private fun StepList(route: NavRoute) {
    val colors = TwinTheme.colors
    val resources = LocalContext.current.resources
    val p = remember(resources) { NavPhrases(resources) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        route.steps.forEach { step ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                Icon(step.kind.icon(), contentDescription = null, tint = colors.accent, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.instruction(step.kind, step.road, step.exit, step.side), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                    if (step.distanceM > 0) {
                        Text(LiveMapFormat.distance(step.distanceM), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                    }
                }
            }
        }
    }
}
