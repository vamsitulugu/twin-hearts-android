package com.twinhearts.app.feature.games.ui.play

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.engine.PuzzleEngine
import com.twinhearts.app.feature.games.domain.engine.PuzzlePhase
import com.twinhearts.app.feature.games.domain.engine.PuzzlePiece
import com.twinhearts.app.feature.games.domain.engine.PuzzleState
import com.twinhearts.app.feature.games.domain.model.Players
import com.twinhearts.app.feature.games.ui.components.CoopChat
import com.twinhearts.app.feature.games.ui.components.CountdownNumber
import com.twinhearts.app.feature.games.ui.components.GameHost
import com.twinhearts.app.feature.games.ui.components.GameInput
import com.twinhearts.app.feature.games.ui.components.GameTimerBar
import com.twinhearts.app.feature.games.ui.components.HeroTitle
import com.twinhearts.app.feature.games.ui.components.InfoChip
import com.twinhearts.app.feature.games.ui.components.SelectCard
import com.twinhearts.app.feature.games.ui.components.StatTile
import com.twinhearts.app.feature.games.ui.components.rememberNow

@Composable
fun PuzzleScreen(onExit: () -> Unit, viewModel: PuzzleViewModel = hiltViewModel()) {
    GameHost(
        viewModel = viewModel,
        title = stringResource(R.string.games_puzzle_title),
        onExit = onExit,
        alongside = { viewModel.runClock() },
    ) { state, players ->
        when (state.phase) {
            PuzzlePhase.Setup -> Setup(viewModel)
            PuzzlePhase.Countdown -> {
                val now = rememberNow(120L)
                CountdownNumber(Math.ceil((state.countdownAt - now) / 1000.0).toInt())
            }
            PuzzlePhase.Playing -> Playing(state, players, viewModel)
            PuzzlePhase.Result -> PuzzleDone(state, viewModel)
        }
    }
}

// ── setup ──

@Composable
private fun Setup(viewModel: PuzzleViewModel) {
    val colors = TwinTheme.colors
    val setup by viewModel.setup.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.photoPicked(it.toString()) }
    }
    val shape = TwinTheme.shapes.card

    HeroTitle(stringResource(R.string.games_pz_title))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, if (setup.photoUrl != null) colors.success else colors.line2, shape)
            .clickable(enabled = !setup.uploading) { picker.launch("image/*") }
            .padding(TwinTheme.spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        val url = setup.photoUrl
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = stringResource(R.string.games_pz_photo_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(120.dp).clip(TwinTheme.shapes.small),
            )
            Text(stringResource(R.string.games_pz_photo_ready), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        } else {
            Text("📷", fontSize = 32.sp)
            Text(stringResource(R.string.games_pz_choose), style = MaterialTheme.typography.titleSmall, color = colors.ink, textAlign = TextAlign.Center)
            Text(stringResource(R.string.games_pz_choose_sub), style = MaterialTheme.typography.labelMedium, color = colors.inkFaint, textAlign = TextAlign.Center)
        }
    }

    val choices = PuzzleEngine.gridChoices
    (choices.map { it as Int? } + listOf<Int?>(null)).chunked(3).forEach { rowItems ->
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
            rowItems.forEach { n ->
                if (n != null) {
                    SelectCard(selected = !setup.custom && setup.gridN == n, onClick = { viewModel.pickSize(n) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.games_pz_size_label, n), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                        Text(stringResource(R.string.games_pz_size_pieces, n * n), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                    }
                } else {
                    SelectCard(selected = setup.custom, onClick = viewModel::openCustom, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.games_pz_custom), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                        Text(
                            if (setup.custom) stringResource(R.string.games_pz_size_label, setup.gridN) else stringResource(R.string.games_pz_custom_range),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.inkMuted,
                        )
                    }
                }
            }
        }
    }
    if (setup.custom) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            GlassButton("−", onClick = { viewModel.pickSize(setup.gridN - 1) }, style = GlassButtonStyle.Secondary, enabled = setup.gridN > PuzzleEngine.MIN_GRID)
            Text("${setup.gridN}", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
            GlassButton("+", onClick = { viewModel.pickSize(setup.gridN + 1) }, style = GlassButtonStyle.Secondary, enabled = setup.gridN < PuzzleEngine.MAX_GRID)
            Text(stringResource(R.string.games_pz_custom_hint), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        }
    }
    Text(stringResource(R.string.games_pz_explain), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    GlassButton(
        stringResource(R.string.games_pz_start),
        onClick = viewModel::start,
        enabled = setup.photoUrl != null && !setup.uploading,
        loading = setup.uploading,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ── the photo, cut into tiles ──

/** Loads the photo once as a bitmap so tiles can be cut from it. Null while it is loading. */
@Composable
private fun rememberPuzzleImage(url: String): ImageBitmap? {
    val context = LocalContext.current
    var image by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(PHOTO_PX).build()
        val result = context.imageLoader.execute(request)
        image = (result as? SuccessResult)?.drawable?.toBitmap()?.asImageBitmap()
    }
    return image
}

private const val PHOTO_PX = 1024

/** One square piece of the photo (centre-cropped to a square, then cut into n x n), turned by [rot]. */
@Composable
private fun Tile(image: ImageBitmap?, n: Int, row: Int, col: Int, rot: Int, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Canvas(modifier = modifier.aspectRatio(1f).background(colors.glass)) {
        val img = image ?: return@Canvas
        val square = minOf(img.width, img.height)
        val cell = square / n
        val offsetX = (img.width - square) / 2 + col * cell
        val offsetY = (img.height - square) / 2 + row * cell
        rotate(rot.toFloat()) {
            drawImage(
                image = img,
                srcOffset = IntOffset(offsetX, offsetY),
                srcSize = IntSize(cell, cell),
                dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}

// ── playing ──

@Composable
private fun Playing(state: PuzzleState, players: Players, viewModel: PuzzleViewModel) {
    val colors = TwinTheme.colors
    val me = players.side
    val n = state.gridN
    val image = rememberPuzzleImage(state.imageUrl)
    val held by viewModel.held.collectAsState()
    val now = rememberNow(1_000L)
    val mine = state.mine(me)

    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        InfoChip(stringResource(R.string.games_pz_time, PuzzleEngine.elapsedText(maxOf(0L, now - state.startedAt))))
        InfoChip(stringResource(R.string.games_pz_moves, state.moves))
        InfoChip(stringResource(R.string.games_pz_placed, state.placedCount, state.pieces.size))
        InfoChip(stringResource(R.string.games_pz_partner_left, players.partnerName, state.leftFor(me.other)))
    }
    GameTimerBar(state.placedCount / maxOf(1, state.pieces.size).toFloat(), low = false)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
        (0 until n).forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                (0 until n).forEach { c ->
                    val done = state.pieces.any { it.placed && it.row == r && it.col == c }
                    val slotLabel = stringResource(R.string.games_pz_slot, r + 1, c + 1)
                    if (done) {
                        Tile(image, n, r, c, 0, Modifier.weight(1f).clip(TwinTheme.shapes.small))
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(TwinTheme.shapes.small)
                                .background(colors.glass)
                                .border(1.dp, if (held != null) colors.accent else colors.lineSoft, TwinTheme.shapes.small)
                                .clickable(enabled = held != null) { viewModel.placeHeld(r, c) }
                                .semantics { contentDescription = slotLabel },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${r + 1},${c + 1}", style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
                        }
                    }
                }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.games_pz_tray, mine.size), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        Text(
            stringResource(if (held != null) R.string.games_pz_tray_tap_slot else R.string.games_pz_tray_tap_piece),
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkMuted,
        )
    }
    if (mine.isEmpty()) {
        Text(stringResource(R.string.games_pz_tray_empty), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            mine.forEach { piece -> TrayPiece(piece, n, image, selected = held == piece.id, viewModel = viewModel) }
        }
    }
    CoopChat(
        emojis = PuzzleEngine.emojis,
        log = state.chat,
        players = players,
        placeholder = stringResource(R.string.games_pz_chat_hint),
        onSend = viewModel::send,
    )
}

