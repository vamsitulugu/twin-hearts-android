package com.twinhearts.app.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Message
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.SectionHeader
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.profile.domain.ProfileMath
import com.twinhearts.app.feature.profile.domain.model.ConnectionState
import com.twinhearts.app.feature.profile.domain.model.LoveLanguage
import com.twinhearts.app.feature.profile.domain.model.LoveTip
import com.twinhearts.app.feature.profile.domain.model.ProfileData
import com.twinhearts.app.feature.profile.ui.components.ProfileAvatar
import com.twinhearts.app.feature.profile.ui.components.ProfileChip
import com.twinhearts.app.feature.profile.ui.components.ProfileTextArea
import com.twinhearts.app.feature.profile.ui.components.formatDate
import com.twinhearts.app.feature.profile.ui.components.relativeTime

/** One row of a list: icon chip, title, small text and (optionally) an arrow. Not tappable when [onClick] is null. */
@Composable
private fun ListRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    danger: Boolean = false,
) {
    val colors = TwinTheme.colors
    val tap = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier.fillMaxWidth().then(tap).padding(vertical = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        ProfileChip(icon, tint = if (danger) colors.error else colors.inkMuted)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (danger) colors.error else colors.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 3)
        }
        if (onClick != null) Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
    }
}

/** "Partner Details": photo, name, connection state, details list and the connect / remove actions. */
@Composable
fun PartnerPage(
    data: ProfileData,
    nowMs: Long,
    busy: Boolean,
    onViewAvatar: () -> Unit,
    onMessage: () -> Unit,
    onEditDetails: () -> Unit,
    onLoveLanguage: () -> Unit,
    onSendRequest: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    var phone by rememberSaveable { mutableStateOf("") }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = TwinTheme.spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                ProfileAvatar(data.partnerAvatarUrl, stringResource(R.string.profile_view_partner_photo), onViewAvatar.takeIf { data.partnerAvatarUrl != null })
                Text(data.partnerName, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                val pill = when (data.connection) {
                    ConnectionState.Connected -> R.string.profile_connected
                    ConnectionState.Waiting -> R.string.profile_waiting_partner
                    ConnectionState.NotConnected -> R.string.profile_not_connected
                }
                Text(
                    stringResource(pill),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (data.paired) colors.success else colors.inkMuted,
                    modifier = Modifier.background(colors.glass, TwinTheme.shapes.pill).padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.padding(top = TwinTheme.spacing.s)) {
                    if (data.paired) {
                        GlassButton(stringResource(R.string.profile_message), onMessage, Modifier.weight(1f), leadingIcon = Icons.Rounded.Message)
                    }
                    GlassButton(stringResource(R.string.profile_edit_details), onEditDetails, Modifier.weight(1f), style = GlassButtonStyle.Secondary, leadingIcon = Icons.Rounded.Edit)
                }
            }
        }

        GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
            ListRow(Icons.Rounded.Cake, stringResource(R.string.profile_birthday), data.partnerBirthday?.let(::formatDate) ?: stringResource(R.string.profile_not_added), onClick = onEditDetails)
            data.partnerBirthday?.let { ListRow(Icons.Rounded.Star, stringResource(R.string.profile_zodiac), ProfileMath.zodiac(it)) }
            ListRow(Icons.Rounded.Favorite, stringResource(R.string.profile_love_language), data.partnerLoveLanguage?.let { languageLabel(it) } ?: stringResource(R.string.profile_not_chosen), onClick = onLoveLanguage)
            ListRow(Icons.Rounded.MenuBook, stringResource(R.string.profile_about_partner, data.partnerName), data.partnerBio.ifBlank { stringResource(R.string.profile_add_few_words) }, onClick = onEditDetails)
            val seen = data.partnerSeenAtMs
            if (data.paired && seen != null) ListRow(Icons.Rounded.Schedule, stringResource(R.string.profile_last_active), relativeTime(nowMs, seen))
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.profile_partner_connection), style = MaterialTheme.typography.titleMedium, color = colors.ink)
            when (data.connection) {
                ConnectionState.NotConnected -> {
                    Text(stringResource(R.string.profile_find_by_phone), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, modifier = Modifier.padding(vertical = TwinTheme.spacing.s))
                    TwinTextField(phone, { phone = it.filter(Char::isDigit) }, stringResource(R.string.profile_partner_phone), placeholder = stringResource(R.string.profile_phone_placeholder), keyboardType = KeyboardType.Phone, maxLength = 10)
                    GlassButton(stringResource(R.string.profile_send_request), { onSendRequest(phone) }, Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.m), enabled = phone.length == 10, loading = busy)
                    Text(stringResource(R.string.profile_not_connected_status), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, modifier = Modifier.padding(top = TwinTheme.spacing.s))
                }
                ConnectionState.Waiting ->
                    Text(stringResource(R.string.profile_waiting_status), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, modifier = Modifier.padding(top = TwinTheme.spacing.s))
                ConnectionState.Connected ->
                    Text(stringResource(R.string.profile_connected_with, data.partnerName), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, modifier = Modifier.padding(top = TwinTheme.spacing.s))
            }
        }

        if (data.paired) {
            GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
                ListRow(Icons.Rounded.PersonOff, stringResource(R.string.profile_remove_partner), stringResource(R.string.profile_remove_partner_hint), onClick = onRemove, danger = true)
            }
        }
        Spacer(Modifier.size(TwinTheme.spacing.xl))
    }
}

