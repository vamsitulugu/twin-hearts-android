package com.twinhearts.app.feature.home.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.home.data.LoadedPhoto
import com.twinhearts.app.feature.home.domain.CARD_PHOTO_MAX_ZOOM
import com.twinhearts.app.feature.home.domain.CropMath
import com.twinhearts.app.feature.home.domain.model.CardPhoto
import com.twinhearts.app.feature.home.domain.model.CardPhotoSlot
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val MESSAGE_MS = 3500L
private const val FRAME_ASPECT = 16f / 9f
private const val GRID_LINES = 3
private val slots = CardPhotoSlot.entries

/** The built-in picture of a slot (shown until the couple saves their own). */
internal fun defaultCardImage(slot: CardPhotoSlot): Int = when (slot) {
    CardPhotoSlot.Idle -> R.drawable.home_card_idle
    CardPhotoSlot.Hug -> R.drawable.home_card_hug
    CardPhotoSlot.Touch -> R.drawable.home_card_touch
    CardPhotoSlot.MissYou -> R.drawable.home_card_miss
}

private fun slotLabel(slot: CardPhotoSlot): Int = when (slot) {
    CardPhotoSlot.Idle -> R.string.home_photo_slot_idle
    CardPhotoSlot.Hug -> R.string.home_photo_slot_hug
    CardPhotoSlot.Touch -> R.string.home_photo_slot_touch
    CardPhotoSlot.MissYou -> R.string.home_photo_slot_miss
}

/**
 * Full-screen "Card photos" editor (web: `#dwPhotoOv`). Opens from the card's 3-dots menu.
 * [photos] are the pictures already saved; [onChanged] tells Home to read them again.
 */
@Composable
internal fun CardPhotoEditorDialog(
    initialSlot: CardPhotoSlot,
    photos: Map<CardPhotoSlot, CardPhoto>,
    onChanged: () -> Unit,
    onClose: () -> Unit,
    viewModel: CardPhotoEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(initialSlot) { viewModel.open(initialSlot) }
    LaunchedEffect(state.changeCount) { if (state.changeCount > 0) onChanged() }
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(MESSAGE_MS)
            viewModel.clearMessage()
        }
    }
    // The system photo picker needs no permission (it only hands over the photo the user picks).
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onPhotoPicked(uri?.toString())
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        TwinBackground {
            CardPhotoEditorContent(
                state = state,
                saved = photos,
                onClose = onClose,
                onSelectSlot = viewModel::selectSlot,
                onPan = viewModel::pan,
                onZoom = viewModel::zoomTo,
                onChangePhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onSave = viewModel::save,
                onUseDefault = viewModel::useDefault,
            )
        }
    }
}

