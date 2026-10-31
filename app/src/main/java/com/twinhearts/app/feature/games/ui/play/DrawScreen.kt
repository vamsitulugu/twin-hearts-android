package com.twinhearts.app.feature.games.ui.play

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.engine.DrawEngine
import com.twinhearts.app.feature.games.domain.engine.DrawState
import com.twinhearts.app.feature.games.domain.engine.DrawStroke
import com.twinhearts.app.feature.games.domain.engine.DrawTool
import com.twinhearts.app.feature.games.domain.model.Players
import com.twinhearts.app.feature.games.ui.components.EmojiButton
import com.twinhearts.app.feature.games.ui.components.GameHost
import com.twinhearts.app.feature.games.ui.components.GameInput
import com.twinhearts.app.feature.games.ui.components.GameTimerBar
import com.twinhearts.app.feature.games.ui.components.InfoChip
import com.twinhearts.app.feature.games.ui.components.ScoreCard
import com.twinhearts.app.feature.games.ui.components.rememberNow
import kotlinx.coroutines.delay

private val reactionEmojis = listOf("😂", "😍", "🤔", "🔥", "👏")

@Composable
fun DrawScreen(onExit: () -> Unit, viewModel: DrawViewModel = hiltViewModel()) {
    GameHost(
        viewModel = viewModel,
        title = stringResource(R.string.games_draw_title),
        onExit = onExit,
        alongside = { viewModel.runClock() },
    ) { state, players ->
        DrawContent(state, players, viewModel)
    }
}

@Composable
private fun DrawContent(state: DrawState, players: Players, viewModel: DrawViewModel) {
    val me = players.side
    val drawer = state.drawer == me
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m), modifier = Modifier.fillMaxWidth()) {
        ScoreCard(players.myName, state.scoreOf(me), Modifier.weight(1f), active = drawer)
        ScoreCard(players.partnerName, state.scoreOf(me.other), Modifier.weight(1f), active = !drawer)
    }
    if (!state.wordSet) {
        if (drawer) WordPicker(viewModel) else WaitingForWord(players.partnerName)
    } else {
        Round(state, players, drawer, viewModel)
    }
}

// ── picking the word ──