/** "Profile & Settings": the grouped list. Rows that belong to other screens just open them. */
@Composable
fun SettingsPage(
    onEditProfile: () -> Unit,
    onPartner: () -> Unit,
    onSpecialDates: () -> Unit,
    onMilestones: () -> Unit,
    onMemories: () -> Unit,
    onStory: () -> Unit,
    onLove: () -> Unit,
    onGoals: () -> Unit,
    onAppSettings: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = TwinTheme.spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        SectionHeader(stringResource(R.string.profile_group_profile))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
            ListRow(Icons.Rounded.Person, stringResource(R.string.profile_personal_profile), stringResource(R.string.profile_personal_profile_hint), onClick = onEditProfile)
            ListRow(Icons.Rounded.Group, stringResource(R.string.profile_partner_connection), stringResource(R.string.profile_partner_connection_hint), onClick = onPartner)
        }
        SectionHeader(stringResource(R.string.profile_group_relationship))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
            ListRow(Icons.Rounded.CalendarMonth, stringResource(R.string.profile_special_dates), stringResource(R.string.profile_special_dates_hint), onClick = onSpecialDates)
            ListRow(Icons.Rounded.StarBorder, stringResource(R.string.profile_stat_milestones), stringResource(R.string.profile_milestones_hint), onClick = onMilestones)
            ListRow(Icons.Rounded.Image, stringResource(R.string.profile_stat_memories), stringResource(R.string.profile_memories_hint), onClick = onMemories)
            ListRow(Icons.Rounded.MenuBook, stringResource(R.string.profile_our_story), stringResource(R.string.profile_story_row_hint), onClick = onStory)
            ListRow(Icons.Rounded.Favorite, stringResource(R.string.profile_love_languages), stringResource(R.string.profile_love_languages_hint), onClick = onLove)
            ListRow(Icons.Rounded.Flag, stringResource(R.string.profile_goals), stringResource(R.string.profile_goals_hint), onClick = onGoals)
        }
        SectionHeader(stringResource(R.string.profile_group_app))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
            ListRow(Icons.Rounded.Notifications, stringResource(R.string.profile_notifications), stringResource(R.string.profile_notifications_hint), onClick = onAppSettings)
            ListRow(Icons.Rounded.Lock, stringResource(R.string.profile_privacy), stringResource(R.string.profile_privacy_hint), onClick = onAppSettings)
            ListRow(Icons.Rounded.Cloud, stringResource(R.string.profile_backup), stringResource(R.string.profile_backup_hint), onClick = onAppSettings)
            ListRow(Icons.Rounded.Palette, stringResource(R.string.profile_theme), stringResource(R.string.profile_theme_hint), onClick = onAppSettings)
            ListRow(Icons.Rounded.Download, stringResource(R.string.profile_export), stringResource(R.string.profile_export_hint), onClick = onExport)
        }
        Spacer(Modifier.size(TwinTheme.spacing.xl))
    }
}

