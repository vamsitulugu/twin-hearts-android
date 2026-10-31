package com.twinhearts.app.feature.games.ui.play

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.engine.MindReaderEngine
import com.twinhearts.app.feature.games.domain.engine.MindReaderState
import com.twinhearts.app.feature.games.domain.engine.MrMatch
import com.twinhearts.app.feature.games.domain.engine.MrMode
import com.twinhearts.app.feature.games.domain.engine.MrPhase
import com.twinhearts.app.feature.games.domain.model.Players
import com.twinhearts.app.feature.games.domain.model.Side
import com.twinhearts.app.feature.games.ui.components.GameHost
import com.twinhearts.app.feature.games.ui.components.GameTimerBar
import com.twinhearts.app.feature.games.ui.components.HeroTitle
import com.twinhearts.app.feature.games.ui.components.InfoChip
import com.twinhearts.app.feature.games.ui.components.MindReaderCatalog
import com.twinhearts.app.feature.games.ui.components.SelectCard
import com.twinhearts.app.feature.games.ui.components.StatTile
import com.twinhearts.app.feature.games.ui.components.CountdownNumber
import com.twinhearts.app.feature.games.ui.components.rememberNow
import kotlinx.coroutines.delay

@Composable
fun MindReaderScreen(onExit: () -> Unit, viewModel: MindReaderViewModel = hiltViewModel()) {
    GameHost(
        viewModel = viewModel,
        title = stringResource(R.string.games_mindreader_title),
        onExit = onExit,
        alongside = { viewModel.runClock() },
    ) { state, players ->
        when (state.phase) {
            MrPhase.Lobby -> Lobby(state, players, viewModel)
            MrPhase.Countdown -> Countdown(state)
            MrPhase.Answering -> Answering(state, players, viewModel)
            MrPhase.Reveal -> Reveal(state, players, viewModel)
            MrPhase.Summary -> Summary(state, viewModel)
        }
    }
}

// ── lobby ──

@Composable
private fun Lobby(state: MindReaderState, players: Players, viewModel: MindReaderViewModel) {
    val colors = TwinTheme.colors
    var modeName by rememberSaveable { mutableStateOf(MrMode.Random.name) }
    var category by rememberSaveable { mutableStateOf(MindReaderEngine.RANDOM) }
    val mode = MrMode.valueOf(modeName)

    HeroTitle(stringResource(R.string.games_mr_title))
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CompatRing(state.compatPct, 120.dp)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
        StatTile("${state.totalRounds}", stringResource(R.string.games_mr_stat_rounds), Modifier.weight(1f))
        StatTile("${state.bestStreak}", stringResource(R.string.games_mr_stat_streak), Modifier.weight(1f))
        StatTile("${state.achievements.size}", stringResource(R.string.games_mr_stat_ach), Modifier.weight(1f))
    }

    val modes = listOf(
        Triple(MrMode.Random, "🎲" to R.string.games_mr_mode_random, R.string.games_mr_mode_random_sub),
        Triple(MrMode.Category, "🗂️" to R.string.games_mr_mode_category, R.string.games_mr_mode_category_sub),
        Triple(MrMode.BestOfTen, "🏁" to R.string.games_mr_mode_best10, R.string.games_mr_mode_best10_sub),
        Triple(MrMode.Endless, "♾️" to R.string.games_mr_mode_endless, R.string.games_mr_mode_endless_sub),
    )
    modes.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
            pair.forEach { (m, name, sub) ->
                SelectCard(selected = mode == m, onClick = { modeName = m.name }, modifier = Modifier.weight(1f)) {
                    Text(name.first, style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(name.second), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                    Text(stringResource(sub), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                }
            }
        }
    }

    if (mode == MrMode.Category) {
        MindReaderEngine.categories.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { cat ->
                    SelectCard(selected = category == cat, onClick = { category = cat }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(MindReaderCatalog.categoryLabel(cat)), style = MaterialTheme.typography.labelMedium, color = colors.ink, textAlign = TextAlign.Center)
                    }
                }
                if (pair.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }

    GlassButton(
        text = stringResource(R.string.games_mr_start),
        onClick = { viewModel.start(mode, if (mode == MrMode.Category) category else MindReaderEngine.RANDOM) },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
        GlassButton(stringResource(R.string.games_mr_daily), onClick = viewModel::startDaily, style = GlassButtonStyle.Secondary, modifier = Modifier.weight(1f))
        GlassButton(stringResource(R.string.games_mr_weekly), onClick = viewModel::startWeekly, style = GlassButtonStyle.Secondary, modifier = Modifier.weight(1f))
    }

    Achievements(state)

    if (state.history.isNotEmpty()) {
        Text(stringResource(R.string.games_mr_recent), style = MaterialTheme.typography.titleMedium, color = colors.ink)
        state.history.take(8).forEach { item ->
            val q = MindReaderCatalog.of(item.q)
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
                val prefix = if (item.fav) "★ " else ""
                Text(prefix + (q?.let { stringResource(it.text) } ?: ""), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                val mine = if (players.side == Side.One) item.answerOne else item.answerTwo
                val theirs = if (players.side == Side.One) item.answerTwo else item.answerOne
                Text(stringResource(R.string.games_mr_name_answer, players.myName, mine), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                Text(stringResource(R.string.games_mr_name_answer, players.partnerName, theirs), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
        }
    }
}

@Composable
private fun Achievements(state: MindReaderState) {
    if (state.achievements.isEmpty()) return
    state.achievements.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            pair.forEach { id ->
                InfoChip("🏅 " + stringResource(MindReaderCatalog.achievementLabel(id)), highlight = true)
            }
        }
    }
}

