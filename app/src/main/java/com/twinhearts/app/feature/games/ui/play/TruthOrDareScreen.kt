package com.twinhearts.app.feature.games.ui.play

import android.widget.Toast
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.engine.TodPhase
import com.twinhearts.app.feature.games.domain.engine.TodState
import com.twinhearts.app.feature.games.domain.engine.TodType
import com.twinhearts.app.feature.games.domain.engine.TruthOrDareEngine
import com.twinhearts.app.feature.games.domain.model.Players
import com.twinhearts.app.feature.games.ui.components.GameHost
import com.twinhearts.app.feature.games.ui.components.TurnBar
import kotlinx.coroutines.delay
import kotlin.random.Random

private val reactionEmojis = listOf("😂", "😍", "😳", "🔥", "👏")

@Composable
fun TruthOrDareScreen(onExit: () -> Unit, viewModel: TruthOrDareViewModel = hiltViewModel()) {
    val skipped = stringResource(R.string.games_tod_skipped_label)
    GameHost(
        viewModel = viewModel,
        title = stringResource(R.string.games_tod_title),
        onExit = onExit,
        alongside = { viewModel.runAnswerClock(skipped) },
    ) { state, players ->
        TodContent(state, players, viewModel)
    }
}

@Composable
private fun ColumnScope.TodContent(state: TodState, players: Players, vm: TruthOrDareViewModel) {
    val me = players.side
    val myTurnToAsk = state.turn == me
    val answerer = TruthOrDareEngine.answererOf(state)
    val partner = players.partnerName

    val barText = when (state.phase) {
        TodPhase.Idle -> if (myTurnToAsk) stringResource(R.string.games_tod_idle_mine) else stringResource(R.string.games_tod_idle_theirs, partner)
        TodPhase.Spinning -> stringResource(R.string.games_tod_spinning)
        TodPhase.Choose -> if (myTurnToAsk) stringResource(R.string.games_tod_choose_mine) else stringResource(R.string.games_tod_choose_theirs, partner)
        TodPhase.Compose -> if (myTurnToAsk) stringResource(R.string.games_tod_compose_mine, partner) else stringResource(R.string.games_tod_compose_theirs, partner)
        TodPhase.Reveal -> if (answerer == me) stringResource(R.string.games_tod_respond) else stringResource(R.string.games_tod_waiting, partner)
    }
    val streak = stringResource(R.string.games_tod_streak, state.streak)
    TurnBar(text = "$streak  ·  $barText", mine = myTurnToAsk && state.phase != TodPhase.Reveal || answerer == me && state.phase == TodPhase.Reveal)

    if (state.phase != TodPhase.Reveal) {
        CategoryRow(selected = state.category, onSelect = vm::setCategory)
    }

    when (state.phase) {
        TodPhase.Idle, TodPhase.Spinning -> BottleArea(state, myTurnToAsk, partner, onSpin = vm::spin, onFinished = vm::spinFinished)
        TodPhase.Choose -> ChooseArea(myTurnToAsk, partner, onChoose = vm::choose)
        TodPhase.Compose -> ComposeArea(state, myTurnToAsk, partner, onSend = vm::send)
        TodPhase.Reveal -> state.current?.let { RevealArea(state, it.askedBy == me, partner, vm) }
    }
}

@Composable
private fun CategoryRow(selected: String, onSelect: (String) -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        TruthOrDareEngine.categories.forEach { id ->
            val on = id == selected
            Text(
                text = stringResource(categoryLabel(id)),
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                modifier = Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(if (on) colors.primaryStart.copy(alpha = 0.55f) else colors.glass)
                    .border(1.dp, if (on) colors.accent else colors.lineSoft, TwinTheme.shapes.pill)
                    .clickable { onSelect(id) }
                    .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
            )
        }
    }
}

