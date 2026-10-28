package com.twinhearts.app.feature.livemap.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.livemap.ui.screens.AlertsPage
import com.twinhearts.app.feature.livemap.ui.screens.MapPage
import com.twinhearts.app.feature.livemap.ui.screens.MorePage
import com.twinhearts.app.feature.livemap.ui.screens.PartnerPage
import com.twinhearts.app.feature.livemap.ui.screens.PlacesPage
import com.twinhearts.app.feature.livemap.ui.screens.PrivacyPage
import com.twinhearts.app.feature.livemap.ui.screens.SearchPage
import com.twinhearts.app.feature.livemap.ui.screens.TrackPage

private val TABS = listOf(LmScreen.Map, LmScreen.Track, LmScreen.Partner, LmScreen.Places)

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * The Live Map page. It keeps its own back stack of sub-screens (Map, Track, Partner, Places, More,
 * Alerts, Privacy, Search): Back always goes to the screen you came from, and from the Map to the previous app page.
 */
@Composable
fun LiveMapScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: LiveMapViewModel = hiltViewModel(),
    trackVm: TrackViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val track by trackVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasLocationPermission(context)) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        vm.onPermissionResult(granted)
    }
    val requestPermission = {
        val wanted = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        launcher.launch(wanted.toTypedArray())
    }

    // Polling only runs while this page is on screen (R-66).
    LifecycleResumeEffect(Unit) {
        granted = hasLocationPermission(context)
        vm.onVisible()
        onPauseOrDispose { vm.onHidden() }
    }
    LaunchedEffect(Unit) {
        if (!granted && !askedOnce) {
            askedOnce = true
            requestPermission()
        }
    }

    // One-off messages and partner alerts.
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            val text = when (event) {
                is LiveMapEvent.Message -> if (event.arg == null) context.getString(event.res) else context.getString(event.res, event.arg)
                is LiveMapEvent.PartnerArrived -> context.getString(R.string.livemap_alert_toast_arrived, state.partnerName, event.place)
                is LiveMapEvent.PartnerLeft -> context.getString(R.string.livemap_alert_toast_left, state.partnerName, event.place)
                LiveMapEvent.PartnerOffline -> context.getString(R.string.livemap_alert_toast_offline, state.partnerName)
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        trackVm.messages.collect { res -> Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show() }
    }

    BackHandler(enabled = state.stack.size > 1) { vm.back() }

    val screen = state.screen
    TwinBackground {
        Column(Modifier.fillMaxSize()) {
            TwinTopBar(
                title = stringResource(R.string.livemap_title),
                onBackClick = { if (!vm.back()) onBack() },
                onSearchClick = if (screen == LmScreen.Search) null else ({ vm.open(LmScreen.Search) }),
                onMenuClick = if (screen == LmScreen.More) null else ({ vm.open(LmScreen.More) }),
            )
            if (screen in TABS) {
                SegmentedTabs(
                    options = listOf(
                        stringResource(R.string.livemap_tab_map),
                        stringResource(R.string.livemap_tab_track),
                        stringResource(R.string.livemap_tab_partner),
                        stringResource(R.string.livemap_tab_places),
                    ),
                    selectedIndex = TABS.indexOf(screen),
                    onSelect = { i ->
                        if (TABS[i] == LmScreen.Track) trackVm.showFor(false)
                        vm.switchTab(TABS[i])
                    },
                    modifier = Modifier.padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
                )
            }
            Box(Modifier.weight(1f).fillMaxSize()) {
                when (val phase = state.phase) {
                    LiveMapPhase.Loading -> LoadingSkeleton(Modifier.padding(TwinTheme.spacing.screenEdge), lines = 6)
                    is LiveMapPhase.Failed -> ErrorState(phase.error.asText(), vm::load)
                    LiveMapPhase.Ready -> when (screen) {
                        LmScreen.Map -> MapPage(
                            state = state,
                            vm = vm,
                            permissionGranted = granted,
                            onRequestPermission = requestPermission,
                            onVideoCall = { vm.showMessage(R.string.livemap_msg_calls_pending) },
                        )
                        LmScreen.Track -> TrackPage(track, trackVm, state.myName.ifBlank { context.getString(R.string.livemap_you) }, state.partnerName, state.settings.mapStyle)
                        LmScreen.Partner -> PartnerPage(
                            state = state,
                            onDirections = vm::navigateToPartner,
                            onTrack = { trackVm.showFor(true); vm.open(LmScreen.Track) },
                            onLocate = { vm.goToMap(); vm.locatePartner() },
                        )
                        LmScreen.Places -> PlacesPage(state, vm)
                        LmScreen.More -> MorePage(state, vm, trackVm, onOpenSettings)
                        LmScreen.Alerts -> AlertsPage(state, vm)
                        LmScreen.Privacy -> PrivacyPage(state, vm)
                        LmScreen.Search -> SearchPage(state, vm)
                    }
                }
            }
        }
    }
}