// ── countdown ──

@Composable
private fun Countdown(state: MindReaderState) {
    val now = rememberNow(120L)
    val left = Math.ceil((state.countdownAt - now) / 1000.0).toInt()
    Text(stringResource(R.string.games_mr_ready), style = MaterialTheme.typography.bodyMedium, color = TwinTheme.colors.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    CountdownNumber(left)
}

// ── answering ──

@Composable
private fun Answering(state: MindReaderState, players: Players, viewModel: MindReaderViewModel) {
    val colors = TwinTheme.colors
    val question = MindReaderCatalog.of(state.question)
    val me = players.side
    val myLocked = state.lockedOf(me)
    val partnerLocked = state.lockedOf(me.other)
    var draft by rememberSaveable(state.roundIndex) { mutableStateOf("") }
    var picked by rememberSaveable(state.roundIndex) { mutableStateOf<String?>(null) }

    val now = rememberNow(400L)
    val left = MindReaderEngine.secondsLeft(state, now)
    // Time is up: lock whatever is typed or picked.
    LaunchedEffect(left, myLocked, state.roundIndex) {
        if (left == 0 && !myLocked) viewModel.lock(picked ?: draft.ifBlank { MindReaderEngine.NO_ANSWER })
    }

    RoundBar(state)
    GameTimerBar(left / MindReaderEngine.ANSWER_SECONDS.toFloat(), low = left < 12)
    QuestionCard(state)

    if (myLocked) {
        Text(stringResource(R.string.games_mr_waiting, players.partnerName), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    } else if (question?.isChoice == true) {
        stringArrayResource(question.options).forEach { option ->
            SelectCard(selected = picked == option, onClick = { picked = option }, modifier = Modifier.fillMaxWidth()) {
                Text(option, style = MaterialTheme.typography.titleSmall, color = colors.ink)
            }
        }
        GlassButton(stringResource(R.string.games_mr_lock), onClick = { picked?.let { viewModel.lock(it) } }, enabled = picked != null, modifier = Modifier.fillMaxWidth())
    } else {
        TwinTextField(
            value = draft,
            onValueChange = { draft = it },
            label = stringResource(R.string.games_mr_placeholder),
            maxLength = 200,
            modifier = Modifier.fillMaxWidth(),
        )
        GlassButton(stringResource(R.string.games_mr_lock), onClick = { viewModel.lock(draft) }, enabled = draft.isNotBlank(), modifier = Modifier.fillMaxWidth())
    }

    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
        LockPill(
            text = if (myLocked) stringResource(R.string.games_mr_you_locked) else stringResource(R.string.games_mr_answering, players.myName),
            locked = myLocked,
            modifier = Modifier.weight(1f),
        )
        LockPill(
            text = if (partnerLocked) stringResource(R.string.games_mr_partner_locked, players.partnerName) else stringResource(R.string.games_mr_answering, players.partnerName),
            locked = partnerLocked,
            modifier = Modifier.weight(1f),
        )
    }
    Text(stringResource(R.string.games_mr_hidden_note), style = MaterialTheme.typography.labelMedium, color = colors.inkFaint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun RoundBar(state: MindReaderState) {
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        InfoChip(
            if (state.targetRounds > 0) stringResource(R.string.games_mr_round_of, state.roundIndex, state.targetRounds)
            else stringResource(R.string.games_mr_round, state.roundIndex),
        )
        InfoChip(stringResource(R.string.games_mr_streak_chip, state.curStreak))
    }
}

@Composable
private fun QuestionCard(state: MindReaderState) {
    val colors = TwinTheme.colors
    val question = MindReaderCatalog.of(state.question)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(MindReaderCatalog.categoryLabel(state.questionCat)), style = MaterialTheme.typography.labelMedium, color = colors.accent)
        Text(
            question?.let { stringResource(it.text) } ?: "",
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
            modifier = Modifier.padding(top = TwinTheme.spacing.xs),
        )
    }
}

