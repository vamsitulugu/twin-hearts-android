package com.twinhearts.app.feature.chat.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.PillSwitch
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.chat.domain.CropRect
import com.twinhearts.app.feature.chat.domain.WallpaperSwatchIds
import com.twinhearts.app.feature.chat.domain.clampOffset
import com.twinhearts.app.feature.chat.domain.fileEmoji
import com.twinhearts.app.feature.chat.domain.firstLink
import com.twinhearts.app.feature.chat.domain.formatFileSize
import com.twinhearts.app.feature.chat.domain.linkHost
import com.twinhearts.app.feature.chat.domain.minCoverScale
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import com.twinhearts.app.feature.chat.domain.model.ViewOnceResult
import com.twinhearts.app.feature.chat.domain.model.Wallpaper
import com.twinhearts.app.feature.chat.domain.model.WallpaperMode
import com.twinhearts.app.feature.chat.domain.visibleCrop
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

/**
 * Why this file exists: everything Chat shows on top of the conversation that is about pictures:
 * the full-screen viewer, the view-once viewer, the "Shared media" screen and the chat wallpaper
 * (background drawing, the picker screen and the "move and scale" cutter).
 */

private val SharedCell = 4.dp
private val SwatchSize = 56.dp
private val PreviewHeight = 220.dp

/** Empty space kept under the wallpaper buttons (on top of the system bar) so they are never cut off. */
private val SheetBottomGap = 28.dp
private const val SHARED_COLUMNS = 3
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val MAX_ZOOM = 4f
private const val MAX_DIM = 90
private const val PERCENT = 100f
private const val DIM_STEPS = 17
private const val SWATCH_DIM_ALPHA = 0.5f

// ───────────── viewers ─────────────

@Composable
internal fun ImageViewerDialog(urls: List<String>, startIndex: Int, onClose: () -> Unit) {
    val colors = TwinTheme.colors
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, urls.lastIndex.coerceAtLeast(0))) { urls.size }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page -> ZoomableImage(urls[page]) }
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.chat_close), tint = colors.ink) }
                if (urls.size > 1) {
                    Text(
                        stringResource(R.string.chat_viewer_count, pager.currentPage + 1, urls.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.size(TwinTheme.shapes.minTouch))
                }
            }
        }
    }
}

/** One picture; double-tap zooms in and out. */
@Composable
private fun ZoomableImage(url: Any) {
    var zoomed by remember { mutableStateOf(false) }
    AsyncImage(
        model = url,
        contentDescription = stringResource(R.string.chat_preview_photo),
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { zoomed = !zoomed }) }
            .graphicsLayer(scaleX = if (zoomed) DOUBLE_TAP_ZOOM else 1f, scaleY = if (zoomed) DOUBLE_TAP_ZOOM else 1f),
    )
}

/** A view-once picture. The window blocks screenshots, and the picture is gone when it closes. */
@Composable
internal fun ViewOnceDialog(result: ViewOnceResult, onClose: () -> Unit) {
    val colors = TwinTheme.colors
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, securePolicy = SecureFlagPolicy.SecureOn),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = result.url,
                contentDescription = stringResource(R.string.chat_preview_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.chat_close), tint = colors.ink) }
                Text("🔒 ${stringResource(R.string.chat_view_once_note)}", style = MaterialTheme.typography.labelLarge, color = colors.ink)
            }
        }
    }
}

// ───────────── shared media ─────────────

