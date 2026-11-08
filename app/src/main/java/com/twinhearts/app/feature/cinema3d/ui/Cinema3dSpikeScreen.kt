package com.twinhearts.app.feature.cinema3d.ui

import android.os.Build
import android.util.Log
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.feature.cinema3d.data.CinemaFiles
import com.twinhearts.app.feature.cinema3d.domain.CameraRig
import com.twinhearts.app.feature.cinema3d.domain.FrameStats
import com.twinhearts.app.feature.cinema3d.domain.ViewPreset
import com.twinhearts.app.feature.cinema3d.domain.applyPreset
import com.twinhearts.app.feature.cinema3d.render.FilamentStage
import com.twinhearts.app.feature.cinema3d.render.ModelInfo
import com.twinhearts.app.feature.cinema3d.render.StageModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

private const val TAG = "Cinema3dSpike"

/** Names the spike looks up inside each model, to prove the exported names reached Filament. */
private val HALL_PROBES = listOf("screen_surface", "screen_bezel", "seat_A1", "seat_E8", "seat_I15", "door_left", "door_right")
private val LOBBY_PROBES = listOf("poster_0", "poster_9")

/**
 * C1 spike: the hall and lobby inside Filament inside Compose, with look/orbit controls.
 * Developer test page, so the labels are plain English here rather than string resources.
 */
@Composable
fun Cinema3dSpikeScreen(
    onBack: () -> Unit,
    viewModel: Cinema3dSpikeViewModel = hiltViewModel(),
) {
    val load by viewModel.load.collectAsStateWithLifecycle()
    val controls by viewModel.controls.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = load) {
            SpikeLoad.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            SpikeLoad.Failed -> SpikeMessage(
                text = "Couldn't read the theatre files. Check that app/src/main/assets/cinema3d/ has the two .glb files and the layout json.",
                actionLabel = "Try again",
                onAction = viewModel::retry,
                onBack = onBack,
            )
            is SpikeLoad.Ready -> SpikeStage(
                files = state.files,
                controls = controls,
                onBack = onBack,
                onPreset = viewModel::selectPreset,
                onToggleLobby = viewModel::toggleLobby,
                onToggleQuality = viewModel::toggleQuality,
                onBrightness = viewModel::setBrightness,
            )
        }
    }
}

@Composable
private fun SpikeStage(
    files: CinemaFiles,
    controls: SpikeControls,
    onBack: () -> Unit,
    onPreset: (ViewPreset) -> Unit,
    onToggleLobby: () -> Unit,
    onToggleQuality: () -> Unit,
    onBrightness: (Float) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val surfaceView = remember { SurfaceView(context) }
    val rig = remember { CameraRig() }

    var stage by remember { mutableStateOf<FilamentStage?>(null) }
    var startError by remember { mutableStateOf<String?>(null) }
    var hall by remember { mutableStateOf<StageModel?>(null) }
    var lobby by remember { mutableStateOf<StageModel?>(null) }
    var modelsFailed by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(FrameStats()) }

    // Create Filament once the surface view exists; free it when the screen goes away.
    DisposableEffect(surfaceView) {
        var created: FilamentStage? = null
        try {
            created = FilamentStage(surfaceView).also {
                it.rig = rig
                stage = it
            }
        } catch (e: Throwable) {
            // Most likely: no OpenGL ES 3.0, or a CPU type Filament has no library for.
            Log.e(TAG, "Filament could not start", e)
            startError = "${e.javaClass.simpleName}: ${e.message}"
        }
        onDispose {
            stage = null
            hall = null
            lobby = null
            created?.destroy()
        }
    }

    // Draw only while the screen is visible.
    DisposableEffect(stage, lifecycle) {
        val current = stage
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> current?.start()
                Lifecycle.Event.ON_PAUSE -> current?.stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            current?.stop()
        }
    }

    // Load both models into the scene (main thread; this is the "how long does it take" number).
    LaunchedEffect(stage, files) {
        val s = stage ?: return@LaunchedEffect
        val hallModel = s.loadModel("Hall", files.hall.duplicate(), HALL_PROBES)
        yield()
        val lobbyModel = s.loadModel("Lobby", files.lobby.duplicate(), LOBBY_PROBES)
        modelsFailed = hallModel == null || lobbyModel == null
        hall = hallModel
        lobby = lobbyModel
    }

    LaunchedEffect(stage, lobby, controls.showLobby) {
        val s = stage ?: return@LaunchedEffect
        val model = lobby ?: return@LaunchedEffect
        s.setVisible(model, controls.showLobby)
    }
    LaunchedEffect(stage, controls.highQuality) { stage?.setHighQuality(controls.highQuality) }
    LaunchedEffect(stage, controls.brightnessEv) { stage?.setBrightness(controls.brightnessEv) }
    LaunchedEffect(controls.preset, files) { applyPreset(rig, controls.preset, files.layout) }

    LaunchedEffect(stage) {
        val s = stage ?: return@LaunchedEffect
        while (true) {
            delay(STATS_REFRESH_MS)
            stats = s.frameStats
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { surfaceView }, modifier = Modifier.fillMaxSize())

        // Touch layer: one finger looks around, two fingers pinch to walk (or zoom in orbit).
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(rig) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        rig.drag(pan.x, pan.y)
                        if (zoom != 1f) rig.pinch(zoom)
                    }
                },
        )

        val startFailure = startError
        if (startFailure != null) {
            SpikeMessage(
                text = "3D could not start on this phone.\n$startFailure",
                actionLabel = null,
                onAction = {},
                onBack = onBack,
            )
        } else {
            Hud(
                stats = stats,
                hall = hall?.info,
                lobby = lobby?.info,
                modelsFailed = modelsFailed,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp),
            )
            Controls(
                controls = controls,
                reportProvider = { buildReport(stats, hall?.info, lobby?.info, controls) },
                onBack = onBack,
                onPreset = onPreset,
                onToggleLobby = onToggleLobby,
                onToggleQuality = onToggleQuality,
                onBrightness = onBrightness,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun Hud(
    stats: FrameStats,
    hall: ModelInfo?,
    lobby: ModelInfo?,
    modelsFailed: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(Color(0x99000000))
            .padding(8.dp),
    ) {
        Text("%.0f fps   avg %.1f ms   worst %.1f ms".format(stats.fps, stats.averageMs, stats.worstMs), color = Color.White, fontSize = 12.sp)
        when {
            modelsFailed -> Text("A model failed to load. See logcat tag Cinema3dSpike.", color = Color(0xFFFF8A80), fontSize = 12.sp)
            hall == null -> Text("Loading models…", color = Color.White, fontSize = 12.sp)
            else -> {
                ModelLines(hall)
                if (lobby != null) ModelLines(lobby)
            }
        }
    }
}

