package com.twinhearts.app.feature.games.ui.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.engine.LudoEngine
import com.twinhearts.app.feature.games.domain.engine.LudoEvent
import com.twinhearts.app.feature.games.domain.engine.LudoEventKind
import com.twinhearts.app.feature.games.domain.engine.LudoState
import com.twinhearts.app.feature.games.domain.model.Outcome
import com.twinhearts.app.feature.games.domain.model.Players
import com.twinhearts.app.feature.games.domain.model.Side
import com.twinhearts.app.feature.games.ui.components.GameHost
import com.twinhearts.app.feature.games.ui.components.TurnBar
import kotlin.math.abs

private const val GRID = 15

@Composable
fun LudoScreen(onExit: () -> Unit, viewModel: LudoViewModel = hiltViewModel()) {
    val local by viewModel.local.collectAsStateWithLifecycle()
    GameHost(
        viewModel = viewModel,
        title = stringResource(R.string.games_ludo_title),
        onExit = onExit,
        alongside = { viewModel.runClock() },
    ) { state, players ->
        LudoContent(
            state = state,
            players = players,
            rolling = local.rolling,
            secondsLeft = local.secondsLeft,
            onRoll = viewModel::roll,
            onToken = viewModel::moveToken,
            onRematch = viewModel::rematch,
        )
    }
}