/** Photos and videos, documents and links of this chat, in three tabs (web: "Shared media"). */
@Composable
internal fun SharedMediaScreen(
    messages: List<ChatMessage>,
    onOpenImages: (List<String>, Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    onClose: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val media = remember(messages) {
        messages.filter {
            !it.deleted && it.mediaUrl != null &&
                (it.kind == MessageKind.Image || it.kind == MessageKind.ImageGroup || it.kind == MessageKind.Video || it.kind == MessageKind.Gif)
        }.sortedByDescending { it.createdAt }
    }
    val docs = remember(messages) {
        messages.filter { !it.deleted && it.mediaUrl != null && (it.kind == MessageKind.File || it.kind == MessageKind.Audio) }
            .sortedByDescending { it.createdAt }
    }
    val links = remember(messages) {
        messages.filter { !it.deleted && firstLink(it.text) != null }.sortedByDescending { it.createdAt }
    }
    FullScreenSheet(stringResource(R.string.chat_shared_title), onClose) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = TwinTheme.spacing.screenEdge),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            SegmentedTabs(
                options = listOf(
                    stringResource(R.string.chat_filter_media),
                    stringResource(R.string.chat_filter_docs),
                    stringResource(R.string.chat_filter_links),
                ),
                selectedIndex = tab,
                onSelect = { tab = it },
            )
            when (tab) {
                0 -> if (media.isEmpty()) {
                    EmptyState(title = stringResource(R.string.chat_shared_empty), icon = Icons.Rounded.Image)
                } else {
                    val urls = media.flatMap { it.groupUrls.ifEmpty { listOfNotNull(it.mediaUrl) } }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(SHARED_COLUMNS),
                        horizontalArrangement = Arrangement.spacedBy(SharedCell),
                        verticalArrangement = Arrangement.spacedBy(SharedCell),
                    ) {
                        items(urls.size) { index ->
                            AsyncImage(
                                model = urls[index],
                                contentDescription = stringResource(R.string.chat_preview_photo),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.aspectRatio(1f).clip(TwinTheme.shapes.small).clickable { onOpenImages(urls, index) },
                            )
                        }
                    }
                }
                1 -> if (docs.isEmpty()) {
                    EmptyState(title = stringResource(R.string.chat_shared_empty), icon = Icons.Rounded.Image)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                        items(docs, key = { it.clientId }) { doc ->
                            SharedRow(
                                icon = if (doc.kind == MessageKind.Audio) "🎵" else fileEmoji(doc.fileName),
                                title = doc.fileName ?: stringResource(R.string.chat_preview_file),
                                subtitle = formatFileSize(doc.fileSize),
                                onClick = { doc.mediaUrl?.let(onOpenUrl) },
                            )
                        }
                    }
                }
                else -> if (links.isEmpty()) {
                    EmptyState(title = stringResource(R.string.chat_shared_empty), icon = Icons.Rounded.Image)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                        items(links, key = { it.clientId }) { m ->
                            val url = firstLink(m.text).orEmpty()
                            SharedRow(icon = "🔗", title = linkHost(url) ?: url, subtitle = url, onClick = { onOpenUrl(url) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TwinTheme.shapes.small)
            .background(ChatStyle.Glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Text(icon, style = MaterialTheme.typography.headlineSmall)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.ink, maxLines = 1)
            if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, maxLines = 1)
        }
    }
}

// ───────────── wallpaper: drawing ─────────────

/** The chat background. "Default" draws nothing so the app's own burgundy background shows. */
@Composable
internal fun WallpaperLayer(wallpaper: Wallpaper, modifier: Modifier = Modifier) {
    val dimAlpha = wallpaper.dim.coerceIn(0, MAX_DIM) / PERCENT
    Box(modifier) {
        when (wallpaper.mode) {
            WallpaperMode.Default -> Unit
            WallpaperMode.Swatch -> Box(Modifier.fillMaxSize().background(swatchBrush(wallpaper.swatch)))
            WallpaperMode.Custom -> wallpaper.image?.let { path ->
                AsyncImage(
                    model = if (path.startsWith("/")) File(path) else path,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (wallpaper.mode != WallpaperMode.Default && dimAlpha > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)))
        }
    }
}

@Composable
private fun swatchBrush(id: String?): Brush {
    val c = TwinTheme.colors
    return when (id) {
        "noir" -> Brush.verticalGradient(listOf(Color.Black, c.background))
        "wine" -> Brush.verticalGradient(listOf(c.brand, c.background))
        "slate" -> Brush.verticalGradient(listOf(c.neutralGlass.copy(alpha = SWATCH_DIM_ALPHA), c.background))
        else -> Brush.verticalGradient(listOf(c.background, c.backgroundGlow))
    }
}

@Composable
private fun swatchName(id: String): String = stringResource(
    when (id) {
        "noir" -> R.string.chat_swatch_noir
        "wine" -> R.string.chat_swatch_wine
        "slate" -> R.string.chat_swatch_slate
        else -> R.string.chat_swatch_midnight
    },
)

// ───────────── wallpaper: picker screen ─────────────

/**
 * "Chat wallpaper": pick Default, a colour or your own photo (cut with move and scale), set how
 * dark it is, and choose whether it is for both of you. The cutting itself is done by [onCut].
 */
