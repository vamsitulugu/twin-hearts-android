package com.twinhearts.app.feature.fights.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.fights.domain.model.ActivityFilter
import com.twinhearts.app.feature.fights.domain.model.ReconnectActivity

/** Why this file exists: the Reconnect Activities screen: filter tabs and eight tap-to-open activity cards. */
@Composable
fun ActivitiesView(
    filter: ActivityFilter,
    onFilter: (ActivityFilter) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.core_back),
                tint = colors.ink,
                modifier = Modifier.size(TwinTheme.shapes.minTouch).clip(TwinTheme.shapes.circle).clickable(role = Role.Button, onClick = onBack).padding(12.dp),
            )
            Text(stringResource(R.string.fights_act_title), style = MaterialTheme.typography.titleLarge, color = colors.ink, fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.fights_act_lead), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            items(ActivityFilter.entries.toList(), key = { it.name }) { f ->
                FightChip(stringResource(filterLabel(f)), f == filter, { onFilter(f) })
            }
        }
        ReconnectActivity.entries
            .filter { filter == ActivityFilter.All || it.category == filter }
            .forEach { ActivityCard(it) }
    }
}

@Composable
private fun ActivityCard(activity: ReconnectActivity) {
    val colors = TwinTheme.colors
    var open by rememberSaveable(activity.key) { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(TwinTheme.shapes.small)
                .clickable(role = Role.Button) { open = !open },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            ToneTile(activityIcon(activity), toneColor(activity.tone))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(activityTitle(activity)), style = MaterialTheme.typography.titleSmall, color = colors.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                    Icon(Icons.Rounded.Schedule, null, tint = colors.inkFaint, modifier = Modifier.size(14.dp))
                    Text(stringResource(R.string.fights_minutes, activity.minutes), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
                }
                Text(stringResource(activityShort(activity)), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
            Icon(
                if (open) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null, tint = colors.inkFaint, modifier = Modifier.size(20.dp),
            )
        }
        AnimatedVisibility(visible = open) {
            Text(
                stringResource(activityHow(activity)),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink,
                modifier = Modifier.padding(top = TwinTheme.spacing.m),
            )
        }
    }
}