@Composable
private fun WordPicker(viewModel: DrawViewModel) {
    val colors = TwinTheme.colors
    val context = LocalContext.current
    var word by rememberSaveable { mutableStateOf("") }
    val typeFirst = stringResource(R.string.games_dw_type_first)
    val noVoice = stringResource(R.string.games_tod_no_voice)
    val voice = rememberVoiceInput(onUnavailable = { Toast.makeText(context, noVoice, Toast.LENGTH_SHORT).show() }) { word = it }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.games_dw_what), style = MaterialTheme.typography.titleMedium, color = colors.ink)
        TwinTextField(
            value = word,
            onValueChange = { word = it },
            label = stringResource(R.string.games_dw_placeholder),
            maxLength = 40,
            modifier = Modifier.padding(top = TwinTheme.spacing.s),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = TwinTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            DrawEngine.suggestions.forEach { suggestion ->
                Text(
                    suggestion,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.ink,
                    modifier = Modifier
                        .clip(TwinTheme.shapes.pill)
                        .background(colors.glass)
                        .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
                        .clickable { word = suggestion }
                        .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
            GlassButton(stringResource(R.string.games_tod_voice), onClick = voice, style = GlassButtonStyle.Secondary, modifier = Modifier.weight(1f))
            GlassButton(
                stringResource(R.string.games_dw_start),
                onClick = {
                    if (!viewModel.setWord(word)) Toast.makeText(context, typeFirst, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1.4f),
            )
        }
    }
}

@Composable
private fun WaitingForWord(partnerName: String) {
    val colors = TwinTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = TwinTheme.spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✍️", fontSize = 40.sp)
        Text(stringResource(R.string.games_dw_picking, partnerName), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
    }
}

// ── a round ──

@Composable
private fun Round(state: DrawState, players: Players, drawer: Boolean, viewModel: DrawViewModel) {
    val colors = TwinTheme.colors
    val now = rememberNow(500L)
    val left = DrawEngine.secondsLeft(state, now)
    val elapsed = DrawEngine.ROUND_SECONDS - left
    var tool by rememberSaveable { mutableStateOf(DrawTool.Pen) }
    var color by rememberSaveable { mutableStateOf(DrawEngine.colors.first()) }
    var size by rememberSaveable { mutableStateOf(DrawEngine.sizes[1]) }

    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
        InfoChip(stringResource(R.string.games_dw_time, left), highlight = left < 15)
        Text(
            text = if (drawer) stringResource(R.string.games_dw_draw_banner, state.word)
            else stringResource(R.string.games_dw_partner_drawing, players.partnerName),
            style = MaterialTheme.typography.titleSmall,
            color = colors.ink,
        )
    }
    GameTimerBar(left / DrawEngine.ROUND_SECONDS.toFloat(), low = left < 15)
    if (!drawer) {
        Text(
            DrawEngine.hint(state.word, elapsed),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (drawer) Toolbar(tool, color, size, onTool = { tool = it }, onColor = { color = it }, onSize = { size = it }, viewModel = viewModel)

    Board(state, drawer, tool, color, size, onStroke = viewModel::addStroke)

    state.guesses.takeLast(6).forEach { guess ->
        val name = if (guess.by == players.side) players.myName else players.partnerName
        Text(
            stringResource(R.string.games_dw_guess_row, name, guess.text),
            style = MaterialTheme.typography.bodySmall,
            color = if (guess.correct) colors.success else colors.inkMuted,
        )
    }
    ReactionRow(state, players, viewModel)
    if (!drawer) GuessRow(viewModel)
}

@Composable
private fun Toolbar(
    tool: DrawTool,
    color: Int,
    size: Int,
    onTool: (DrawTool) -> Unit,
    onColor: (Int) -> Unit,
    onSize: (Int) -> Unit,
    viewModel: DrawViewModel,
) {
    val colors = TwinTheme.colors
    val colorLabel = stringResource(R.string.games_dw_color)
    val sizeLabel = stringResource(R.string.games_dw_size)
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
            DrawEngine.colors.forEach { swatch ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(TwinTheme.shapes.circle)
                        .background(Color(swatch))
                        .border(if (swatch == color) 3.dp else 1.dp, if (swatch == color) colors.ink else colors.lineSoft, TwinTheme.shapes.circle)
                        .clickable { onColor(swatch) }
                        .semantics { contentDescription = colorLabel },
                )
            }
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
            ToolButton("✏️", R.string.games_dw_tool_pen, tool == DrawTool.Pen) { onTool(DrawTool.Pen) }
            ToolButton("🖊️", R.string.games_dw_tool_highlighter, tool == DrawTool.Highlighter) { onTool(DrawTool.Highlighter) }
            ToolButton("🧽", R.string.games_dw_tool_eraser, tool == DrawTool.Eraser) { onTool(DrawTool.Eraser) }
            ToolButton("🪣", R.string.games_dw_tool_fill, tool == DrawTool.Fill) { onTool(DrawTool.Fill) }
            DrawEngine.sizes.forEach { s ->
                Box(
                    modifier = Modifier
                        .size(TwinTheme.shapes.minTouch)
                        .clip(TwinTheme.shapes.small)
                        .background(if (s == size) colors.primaryStart.copy(alpha = 0.55f) else colors.glass)
                        .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
                        .clickable { onSize(s) }
                        .semantics { contentDescription = sizeLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.size((s * 1.6f).coerceAtLeast(6f).dp).clip(TwinTheme.shapes.circle).background(colors.ink))
                }
            }
            ToolButton("↩️", R.string.games_dw_tool_undo, false, viewModel::undo)
            ToolButton("↪️", R.string.games_dw_tool_redo, false, viewModel::redo)
            ToolButton("🗑️", R.string.games_dw_tool_clear, false, viewModel::clear)
            ToolButton("🔀", R.string.games_dw_tool_new_word, false, viewModel::newWord)
        }
    }
}

@Composable
private fun ToolButton(emoji: String, description: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val label = stringResource(description)
    Box(
        modifier = Modifier
            .size(TwinTheme.shapes.minTouch)
            .clip(TwinTheme.shapes.small)
            .background(if (selected) colors.primaryStart.copy(alpha = 0.55f) else colors.glass)
            .border(1.dp, if (selected) colors.accent else colors.lineSoft, TwinTheme.shapes.small)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 20.sp)
    }
}

// ── the drawing board ──

