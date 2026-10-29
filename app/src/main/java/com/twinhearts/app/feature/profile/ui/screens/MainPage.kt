package com.twinhearts.app.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.profile.domain.ProfileMath
import com.twinhearts.app.feature.profile.domain.model.ConnectionState
import com.twinhearts.app.feature.profile.domain.model.ImportantDate
import com.twinhearts.app.feature.profile.domain.model.ImportantKind
import com.twinhearts.app.feature.profile.domain.model.ProfileData
import com.twinhearts.app.feature.profile.ui.components.ProfileAvatar
import com.twinhearts.app.feature.profile.ui.components.ProfileChip
import com.twinhearts.app.feature.profile.ui.components.formatDate
import com.twinhearts.app.feature.profile.ui.components.relativeTime
import com.twinhearts.app.feature.profile.ui.components.spanText
import java.time.LocalDate

/**
 * The first Profile screen ("Our Profile"): couple hero, quick actions, four counters,
 * the next important date and the Our Story card. Stateless: only reads [data] and calls lambdas.
 */
@Composable
fun MainPage(
    data: ProfileData,
    todayEpochDay: Long,
    nowMs: Long,
    uploadingAvatar: Boolean,
    onMore: () -> Unit,
    onPickAvatar: () -> Unit,
    onViewPartnerAvatar: () -> Unit,
    onEditProfile: () -> Unit,
    onPartnerDetails: () -> Unit,
    onAddDate: () -> Unit,
    onMemories: () -> Unit,
    onMilestones: () -> Unit,
    onDates: () -> Unit,
    onDreams: () -> Unit,
    onStory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val context = LocalContext.current
    val today = LocalDate.ofEpochDay(todayEpochDay)
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = TwinTheme.spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onMore, modifier = Modifier.background(colors.glass, CircleShape)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.profile_more), tint = colors.ink)
            }
        }

        // Hero: me (tap = change my photo), the heart with "Together since", my partner (tap = see photo).
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileAvatar(data.myAvatarUrl, stringResource(R.string.profile_change_my_photo), onPickAvatar, showCamera = true, uploading = uploadingAvatar)
                    Text(data.myName.ifBlank { stringResource(R.string.profile_you) }, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = TwinTheme.spacing.s))
                }
                Column(Modifier.weight(1f).padding(top = TwinTheme.spacing.s), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).background(colors.background, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(36.dp))
                    }
                    Text(stringResource(R.string.profile_together_since), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted, modifier = Modifier.padding(top = TwinTheme.spacing.s))
                    Row(
                        Modifier.clickable(role = Role.Button, onClick = onEditProfile).padding(TwinTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
                    ) {
                        Text(data.anniversary?.let(::formatDate) ?: stringResource(R.string.profile_set_date), style = MaterialTheme.typography.titleSmall, color = colors.ink, textAlign = TextAlign.Center)
                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.ink, modifier = Modifier.size(14.dp))
                    }
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileAvatar(data.partnerAvatarUrl, stringResource(R.string.profile_view_partner_photo), onViewPartnerAvatar.takeIf { data.partnerAvatarUrl != null })
                    Text(data.partnerName, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = TwinTheme.spacing.s))
                    val seen = data.partnerSeenAtMs
                    val presence = when {
                        data.connection != ConnectionState.Connected -> stringResource(R.string.profile_not_connected)
                        ProfileMath.isOnline(seen, nowMs) -> stringResource(R.string.profile_online)
                        seen != null -> stringResource(R.string.profile_last_seen, relativeTime(nowMs, seen))
                        else -> ""
                    }
                    if (presence.isNotEmpty()) Text(presence, style = MaterialTheme.typography.labelSmall, color = colors.inkMuted, textAlign = TextAlign.Center)
                }
            }
            data.anniversary?.let { from ->
                val span = spanText(context, ProfileMath.togetherSpan(from, today))
                if (span.isNotEmpty()) {
                    Text(span, style = MaterialTheme.typography.titleSmall, color = colors.accent, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.m))
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            ActionTile(Icons.Rounded.Person, stringResource(R.string.profile_edit_profile), onEditProfile, Modifier.weight(1f))
            ActionTile(Icons.Rounded.Group, stringResource(R.string.profile_partner_details), onPartnerDetails, Modifier.weight(1f))
            ActionTile(Icons.Rounded.CalendarMonth, stringResource(R.string.profile_add_date), onAddDate, Modifier.weight(1f), primary = true)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            StatCard(Icons.Rounded.Image, data.memoriesCount, stringResource(R.string.profile_stat_memories), onMemories, Modifier.weight(1f))
            StatCard(Icons.Rounded.StarBorder, data.milestones.size, stringResource(R.string.profile_stat_milestones), onMilestones, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            StatCard(Icons.Rounded.CalendarMonth, data.dates.size, stringResource(R.string.profile_stat_dates), onDates, Modifier.weight(1f))
            StatCard(Icons.Rounded.FavoriteBorder, data.dreamsCount, stringResource(R.string.profile_stat_dreams), onDreams, Modifier.weight(1f))
        }

        val next = ProfileMath.nextImportant(data.dates, data.anniversary, data.myBirthday, data.partnerBirthday, today)
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.profile_next_date), style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
                LinkText(stringResource(R.string.profile_view_all), onDates)
            }
            if (next == null) {
                Text(stringResource(R.string.profile_no_dates), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, modifier = Modifier.padding(top = TwinTheme.spacing.m))
            } else {
                NextDateRow(next, data.partnerName, Modifier.padding(top = TwinTheme.spacing.m))
            }
        }

        GlassCard(Modifier.fillMaxWidth(), onClick = onStory) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                ProfileChip(Icons.Rounded.MenuBook)
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.profile_our_story), style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
                        LinkText(stringResource(R.string.profile_edit), onStory)
                    }
                    Text(
                        data.myBio.ifBlank { stringResource(R.string.profile_story_hint) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Box(Modifier.size(TwinTheme.spacing.xl))
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Row(
        Modifier.clickable(role = Role.Button, onClick = onClick).padding(TwinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = TwinTheme.colors.inkMuted)
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TwinTheme.colors.inkMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ActionTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    val colors = TwinTheme.colors
    Column(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .background(if (primary) colors.neutralGlass.copy(alpha = 0.55f) else colors.glass, TwinTheme.shapes.card)
            .padding(vertical = TwinTheme.spacing.l, horizontal = TwinTheme.spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.ink, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    GlassCard(modifier, onClick = onClick, contentPadding = TwinTheme.spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            ProfileChip(icon)
            Column {
                Text(value.toString(), style = MaterialTheme.typography.titleLarge, color = colors.ink)
                Text(label, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NextDateRow(next: ImportantDate, partnerName: String, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val title = when (next.kind) {
        ImportantKind.Event -> next.title
        ImportantKind.Anniversary -> stringResource(R.string.profile_anniversary)
        ImportantKind.MyBirthday -> stringResource(R.string.profile_your_birthday)
        ImportantKind.PartnerBirthday -> stringResource(R.string.profile_partner_birthday, partnerName)
    }
    val icon = when {
        next.kind == ImportantKind.Anniversary -> Icons.Rounded.Favorite
        next.kind == ImportantKind.MyBirthday || next.kind == ImportantKind.PartnerBirthday || next.category == "Birthday" -> Icons.Rounded.Cake
        else -> Icons.Rounded.CardGiftcard
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        ProfileChip(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(formatDate(next.date), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (next.daysLeft == 0) {
                Text(stringResource(R.string.profile_today), style = MaterialTheme.typography.titleMedium, color = colors.accent)
            } else {
                Text(next.daysLeft.toString(), style = MaterialTheme.typography.titleLarge, color = colors.accent)
                Text(
                    LocalContext.current.resources.getQuantityString(R.plurals.profile_days_left, next.daysLeft),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkMuted,
                )
            }
        }
    }
}
