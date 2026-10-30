package com.twinhearts.app.feature.livemap.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.PillSwitch
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.DarkTwinColors
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.livemap.domain.GeoMath
import com.twinhearts.app.feature.livemap.domain.LiveMapFormat
import com.twinhearts.app.feature.livemap.domain.model.LivePosition
import com.twinhearts.app.feature.livemap.domain.spotKey
import com.twinhearts.app.feature.livemap.ui.DestinationKind
import com.twinhearts.app.feature.livemap.ui.LiveMapUiState
import com.twinhearts.app.feature.livemap.ui.LiveMapViewModel
import com.twinhearts.app.feature.livemap.ui.components.LmBanner
import com.twinhearts.app.feature.livemap.ui.components.LmDot
import com.twinhearts.app.feature.livemap.ui.components.LmMapFab
import com.twinhearts.app.feature.livemap.ui.components.LmTile
import com.twinhearts.app.feature.livemap.ui.map.LiveMapView
import com.twinhearts.app.feature.livemap.ui.map.MapColors
import com.twinhearts.app.feature.livemap.ui.map.MapMarker
import com.twinhearts.app.feature.livemap.ui.map.MapOverlay
import com.twinhearts.app.feature.livemap.ui.map.MapPoint
import com.twinhearts.app.feature.livemap.ui.map.MapStyles
import com.twinhearts.app.feature.livemap.ui.nav.ArrivalCard
import com.twinhearts.app.feature.livemap.ui.nav.DirectionsSection
import com.twinhearts.app.feature.livemap.ui.nav.KeepScreenOn
import com.twinhearts.app.feature.livemap.ui.nav.NavBottomBar
import com.twinhearts.app.feature.livemap.ui.nav.NavTopBanner
import java.util.Calendar

