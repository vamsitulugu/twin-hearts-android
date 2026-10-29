package com.twinhearts.app.feature.games.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.ui.play.Prize
import com.twinhearts.app.feature.games.ui.play.PrizeLine

/** The "You won!" / "Level Up!" pop-up. */
@Composable
fun PrizeDialog(prize: Prize, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.xl) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                Text(prize.emoji, style = MaterialTheme.typography.displayMedium)
                Text(
                    stringResource(prize.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                )
                prize.lines.forEach { line ->
                    Text(
                        text = prizeLineText(line),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                    )
                }
                GlassButton(
                    text = stringResource(R.string.games_prize_continue),
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = TwinTheme.spacing.s),
                )
            }
        }
    }
}

@Composable
private fun prizeLineText(line: PrizeLine): String = when (line) {
    is PrizeLine.Xp -> stringResource(R.string.games_prize_xp, line.amount)
    is PrizeLine.Coins -> stringResource(R.string.games_prize_coins, line.amount)
    is PrizeLine.Level -> stringResource(R.string.games_prize_level, line.level)
    is PrizeLine.Text -> stringResource(line.res)
    is PrizeLine.Final -> stringResource(R.string.games_prize_final, line.scoreOne, line.scoreTwo)
    is PrizeLine.Penalty -> stringArrayResource(R.array.games_tod_penalties)[line.index]
}