@Composable
private fun BottleArea(state: TodState, myTurn: Boolean, partnerName: String, onSpin: () -> Unit, onFinished: () -> Unit) {
    val colors = TwinTheme.colors
    val rotation = remember { Animatable(0f) }
    val spinning = state.phase == TodPhase.Spinning
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val canSpin = myTurn && state.phase == TodPhase.Idle

    // Both phones turn the bottle; only the person who spun reports that it stopped.
    LaunchedEffect(spinning) {
        if (spinning) {
            val turns = Random.nextInt(5, 9) * 360f + Random.nextInt(0, 360)
            rotation.animateTo(rotation.value + turns, tween(3200, easing = CubicBezierEasing(0.13f, 0.79f, 0.25f, 1f)))
            if (myTurn) onFinished()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(TwinTheme.shapes.circle)
                .background(colors.glass)
                .border(1.dp, colors.line, TwinTheme.shapes.circle)
                .pointerInput(canSpin) {
                    var travelled = 0f
                    detectDragGestures(
                        onDragStart = { travelled = 0f },
                        onDragEnd = { if (canSpin && travelled > FLICK_PX) onSpin() },
                    ) { change, drag ->
                        change.consume()
                        travelled += kotlin.math.abs(drag.x) + kotlin.math.abs(drag.y)
                    }
                }
                .clickable(enabled = canSpin, onClick = onSpin),
            contentAlignment = Alignment.Center,
        ) {
            Text("🍾", fontSize = 110.sp, modifier = Modifier.graphicsLayer { rotationZ = rotation.value })
        }
        GlassButton(
            text = stringResource(if (spinning) R.string.games_tod_spinning_btn else R.string.games_tod_spin_btn),
            onClick = onSpin,
            enabled = canSpin,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = if (myTurn) stringResource(R.string.games_tod_hint_mine) else stringResource(R.string.games_tod_waiting, partnerName),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted,
        )
        if (state.phase == TodPhase.Idle) {
            GlassButton(
                text = stringResource(if (showHistory) R.string.games_tod_history_hide else R.string.games_tod_history_show),
                onClick = { showHistory = !showHistory },
                style = GlassButtonStyle.Secondary,
            )
            if (showHistory) HistoryList(state)
        }
    }
}

private const val FLICK_PX = 60f

@Composable
private fun HistoryList(state: TodState) {
    val colors = TwinTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
        if (state.history.isEmpty()) {
            Text(stringResource(R.string.games_tod_history_empty), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        }
        state.history.forEach { item ->
            val icon = stringResource(if (item.type == TodType.Truth) R.string.games_tod_truth_emoji else R.string.games_tod_dare_emoji)
            Text(
                "$icon ${item.text} → ${item.answer}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                modifier = Modifier.padding(vertical = TwinTheme.spacing.xs),
            )
        }
    }
}

@Composable
private fun ChooseArea(myTurn: Boolean, partner: String, onChoose: (TodType) -> Unit) {
    if (myTurn) {
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m), modifier = Modifier.fillMaxWidth()) {
            ChoiceButton(stringResource(R.string.games_tod_truth_emoji), stringResource(R.string.games_tod_truth), Modifier.weight(1f)) { onChoose(TodType.Truth) }
            ChoiceButton(stringResource(R.string.games_tod_dare_emoji), stringResource(R.string.games_tod_dare), Modifier.weight(1f)) { onChoose(TodType.Dare) }
        }
    } else {
        WaitingBlock("🍾", stringResource(R.string.games_tod_picking, partner))
    }
}

@Composable
private fun ChoiceButton(emoji: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Column(
        modifier = modifier
            .clip(TwinTheme.shapes.card)
            .background(colors.primaryBrush)
            .clickable(onClick = onClick)
            .padding(vertical = TwinTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 36.sp)
        Text(label, style = MaterialTheme.typography.titleMedium, color = colors.ink)
    }
}