/** The "Map" tab: both people on a live map, ETA, quick actions, directions and love notes. */
@Composable
fun MapPage(
    state: LiveMapUiState,
    vm: LiveMapViewModel,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onVideoCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val context = LocalContext.current
    var notesOpen by rememberSaveable { mutableStateOf(false) }

    val mapColors = remember(colors) {
        MapColors(
            me = DarkTwinColors.accent.toArgb(),
            partner = DarkTwinColors.pinkSoft.toArgb(),
            route = colors.orchid.toArgb(),
            place = colors.success.toArgb(),
            stop = colors.inkMuted.toArgb(),
            destination = colors.error.toArgb(),
            note = DarkTwinColors.pinkSoft.toArgb(),
            label = colors.ink.toArgb(),
            labelHalo = colors.background.toArgb(),
            outline = Color.White.toArgb(),
            alt = colors.inkFaint.toArgb(),
        )
    }
    val style = remember(state.settings.mapStyle) {
        MapStyles.sourceFor(state.settings.mapStyle, MapStyles.isNight(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)))
    }
    val me = state.myFix?.let { MapPoint(it.lat, it.lng) } ?: state.positions.me?.let { MapPoint(it.lat, it.lng) }
    val partner = state.positions.partner?.let { MapPoint(it.lat, it.lng) }
    val nav = state.nav
    val navigating = nav != null
    val overlay = remember(state, me, partner) {
        val dest = state.destination
        val chosen = if (nav != null) nav.remainingLine() else dest?.route?.line.orEmpty()
        MapOverlay(
            me = nav?.progress?.snapped?.let { MapPoint(it.lat, it.lng) } ?: me,
            partner = partner,
            myTrail = if (state.settings.showRouteLine && !navigating) state.myTrail else emptyList(),
            partnerTrail = if (state.settings.showRouteLine && !navigating) state.partnerTrail else emptyList(),
            route = chosen.map { MapPoint(it.lat, it.lng) },
            altRoutes = if (navigating || dest == null) {
                emptyList()
            } else {
                dest.routes.filterIndexed { i, _ -> i != dest.selected }.map { r -> r.line.map { MapPoint(it.lat, it.lng) } }
            },
            places = state.shared.places.map { MapMarker(MapPoint(it.lat, it.lng), if (state.settings.showPlaceLabels) it.name.ifBlank { it.category } else "") },
            destination = nav?.destination?.let { MapPoint(it.lat, it.lng) } ?: state.destination?.point,
            loveNotes = state.shared.loveNotes.map { MapPoint(it.lat, it.lng) },
        )
    }

    Column(modifier.fillMaxSize()) {
        if (!navigating) TogetherCard(state, permissionGranted, onRequestPermission, vm, Modifier.padding(horizontal = TwinTheme.spacing.screenEdge))
        val emergency = state.positions.partner
        if (!navigating && emergency != null && emergency.emergency && emergency.updatedAtMs != state.dismissedEmergencyAt) {
            LmBanner(
                text = stringResource(R.string.livemap_emergency_banner, state.partnerName),
                modifier = Modifier.padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
                actionLabel = stringResource(R.string.livemap_dismiss),
                onAction = vm::dismissEmergencyBanner,
            )
        }
        if (state.offline && !navigating) {
            LmBanner(
                text = stringResource(R.string.livemap_offline_banner),
                modifier = Modifier.padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
                color = colors.inkMuted,
            )
        }
        if (!permissionGranted && !navigating) {
            LmBanner(
                text = stringResource(R.string.livemap_permission_banner),
                modifier = Modifier.padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
                actionLabel = stringResource(R.string.livemap_allow),
                onAction = onRequestPermission,
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = if (navigating) 0.dp else TwinTheme.spacing.s)) {
            LiveMapView(
                style = style,
                overlay = overlay,
                colors = mapColors,
                modifier = Modifier.fillMaxSize(),
                camera = state.camera,
                tilted = state.settings.threeD,
                initialCenter = state.lastCenter ?: me ?: partner ?: MapPoint(20.5937, 78.9629),
                initialZoom = if (state.lastCenter != null || me != null || partner != null) state.lastZoom else 4.5,
                onLongPress = { if (!navigating) vm.onLongPress(it) },
                onCameraIdle = vm::onCameraIdle,
                navigating = navigating,
                onUserGesture = vm::onUserMovedMap,
            )
            if (nav != null) {
                KeepScreenOn()
                if (nav.arrived) {
                    ArrivalCard(nav, vm::endNavigation, Modifier.align(Alignment.BottomCenter).padding(TwinTheme.spacing.s))
                } else {
                    NavTopBanner(nav, Modifier.align(Alignment.TopCenter).padding(TwinTheme.spacing.s))
                    NavBottomBar(
                        nav,
                        onEnd = vm::endNavigation,
                        onMute = vm::toggleNavMute,
                        onRecenter = vm::recenterNavigation,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(TwinTheme.spacing.s),
                    )
                }
            }
            if (!navigating) Column(
                Modifier.align(Alignment.TopEnd).padding(TwinTheme.spacing.s),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                LmMapFab(Icons.Rounded.MyLocation, stringResource(R.string.livemap_locate_me), vm::locateMe)
                LmMapFab(Icons.Rounded.Layers, stringResource(R.string.livemap_3d), { vm.setThreeD(!state.settings.threeD) }, active = state.settings.threeD)
            }
            val w = state.weather
            if (!navigating && state.settings.showWeather && w != null) {
                GlassCard(Modifier.align(Alignment.TopStart).padding(TwinTheme.spacing.s), contentPadding = TwinTheme.spacing.s) {
                    Text(stringResource(R.string.livemap_weather_line, w.temperatureC, w.humidityPct, w.windKmh), style = MaterialTheme.typography.labelMedium, color = colors.ink)
                    if (w.uvIndex != null) Text(stringResource(R.string.livemap_uv, w.uvIndex), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                }
            }
        }

        if (!navigating) Column(
            Modifier.fillMaxWidth().heightIn(max = 340.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            val destination = state.destination
            if (destination != null) DestinationCard(state, vm, onShare = { text ->
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
            })
            else EtaCard(state, vm)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                LmTile(Icons.Rounded.Person, stringResource(R.string.livemap_locate_partner), vm::locatePartner, Modifier.weight(1f))
                LmTile(Icons.Rounded.Flag, stringResource(R.string.livemap_meeting_point), vm::showMeetingPoint, Modifier.weight(1f))
                LmTile(Icons.Rounded.Favorite, stringResource(R.string.livemap_love_note), { notesOpen = true }, Modifier.weight(1f))
                LmTile(Icons.Rounded.Videocam, stringResource(R.string.livemap_video_call), onVideoCall, Modifier.weight(1f))
            }
        }
    }
    if (notesOpen) LoveNotesDialog(state, vm, onDismiss = { notesOpen = false })
}