@Composable
internal fun WallpaperScreen(
    current: Wallpaper,
    shared: Boolean,
    saving: Boolean,
    onMeasure: suspend (Uri) -> Pair<Int, Int>?,
    onCut: suspend (Uri, CropRect) -> File?,
    onSave: (WallpaperMode, String?, File?, Int, Boolean) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = TwinTheme.colors
    var mode by rememberSaveable { mutableStateOf(current.mode) }
    var swatch by rememberSaveable { mutableStateOf(current.swatch) }
    var dim by rememberSaveable { mutableIntStateOf(current.dim) }
    var both by rememberSaveable { mutableStateOf(shared) }
    var cutPath by rememberSaveable { mutableStateOf(current.image?.takeIf { current.mode == WallpaperMode.Custom }) }
    var picked by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) picked = uri }
    val preview = Wallpaper(mode, swatch, cutPath, dim)
    FullScreenSheet(stringResource(R.string.chat_wallpaper_title), onClose) {
        val pickedUri = picked
        if (pickedUri != null) {
            WallpaperCutter(
                uri = pickedUri,
                onMeasure = onMeasure,
                onCut = onCut,
                onDone = { file ->
                    picked = null
                    if (file != null) {
                        cutPath = file.absolutePath
                        mode = WallpaperMode.Custom
                    }
                },
            )
        } else {
            Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = TwinTheme.spacing.screenEdge),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                Box(
                    Modifier.fillMaxWidth().height(PreviewHeight).clip(TwinTheme.shapes.card).border(1.dp, colors.lineSoft, TwinTheme.shapes.card),
                ) {
                    Box(Modifier.fillMaxSize().background(colors.background))
                    WallpaperLayer(preview, Modifier.fillMaxSize())
                    Column(Modifier.fillMaxSize().padding(TwinTheme.spacing.m), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                        Text(
                            stringResource(R.string.chat_wallpaper_sample_theirs),
                            color = colors.ink,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clip(TwinTheme.shapes.small).background(colors.neutralGlass.copy(alpha = SWATCH_DIM_ALPHA)).padding(TwinTheme.spacing.s),
                        )
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text(
                                stringResource(R.string.chat_wallpaper_sample_mine),
                                color = colors.ink,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.clip(TwinTheme.shapes.small).background(ChatStyle.SentBubble).padding(TwinTheme.spacing.s),
                            )
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    Tile(stringResource(R.string.chat_wallpaper_default), mode == WallpaperMode.Default, { mode = WallpaperMode.Default }) {
                        Box(Modifier.fillMaxSize().background(colors.background))
                    }
                    WallpaperSwatchIds.forEach { id ->
                        Tile(swatchName(id), mode == WallpaperMode.Swatch && swatch == id, { mode = WallpaperMode.Swatch; swatch = id }) {
                            Box(Modifier.fillMaxSize().background(swatchBrush(id)))
                        }
                    }
                    Tile(stringResource(R.string.chat_wallpaper_photo), mode == WallpaperMode.Custom, {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        val path = cutPath
                        if (path != null) {
                            AsyncImage(model = File(path), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Image, null, tint = colors.ink) }
                        }
                    }
                }
                if (mode != WallpaperMode.Default) {
                    Text(stringResource(R.string.chat_wallpaper_dim, dim), style = MaterialTheme.typography.labelLarge, color = colors.inkMuted)
                    Slider(
                        value = dim.toFloat(),
                        onValueChange = { dim = it.roundToInt() },
                        valueRange = 0f..MAX_DIM.toFloat(),
                        steps = DIM_STEPS,
                        colors = SliderDefaults.colors(thumbColor = colors.pinkSoft, activeTrackColor = colors.accent, inactiveTrackColor = colors.lineSoft),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.chat_wallpaper_shared), style = MaterialTheme.typography.bodyLarge, color = colors.ink, modifier = Modifier.weight(1f))
                    PillSwitch(checked = both, onCheckedChange = { both = it })
                }
            }
            // Save and Reset stay pinned at the bottom (they used to scroll away under the screen edge).
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = TwinTheme.spacing.screenEdge)
                    .padding(top = TwinTheme.spacing.s, bottom = SheetBottomGap),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                GlassButton(
                    text = stringResource(R.string.chat_wallpaper_save),
                    onClick = {
                        val file = cutPath?.let { File(it) }
                        onSave(mode, swatch, if (mode == WallpaperMode.Custom) file else null, dim, both)
                        onClose()
                    },
                    enabled = !saving && (mode != WallpaperMode.Custom || cutPath != null),
                    loading = saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                GlassButton(
                    text = stringResource(R.string.chat_wallpaper_reset),
                    onClick = { onReset(); onClose() },
                    style = GlassButtonStyle.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            }
        }
    }
}