@Composable
private fun ColumnScope.LudoContent(
    state: LudoState,
    players: Players,
    rolling: Boolean,
    secondsLeft: Int,
    onRoll: () -> Unit,
    onToken: (Int) -> Unit,
    onRematch: () -> Unit,
) {
    val colors = TwinTheme.colors
    val me = players.side
    val playing = state.outcome == Outcome.None
    val myTurn = playing && state.turn == me
    val movable = if (myTurn && state.dice != 0 && !state.noMove) LudoEngine.movable(state.tokens(me), state.dice) else emptyList()
    val canRoll = myTurn && state.dice == 0 && !rolling

    val barText = when {
        state.outcome.isWonBy(me) -> stringResource(R.string.games_ludo_you_won)
        !playing -> stringResource(R.string.games_ludo_partner_won, players.partnerName)
        myTurn -> stringResource(R.string.games_ludo_your_turn)
        else -> stringResource(R.string.games_turn_theirs, players.partnerName)
    }
    TurnBar(text = barText, mine = myTurn)

    if (playing) {
        val fraction = (secondsLeft.toFloat() / LudoEngine.TURN_SECONDS).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(TwinTheme.shapes.pill)
                .background(colors.lineSoft),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(TwinTheme.shapes.pill)
                    .background(if (secondsLeft <= 8) colors.error else colors.accent),
            )
        }
    }

    LudoBoard(state = state, me = me, movable = movable, onToken = onToken)

    Dice(value = state.dice, rolling = rolling, active = canRoll, onClick = onRoll, modifier = Modifier.align(Alignment.CenterHorizontally))

    val redName = if (me == Side.One) players.myName else players.partnerName
    val greenName = if (me == Side.Two) players.myName else players.partnerName
    Text(
        stringResource(R.string.games_ludo_legend, redName, greenName),
        style = MaterialTheme.typography.labelMedium,
        color = colors.inkMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    if (state.history.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TwinTheme.shapes.small)
                .background(colors.glass)
                .padding(TwinTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
        ) {
            state.history.forEach { event ->
                Text(historyText(event, players), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
        }
    }

    if (!playing) {
        GlassButton(text = stringResource(R.string.games_rematch), onClick = onRematch, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun historyText(event: LudoEvent, players: Players): String {
    val mine = event.side == players.side
    val name = if (mine) players.myName else players.partnerName
    return when (event.kind) {
        LudoEventKind.Moved ->
            if (mine) stringResource(R.string.games_ludo_hist_moved, event.token + 1, event.dice)
            else stringResource(R.string.games_ludo_hist_moved_other, name, event.token + 1, event.dice)
        LudoEventKind.Captured -> stringResource(R.string.games_ludo_hist_captured, event.token + 1)
        LudoEventKind.NoMove -> stringResource(R.string.games_ludo_hist_nomove, name, event.dice)
        LudoEventKind.TimedOut -> stringResource(R.string.games_ludo_hist_timeout, name)
    }
}

@Composable
private fun Dice(value: Int, rolling: Boolean, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(shape)
            .background(colors.glass)
            .border(if (active) 3.dp else 1.dp, if (active) colors.accent else colors.lineSoft, shape)
            .clickable(enabled = active, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (rolling || value == 0) "🎲" else "$value",
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.ink,
        )
    }
}

@Composable
private fun LudoBoard(state: LudoState, me: Side, movable: List<Int>, onToken: (Int) -> Unit) {
    val colors = TwinTheme.colors
    val measurer = rememberTextMeasurer()
    val sideColor = mapOf(Side.One to colors.error, Side.Two to colors.success)
    val ringFill = colors.glass
    val lineColor = colors.lineSoft
    val dim = colors.neutralGlass.copy(alpha = 0.25f)
    val ink = colors.ink
    val accent = colors.accent

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(TwinTheme.shapes.card)
            .background(colors.card)
            .pointerInput(state, movable) {
                detectTapGestures { tap ->
                    if (movable.isEmpty()) return@detectTapGestures
                    val cell = size.width / GRID.toFloat()
                    val mine = state.tokens(me)
                    val hit = movable.minByOrNull { token ->
                        val (row, col) = LudoEngine.cellOf(me, token, mine[token])
                        abs(tap.x - (col + 0.5f) * cell) + abs(tap.y - (row + 0.5f) * cell)
                    } ?: return@detectTapGestures
                    val (row, col) = LudoEngine.cellOf(me, hit, mine[hit])
                    val distance = abs(tap.x - (col + 0.5f) * cell) + abs(tap.y - (row + 0.5f) * cell)
                    if (distance <= cell * 1.4f) onToken(hit)
                }
            },
    ) {
        val cell = size.width / GRID
        fun rect(row: Int, col: Int, color: Color, spanRows: Int = 1, spanCols: Int = 1, radius: Float = 0.18f) =
            drawRoundRect(
                color = color,
                topLeft = Offset(col * cell + 1f, row * cell + 1f),
                size = Size(spanCols * cell - 2f, spanRows * cell - 2f),
                cornerRadius = CornerRadius(cell * radius),
            )

        // Yards: mine and my partner's in colour, the two unused ones dim.
        LudoEngine.baseOrigin.forEach { (side, origin) ->
            rect(origin.first, origin.second, sideColor.getValue(side).copy(alpha = 0.28f), 6, 6, 0.4f)
        }
        LudoEngine.decorativeYards.forEach { rect(it.first, it.second, dim, 6, 6, 0.4f) }
        LudoEngine.decorativeLanes.flatten().forEach { rect(it.first, it.second, dim) }

        LudoEngine.ringCells.forEach { rect(it.first, it.second, ringFill) }
        LudoEngine.ringCells.forEachIndexed { index, (row, col) ->
            if (LudoEngine.isSafeRingCell(index)) {
                drawCircle(accent.copy(alpha = 0.7f), radius = cell * 0.16f, center = Offset((col + 0.5f) * cell, (row + 0.5f) * cell))
            }
        }
        LudoEngine.homeLane.forEach { (side, lane) ->
            lane.forEach { rect(it.first, it.second, sideColor.getValue(side).copy(alpha = 0.55f)) }
        }
        rect(LudoEngine.center.first, LudoEngine.center.second, accent.copy(alpha = 0.6f))

        // Tokens
        val used = HashMap<Pair<Int, Int>, Int>()
        listOf(Side.One, Side.Two).forEach { side ->
            state.tokens(side).forEachIndexed { token, position ->
                val spot = LudoEngine.cellOf(side, token, position)
                val stack = used.merge(spot, 1, Int::plus)!! - 1
                val center = Offset((spot.second + 0.5f) * cell + stack * cell * 0.14f, (spot.first + 0.5f) * cell - stack * cell * 0.14f)
                drawToken(center, cell, sideColor.getValue(side), ink, accent, highlight = side == me && token in movable)
                val label = measurer.measure("${token + 1}", TextStyle(color = Color.White, fontSize = (cell * 0.34f / density).sp, fontWeight = FontWeight.ExtraBold))
                drawText(label, topLeft = Offset(center.x - label.size.width / 2f, center.y - label.size.height / 2f))
            }
        }
    }
}

private fun DrawScope.drawToken(center: Offset, cell: Float, color: Color, border: Color, accent: Color, highlight: Boolean) {
    if (highlight) drawCircle(accent, radius = cell * 0.46f, center = center, style = Stroke(width = cell * 0.09f))
    drawCircle(color, radius = cell * 0.34f, center = center)
    drawCircle(border.copy(alpha = 0.85f), radius = cell * 0.34f, center = center, style = Stroke(width = cell * 0.05f))
}
