package com.twinhearts.app.feature.games.ui.play

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.engine.MemoryCard
import com.twinhearts.app.feature.games.domain.engine.MemoryState
import com.twinhearts.app.feature.games.domain.model.Outcome
import com.twinhearts.app.feature.games.domain.model.Players
import com.twinhearts.app.feature.games.ui.components.GameHost
import com.twinhearts.app.feature.games.ui.components.ScoreCard
import com.twinhearts.app.feature.games.ui.components.TurnBar

@Composable
fun MemoryScreen(onExit: () -> Unit, viewModel: MemoryViewModel = hiltViewModel()) {
    GameHost(viewModel = viewModel, title = stringResource(R.string.games_memory_title), onExit = onExit) { state, players ->
        MemoryContent(state, players, onFlip = viewModel::flip, onRematch = viewModel::rematch)
    }
}

private const val COLUMNS = 4

@Composable
private fun MemoryContent(state: MemoryState, players: Players, onFlip: (Int) -> Unit, onRematch: () -> Unit) {
    val me = players.side
    val myTurn = state.outcome == Outcome.None && state.turn == me
    val text = when {
        state.outcome == Outcome.Draw -> stringResource(R.string.games_tie)
        state.outcome.isWonBy(me) -> stringResource(R.string.games_you_won)
        state.outcome != Outcome.None -> stringResource(R.string.games_partner_won, players.partnerName)
        myTurn -> stringResource(R.string.games_memory_your_turn)
        else -> stringResource(R.string.games_turn_theirs, players.partnerName)
    }
    TurnBar(text = text, mine = myTurn)

    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m), modifier = Modifier.fillMaxWidth()) {
        ScoreCard(players.myName, state.scoreOf(me), Modifier.weight(1f), active = myTurn)
        ScoreCard(players.partnerName, state.scoreOf(me.other), Modifier.weight(1f), active = !myTurn && state.outcome == Outcome.None)
    }

    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
        state.cards.chunked(COLUMNS).forEachIndexed { row, cards ->
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                cards.forEachIndexed { col, card ->
                    val index = row * COLUMNS + col
                    FlipCard(
                        card = card,
                        faceUp = card.matched || index in state.flipped,
                        enabled = myTurn && !card.matched && index !in state.flipped && state.flipped.size < 2,
                        onClick = { onFlip(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (state.outcome != Outcome.None) {
        GlassButton(text = stringResource(R.string.games_play_again), onClick = onRematch, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FlipCard(card: MemoryCard, faceUp: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    val rotation by animateFloatAsState(if (faceUp) 180f else 0f, tween(280), label = "memoryFlip")
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
            .clip(shape)
            .background(
                when {
                    card.matched -> colors.success.copy(alpha = 0.3f)
                    rotation > 90f -> colors.glass
                    else -> colors.primaryStart.copy(alpha = 0.55f)
                },
            )
            .border(1.dp, if (card.matched) colors.success else colors.lineSoft, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (rotation > 90f) {
            Text(card.emoji, fontSize = 30.sp, modifier = Modifier.graphicsLayer { rotationY = 180f })
        }
    }
}