@Composable
private fun WaitingBlock(emoji: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = TwinTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Text(emoji, fontSize = 40.sp)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TwinTheme.colors.inkMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ComposeArea(state: TodState, myTurn: Boolean, partner: String, onSend: (String) -> Unit) {
    val context = LocalContext.current
    val truth = state.pendingType == TodType.Truth
    if (!myTurn) {
        WaitingBlock("✍️", stringResource(if (truth) R.string.games_tod_writing_truth else R.string.games_tod_writing_dare, partner))
        return
    }
    var text by rememberSaveable(state.phase, state.turn) { mutableStateOf("") }
    val writeFirst = stringResource(R.string.games_tod_write_first)
    val noVoice = stringResource(R.string.games_tod_no_voice)
    val voice = rememberVoiceInput(onUnavailable = { Toast.makeText(context, noVoice, Toast.LENGTH_SHORT).show() }) { text = it }
    val suggestions = stringArrayResource(suggestionsRes(state.category, truth))

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        TwinTextField(
            value = text,
            onValueChange = { text = it },
            label = stringResource(if (truth) R.string.games_tod_compose_label_truth else R.string.games_tod_compose_label_dare),
            placeholder = stringResource(if (truth) R.string.games_tod_placeholder_truth else R.string.games_tod_placeholder_dare),
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier.padding(vertical = TwinTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            suggestions.forEach { suggestion ->
                Text(
                    suggestion,
                    style = MaterialTheme.typography.bodySmall,
                    color = TwinTheme.colors.inkMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TwinTheme.shapes.small)
                        .background(TwinTheme.colors.neutralGlass.copy(alpha = 0.25f))
                        .clickable { text = suggestion }
                        .padding(TwinTheme.spacing.m),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
            GlassButton(stringResource(R.string.games_tod_voice), onClick = voice, style = GlassButtonStyle.Secondary, modifier = Modifier.weight(1f))
            GlassButton(
                stringResource(R.string.games_tod_send),
                onClick = { if (text.isBlank()) Toast.makeText(context, writeFirst, Toast.LENGTH_SHORT).show() else onSend(text) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RevealArea(state: TodState, amAsker: Boolean, partner: String, vm: TruthOrDareViewModel) {
    val colors = TwinTheme.colors
    val context = LocalContext.current
    val card = state.current ?: return
    val speak = rememberSpeaker()
    var answer by rememberSaveable(card.text) { mutableStateOf("") }
    val noVoice = stringResource(R.string.games_tod_no_voice)
    val voice = rememberVoiceInput(onUnavailable = { Toast.makeText(context, noVoice, Toast.LENGTH_SHORT).show() }) { answer = it }
    val doneLabel = stringResource(R.string.games_tod_done_label)
    val skippedLabel = stringResource(R.string.games_tod_skipped_label)

    // A reaction from either phone floats up for a moment.
    var floating by remember { mutableStateOf<String?>(null) }
    var lastNonce by remember { mutableStateOf(state.reaction?.nonce) }
    LaunchedEffect(state.reaction?.nonce) {
        val reaction = state.reaction
        if (reaction != null && reaction.nonce != lastNonce) {
            lastNonce = reaction.nonce
            floating = reaction.emoji
            delay(1_200)
            floating = null
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Text(
                stringResource(if (card.type == TodType.Truth) R.string.games_tod_truth_up else R.string.games_tod_dare_up),
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent,
            )
            Text(card.text, style = MaterialTheme.typography.headlineSmall, color = colors.ink, textAlign = TextAlign.Center)
            Box(contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    reactionEmojis.forEach { emoji ->
                        Text(
                            emoji,
                            fontSize = 26.sp,
                            modifier = Modifier
                                .clip(TwinTheme.shapes.circle)
                                .background(colors.glass)
                                .clickable { vm.react(emoji) }
                                .padding(TwinTheme.spacing.s),
                        )
                    }
                }
                floating?.let { Text(it, fontSize = 64.sp) }
            }
            GlassButton(stringResource(R.string.games_tod_read), onClick = { speak(card.text) }, style = GlassButtonStyle.Secondary)

            val given = card.answer
            when {
                given != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.games_tod_answered, if (amAsker) partner else stringResource(R.string.games_tod_you)),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkMuted,
                    )
                    Text(given, style = MaterialTheme.typography.titleMedium, color = colors.ink, textAlign = TextAlign.Center)
                }
                amAsker -> Text(stringResource(R.string.games_tod_waiting, partner), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                card.type == TodType.Truth -> {
                    TwinTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = stringResource(R.string.games_tod_your_answer),
                        placeholder = stringResource(R.string.games_tod_answer_hint),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                        GlassButton(stringResource(R.string.games_tod_voice), onClick = voice, style = GlassButtonStyle.Secondary, modifier = Modifier.weight(1f))
                        GlassButton(stringResource(R.string.games_tod_skip), onClick = { vm.skip(skippedLabel) }, style = GlassButtonStyle.Secondary, modifier = Modifier.weight(1f))
                    }
                    GlassButton(
                        stringResource(R.string.games_tod_submit),
                        onClick = { vm.answer(answer) },
                        enabled = answer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                    GlassButton(stringResource(R.string.games_tod_skip), onClick = { vm.skip(skippedLabel) }, style = GlassButtonStyle.Secondary, modifier = Modifier.weight(1f))
                    GlassButton(stringResource(R.string.games_tod_completed), onClick = { vm.dareDone(doneLabel) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@StringRes
private fun categoryLabel(id: String): Int = when (id) {
    "romantic" -> R.string.games_tod_cat_romantic
    "cute" -> R.string.games_tod_cat_cute
    "funny" -> R.string.games_tod_cat_funny
    "deep" -> R.string.games_tod_cat_deep
    "hot" -> R.string.games_tod_cat_hot
    else -> R.string.games_tod_cat_adventure
}

@ArrayRes
private fun suggestionsRes(category: String, truth: Boolean): Int = when (category) {
    "cute" -> if (truth) R.array.games_tod_cute_truths else R.array.games_tod_cute_dares
    "funny" -> if (truth) R.array.games_tod_funny_truths else R.array.games_tod_funny_dares
    "deep" -> if (truth) R.array.games_tod_deep_truths else R.array.games_tod_deep_dares
    "hot" -> if (truth) R.array.games_tod_hot_truths else R.array.games_tod_hot_dares
    "adventure" -> if (truth) R.array.games_tod_adventure_truths else R.array.games_tod_adventure_dares
    else -> if (truth) R.array.games_tod_romantic_truths else R.array.games_tod_romantic_dares
}
