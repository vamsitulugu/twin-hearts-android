package com.twinhearts.app.feature.games.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.model.GameId
import com.twinhearts.app.feature.games.domain.model.Players

/**
 * Under the players strip of every game. When my partner is not in this game yet it shows an
 * "Invite" button (they get an Accept / Decline card); once they are here it says so.
 */
@Composable
fun InviteBar(
    players: Players,
    game: GameId,
    sent: Boolean,
    onInvite: () -> Unit,
    onAnswer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val partnerHere = players.partnerOnline && players.partnerPresence.game == game.key
    val incoming = players.pendingRequest?.takeIf { it.game == game.key }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(TwinTheme.spacing.s)) {
        if (incoming != null) {
            Text(
                text = stringResource(R.string.games_invited_here, players.partnerName),
                style = MaterialTheme.typography.bodyMedium,
                color = TwinTheme.colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(TwinTheme.spacing.s)) {
                GlassButton(text = stringResource(R.string.games_request_accept), onClick = onAnswer, modifier = Modifier.weight(1f))
                GlassButton(text = stringResource(R.string.games_request_decline), onClick = onAnswer, modifier = Modifier.weight(1f))
            }
        }
        when {
            partnerHere -> Text(
                text = stringResource(R.string.games_partner_here, players.partnerName),
                style = MaterialTheme.typography.bodySmall,
                color = TwinTheme.colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            sent -> Text(
                text = stringResource(R.string.games_invite_sent, players.partnerName),
                style = MaterialTheme.typography.bodySmall,
                color = TwinTheme.colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> GlassButton(
                text = stringResource(R.string.games_invite_partner, players.partnerName),
                onClick = onInvite,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
