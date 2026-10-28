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
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalWifiOff
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SectionHeader
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.livemap.data.LiveMapSettings
import com.twinhearts.app.feature.livemap.domain.model.AlertKey
import com.twinhearts.app.feature.livemap.domain.model.MapStyleKind
import com.twinhearts.app.feature.livemap.domain.model.Vehicle
import com.twinhearts.app.feature.livemap.ui.AlertFeedState
import com.twinhearts.app.feature.livemap.ui.LiveMapUiState
import com.twinhearts.app.feature.livemap.ui.LiveMapViewModel
import com.twinhearts.app.feature.livemap.ui.LmScreen
import com.twinhearts.app.feature.livemap.ui.TrackViewModel
import com.twinhearts.app.feature.livemap.ui.components.LmRow
import com.twinhearts.app.feature.livemap.ui.components.LmToggleRow
import java.text.DateFormat
import java.util.Date

/** "More Options": map type, map switches, travel mode and links to the other screens. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MorePage(
    state: LiveMapUiState,
    vm: LiveMapViewModel,
    trackVm: TrackViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s: LiveMapSettings = state.settings
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(TwinTheme.spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        SectionHeader(stringResource(R.string.livemap_map_type))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            MapStyleKind.entries.forEach { k ->
                TwinFilterChip(
                    stringResource(
                        when (k) {
                            MapStyleKind.Street -> R.string.livemap_style_street
                            MapStyleKind.Dark -> R.string.livemap_style_dark
                            MapStyleKind.Satellite -> R.string.livemap_style_satellite
                            MapStyleKind.Hybrid -> R.string.livemap_style_hybrid
                            MapStyleKind.Auto -> R.string.livemap_style_auto
                        },
                    ),
                    s.mapStyle == k, { vm.setMapStyle(k) },
                )
            }
        }
        GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
            LmToggleRow(Icons.Rounded.Layers, stringResource(R.string.livemap_3d), s.threeD, vm::setThreeD)
            LmToggleRow(Icons.Rounded.Person, stringResource(R.string.livemap_follow_partner), s.followPartner, vm::setFollowPartner)
            LmToggleRow(Icons.Rounded.Timeline, stringResource(R.string.livemap_show_route_line), s.showRouteLine, vm::setShowRouteLine)
            LmToggleRow(Icons.Rounded.Place, stringResource(R.string.livemap_show_place_labels), s.showPlaceLabels, vm::setShowPlaceLabels)
            LmToggleRow(Icons.Rounded.WbSunny, stringResource(R.string.livemap_weather_layer), s.showWeather, vm::setShowWeather)
        }
        SectionHeader(stringResource(R.string.livemap_travel_mode))
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Vehicle.entries.forEach { v ->
                TwinFilterChip(
                    stringResource(
                        when (v) {
                            Vehicle.Walking -> R.string.livemap_veh_walking
                            Vehicle.Bike -> R.string.livemap_veh_bike
                            Vehicle.Car -> R.string.livemap_veh_car
                            Vehicle.Bus -> R.string.livemap_veh_bus
                        },
                    ),
                    s.vehicle == v, { vm.setVehicle(v) },
                )
            }
        }
        GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
            LmRow(Icons.Rounded.Notifications, stringResource(R.string.livemap_alerts_title), onClick = { vm.open(LmScreen.Alerts) }, trailing = { Icon(Icons.Rounded.ChevronRight, null, tint = TwinTheme.colors.inkMuted) })
            LmRow(Icons.Rounded.Lock, stringResource(R.string.livemap_privacy_title), onClick = { vm.open(LmScreen.Privacy) }, trailing = { Icon(Icons.Rounded.ChevronRight, null, tint = TwinTheme.colors.inkMuted) })
            LmRow(Icons.Rounded.Delete, stringResource(R.string.livemap_clear_history), subtitle = stringResource(R.string.livemap_clear_history_hint), onClick = { confirmClear = true }, tint = TwinTheme.colors.error)
            LmRow(Icons.Rounded.Settings, stringResource(R.string.livemap_app_settings), onClick = onOpenSettings, trailing = { Icon(Icons.Rounded.ChevronRight, null, tint = TwinTheme.colors.inkMuted) })
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = TwinTheme.colors.card,
            title = { Text(stringResource(R.string.livemap_clear_history), color = TwinTheme.colors.ink) },
            text = { Text(stringResource(R.string.livemap_clear_history_confirm), color = TwinTheme.colors.inkMuted) },
            confirmButton = { TextButton(onClick = { trackVm.clearMyHistory(); confirmClear = false }) { Text(stringResource(R.string.livemap_delete)) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.livemap_close)) } },
        )
    }
}

/** "Location Alerts": what to be told about the partner, what the partner is told about me, and recent activity. */
@Composable
fun AlertsPage(state: LiveMapUiState, vm: LiveMapViewModel, modifier: Modifier = Modifier) {
    val a = state.shared.alerts
    val timeFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(TwinTheme.spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        SectionHeader(stringResource(R.string.livemap_alerts_about_partner, state.partnerName))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
            LmToggleRow(Icons.Rounded.Login, stringResource(R.string.livemap_alert_arrive), a.arrive, { vm.setAlert(AlertKey.Arrive, it) })
            LmToggleRow(Icons.Rounded.Logout, stringResource(R.string.livemap_alert_leave), a.leave, { vm.setAlert(AlertKey.Leave, it) })
            LmToggleRow(Icons.Rounded.SignalWifiOff, stringResource(R.string.livemap_alert_offline), a.offline, { vm.setAlert(AlertKey.Offline, it) })
        }
        SectionHeader(stringResource(R.string.livemap_alerts_about_me))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
            LmToggleRow(Icons.Rounded.BatteryAlert, stringResource(R.string.livemap_alert_battery), a.battery, { vm.setAlert(AlertKey.Battery, it) })
            LmToggleRow(Icons.Rounded.SignalWifiOff, stringResource(R.string.livemap_alert_network), a.network, { vm.setAlert(AlertKey.Network, it) })
            LmToggleRow(Icons.Rounded.GpsOff, stringResource(R.string.livemap_alert_gps), a.gps, { vm.setAlert(AlertKey.Gps, it) })
        }
        SectionHeader(stringResource(R.string.livemap_recent_activity))
        when (val f = state.feed) {
            AlertFeedState.Loading -> LoadingSkeleton(lines = 3)
            AlertFeedState.Empty -> EmptyState(stringResource(R.string.livemap_feed_empty_title), message = stringResource(R.string.livemap_feed_empty_message))
            is AlertFeedState.Error -> ErrorState(f.error.asText(), vm::loadFeed)
            is AlertFeedState.Content -> f.events.take(30).forEach { e ->
                LmRow(
                    Icons.Rounded.LocationOn,
                    stringResource(if (e.entered) R.string.livemap_feed_arrived else R.string.livemap_feed_left, state.partnerName, e.label),
                    subtitle = timeFormat.format(Date(e.atMs)),
                )
            }
        }
    }
}