/** "Milestones": a timeline grouped by year, newest first, with delete and an Add button. */
@Composable
fun MilestonesPage(
    data: ProfileData,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = TwinTheme.spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        GlassButton(stringResource(R.string.profile_add_milestone), onAdd, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Add)
        val all = data.milestones.sortedByDescending { it.date }
        if (all.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.profile_milestones_empty_title),
                message = stringResource(R.string.profile_milestones_empty_message),
                icon = Icons.Rounded.Favorite,
                actionLabel = stringResource(R.string.profile_add_milestone),
                onAction = onAdd,
            )
        } else {
            var year = -1
            all.forEach { m ->
                if (m.date.year != year) {
                    year = m.date.year
                    Text(year.toString(), style = MaterialTheme.typography.titleMedium, color = colors.accent, modifier = Modifier.padding(top = TwinTheme.spacing.s))
                }
                GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                        Text(m.emoji, style = MaterialTheme.typography.headlineSmall)
                        Column(Modifier.weight(1f)) {
                            Text(m.title, style = MaterialTheme.typography.titleSmall, color = colors.ink)
                            Text(formatDate(m.date), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                            if (m.note.isNotBlank()) Text(m.note, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.padding(top = TwinTheme.spacing.xs))
                        }
                        androidx.compose.material3.IconButton(onClick = { onDelete(m.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.profile_delete_milestone), tint = colors.inkMuted)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.size(TwinTheme.spacing.xl))
    }
}

@Composable
internal fun languageLabel(language: LoveLanguage): String = stringResource(
    when (language) {
        LoveLanguage.WordsOfAffirmation -> R.string.profile_ll_words
        LoveLanguage.QualityTime -> R.string.profile_ll_time
        LoveLanguage.ActsOfService -> R.string.profile_ll_acts
        LoveLanguage.PhysicalTouch -> R.string.profile_ll_touch
        LoveLanguage.ReceivingGifts -> R.string.profile_ll_gifts
    },
)

/** "Love Languages": pick one for you and one for your partner; tap again to clear. */
@Composable
fun LovePage(
    data: ProfileData,
    onPick: (mine: Boolean, language: LoveLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = TwinTheme.spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        LanguageGroup(stringResource(R.string.profile_your_love_language), data.myLoveLanguage) { onPick(true, it) }
        LanguageGroup(stringResource(R.string.profile_partner_love_language, data.partnerName), data.partnerLoveLanguage) { onPick(false, it) }
        val mine = data.myLoveLanguage
        val theirs = data.partnerLoveLanguage
        if (mine != null && theirs != null) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                    Text("💡", style = MaterialTheme.typography.titleMedium)
                    Text(tipText(ProfileMath.loveTip(mine, theirs)), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                }
            }
        }
        Spacer(Modifier.size(TwinTheme.spacing.xl))
    }
}

@Composable
private fun tipText(tip: LoveTip): String = stringResource(
    when (tip) {
        LoveTip.WordsAndQuality -> R.string.profile_tip_words_quality
        LoveTip.WordsAndActs -> R.string.profile_tip_words_acts
        LoveTip.QualityAndTouch -> R.string.profile_tip_quality_touch
        LoveTip.QualityAndWords -> R.string.profile_tip_quality_words
        LoveTip.ActsAndGifts -> R.string.profile_tip_acts_gifts
        LoveTip.TouchAndGifts -> R.string.profile_tip_touch_gifts
        LoveTip.Default -> R.string.profile_tip_default
    },
)

@Composable
private fun LanguageGroup(title: String, current: LoveLanguage?, onPick: (LoveLanguage) -> Unit) {
    val colors = TwinTheme.colors
    SectionHeader(title)
    GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.s) {
        LoveLanguage.entries.forEach { language ->
            val selected = language == current
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { onPick(language) }.padding(TwinTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                Text(language.emoji, style = MaterialTheme.typography.titleMedium)
                Text(languageLabel(language), style = MaterialTheme.typography.bodyLarge, color = colors.ink, modifier = Modifier.weight(1f))
                androidx.compose.material3.RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = colors.accent, unselectedColor = colors.inkMuted),
                )
            }
        }
    }
}

/** "Our Story": two text areas (about you, about your partner) and Save. */
@Composable
fun StoryPage(
    data: ProfileData,
    busy: Boolean,
    onSave: (myBio: String, partnerBio: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mine by rememberSaveable(data.myBio) { mutableStateOf(data.myBio) }
    var theirs by rememberSaveable(data.partnerBio) { mutableStateOf(data.partnerBio) }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = TwinTheme.spacing.screenEdge), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        ProfileTextArea(mine, { mine = it }, stringResource(R.string.profile_about_you), placeholder = stringResource(R.string.profile_write_sweet))
        ProfileTextArea(theirs, { theirs = it }, stringResource(R.string.profile_about_partner, data.partnerName), placeholder = stringResource(R.string.profile_about_partner_hint))
        GlassButton(stringResource(R.string.profile_save), { onSave(mine, theirs) }, Modifier.fillMaxWidth(), loading = busy)
        Spacer(Modifier.size(TwinTheme.spacing.xl))
    }
}