@Composable
private fun ModelLines(info: ModelInfo) {
    Text(
        "${info.label}: ${info.loadMillis} ms, ${info.entityCount} nodes, ${info.renderableCount} meshes",
        color = Color.White,
        fontSize = 12.sp,
    )
    val missing = info.probes.filterNot { it.second }.map { it.first }
    Text(
        if (missing.isEmpty()) "  names found: ${info.probes.size}/${info.probes.size}" else "  MISSING: ${missing.joinToString()}",
        color = if (missing.isEmpty()) Color(0xFFA5D6A7) else Color(0xFFFF8A80),
        fontSize = 12.sp,
    )
}

@Composable
private fun Controls(
    controls: SpikeControls,
    reportProvider: () -> String,
    onBack: () -> Unit,
    onPreset: (ViewPreset) -> Unit,
    onToggleLobby: () -> Unit,
    onToggleQuality: () -> Unit,
    onBrightness: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier
            .fillMaxWidth()
            .background(Color(0xB3000000))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ViewPreset.entries.forEach { preset ->
                FilterChip(
                    selected = controls.preset == preset,
                    onClick = { onPreset(preset) },
                    label = { Text(preset.label) },
                )
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = controls.showLobby, onClick = onToggleLobby, label = { Text("Lobby") })
            FilterChip(
                selected = controls.highQuality,
                onClick = onToggleQuality,
                label = { Text(if (controls.highQuality) "Quality: high" else "Quality: low") },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Brightness", color = Color.White, fontSize = 12.sp)
            Slider(
                value = controls.brightnessEv,
                onValueChange = onBrightness,
                valueRange = Cinema3dSpikeViewModel.MIN_EV..Cinema3dSpikeViewModel.MAX_EV,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Back") }
            TextButton(onClick = { clipboard.setText(AnnotatedString(reportProvider())) }) { Text("Copy test report") }
        }
    }
}

@Composable
private fun SpikeMessage(
    text: String,
    actionLabel: String?,
    onAction: () -> Unit,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(24.dp)) {
        Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null) Button(onClick = onAction) { Text(actionLabel) }
            TextButton(onClick = onBack) { Text("Back") }
        }
    }
}

/** Plain text the tester can paste back: phone, speed, load times, which names were found. */
private fun buildReport(stats: FrameStats, hall: ModelInfo?, lobby: ModelInfo?, controls: SpikeControls): String = buildString {
    appendLine("Cinema 3D spike (C1)")
    appendLine("Phone: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
    appendLine("View: ${controls.preset.label}, lobby ${if (controls.showLobby) "on" else "off"}, quality ${if (controls.highQuality) "high" else "low"}, brightness ${"%.1f".format(controls.brightnessEv)} EV")
    appendLine("Speed: %.1f fps, avg %.1f ms, worst %.1f ms".format(stats.fps, stats.averageMs, stats.worstMs))
    listOf(hall, lobby).forEach { info ->
        if (info == null) {
            appendLine("(model not loaded)")
        } else {
            appendLine("${info.label}: loaded in ${info.loadMillis} ms, ${info.entityCount} nodes, ${info.renderableCount} meshes")
            appendLine("  names: " + info.probes.joinToString { "${it.first}=${if (it.second) "ok" else "MISSING"}" })
        }
    }
}

private const val STATS_REFRESH_MS = 500L