/** "Location privacy": pause, invisible mode, approximate location and emergency share. */
@Composable
fun PrivacyPage(state: LiveMapUiState, vm: LiveMapViewModel, modifier: Modifier = Modifier) {
    val s = state.settings
    var confirmEmergency by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(TwinTheme.spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.livemap_pause_title), style = MaterialTheme.typography.titleMedium, color = TwinTheme.colors.ink)
            Text(
                stringResource(if (s.isPaused) R.string.livemap_pause_active else R.string.livemap_pause_hint),
                style = MaterialTheme.typography.bodySmall, color = TwinTheme.colors.inkMuted,
                modifier = Modifier.padding(bottom = TwinTheme.spacing.s),
            )
            if (s.isPaused) {
                GlassButton(stringResource(R.string.livemap_resume), vm::resume, Modifier.fillMaxWidth())
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    GlassButton(stringResource(R.string.livemap_pause_15), { vm.pause(15) }, Modifier.weight(1f), style = GlassButtonStyle.Secondary)
                    GlassButton(stringResource(R.string.livemap_pause_60), { vm.pause(60) }, Modifier.weight(1f), style = GlassButtonStyle.Secondary)
                    GlassButton(stringResource(R.string.livemap_pause_manual), { vm.pause(null) }, Modifier.weight(1f), style = GlassButtonStyle.Secondary)
                }
            }
        }
        GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
            LmToggleRow(Icons.Rounded.VisibilityOff, stringResource(R.string.livemap_invisible), s.invisible, vm::setInvisible, subtitle = stringResource(R.string.livemap_invisible_hint))
            LmToggleRow(Icons.Rounded.Layers, stringResource(R.string.livemap_approximate), s.approximate, vm::setApproximate, subtitle = stringResource(R.string.livemap_approximate_hint))
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.livemap_emergency_title), style = MaterialTheme.typography.titleMedium, color = TwinTheme.colors.ink)
            Text(stringResource(R.string.livemap_emergency_hint, state.partnerName), style = MaterialTheme.typography.bodySmall, color = TwinTheme.colors.inkMuted, modifier = Modifier.padding(bottom = TwinTheme.spacing.s))
            GlassButton(stringResource(R.string.livemap_emergency_button), { confirmEmergency = true }, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Warning)
        }
    }
    if (confirmEmergency) {
        AlertDialog(
            onDismissRequest = { confirmEmergency = false },
            containerColor = TwinTheme.colors.card,
            title = { Text(stringResource(R.string.livemap_emergency_title), color = TwinTheme.colors.ink) },
            text = { Text(stringResource(R.string.livemap_emergency_confirm, state.partnerName), color = TwinTheme.colors.inkMuted) },
            confirmButton = { TextButton(onClick = { vm.emergencyShare(); confirmEmergency = false }) { Text(stringResource(R.string.livemap_send)) } },
            dismissButton = { TextButton(onClick = { confirmEmergency = false }) { Text(stringResource(R.string.livemap_close)) } },
        )
    }
}

