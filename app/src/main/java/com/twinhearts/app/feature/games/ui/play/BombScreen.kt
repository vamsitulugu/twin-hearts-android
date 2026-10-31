package com.twinhearts.app.feature.games.ui.play

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.engine.BombDifficulty
import com.twinhearts.app.feature.games.domain.engine.BombEngine
import com.twinhearts.app.feature.games.domain.engine.BombPhase
import com.twinhearts.app.feature.games.domain.engine.BombResult
import com.twinhearts.app.feature.games.domain.engine.BombState
import com.twinhearts.app.feature.games.domain.model.Players
import com.twinhearts.app.feature.games.ui.components.CoopChat
import com.twinhearts.app.feature.games.ui.components.CountdownNumber
import com.twinhearts.app.feature.games.ui.components.GameHost
import com.twinhearts.app.feature.games.ui.components.GameTimerBar
import com.twinhearts.app.feature.games.ui.components.HeroTitle
import com.twinhearts.app.feature.games.ui.components.InfoChip
import com.twinhearts.app.feature.games.ui.components.SelectCard
import com.twinhearts.app.feature.games.ui.components.StatTile
import com.twinhearts.app.feature.games.ui.components.rememberNow
import kotlinx.coroutines.delay

@Composable
fun BombScreen(onExit: () -> Unit, viewModel: BombViewModel = hiltViewModel()) {
    GameHost(
        viewModel = viewModel,
        title = stringResource(R.string.games_bomb_title),
        onExit = onExit,
        alongside = { viewModel.runClock() },
    ) { state, players ->
        when (state.phase) {
            BombPhase.Lobby -> Lobby(state, viewModel)
            BombPhase.Countdown -> Countdown(state, players)
            BombPhase.Playing -> Playing(state, players, viewModel)
            BombPhase.Result -> BombOutcome(state, viewModel)
        }
    }
}

private fun difficultyName(d: BombDifficulty): Int = when (d) {
    BombDifficulty.Easy -> R.string.games_bm_diff_easy
    BombDifficulty.Medium -> R.string.games_bm_diff_medium
    BombDifficulty.Hard -> R.string.games_bm_diff_hard
    BombDifficulty.Extreme -> R.string.games_bm_diff_extreme
}

private fun bestText(ms: Long): String = if (ms == 0L) "—" else "${Math.round(ms / 1000f)}s"

// ── lobby ──

@Composable
private fun Lobby(state: BombState, viewModel: BombViewModel) {
    val colors = TwinTheme.colors
    var selectedName by rememberSaveable { mutableStateOf(BombDifficulty.Easy.name) }
    var hint by rememberSaveable { mutableStateOf(false) }
    var fog by rememberSaveable { mutableStateOf(false) }
    val difficulty = BombDifficulty.valueOf(selectedName)

    HeroTitle(stringResource(R.string.games_bm_title))
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
        StatTile("${state.teamStreak}", stringResource(R.string.games_bm_stat_streak), Modifier.weight(1f))
        StatTile(bestText(state.bestTimeMs), stringResource(R.string.games_bm_stat_fastest), Modifier.weight(1f))
        StatTile("${state.round}", stringResource(R.string.games_bm_stat_round), Modifier.weight(1f))
    }
    BombDifficulty.entries.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
            pair.forEach { d ->
                SelectCard(selected = difficulty == d, onClick = { selectedName = d.name }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(difficultyName(d)), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                    Text(
                        stringResource(if (d.moving) R.string.games_bm_diff_meta_moving else R.string.games_bm_diff_meta, d.size, d.seconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
        SelectCard(selected = hint, onClick = { hint = !hint }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.games_bm_hint_mode), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        }
        SelectCard(selected = fog, onClick = { fog = !fog }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.games_bm_fog_mode), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        }
    }
    Text(stringResource(R.string.games_bm_explain), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    GlassButton(stringResource(R.string.games_bm_start), onClick = { viewModel.start(difficulty, hint, fog) }, modifier = Modifier.fillMaxWidth())
}

// ── countdown ──

@Composable
private fun Countdown(state: BombState, players: Players) {
    val now = rememberNow(120L)
    val left = Math.ceil((state.countdownAt - now) / 1000.0).toInt()
    RoleBanner(state.guide == players.side, countdown = true, partnerName = players.partnerName)
    CountdownNumber(left)
}

@Composable
private fun RoleBanner(guide: Boolean, countdown: Boolean, partnerName: String) {
    val colors = TwinTheme.colors
    val text = when {
        countdown && guide -> stringResource(R.string.games_bm_you_guide)
        countdown -> stringResource(R.string.games_bm_you_mover)
        guide -> stringResource(R.string.games_bm_guide_banner, partnerName)
        else -> stringResource(R.string.games_bm_mover_banner)
    }
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = colors.ink,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(TwinTheme.shapes.pill)
            .background(if (guide) colors.glass else colors.primaryBrush)
            .border(1.dp, if (guide) colors.lineSoft else colors.accent, TwinTheme.shapes.pill)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
    )
}

// ── playing ──