@Composable
private fun LockPill(text: String, locked: Boolean, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (locked) colors.success else colors.inkMuted,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(TwinTheme.shapes.pill)
            .background(colors.glass)
            .border(1.dp, if (locked) colors.success else colors.lineSoft, TwinTheme.shapes.pill)
            .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.s),
    )
}

// ── reveal ──

@Composable
private fun Reveal(state: MindReaderState, players: Players, viewModel: MindReaderViewModel) {
    val colors = TwinTheme.colors
    val me = players.side
    var flipped by remember(state.roundIndex) { mutableStateOf(false) }
    LaunchedEffect(state.roundIndex) {
        delay(700)
        flipped = true
    }
    val lastRound = state.targetRounds > 0 && state.roundIndex >= state.targetRounds
    val match = state.match

    RoundBar(state)
    QuestionCard(state)
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m), modifier = Modifier.fillMaxWidth()) {
        FlipCard(players.myName, state.answerOf(me).orEmpty(), flipped, Modifier.weight(1f))
        FlipCard(players.partnerName, state.answerOf(me.other).orEmpty(), flipped, Modifier.weight(1f))
    }
    if (!flipped || match == null) {
        Text(stringResource(R.string.games_mr_flipping), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        return
    }
    Text(
        text = stringResource(matchLabel(match)) + " " + stringResource(R.string.games_mr_points, MindReaderEngine.points(match)),
        style = MaterialTheme.typography.titleMedium,
        color = if (match == MrMatch.Different) colors.inkMuted else colors.ink,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(TwinTheme.shapes.pill)
            .background(if (match == MrMatch.Different) colors.glass else colors.primaryBrush)
            .padding(TwinTheme.spacing.m),
    )
    Text(
        stringResource(R.string.games_mr_score_tick, state.compatPct, state.totalPts, state.maxPts),
        style = MaterialTheme.typography.labelMedium,
        color = colors.inkMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    val fav = state.history.firstOrNull()?.fav == true
    GlassButton(
        stringResource(if (fav) R.string.games_mr_fav_on else R.string.games_mr_fav_add),
        onClick = viewModel::toggleFavorite,
        style = GlassButtonStyle.Secondary,
        modifier = Modifier.fillMaxWidth(),
    )
    GlassButton(
        stringResource(if (lastRound) R.string.games_mr_results else R.string.games_mr_next),
        onClick = viewModel::next,
        modifier = Modifier.fillMaxWidth(),
    )
    GlassButton(stringResource(R.string.games_mr_end), onClick = viewModel::endMatch, style = GlassButtonStyle.Ghost, modifier = Modifier.fillMaxWidth())
}

private fun matchLabel(match: MrMatch): Int = when (match) {
    MrMatch.Exact -> R.string.games_mr_match_exact
    MrMatch.Near -> R.string.games_mr_match_near
    MrMatch.Funny -> R.string.games_mr_match_funny
    MrMatch.Different -> R.string.games_mr_match_diff
}

@Composable
private fun FlipCard(name: String, answer: String, flipped: Boolean, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(500), label = "mrFlip")
    Box(
        modifier = modifier
            .height(130.dp)
            .graphicsLayer { rotationY = rotation; cameraDistance = 14f * density }
            .clip(shape)
            .background(if (rotation > 90f) colors.glass else colors.primaryStart.copy(alpha = 0.55f))
            .border(1.dp, colors.line2, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (rotation > 90f) {
            Column(
                modifier = Modifier.graphicsLayer { rotationY = 180f }.padding(TwinTheme.spacing.s),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(name, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                Text(answer, style = MaterialTheme.typography.titleSmall, color = colors.ink, textAlign = TextAlign.Center)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔒", style = MaterialTheme.typography.headlineMedium)
                Text(name, style = MaterialTheme.typography.labelMedium, color = colors.ink)
            }
        }
    }
}

// ── summary ──

@Composable
private fun Summary(state: MindReaderState, viewModel: MindReaderViewModel) {
    val colors = TwinTheme.colors
    val pct = state.compatPct
    val icon = when {
        pct >= 90 -> "💞"
        pct >= 70 -> "💕"
        pct >= 45 -> "💗"
        else -> "🌱"
    }
    val title = when {
        pct >= 90 -> R.string.games_mr_sum_soulmates
        pct >= 70 -> R.string.games_mr_sum_great
        pct >= 45 -> R.string.games_mr_sum_getting
        else -> R.string.games_mr_sum_grow
    }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        Text(icon, style = MaterialTheme.typography.displayMedium)
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, color = colors.ink, textAlign = TextAlign.Center)
        Text(stringResource(R.string.games_mr_sum_sub, state.roundIndex), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        CompatRing(pct, 140.dp)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
        StatTile("${state.totalRounds}", stringResource(R.string.games_mr_sum_lifetime), Modifier.weight(1f))
        StatTile("${state.bestStreak}", stringResource(R.string.games_mr_stat_streak), Modifier.weight(1f))
        StatTile("${state.funnyMatches}", stringResource(R.string.games_mr_sum_funny), Modifier.weight(1f))
    }
    state.catStats.entries.sortedByDescending { it.value.total }.take(6).forEach { (cat, stat) ->
        val percent = MindReaderEngine.percentOf(stat)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(MindReaderCatalog.categoryLabel(cat)), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.weight(1.3f))
            GameTimerBar(percent / 100f, low = false, modifier = Modifier.weight(2f))
            Text("$percent%", style = MaterialTheme.typography.labelMedium, color = colors.ink)
        }
    }
    Achievements(state)
    GlassButton(stringResource(R.string.games_mr_back), onClick = viewModel::backToLobby, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun CompatRing(percent: Int, diameter: Dp, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val sweep by animateFloatAsState(percent.coerceIn(0, 100) * 3.6f, tween(800), label = "compat")
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            val corner = Offset(stroke / 2, stroke / 2)
            drawArc(colors.lineSoft, 0f, 360f, false, corner, arc, style = Stroke(stroke))
            drawArc(colors.primaryBrush, -90f, sweep, false, corner, arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$percent%", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
            Text(stringResource(R.string.games_mr_compat), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
        }
    }
}