/** "Live together": both people, their status and the sharing switch. */
@Composable
private fun TogetherCard(
    state: LiveMapUiState,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    vm: LiveMapViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val me = state.positions.me
    val partner = state.positions.partner
    GlassCard(modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m), verticalAlignment = Alignment.CenterVertically) {
            PersonStatus(state.myName.ifBlank { stringResource(R.string.livemap_you) }, me, state.settings.isPaused, state.nowMs, Modifier.weight(1f))
            PersonStatus(state.partnerName, partner, partner?.paused == true, state.nowMs, Modifier.weight(1f))
        }
        val myPos = state.myFix?.let { MapPoint(it.lat, it.lng) } ?: state.positions.me?.let { MapPoint(it.lat, it.lng) }
        if (myPos != null && partner != null) {
            val d = GeoMath.distanceM(myPos.lat, myPos.lng, partner.lat, partner.lng)
            Text(
                if (d <= GeoMath.TOGETHER_RADIUS_M) stringResource(R.string.livemap_together_now) else stringResource(R.string.livemap_apart, LiveMapFormat.distance(d)),
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent,
                modifier = Modifier.padding(top = TwinTheme.spacing.s),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.livemap_share_location), style = MaterialTheme.typography.titleSmall, color = colors.ink, modifier = Modifier.weight(1f))
            PillSwitch(
                checked = state.settings.sharing == true,
                onCheckedChange = { on -> if (on && !permissionGranted) onRequestPermission() else vm.setSharing(on) },
            )
        }
    }
}

@Composable
private fun PersonStatus(name: String, pos: LivePosition?, paused: Boolean, nowMs: Long, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
        Avatar(name = name, size = 40.dp)
        Column {
            Text(name, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                LmDot(online = pos?.online == true && !paused)
                Text(statusText(pos, paused, nowMs), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 2)
            }
        }
    }
}

@Composable
internal fun statusText(pos: LivePosition?, paused: Boolean, nowMs: Long): String = when {
    pos == null -> stringResource(R.string.livemap_status_unknown)
    paused -> stringResource(R.string.livemap_status_paused)
    pos.online -> activityLabel(GeoMath.activity(pos.speedMps))
    else -> stringResource(R.string.livemap_status_last_seen, LiveMapFormat.ago(nowMs, pos.updatedAtMs))
}

@Composable
internal fun activityLabel(a: GeoMath.Activity): String = stringResource(
    when (a) {
        GeoMath.Activity.Stationary -> R.string.livemap_act_still
        GeoMath.Activity.Walking -> R.string.livemap_act_walking
        GeoMath.Activity.Running -> R.string.livemap_act_running
        GeoMath.Activity.Cycling -> R.string.livemap_act_cycling
        GeoMath.Activity.Driving -> R.string.livemap_act_driving
    },
)

