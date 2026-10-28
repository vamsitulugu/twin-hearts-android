package com.twinhearts.app.feature.livemap.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SocialDistance
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.StatTile
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.livemap.domain.GeoMath
import com.twinhearts.app.feature.livemap.domain.LiveMapFormat
import com.twinhearts.app.feature.livemap.ui.LiveMapUiState
import com.twinhearts.app.feature.livemap.ui.components.LmDot

/**
 * The "Partner" tab. Stateless: it only reads [state] and calls the lambdas, so it can be previewed.
 * Per the brief it shows no battery or network information.
 */
@Composable
fun PartnerPage(
    state: LiveMapUiState,
    onDirections: () -> Unit,
    onTrack: () -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val partner = state.positions.partner
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(TwinTheme.spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                Avatar(name = state.partnerName, size = 64.dp)
                Column {
                    Text(state.partnerName, style = MaterialTheme.typography.titleLarge, color = colors.ink)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                        LmDot(online = partner?.online == true && !partner.paused)
                        Text(statusText(partner, partner?.paused == true, state.nowMs), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                    }
                }
            }
        }
        if (partner == null) {
            EmptyState(
                title = stringResource(R.string.livemap_partner_none_title),
                message = stringResource(R.string.livemap_partner_none_message, state.partnerName),
            )
            return@Column
        }
        val me = state.myFix?.let { it.lat to it.lng } ?: state.positions.me?.let { it.lat to it.lng }
        val away = me?.let { GeoMath.distanceM(it.first, it.second, partner.lat, partner.lng) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            StatTile(activityLabel(GeoMath.activity(partner.speedMps)), stringResource(R.string.livemap_stat_status), Modifier.weight(1f), Icons.Rounded.Speed)
            StatTile(LiveMapFormat.ago(state.nowMs, partner.updatedAtMs), stringResource(R.string.livemap_stat_updated), Modifier.weight(1f), Icons.Rounded.AccessTime)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            StatTile(away?.let { LiveMapFormat.distance(it) } ?: "—", stringResource(R.string.livemap_stat_away), Modifier.weight(1f), Icons.Rounded.SocialDistance)
            StatTile(away?.let { LiveMapFormat.minutes(GeoMath.driveMinutes(it)) } ?: "—", stringResource(R.string.livemap_stat_eta), Modifier.weight(1f), Icons.Rounded.Directions)
        }
        val here = GeoMath.nearestSaved(partner.lat, partner.lng, state.shared.places)
        GlassCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.livemap_partner_location), style = MaterialTheme.typography.labelLarge, color = colors.inkMuted)
            Text(
                here?.let { stringResource(R.string.livemap_at_place, it.name.ifBlank { it.category }) }
                    ?: state.partnerArea?.let { stringResource(R.string.livemap_near, it.main) }
                    ?: stringResource(R.string.livemap_status_unknown),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
            )
            state.partnerArea?.takeIf { here == null }?.let { Text(it.sub, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted) }
        }
        GlassButton(stringResource(R.string.livemap_get_directions), onDirections, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Directions)
        GlassButton(stringResource(R.string.livemap_view_route_history), onTrack, Modifier.fillMaxWidth(), style = GlassButtonStyle.Secondary, leadingIcon = Icons.Rounded.Timeline)
        GlassButton(stringResource(R.string.livemap_locate_partner), onLocate, Modifier.fillMaxWidth(), style = GlassButtonStyle.Ghost)
    }
}