@Composable
internal fun CardPhotoEditorContent(
    state: CardPhotoEditorUiState,
    saved: Map<CardPhotoSlot, CardPhoto>,
    onClose: () -> Unit,
    onSelectSlot: (CardPhotoSlot) -> Unit,
    onPan: (dx: Float, dy: Float, stageWidthPx: Float) -> Unit,
    onZoom: (zoom: Float, focusX: Float, focusY: Float) -> Unit,
    onChangePhoto: () -> Unit,
    onSave: () -> Unit,
    onUseDefault: () -> Unit,
) {
    val colors = TwinTheme.colors
    val pending = state.pending[state.slot]
    val savedPhoto = saved[state.slot]
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, stringResource(R.string.home_photo_close), tint = colors.ink)
            }
            Text(
                stringResource(R.string.home_photo_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(TwinTheme.shapes.minTouch))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            SegmentedTabs(
                options = slots.map { stringResource(slotLabel(it)) },
                selectedIndex = slots.indexOf(state.slot),
                onSelect = { onSelectSlot(slots[it]) },
            )
            PhotoStage(
                slot = state.slot,
                saved = savedPhoto,
                pending = pending,
                onPan = onPan,
                onZoom = onZoom,
            )
            Text(
                text = stringResource(if (pending != null) R.string.home_photo_hint_crop else R.string.home_photo_hint_pick),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            // Kept in the layout (just invisible) so nothing jumps when a photo is picked, like the web.
            ZoomRow(
                visible = pending != null,
                zoom = pending?.crop?.zoom ?: 1f,
                onZoom = { onZoom(it, 0.5f, 0.5f) },
            )
            if (pending == null && savedPhoto != null) {
                Text(
                    text = stringResource(R.string.home_photo_use_default),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.pinkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TwinTheme.shapes.minTouch)
                        .clickable(enabled = !state.isBusy, role = Role.Button, onClick = onUseDefault)
                        .padding(TwinTheme.spacing.m),
                )
            }
            HomeBanner(state.message)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HomeCardStyle.EditorBase)
                .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            GlassButton(
                text = stringResource(R.string.home_photo_change),
                onClick = onChangePhoto,
                enabled = !state.isBusy,
                style = GlassButtonStyle.Secondary,
                leadingIcon = Icons.Rounded.Image,
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                text = stringResource(if (state.isBusy) R.string.home_photo_saving else R.string.home_photo_save),
                onClick = onSave,
                enabled = pending != null && !state.isBusy,
                leadingIcon = Icons.Rounded.Check,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** The 16:9 frame: the saved/default picture, or the picked photo that can be dragged and pinched. */
@Composable
private fun PhotoStage(
    slot: CardPhotoSlot,
    saved: CardPhoto?,
    pending: PendingPhoto?,
    onPan: (Float, Float, Float) -> Unit,
    onZoom: (Float, Float, Float) -> Unit,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    val currentPending by rememberUpdatedState(pending)
    val currentOnPan by rememberUpdatedState(onPan)
    val currentOnZoom by rememberUpdatedState(onZoom)
    var frame by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(FRAME_ASPECT)
            .clip(shape)
            .background(HomeCardStyle.StageBase)
            .border(1.dp, colors.lineSoft, shape)
            .onSizeChanged { frame = it }
            .pointerInput(pending != null) {
                if (pending == null) return@pointerInput
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val now = currentPending ?: return@detectTransformGestures
                    val width = frame.width.toFloat()
                    val height = frame.height.toFloat()
                    if (width <= 0f || height <= 0f) return@detectTransformGestures
                    if (zoom != 1f) currentOnZoom(now.crop.zoom * zoom, centroid.x / width, centroid.y / height)
                    if (pan != Offset.Zero) currentOnPan(pan.x, pan.y, width)
                }
            },
    ) {
        if (pending == null) {
            val fallback = painterResource(defaultCardImage(slot))
            if (saved != null) {
                AsyncImage(
                    model = saved.url,
                    contentDescription = null,
                    placeholder = fallback,
                    error = fallback,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Image(fallback, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        } else {
            CropCanvas(pending)
        }
        Text(
            text = stringResource(R.string.home_photo_ratio_badge),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.ink,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(TwinTheme.spacing.s)
                .clip(TwinTheme.shapes.pill)
                .background(HomeCardStyle.Chip)
                .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
        )
    }
}

/** Draws the visible part of the photo, plus the rule-of-thirds guide lines. */
@Composable
private fun CropCanvas(pending: PendingPhoto) {
    val image = remember(pending.photo) { (pending.photo.payload as Bitmap).asImageBitmap() }
    val guide = TwinTheme.colors.ink.copy(alpha = 0.45f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rect = CropMath.rect(pending.crop, pending.photo.width, pending.photo.height)
        drawImage(
            image = image,
            srcOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
            srcSize = IntSize(rect.width.roundToInt().coerceAtLeast(1), rect.height.roundToInt().coerceAtLeast(1)),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.High,
        )
        for (i in 1 until GRID_LINES) {
            val x = size.width * i / GRID_LINES
            val y = size.height * i / GRID_LINES
            drawLine(guide, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            drawLine(guide, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
    }
}

@Composable
private fun ZoomRow(visible: Boolean, zoom: Float, onZoom: (Float) -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().alpha(if (visible) 1f else 0f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Icon(Icons.Rounded.Remove, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(18.dp))
        Slider(
            value = zoom,
            onValueChange = onZoom,
            valueRange = 1f..CARD_PHOTO_MAX_ZOOM,
            enabled = visible,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.lineSoft,
            ),
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(18.dp))
    }
}

private val previewPhoto = CardPhoto(url = "", path = "")

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 760, widthDp = 360)
@Composable
private fun EditorIdlePreview() {
    TwinTheme {
        TwinBackground {
            CardPhotoEditorContent(CardPhotoEditorUiState(), mapOf(CardPhotoSlot.Idle to previewPhoto), {}, {}, { _, _, _ -> }, { _, _, _ -> }, {}, {}, {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 760, widthDp = 360)
@Composable
private fun EditorCroppingPreview() {
    val bitmap = Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888)
    val pending = PendingPhoto(LoadedPhoto(1600, 1200, bitmap), CropMath.initial(1600, 1200))
    TwinTheme {
        TwinBackground {
            CardPhotoEditorContent(
                CardPhotoEditorUiState(pending = mapOf(CardPhotoSlot.Idle to pending)),
                emptyMap(), {}, {}, { _, _, _ -> }, { _, _, _ -> }, {}, {}, {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 760, widthDp = 360, fontScale = 1.3f)
@Composable
private fun EditorErrorLargeFontPreview() {
    TwinTheme {
        TwinBackground {
            CardPhotoEditorContent(
                CardPhotoEditorUiState(slot = CardPhotoSlot.MissYou, message = HomeMessage.Problem(R.string.home_photo_too_large)),
                emptyMap(), {}, {}, { _, _, _ -> }, { _, _, _ -> }, {}, {}, {},
            )
        }
    }
}