@Composable
private fun EtaCard(state: LiveMapUiState, vm: LiveMapViewModel) {
    val partner = state.positions.partner
    val me = state.myFix?.let { MapPoint(it.lat, it.lng) } ?: state.positions.me?.let { MapPoint(it.lat, it.lng) }
    GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
        if (partner == null || me == null) {
            Text(stringResource(R.string.livemap_eta_waiting, state.partnerName), style = MaterialTheme.typography.bodyMedium, color = TwinTheme.colors.inkMuted)
        } else {
            val d = GeoMath.distanceM(me.lat, me.lng, partner.lat, partner.lng)
            Text(
                stringResource(R.string.livemap_eta_line, state.partnerName, LiveMapFormat.distance(d), LiveMapFormat.minutes(GeoMath.driveMinutes(d))),
                style = MaterialTheme.typography.titleSmall,
                color = TwinTheme.colors.ink,
            )
            state.partnerArea?.let { Text(stringResource(R.string.livemap_near, it.main), style = MaterialTheme.typography.bodySmall, color = TwinTheme.colors.inkMuted) }
            GlassButton(stringResource(R.string.livemap_get_directions), vm::navigateToPartner, Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s))
        }
    }
}

@Composable
private fun DestinationCard(state: LiveMapUiState, vm: LiveMapViewModel, onShare: (String) -> Unit) {
    val d = state.destination ?: return
    val colors = TwinTheme.colors
    val key = spotKey(d.point.lat, d.point.lng)
    val starred = state.shared.favorites.any { spotKey(it.lat, it.lng) == key }
    val title = d.name.ifBlank {
        stringResource(if (d.kind == DestinationKind.Meeting) R.string.livemap_meeting_point else R.string.livemap_dropped_pin)
    }
    GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 2)
                if (d.address.isNotBlank()) Text(d.address, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 2)
            }
            LmMapFab(Icons.Rounded.Close, stringResource(R.string.livemap_clear_route), vm::clearDestination)
        }
        DirectionsSection(
            destination = d,
            onMode = vm::setTravelMode,
            onSelect = vm::selectRoute,
            onStart = vm::startNavigation,
            onRetry = vm::retryRoutes,
            modifier = Modifier.padding(vertical = TwinTheme.spacing.s),
        )
        if (d.kind == DestinationKind.Search || d.kind == DestinationKind.Pin) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                GlassButton(stringResource(R.string.livemap_save_place), vm::saveDestinationAsPlace, Modifier.weight(1f), style = GlassButtonStyle.Secondary, leadingIcon = Icons.Rounded.Place)
                GlassButton(
                    stringResource(if (starred) R.string.livemap_favorited else R.string.livemap_favorite),
                    vm::toggleFavoriteDestination,
                    Modifier.weight(1f),
                    style = GlassButtonStyle.Secondary,
                    leadingIcon = if (starred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                )
                GlassButton(
                    stringResource(R.string.livemap_share),
                    { onShare("$title https://maps.google.com/?q=${d.point.lat},${d.point.lng}") },
                    Modifier.weight(1f),
                    style = GlassButtonStyle.Secondary,
                    leadingIcon = Icons.Rounded.Share,
                )
            }
        }
    }
}

@Composable
private fun LoveNotesDialog(state: LiveMapUiState, vm: LiveMapViewModel, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TwinTheme.colors.card,
        title = { Text(stringResource(R.string.livemap_love_note), color = TwinTheme.colors.ink) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                TwinTextField(text, { text = it }, stringResource(R.string.livemap_note_hint), maxLength = 200)
                state.shared.loveNotes.sortedByDescending { it.createdAtMs }.take(20).forEach { note ->
                    val mine = note.fromRole == state.myRole
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(note.message, style = MaterialTheme.typography.bodyMedium, color = TwinTheme.colors.ink)
                            Text(
                                (if (mine) stringResource(R.string.livemap_you) else state.partnerName) + " · " + LiveMapFormat.ago(state.nowMs, note.createdAtMs),
                                style = MaterialTheme.typography.labelSmall, color = TwinTheme.colors.inkMuted,
                            )
                        }
                        TextButton(onClick = { vm.flyTo(MapPoint(note.lat, note.lng)); onDismiss() }) { Text(stringResource(R.string.livemap_view)) }
                        if (mine) TextButton(onClick = { vm.deleteLoveNote(note.id) }) { Text(stringResource(R.string.livemap_delete)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.sendLoveNote(text); text = "" }, enabled = text.isNotBlank()) { Text(stringResource(R.string.livemap_send)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.livemap_close)) } },
    )
}
