package com.twinhearts.app.feature.livemap.ui.nav

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.livemap.data.nav.NavPhrases
import com.twinhearts.app.feature.livemap.domain.LiveMapFormat
import com.twinhearts.app.feature.livemap.domain.nav.ManeuverKind
import com.twinhearts.app.feature.livemap.domain.nav.NavSession
import com.twinhearts.app.feature.livemap.ui.components.LmBanner
import com.twinhearts.app.feature.livemap.ui.components.LmMapFab
import java.util.Date
import kotlin.math.roundToInt

/** Keeps the screen awake while navigating, and lets it sleep again afterwards. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * Big "next turn" card at the top: arrow, distance, instruction, and the turn after it when it
 * comes soon. Below it, short status lines for rerouting, no GPS or a failed reroute.
 */
@Composable
fun NavTopBanner(session: NavSession, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val resources = LocalContext.current.resources
    val phrases = remember(resources) { NavPhrases(resources) }
    val progress = session.progress
    val next = progress?.next
    Column(modifier, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                val kind = next?.kind ?: if (progress == null) ManeuverKind.Depart else ManeuverKind.Arrive
                Icon(kind.icon(), contentDescription = null, tint = colors.accent, modifier = Modifier.size(48.dp))
                Column(Modifier.weight(1f)) {
                    if (progress != null && next != null) {
                        Text(LiveMapFormat.distance(progress.distanceToNextM), style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                        Text(
                            phrases.instruction(next.kind, next.road, next.exit, next.side),
                            style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 2,
                        )
                        // The turn after next, when it follows quickly (like Google's "Then ...").
                        val after = session.route.steps.getOrNull(progress.stepIndex + 2)
                        if (after != null && after.kind.spoken && next.distanceM < THEN_SHOW_WITHIN_M) {
                            Text(
                                stringResource(R.string.livemap_nav_then, phrases.instruction(after.kind, after.road, after.exit, after.side).replaceFirstChar { it.lowercase() }),
                                style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1,
                            )
                        }
                    } else {
                        Text(
                            if (progress == null) stringResource(R.string.livemap_nav_gps_lost) else phrases.instruction(ManeuverKind.Arrive, "", null, null),
                            style = MaterialTheme.typography.titleMedium, color = colors.ink,
                        )
                    }
                }
            }
        }
        if (session.rerouting) LmBanner(stringResource(R.string.livemap_nav_rerouting), Modifier.fillMaxWidth(), color = colors.accent)
        else if (session.rerouteFailed) LmBanner(stringResource(R.string.livemap_nav_reroute_failed), Modifier.fillMaxWidth())
        else if (session.gpsLost && progress != null) LmBanner(stringResource(R.string.livemap_nav_gps_lost), Modifier.fillMaxWidth(), color = colors.inkMuted)
    }
}

/** Bottom bar: End, time left, distance left and arrival clock, and the voice switch. */
@Composable
fun NavBottomBar(
    session: NavSession,
    onEnd: () -> Unit,
    onMute: () -> Unit,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val context = LocalContext.current
    val progress = session.progress
    val remainingS = progress?.remainingS ?: session.route.durationS
    val remainingM = progress?.remainingM ?: session.route.distanceM
    val arrival = remember(remainingS.roundToInt() / 20) {
        DateFormat.getTimeFormat(context).format(Date(System.currentTimeMillis() + (remainingS * 1000).toLong()))
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), horizontalAlignment = Alignment.End) {
        if (!session.following) {
            GlassButton(stringResource(R.string.livemap_nav_recenter), onRecenter, style = GlassButtonStyle.Secondary, leadingIcon = Icons.Rounded.MyLocation)
        }
        GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                GlassButton(stringResource(R.string.livemap_nav_end), onEnd, style = GlassButtonStyle.Secondary)
                Column(Modifier.weight(1f)) {
                    Text(
                        LiveMapFormat.minutes(maxOf(1, (remainingS / 60).roundToInt())),
                        style = MaterialTheme.typography.titleLarge, color = colors.accent,
                    )
                    Text(
                        stringResource(R.string.livemap_dir_total, LiveMapFormat.distance(remainingM), arrival),
                        style = MaterialTheme.typography.bodySmall, color = colors.inkMuted,
                    )
                }
                LmMapFab(
                    icon = if (session.muted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                    description = stringResource(if (session.muted) R.string.livemap_nav_unmute else R.string.livemap_nav_mute),
                    onClick = onMute,
                    active = !session.muted,
                )
            }
        }
    }
}

/** Shown when the trip is over: where you arrived and how long and far it was. */
@Composable
fun ArrivalCard(session: NavSession, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val minutes = maxOf(1, ((System.currentTimeMillis() - session.startedAtMs) / 60_000L).toInt())
    GlassCard(modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.l) {
        Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Text(stringResource(R.string.livemap_nav_arrived_title), style = MaterialTheme.typography.titleLarge, color = colors.ink)
            Text(
                stringResource(R.string.livemap_nav_arrived_body, session.destinationName.ifBlank { stringResource(R.string.livemap_dropped_pin) }),
                style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted,
            )
            Text(
                stringResource(R.string.livemap_nav_trip_summary, LiveMapFormat.distance(session.route.distanceM), LiveMapFormat.minutes(minutes)),
                style = MaterialTheme.typography.titleSmall, color = colors.accent,
            )
            GlassButton(stringResource(R.string.livemap_nav_done), onDone, Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s))
        }
    }
}

private const val THEN_SHOW_WITHIN_M = 300.0
