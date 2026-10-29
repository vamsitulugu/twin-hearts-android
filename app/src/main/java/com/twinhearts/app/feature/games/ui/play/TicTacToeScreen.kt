package com.twinhearts.app.feature.games.ui.play

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.engine.TicTacToeEngine
import com.twinhearts.app.feature.games.domain.engine.TicTacToeState
import com.twinhearts.app.feature.games.domain.model.Outcome
import com.twinhearts.app.feature.games.domain.model.Players
import com.twinhearts.app.feature.games.ui.components.GameHost
import com.twinhearts.app.feature.games.ui.components.ScoreCard
import com.twinhearts.app.feature.games.ui.components.TurnBar

@Composable
fun TicTacToeScreen(onExit: () -> Unit, viewModel: TicTacToeViewModel = hiltViewModel()) {
    GameHost(viewModel = viewModel, title = stringResource(R.string.games_ttt_title), onExit = onExit) { state, players ->
        TicTacToeContent(state, players, onCell = viewModel::play, onRematch = viewModel::rematch)
    }
}

@Composable
private fun TicTacToeContent(
    state: TicTacToeState,
    players: Players,
    onCell: (Int) -> Unit,
    onRematch: () -> Unit,
) {
    val me = players.side
    val myTurn = state.outcome == Outcome.None && state.turn == me
    val text = when {
        state.outcome == Outcome.Draw -> stringResource(R.string.games_draw)
        state.outcome.isWonBy(me) -> stringResource(R.string.games_you_won)
        state.outcome != Outcome.None -> stringResource(R.string.games_partner_won, players.partnerName)
        myTurn -> stringResource(R.string.games_turn_yours)
        else -> stringResource(R.string.games_turn_theirs_ttt, players.partnerName)
    }
    TurnBar(text = text, mine = myTurn)

    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m), modifier = Modifier.fillMaxWidth()) {
        ScoreCard(stringResource(R.string.games_ttt_player_line, players.myName, TicTacToeEngine.symbolOf(me)), state.scoreOf(me), Modifier.weight(1f), active = myTurn)
        ScoreCard(stringResource(R.string.games_ttt_player_line, players.partnerName, TicTacToeEngine.symbolOf(me.other)), state.scoreOf(me.other), Modifier.weight(1f), active = !myTurn && state.outcome == Outcome.None)
    }

    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    Cell(
                        symbol = state.board[index],
                        winning = index in state.winningLine,
                        enabled = myTurn && state.board[index].isEmpty(),
                        onClick = { onCell(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (state.outcome != Outcome.None) {
        GlassButton(
            text = stringResource(R.string.games_play_again),
            onClick = onRematch,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Cell(symbol: String, winning: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(if (winning) colors.primaryStart.copy(alpha = 0.45f) else colors.glass)
            .border(1.dp, if (winning) colors.accent else colors.lineSoft, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            fontSize = 44.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (symbol == "X") colors.brand else colors.accent,
        )
    }
}