@Composable
private fun Board(
    state: DrawState,
    drawer: Boolean,
    tool: DrawTool,
    color: Int,
    brush: Int,
    onStroke: (DrawStroke) -> Unit,
) {
    val colors = TwinTheme.colors
    val boardDescription = stringResource(R.string.games_dw_canvas_desc)
    var pixels by remember { mutableStateOf(IntSize.Zero) }
    val live = remember { mutableStateListOf<Float>() }
    val saved = remember(state.strokes, pixels) {
        if (pixels.width > 0 && pixels.height > 0) DrawRaster.render(state.strokes, pixels.width, pixels.height).asImageBitmap() else null
    }
    fun finish(points: List<Float>) {
        if (points.size >= 2) onStroke(DrawStroke(System.nanoTime(), tool, color, brush, points.toList()))
    }

    var drawModifier: Modifier = Modifier
    if (drawer) {
        drawModifier = Modifier
            .pointerInput(tool, color, brush, pixels) {
                detectTapGestures { tap ->
                    if (pixels.width == 0) return@detectTapGestures
                    val x = tap.x / pixels.width
                    val y = tap.y / pixels.height
                    // A tap is a dot with the pen, or a bucket fill.
                    finish(if (tool == DrawTool.Fill) listOf(x, y) else listOf(x, y, x, y))
                }
            }
            .pointerInput(tool, color, brush, pixels) {
                detectDragGestures(
                    onDragStart = { start ->
                        live.clear()
                        if (tool != DrawTool.Fill && pixels.width > 0) {
                            live.add(start.x / pixels.width)
                            live.add(start.y / pixels.height)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (tool != DrawTool.Fill && pixels.width > 0 && live.size >= 2) {
                            val x = change.position.x / pixels.width
                            val y = change.position.y / pixels.height
                            val dx = x - live[live.size - 2]
                            val dy = y - live[live.size - 1]
                            if (dx * dx + dy * dy > MIN_STEP * MIN_STEP) {
                                live.add(x)
                                live.add(y)
                            }
                        }
                    },
                    onDragEnd = {
                        finish(live.toList())
                        live.clear()
                    },
                    onDragCancel = { live.clear() },
                )
            }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(TwinTheme.shapes.card)
            .background(colors.glass)
            .border(1.dp, colors.line2, TwinTheme.shapes.card)
            .onSizeChanged { pixels = it }
            .semantics { contentDescription = boardDescription },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .then(drawModifier),
        ) {
            saved?.let { drawImage(it) }
            if (live.size >= 2) {
                val w = this.size.width
                val h = this.size.height
                val path = Path().apply {
                    moveTo(live[0] * w, live[1] * h)
                    var i = 2
                    while (i + 1 < live.size) {
                        lineTo(live[i] * w, live[i + 1] * h)
                        i += 2
                    }
                    if (live.size == 2) lineTo(live[0] * w + 0.1f, live[1] * h)
                }
                val width = DrawRaster.widthOf(brush, tool, this.size.width / 360f)
                drawPath(
                    path = path,
                    color = if (tool == DrawTool.Eraser) Color.Black else Color(color).copy(alpha = if (tool == DrawTool.Highlighter) 0.35f else 1f),
                    style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = if (tool == DrawTool.Eraser) BlendMode.Clear else BlendMode.SrcOver,
                )
            }
        }
    }
}

private const val MIN_STEP = 0.004f

// ── reactions and guessing ──

@Composable
private fun ReactionRow(state: DrawState, players: Players, viewModel: DrawViewModel) {
    val colors = TwinTheme.colors
    var lastNonce by remember { mutableStateOf(state.reaction?.nonce) }
    var shown by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.reaction?.nonce) {
        val reaction = state.reaction
        if (reaction != null && reaction.nonce != lastNonce) {
            lastNonce = reaction.nonce
            val name = if (reaction.by == players.side) players.myName else players.partnerName
            shown = name + " " + reaction.emoji
            delay(2_200)
            shown = null
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
        reactionEmojis.forEach { emoji -> EmojiButton(emoji, onClick = { viewModel.react(emoji) }) }
        shown?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = colors.ink) }
    }
}

@Composable
private fun GuessRow(viewModel: DrawViewModel) {
    var text by rememberSaveable { mutableStateOf("") }
    val checking by viewModel.checking.collectAsState()
    GameInput(
        value = text,
        onValueChange = { text = it },
        placeholder = stringResource(if (checking) R.string.games_dw_checking else R.string.games_dw_guess_hint),
        enabled = !checking,
        onSend = {
            if (text.isNotBlank()) {
                viewModel.guess(text)
                text = ""
            }
        },
        sendLabel = stringResource(R.string.games_dw_guess),
    )
}