@Composable
private fun Playing(state: BombState, players: Players, viewModel: BombViewModel) {
    val colors = TwinTheme.colors
    val guide = state.guide == players.side
    val now = rememberNow(250L)
    val left = BombEngine.secondsLeft(state, now)

    RoleBanner(guide, countdown = false, partnerName = players.partnerName)
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        InfoChip(stringResource(R.string.games_bm_time, left), highlight = left < 15)
        InfoChip(stringResource(R.string.games_bm_moves, state.moves))
        InfoChip(stringResource(difficultyName(state.difficulty)))
    }
    GameTimerBar(left / state.timeLimit.toFloat(), low = left < 15)
    Field(state, guide, viewModel)
    if (!guide && state.fogMode) {
        Text(stringResource(R.string.games_bm_fog_note), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    CoopChat(
        emojis = if (guide) BombEngine.emojis else BombEngine.moverEmojis,
        log = state.chat,
        players = players,
        placeholder = stringResource(R.string.games_bm_chat_hint),
        onSend = viewModel::send,
    )
}

@Composable
private fun Field(state: BombState, guide: Boolean, viewModel: BombViewModel) {
    val size = state.size
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
        (0 until size).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                (0 until size).forEach { col ->
                    val idx = row * size + col
                    Tile(
                        state = state,
                        idx = idx,
                        guide = guide,
                        onClick = { if (guide) viewModel.ping(idx) else viewModel.move(idx) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Tile(state: BombState, idx: Int, guide: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    val isBomb = idx in state.bombs
    val isHere = idx == state.pos
    val isGoal = idx == state.goal
    val isStart = idx == state.start
    val exploded = !guide && state.explodedAt == idx
    val fogged = !guide && BombEngine.isFogged(state, idx)
    val danger = !guide && !fogged && BombEngine.isDanger(state, idx)
    val reachable = !guide && BombEngine.adjacent(state.size, state.pos, idx)

    var pinged by remember { mutableStateOf(false) }
    val ping = state.ping
    LaunchedEffect(ping?.nonce) {
        if (ping != null && ping.idx == idx && System.currentTimeMillis() - ping.nonce < PING_VISIBLE_MS * 2) {
            pinged = true
            delay(PING_VISIBLE_MS)
            pinged = false
        }
    }

    val fill = when {
        exploded -> colors.error.copy(alpha = 0.6f)
        isGoal -> colors.success.copy(alpha = 0.35f)
        isStart -> colors.primaryStart.copy(alpha = 0.45f)
        guide && isBomb -> colors.error.copy(alpha = 0.3f)
        danger -> Color(0xFFF59E0B).copy(alpha = 0.3f)
        else -> colors.glass
    }
    val label = when {
        isHere -> stringResource(R.string.games_bm_tile_you)
        isGoal -> stringResource(R.string.games_bm_tile_goal)
        isStart -> stringResource(R.string.games_bm_tile_start)
        guide && isBomb -> stringResource(R.string.games_bm_tile_bomb)
        else -> stringResource(R.string.games_bm_tile, idx + 1)
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(fill)
            .border(1.dp, if (reachable) colors.accent else colors.lineSoft, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        val fontSize = if (state.size > 10) 12.sp else 16.sp
        when {
            exploded -> Text("💥", fontSize = fontSize)
            isHere -> Text("🧍", fontSize = fontSize)
            isGoal -> Text("🏁", fontSize = fontSize)
            guide && isBomb -> Text("💣", fontSize = fontSize)
            isStart -> Text("🏠", fontSize = fontSize)
        }
        if (pinged) Text("📍", fontSize = fontSize)
        if (fogged) Box(modifier = Modifier.matchParentSize().background(colors.background.copy(alpha = 0.85f)))
    }
}

private const val PING_VISIBLE_MS = 1_600L

// ── result ──

@Composable
private fun BombOutcome(state: BombState, viewModel: BombViewModel) {
    val colors = TwinTheme.colors
    val won = state.result == BombResult.Won
    val icon = when (state.result) {
        BombResult.Won -> "🎉"
        BombResult.Exploded -> "💥"
        else -> "⏱️"
    }
    val title = when (state.result) {
        BombResult.Won -> R.string.games_bm_won
        BombResult.Exploded -> R.string.games_bm_boom
        else -> R.string.games_bm_timeout
    }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        Text(icon, style = MaterialTheme.typography.displayMedium)
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, color = colors.ink, textAlign = TextAlign.Center)
        Text(
            text = when (state.result) {
                BombResult.Won -> stringResource(R.string.games_bm_won_sub, "%.1f".format(state.finishMs / 1000f), state.moves)
                BombResult.Exploded -> stringResource(R.string.games_bm_boom_sub)
                else -> stringResource(R.string.games_bm_timeout_sub)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
        StatTile("${state.teamStreak}", stringResource(R.string.games_stat_streak_label), Modifier.weight(1f))
        StatTile(bestText(state.bestTimeMs), stringResource(R.string.games_stat_best_label), Modifier.weight(1f))
    }
    GlassButton(stringResource(R.string.games_bm_next), onClick = viewModel::nextRound, modifier = Modifier.fillMaxWidth())
    GlassButton(stringResource(R.string.games_bm_change), onClick = viewModel::changeDifficulty, style = GlassButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
}
