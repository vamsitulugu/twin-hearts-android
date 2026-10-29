package com.twinhearts.app.feature.globe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.StatTile
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.globe.domain.model.AchievementId
import com.twinhearts.app.feature.globe.domain.model.TravelStats
import com.twinhearts.app.feature.globe.domain.spentShortLabel
import com.twinhearts.app.feature.globe.ui.components.SheetLabel
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val HEAT_FILL_BASE = 0.2f
private const val HEAT_FILL_STEP = 0.2f

/** The Travel Stats page: totals, a 12-month activity strip, favourite cities, moods and achievements. */
@Composable
fun StatsView(stats: TravelStats, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TwinTheme.spacing.l)
                .navigationBarsPadding()
                .padding(bottom = TwinTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = TwinTheme.spacing.s)) {
                Text(
                    stringResource(R.string.globe_stats_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = SerifFamily,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.globe_close),
                    onClick = onClose,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                StatTile(stats.memories.toString(), stringResource(R.string.globe_stats_memories), Modifier.weight(1f))
                StatTile(stats.cities.toString(), stringResource(R.string.globe_stats_cities), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                StatTile(stats.countries.toString(), stringResource(R.string.globe_stats_countries), Modifier.weight(1f))
                StatTile(spentShortLabel(stats.totalCost), stringResource(R.string.globe_stats_spent), Modifier.weight(1f))
            }

            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                SheetLabel(stringResource(R.string.globe_stats_activity))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    stats.heat.forEach { cell ->
                        val label = stringResource(R.string.globe_stats_activity_cell, monthName(cell.month), cell.count)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (cell.level == 0) colors.glass else colors.accent.copy(alpha = HEAT_FILL_BASE + HEAT_FILL_STEP * cell.level))
                                .semantics { contentDescription = label },
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(monthName(stats.heat.first().month), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted, modifier = Modifier.weight(1f))
                    Text(monthName(stats.heat.last().month), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                }
            }

            if (stats.topCities.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    SheetLabel(stringResource(R.string.globe_stats_top_cities))
                    val max = stats.topCities.maxOf { it.count }.coerceAtLeast(1)
                    stats.topCities.forEachIndexed { index, city ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}. ${city.city}", style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
                                Text(city.count.toString(), style = MaterialTheme.typography.labelLarge, color = colors.accent, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .fillMaxWidth(city.count.toFloat() / max)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.primaryBrush),
                            )
                        }
                    }
                }
            }

            if (stats.moods.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    SheetLabel(stringResource(R.string.globe_stats_moods))
                    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                        stats.moods.forEach { mood ->
                            GlassCard(modifier = Modifier.weight(1f), contentPadding = TwinTheme.spacing.s) {
                                Text(mood.emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text(
                                    mood.count.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.ink,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                SheetLabel(stringResource(R.string.globe_stats_achievements))
                stats.achievements.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { a ->
                            val info = achievementInfo(a.id)
                            GlassCard(
                                modifier = Modifier.weight(1f).alpha(if (a.unlocked) 1f else 0.45f),
                                contentPadding = TwinTheme.spacing.s,
                            ) {
                                Text(info.icon, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text(stringResource(info.name), style = MaterialTheme.typography.labelLarge, color = colors.ink, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(info.desc), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                                if (a.unlocked) {
                                    Text(stringResource(R.string.globe_stats_unlocked), style = MaterialTheme.typography.labelSmall, color = colors.success, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(TwinTheme.spacing.s))
        }
    }
}

private fun monthName(month: String): String =
    runCatching { YearMonth.parse(month).month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }.getOrDefault(month)

private data class AchievementInfo(val icon: String, val name: Int, val desc: Int)

private fun achievementInfo(id: AchievementId): AchievementInfo = when (id) {
    AchievementId.FirstMemory -> AchievementInfo("🌟", R.string.globe_ach_first_memory_name, R.string.globe_ach_first_memory_desc)
    AchievementId.CityExplorer -> AchievementInfo("🏙️", R.string.globe_ach_city_explorer_name, R.string.globe_ach_city_explorer_desc)
    AchievementId.WorldTraveler -> AchievementInfo("🌍", R.string.globe_ach_world_traveler_name, R.string.globe_ach_world_traveler_desc)
    AchievementId.BigSpender -> AchievementInfo("💰", R.string.globe_ach_big_spender_name, R.string.globe_ach_big_spender_desc)
    AchievementId.Photographer -> AchievementInfo("📸", R.string.globe_ach_photographer_name, R.string.globe_ach_photographer_desc)
    AchievementId.ContinentHopper -> AchievementInfo("✈️", R.string.globe_ach_continent_hopper_name, R.string.globe_ach_continent_hopper_desc)
    AchievementId.Romantics -> AchievementInfo("💑", R.string.globe_ach_romantics_name, R.string.globe_ach_romantics_desc)
    AchievementId.GlobeMasters -> AchievementInfo("🌐", R.string.globe_ach_globe_masters_name, R.string.globe_ach_globe_masters_desc)
    AchievementId.Storytellers -> AchievementInfo("🎙️", R.string.globe_ach_storytellers_name, R.string.globe_ach_storytellers_desc)
}