@Composable
private fun Tile(label: String, selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = TwinTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .size(SwatchSize)
                .clip(TwinTheme.shapes.small)
                .border(if (selected) 2.dp else 1.dp, if (selected) colors.pinkSoft else colors.lineSoft, TwinTheme.shapes.small),
        ) { content() }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) colors.pinkSoft else colors.inkMuted)
    }
}

// ───────────── wallpaper: move and scale ─────────────

/** Drag and pinch the photo so the part you want fills the screen, then tap "Use this photo". */
@Composable
private fun WallpaperCutter(
    uri: Uri,
    onMeasure: suspend (Uri) -> Pair<Int, Int>?,
    onCut: suspend (Uri, CropRect) -> File?,
    onDone: (File?) -> Unit,
) {
    val colors = TwinTheme.colors
    val scope = rememberCoroutineScope()
    var natural by remember(uri) { mutableStateOf<Pair<Int, Int>?>(null) }
    var stageW by remember { mutableFloatStateOf(0f) }
    var stageH by remember { mutableFloatStateOf(0f) }
    var scale by remember(uri) { mutableFloatStateOf(0f) }
    var ox by remember(uri) { mutableFloatStateOf(0f) }
    var oy by remember(uri) { mutableFloatStateOf(0f) }
    var working by remember { mutableStateOf(false) }
    LaunchedEffect(uri) { natural = onMeasure(uri) }
    val nat = natural
    val ready = nat != null && stageW > 0f && stageH > 0f
    val natW = nat?.first?.toFloat() ?: 1f
    val natH = nat?.second?.toFloat() ?: 1f
    val minScale = if (ready) minCoverScale(natW, natH, stageW, stageH) else 1f
    LaunchedEffect(ready) {
        if (ready && scale == 0f) {
            scale = minScale
            ox = clampOffset(0f, natW, scale, stageW)
            oy = clampOffset(0f, natH, scale, stageH)
        }
    }
    val density = LocalDensity.current
    Column(
        Modifier.fillMaxSize().padding(start = TwinTheme.spacing.screenEdge, end = TwinTheme.spacing.screenEdge, top = TwinTheme.spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Text(stringResource(R.string.chat_wallpaper_crop_hint), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(TwinTheme.shapes.card)
                .border(1.dp, colors.lineSoft, TwinTheme.shapes.card)
                .clipToBounds()
                .onSizeChanged { stageW = it.width.toFloat(); stageH = it.height.toFloat() }
                .pointerInput(ready, minScale) {
                    if (!ready) return@pointerInput
                    detectTransformGestures { centroid: Offset, pan: Offset, zoom: Float, _: Float ->
                        val next = (scale * zoom).coerceIn(minScale, minScale * MAX_ZOOM)
                        val ratio = next / scale
                        val nx = centroid.x - (centroid.x - ox) * ratio + pan.x
                        val ny = centroid.y - (centroid.y - oy) * ratio + pan.y
                        scale = next
                        ox = clampOffset(nx, natW, scale, stageW)
                        oy = clampOffset(ny, natH, scale, stageH)
                    }
                },
        ) {
            if (ready && scale > 0f) {
                AsyncImage(
                    model = uri,
                    contentDescription = stringResource(R.string.chat_preview_photo),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .requiredSize(with(density) { (natW * scale).toDp() }, with(density) { (natH * scale).toDp() })
                        .offset { IntOffset(ox.roundToInt(), oy.roundToInt()) },
                )
            }
        }
        Row(
            // Extra gap under the buttons so the gesture bar / screen edge never covers them.
            modifier = Modifier.padding(bottom = SheetBottomGap),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            GlassButton(
                text = stringResource(R.string.chat_cancel),
                onClick = { onDone(null) },
                style = GlassButtonStyle.Ghost,
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                text = stringResource(R.string.chat_wallpaper_use),
                onClick = {
                    working = true
                    scope.launch {
                        val crop = visibleCrop(natW, natH, stageW, stageH, scale, ox, oy)
                        onDone(onCut(uri, crop))
                    }
                },
                enabled = ready && !working,
                loading = working,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
