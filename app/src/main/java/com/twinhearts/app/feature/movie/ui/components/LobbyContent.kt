package com.twinhearts.app.feature.movie.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.movie.domain.formatRuntime
import com.twinhearts.app.feature.movie.domain.model.RoomStatus
import com.twinhearts.app.feature.movie.domain.model.WatchRoom

/**
 * Why this file exists: the "lobby" after a movie is picked. Both people confirm they chose the
 * same movie, press I'M READY, and one presses START TOGETHER. The other must accept (see InviteDialog).
 */
@Composable
fun LobbyContent(
    room: WatchRoom,
    busy: Boolean,
    onReady: () -> Unit,
    onStart: () -> Unit,
    onCancelStart: () -> Unit,
    onCancelMovie: () -> Unit,
    onChange: () -> Unit,
) {
    val colors = TwinTheme.colors
    val waitingForPartner = room.status == RoomStatus.StartRequested && room.startRequestedByMe
    val bothReady = room.myReady && room.partnerReady && room.moviesMatch
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(TwinTheme.spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        room.myTitle.orEmpty(), color = colors.ink, style = MaterialTheme.typography.titleMedium,
                        fontFamily = SerifFamily, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    room.myDurationSec?.let {
                        Text(formatRuntime(it), color = colors.inkMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                GlassButton(stringResource(R.string.movie_change), onChange, style = GlassButtonStyle.Secondary)
            }
        }
        if (room.partnerTitle != null) {
            Text(
                stringResource(if (room.moviesMatch) R.string.movie_match_ok else R.string.movie_match_bad),
                color = if (room.moviesMatch) colors.success else colors.error,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.movie_playback_settings), color = colors.inkMuted, style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.movie_auto_sync), color = colors.ink, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.movie_always_on), color = colors.accent, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            PersonChip(stringResource(R.string.movie_you), mine = true, room = room, Modifier.weight(1f))
            PersonChip(stringResource(R.string.movie_partner), mine = false, room = room, Modifier.weight(1f))
        }
        when {
            waitingForPartner -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                Text(stringResource(R.string.movie_waiting_partner), color = colors.ink, style = MaterialTheme.typography.titleMedium, fontFamily = SerifFamily)
                GlassButton(stringResource(R.string.movie_cancel), onCancelStart, style = GlassButtonStyle.Ghost)
            }
            bothReady -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                Text(stringResource(R.string.movie_both_ready), color = colors.ink, style = MaterialTheme.typography.titleMedium, fontFamily = SerifFamily)
                GlassButton(stringResource(R.string.movie_start), onStart, Modifier.fillMaxWidth(), loading = busy)
            }
            !room.myReady && room.moviesMatch ->
                GlassButton(stringResource(R.string.movie_ready), onReady, Modifier.fillMaxWidth(), loading = busy)
            room.myReady -> Text(
                stringResource(R.string.movie_waiting_join), color = colors.inkMuted,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
        }
        GlassButton(stringResource(R.string.movie_cancel_together), onCancelMovie, Modifier.fillMaxWidth(), style = GlassButtonStyle.Ghost, enabled = !busy)
    }
}

@Composable
private fun PersonChip(name: String, mine: Boolean, room: WatchRoom, modifier: Modifier) {
    val ready = if (mine) room.myReady else room.partnerReady
    val hasMovie = if (mine) room.myTitle != null else room.partnerTitle != null
    val status = when {
        ready -> R.string.movie_ready_state
        hasMovie -> R.string.movie_selecting
        else -> R.string.movie_waiting
    }
    GlassCard(modifier) {
        Text(name, color = TwinTheme.colors.ink, style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(status),
            color = if (ready) TwinTheme.colors.success else TwinTheme.colors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** Big 3 · 2 · 1 shown on both phones until the shared start time. */
@Composable
fun CountdownContent(seconds: Int) {
    val scale by animateFloatAsState(if (seconds > 0) 1f else 0.8f, label = "countdown")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = seconds.coerceAtLeast(1).toString(),
            style = MaterialTheme.typography.displayLarge,
            fontFamily = SerifFamily,
            color = TwinTheme.colors.ink,
            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        )
    }
}