@Composable
private fun TrayPiece(piece: PuzzlePiece, n: Int, image: ImageBitmap?, selected: Boolean, viewModel: PuzzleViewModel) {
    val colors = TwinTheme.colors
    val label = stringResource(R.string.games_pz_piece, piece.row + 1, piece.col + 1)
    val rotateLabel = stringResource(R.string.games_pz_rotate)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        Tile(
            image = image,
            n = n,
            row = piece.row,
            col = piece.col,
            rot = piece.rot,
            modifier = Modifier
                .size(72.dp)
                .clip(TwinTheme.shapes.small)
                .border(if (selected) 3.dp else 1.dp, if (selected) colors.accent else colors.lineSoft, TwinTheme.shapes.small)
                .clickable { viewModel.select(piece.id) }
                .semantics { contentDescription = label },
        )
        if (selected) {
            Text(
                "↻",
                fontSize = 20.sp,
                color = colors.ink,
                modifier = Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.primaryBrush)
                    .clickable { viewModel.rotate(piece.id) }
                    .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs)
                    .semantics { contentDescription = rotateLabel },
            )
        }
    }
}

// ── result ──

@Composable
private fun PuzzleDone(state: PuzzleState, viewModel: PuzzleViewModel) {
    val colors = TwinTheme.colors
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        AsyncImage(
            model = state.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(TwinTheme.shapes.card),
        )
        Text(stringResource(R.string.games_pz_done), style = MaterialTheme.typography.headlineMedium, color = colors.ink, textAlign = TextAlign.Center)
        Text(stringResource(R.string.games_pz_done_sub, PuzzleEngine.elapsedText(state.finishMs), state.moves), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
        StatTile("${state.teamStreak}", stringResource(R.string.games_stat_streak_label), Modifier.weight(1f))
        StatTile(if (state.bestMs == 0L) "—" else "${state.bestMs / 1000}s", stringResource(R.string.games_stat_best_label), Modifier.weight(1f))
    }
    GlassButton(stringResource(R.string.games_pz_new), onClick = viewModel::newPuzzle, modifier = Modifier.fillMaxWidth())
    GlassButton(stringResource(R.string.games_pz_change_photo), onClick = viewModel::changePhoto, style = GlassButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
}