private val CHIP_QUERIES = listOf(
    R.string.livemap_q_restaurants, R.string.livemap_q_cafes, R.string.livemap_q_hospitals, R.string.livemap_q_atms,
    R.string.livemap_q_fuel, R.string.livemap_q_hotels, R.string.livemap_q_pharmacy, R.string.livemap_q_malls,
)

/** Search for a place, then get directions, save it or star it on the map. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchPage(state: LiveMapUiState, vm: LiveMapViewModel, modifier: Modifier = Modifier) {
    val search = state.search
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(TwinTheme.spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        TwinTextField(
            value = search.query,
            onValueChange = vm::onQueryChange,
            label = stringResource(R.string.livemap_search_hint),
            leadingIcon = Icons.Rounded.Place,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            CHIP_QUERIES.forEach { res ->
                val label = stringResource(res)
                TwinFilterChip(label, search.query == label, { vm.searchChip(label) })
            }
        }
        when {
            search.loading -> LoadingSkeleton(lines = 4)
            search.error != null -> ErrorState(search.error.asText(), { vm.onQueryChange(search.query) })
            search.searched && search.hits.isEmpty() -> EmptyState(stringResource(R.string.livemap_search_none_title), message = stringResource(R.string.livemap_search_none_message))
            else -> search.hits.forEach { hit ->
                GlassCard(Modifier.fillMaxWidth(), onClick = { vm.openHit(hit) }, contentPadding = TwinTheme.spacing.m) {
                    Text(hit.name, style = MaterialTheme.typography.titleSmall, color = TwinTheme.colors.ink, maxLines = 2)
                    if (hit.address.isNotBlank()) Text(hit.address, style = MaterialTheme.typography.bodySmall, color = TwinTheme.colors.inkMuted, maxLines = 2)
                }
            }
        }
    }
}
