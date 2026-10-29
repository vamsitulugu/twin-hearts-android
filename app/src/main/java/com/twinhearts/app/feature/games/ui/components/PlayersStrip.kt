package com.twinhearts.app.feature.games.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.model.Players

/** Small photo + name + level strip pinned above every game (same as the web "ghp" strip). */
@Composable
fun PlayersStrip(players: Players, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerChip(players.myName, players.myAvatar, players.me.level, online = true, modifier = Modifier.weight(1f))
        Text("❤️", style = MaterialTheme.typography.bodyMedium)
        PlayerChip(players.partnerName, players.partnerAvatar, players.partner.level, players.partnerOnline, Modifier.weight(1f))
    }
}

@Composable
private fun PlayerChip(name: String, avatar: String?, level: Int, online: Boolean, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Row(
        modifier = modifier
            .clip(TwinTheme.shapes.pill)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
            .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Box {
            Avatar(name = name, size = 32.dp, imageUrl = avatar)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (online) colors.success else colors.inkFaint)
                    .border(1.dp, colors.background, CircleShape),
            )
        }
        Column {
            Text(name, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.games_lv, level), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        }
    }
}
