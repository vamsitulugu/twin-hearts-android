package com.twinhearts.app.feature.globe.ui

import com.twinhearts.app.core.pro.rememberProGate
import com.twinhearts.app.feature.globe.ui.map.SatelliteStyle
import kotlinx.coroutines.delay
import com.twinhearts.app.feature.globe.ui.components.SHEET_HALF_FRACTION
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.globe.domain.FILTER_MOODS
import com.twinhearts.app.feature.globe.domain.flagEmoji
import com.twinhearts.app.feature.globe.domain.model.GlobeFilter
import com.twinhearts.app.feature.globe.domain.model.GlobeViewMode
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.spentLabel
import com.twinhearts.app.feature.globe.domain.travelStats
import com.twinhearts.app.feature.globe.ui.components.MediaViewer
import com.twinhearts.app.feature.globe.ui.components.rememberMediaPickers
import com.twinhearts.app.feature.globe.ui.components.rememberVoiceControl
import com.twinhearts.app.feature.globe.ui.globe.GlobeCanvas
import com.twinhearts.app.feature.globe.ui.globe.MapDrive
import com.twinhearts.app.feature.globe.ui.map.GlobeMapView
import com.twinhearts.app.feature.globe.ui.map.STREET_LEVEL_ZOOM
import com.twinhearts.app.feature.globe.ui.panels.CityPanel
import com.twinhearts.app.feature.globe.ui.panels.DetailActions
import com.twinhearts.app.feature.globe.ui.panels.DetailPanel
import com.twinhearts.app.feature.globe.ui.panels.FormActions
import com.twinhearts.app.feature.globe.ui.panels.FormPanel
import com.twinhearts.app.feature.globe.ui.screens.StatsView
import com.twinhearts.app.feature.globe.ui.screens.TimelineView
import java.time.YearMonth

private const val WIDE_SCREEN_DP = 700

/**
 * Why this file exists: the Memory Globe page. It puts the 3D globe, the flat map or the timeline
 * on screen, with the top bar, filters, search, live location, the stat chips and the sheets on top.
 */
@Composable
fun GlobeScreen(
    onBack: () -> Unit,
    vm: GlobeViewModel = hiltViewModel(),
    formVm: MemoryFormViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val form by formVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val proGate = rememberProGate()
    val proFeature = stringResource(R.string.globe_pin_memory)
    val colors = TwinTheme.colors
    val density = LocalDensity.current
    val wide = LocalConfiguration.current.screenWidthDp >= WIDE_SCREEN_DP

    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    var topHeightPx by remember { mutableIntStateOf(0) }
    var bottomHeightPx by remember { mutableIntStateOf(0) }
    var mapZoom by remember { mutableDoubleStateOf(0.0) }

    fun toast(res: Int, vararg args: Any) = Toast.makeText(context, context.getString(res, *args), Toast.LENGTH_LONG).show()

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        vm.onLocationPermissionResult(result.values.any { it })
    }

    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is GlobeEvent.Message -> toast(e.res, *listOfNotNull(e.arg1, e.arg2).toTypedArray())
                GlobeEvent.AskLocationPermission -> locationPermission.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
    }
    LaunchedEffect(formVm) {
        formVm.events.collect { e ->
            when (e) {
                is MemoryFormEvent.Message -> toast(e.res, *listOfNotNull(e.arg).toTypedArray())
                MemoryFormEvent.Saved -> vm.onMemorySaved()
            }
        }
    }
    // Download the satellite style now, so the map is ready the moment you zoom in.
    LaunchedEffect(Unit) { SatelliteStyle.load() }
    LifecycleResumeEffect(vm) {
        vm.onVisible()
        onPauseOrDispose { vm.onHidden() }
    }
    BackHandler(enabled = s.viewer != null || s.statsOpen || s.panels.isNotEmpty()) { vm.back() }

    fun flagOf(country: String): String = flagEmoji(s.atlas?.find(country)?.iso2)

    // While a half-height sheet is (about to be) open on a phone, the map centres on the top half.
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val sheetPaddingPx = if (!wide && (s.panels.isNotEmpty() || s.selectedKey != null)) (screenHeightPx * SHEET_HALF_FRACTION).toInt() else 0
    val topPad = with(density) { topHeightPx.toDp() }
    val bottomPad = with(density) { bottomHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        // ── the three views ──────────────────────────────────────────────────────────────────
        when (s.view) {
            GlobeViewMode.Globe -> {
                // One camera from space to street: the satellite map takes every finger (pinch, drag, tilt,
                // rotate) and the 3D Earth under it follows and fades into the satellite picture.
                val drive = remember { MapDrive() }
                LaunchedEffect(Unit) { vm.ensureRoads() }
                GlobeCanvas(
                    groups = s.groups,
                    legs = s.legs,
                    glows = s.glows,
                    atlas = s.atlas,
                    selectedKey = s.selectedKey,
                    live = s.live,
                    searchPin = s.searchPin,
                    camera = s.camera,
                    onPinTap = vm::onPinTap,
                    onCameraArrived = vm::onCameraArrived,
                    onStreetLevel = { _, _, _ -> },
                    drive = drive,
                )
                GlobeMapView(
                    groups = s.groups,
                    legs = s.legs,
                    roads = s.roads,
                    selectedKey = s.selectedKey,
                    live = s.live,
                    searchPin = s.searchPin,
                    camera = s.camera,
                    onPinTap = vm::onPinTap,
                    onCameraArrived = vm::onCameraArrived,
                    onZoomChange = { mapZoom = it },
                    bottomPaddingPx = sheetPaddingPx,
                    // The Earth keeps turning by itself, except while a pin is open.
                    spinPaused = s.panels.isNotEmpty() || s.selectedKey != null,
                    drive = drive,
                )
            }
            GlobeViewMode.Map -> GlobeMapView(
                groups = s.groups,
                legs = s.legs,
                roads = s.roads,
                selectedKey = s.selectedKey,
                live = s.live,
                searchPin = s.searchPin,
                camera = s.camera,
                onPinTap = vm::onPinTap,
                onCameraArrived = vm::onCameraArrived,
                onZoomChange = { mapZoom = it },
                bottomPaddingPx = sheetPaddingPx,
            )
            GlobeViewMode.Timeline -> TimelineView(
                memories = s.visible,
                topPadding = topPad,
                bottomPadding = bottomPad,
                onOpen = vm::openDetail,
            )
        }

        // ── top: bar, tabs, filters, search, live info ───────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colors.background.copy(alpha = 0.92f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = TwinTheme.spacing.s)
                .onSizeChanged { topHeightPx = it.height },
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.globe_back), onClick = onBack)
                Text(
                    text = stringResource(R.string.globe_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = SerifFamily,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = TwinTheme.spacing.s),
                )
                GlassIconButton(Icons.Rounded.Search, stringResource(R.string.globe_search), onClick = {
                    searchOpen = !searchOpen
                    if (!searchOpen) vm.clearSearch()
                })
                Box(modifier = Modifier.padding(start = 4.dp)) {
                    GlassIconButton(Icons.Rounded.BarChart, stringResource(R.string.globe_statistics), onClick = vm::openStats)
                }
                Box(modifier = Modifier.padding(start = 4.dp)) {
                    GlassIconButton(Icons.Rounded.FilterList, stringResource(R.string.globe_filter), onClick = { filterOpen = !filterOpen })
                }
            }
            SegmentedTabs(
                options = listOf(
                    stringResource(R.string.globe_tab_globe),
                    stringResource(R.string.globe_tab_map),
                    stringResource(R.string.globe_tab_timeline),
                ),
                selectedIndex = s.view.ordinal,
                onSelect = { vm.setView(GlobeViewMode.entries[it]) },
                modifier = Modifier.padding(horizontal = TwinTheme.spacing.s),
            )
            if (filterOpen) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = TwinTheme.spacing.s)) {
                    item { TwinFilterChip(stringResource(R.string.globe_filter_all), s.filter == GlobeFilter.All, { vm.setFilter(GlobeFilter.All) }) }
                    items(s.years, key = { "y$it" }) { year ->
                        TwinFilterChip(year, s.filter == GlobeFilter.Year(year), { vm.setFilter(GlobeFilter.Year(year)) })
                    }
                    items(FILTER_MOODS, key = { it.first }) { (emoji, _) ->
                        val label = when (emoji) {
                            "😊" -> stringResource(R.string.globe_filter_happy)
                            "🥰" -> stringResource(R.string.globe_filter_romantic)
                            else -> stringResource(R.string.globe_filter_excited)
                        }
                        TwinFilterChip(label, s.filter == GlobeFilter.Mood(emoji), { vm.setFilter(GlobeFilter.Mood(emoji)) })
                    }
                }
            }
            if (searchOpen) {
                SearchBox(s.search, vm)
            }
        }

        // ── right-hand quick buttons and live info ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = topPad + TwinTheme.spacing.s, end = TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            if (s.view != GlobeViewMode.Timeline) {
                Box {
                    GlassIconButton(
                        Icons.Rounded.Sensors,
                        stringResource(R.string.globe_live_location),
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            vm.onLiveButton(granted)
                        },
                    )
                    if (s.live.enabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(colors.success)
                                .border(2.dp, colors.background, CircleShape),
                        )
                    }
                }
                GlassIconButton(Icons.Rounded.CenterFocusStrong, stringResource(R.string.globe_reset_view), onClick = vm::resetView)
            }
        }
        if (s.live.enabled && s.view != GlobeViewMode.Timeline) {
            LiveInfo(s.live, s.partnerName, Modifier.align(Alignment.TopStart).padding(top = topPad + TwinTheme.spacing.s, start = TwinTheme.spacing.m))
        }
        if (s.view != GlobeViewMode.Timeline && mapZoom >= STREET_LEVEL_ZOOM) {
            Text(
                text = stringResource(R.string.globe_street_badge),
                style = MaterialTheme.typography.labelMedium,
                color = colors.ink,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topPad + TwinTheme.spacing.s)
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.card.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        // ── bottom: stat chips and the "Pin a Memory" button ─────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background.copy(alpha = 0.92f))))
                .navigationBarsPadding()
                .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s)
                .onSizeChanged { bottomHeightPx = it.height },
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                StatChip(s.summary.cities.toString(), stringResource(R.string.globe_chip_cities), Modifier.weight(1f), vm::openStats)
                StatChip(s.summary.countries.toString(), stringResource(R.string.globe_chip_countries), Modifier.weight(1f), vm::openStats)
                StatChip(s.summary.memories.toString(), stringResource(R.string.globe_chip_memories), Modifier.weight(1f), vm::openStats)
                StatChip(spentLabel(s.summary.totalCost), stringResource(R.string.globe_chip_spent), Modifier.weight(1.3f), vm::openStats)
            }
            GlassButton(
                text = stringResource(R.string.globe_pin_memory),
                onClick = { proGate.require(proFeature) { vm.requestPin() } },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (s.load == GlobeLoad.Ready && s.isEmpty && s.view != GlobeViewMode.Timeline) {
            EmptyState(
                title = stringResource(R.string.globe_empty_title),
                message = stringResource(R.string.globe_empty_sub),
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // ── sheets ───────────────────────────────────────────────────────────────────────────
        val top = s.panels.lastOrNull()
        AnimatedContent(
            targetState = top,
            transitionSpec = {
                if (targetState == null) {
                    // Closing: the card slides down off the screen.
                    fadeIn(tween(1)) togetherWith (slideOutVertically(tween(SHEET_SLIDE_MS, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(SHEET_SLIDE_MS)))
                } else {
                    // Opening: the card slides up from the bottom edge.
                    (slideInVertically(tween(SHEET_SLIDE_MS + 120, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(SHEET_SLIDE_MS))) togetherWith fadeOut(tween(160))
                }
            },
            label = "globe-panel",
            modifier = Modifier.fillMaxSize(),
        ) { panel ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (panel) {
                    null -> Unit
                    is GlobePanel.City -> {
                        val group = s.group(panel.key)
                        if (group == null) {
                            LaunchedEffect(panel) { vm.closeTopPanel() }
                        } else {
                            CityPanel(
                                wide = wide,
                                group = group,
                                flag = flagOf(group.country),
                                onClose = vm::closeTopPanel,
                                onAddHere = { vm.addMemoryHere(group.key) },
                                onOpen = vm::openDetail,
                                onEdit = vm::editMemory,
                                onDelete = vm::deleteMemory,
                                onMedia = vm::openViewer,
                            )
                        }
                    }
                    is GlobePanel.Detail -> {
                        val memory = s.memory(panel.id)
                        if (memory == null) {
                            LaunchedEffect(panel) { vm.closeTopPanel() }
                        } else {
                            val pickers = rememberMediaPickers(
                                onPhotos = { vm.addPicked(memory.id, it, MediaKind.Photo) },
                                onVideo = { vm.addPicked(memory.id, listOf(it), MediaKind.Video) },
                            )
                            val voice = rememberVoiceControl(
                                maxSeconds = VOICE_NOTE_SECONDS,
                                onFinished = { file, seconds -> vm.addVoice(memory.id, file, seconds) },
                                onMessage = { toast(it) },
                            )
                            DetailPanel(
                                wide = wide,
                                memory = memory,
                                flag = flagOf(memory.country),
                                upload = s.upload,
                                pickers = pickers,
                                voice = voice,
                                actions = DetailActions(
                                    onClose = vm::closeTopPanel,
                                    onEdit = { vm.editMemory(memory.id) },
                                    onDeleteMemory = { vm.deleteMemory(memory.id) },
                                    onDeleteMedia = vm::deleteMedia,
                                    onOpenViewer = vm::openViewer,
                                    onAddJournal = { vm.addJournal(memory.id, it) },
                                    onRetryUpload = vm::retryUpload,
                                    onDismissUpload = vm::dismissUpload,
                                    onPlayError = { toast(R.string.globe_toast_cannot_play) },
                                ),
                            )
                        }
                    }
                    is GlobePanel.Form -> {
                        LaunchedEffect(panel.token) {
                            formVm.start(panel.token, panel.editingId?.let { s.memory(it) }, panel.cityKey?.let { s.group(it) })
                        }
                        if (form.token == panel.token) {
                            val pickers = rememberMediaPickers(
                                onPhotos = { formVm.stagePicked(it, MediaKind.Photo) },
                                onVideo = { formVm.stagePicked(listOf(it), MediaKind.Video) },
                            )
                            val voice = rememberVoiceControl(
                                maxSeconds = null,
                                onFinished = { file, seconds -> formVm.stageVoice(file, seconds) },
                                onMessage = { toast(it) },
                            )
                            FormPanel(
                                wide = wide,
                                state = form,
                                cityTitle = panel.cityKey?.let { s.group(it)?.city },
                                pickers = pickers,
                                voice = voice,
                                actions = FormActions(
                                    onClose = vm::closeTopPanel,
                                    onLocationText = formVm::onLocationText,
                                    onPickPlace = formVm::pickPlace,
                                    onEdit = formVm::edit,
                                    onSave = formVm::save,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // ── loading and error ────────────────────────────────────────────────────────────────
        when (val load = s.load) {
            GlobeLoad.Loading -> Column(
                modifier = Modifier.fillMaxSize().background(colors.background),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m, Alignment.CenterVertically),
            ) {
                Text(stringResource(R.string.globe_title), style = MaterialTheme.typography.headlineMedium, fontFamily = SerifFamily, color = colors.ink)
                Text(stringResource(R.string.globe_loading_sub), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
                CircularProgressIndicator(color = colors.accent)
            }
            is GlobeLoad.Failed -> Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
                ErrorState(message = load.error.asText(), onRetry = vm::retry)
            }
            GlobeLoad.Ready -> Unit
        }

        if (s.statsOpen) {
            val stats = remember(s.memories) { travelStats(s.memories, YearMonth.now()) }
            StatsView(stats = stats, onClose = vm::closeStats)
        }
        s.viewer?.let { MediaViewer(items = it.items, startIndex = it.index, onClose = vm::closeViewer) }
    }
}

private const val VOICE_NOTE_SECONDS = 30
private const val SHEET_SLIDE_MS = 320

/** A number with a caption, tappable: opens the stats page (like the web chips). */
@Composable
private fun StatChip(value: String, caption: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Column(
        modifier = modifier
            .clip(TwinTheme.shapes.pill)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.labelLarge, color = colors.ink, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(caption, style = MaterialTheme.typography.labelSmall, color = colors.inkMuted, maxLines = 1)
    }
}

@Composable
private fun SearchBox(search: SearchUi, vm: GlobeViewModel) {
    val colors = TwinTheme.colors
    Column(modifier = Modifier.padding(horizontal = TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TwinTextField(
            value = search.query,
            onValueChange = vm::onSearchQuery,
            label = stringResource(R.string.globe_search),
            placeholder = stringResource(R.string.globe_search_hint),
            modifier = Modifier.fillMaxWidth(),
        )
        if (search.coordinates != null || search.hits.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TwinTheme.shapes.field)
                    .background(colors.card.copy(alpha = 0.97f))
                    .border(1.dp, colors.line, TwinTheme.shapes.field),
            ) {
                search.coordinates?.let { point ->
                    Text(
                        text = stringResource(R.string.globe_go_to_coordinates) + ": " +
                            stringResource(R.string.globe_coordinates_title, "%.4f".format(java.util.Locale.ROOT, point.lat), "%.4f".format(java.util.Locale.ROOT, point.lng)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { vm.pickCoordinates(point) }
                            .padding(TwinTheme.spacing.m),
                    )
                }
                search.hits.forEach { hit ->
                    Text(
                        text = hit.displayName.ifBlank { hit.city },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { vm.pickSearchHit(hit) }
                            .padding(TwinTheme.spacing.m),
                    )
                }
            }
        }
    }
}

/** "You: online · Partner: offline · 4.2 km apart" while live location is on. */
@Composable
private fun LiveInfo(live: LiveUi, partnerName: String, modifier: Modifier) {
    val colors = TwinTheme.colors
    fun state(person: com.twinhearts.app.feature.globe.domain.model.LivePerson?, online: String, offline: String) =
        if (person?.online == true) online else offline
    val online = stringResource(R.string.globe_live_online)
    val offline = stringResource(R.string.globe_live_offline)
    val distance = live.distanceKm?.let { km ->
        if (km < 1) stringResource(R.string.globe_live_dist_m, (km * 1000).toInt())
        else stringResource(R.string.globe_live_dist_km, "%.1f".format(java.util.Locale.ROOT, km))
    } ?: stringResource(R.string.globe_live_dist_none)
    GlassCard(modifier = modifier, contentPadding = TwinTheme.spacing.s) {
        Text(stringResource(R.string.globe_live_you, state(live.me, online, offline)), style = MaterialTheme.typography.labelMedium, color = colors.success)
        Text(
            stringResource(R.string.globe_live_partner, partnerName.ifBlank { "💕" }, state(live.partner, online, offline)),
            style = MaterialTheme.typography.labelMedium,
            color = colors.ink,
        )
        Text(distance, style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
    }
}

private const val SATELLITE_FADE_MS = 450
private const val SATELLITE_TIMEOUT_MS = 10_000L
