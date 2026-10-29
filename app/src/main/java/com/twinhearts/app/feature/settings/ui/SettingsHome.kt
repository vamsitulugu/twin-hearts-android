package com.twinhearts.app.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.design.CoupleAvatars
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.settings.domain.daysTogether
import com.twinhearts.app.feature.settings.domain.matchesSearch
import com.twinhearts.app.feature.settings.domain.model.PartnerStatus
import com.twinhearts.app.feature.settings.domain.model.ProfileInfo
import com.twinhearts.app.feature.settings.domain.model.SettingsPage
import com.twinhearts.app.feature.settings.ui.components.SettingsRow
import com.twinhearts.app.feature.settings.ui.components.SettingsSection
import com.twinhearts.app.feature.settings.ui.components.asSettingsText
import java.time.LocalDate

/** Why this file exists: the Settings home list: couple header, search box and the grouped rows (web `#setHome`). */

/** What the phone says about notifications. The home list and the Notifications page both show it. */
enum class NotificationStatus { Enabled, CanAsk, Blocked }

/** Actions that need the phone itself (pickers, system screens). Previews leave them empty. */
class SettingsLaunchers(
    val pickPhoto: () -> Unit = {},
    val createBackupFile: () -> Unit = {},
    val chooseBackupFile: () -> Unit = {},
    val choosePhotoFolder: () -> Unit = {},
    val toggleNotifications: () -> Unit = {},
    val openPrivacyPolicy: () -> Unit = {},
    val openHelp: () -> Unit = {},
)

@StringRes
internal fun pageTitle(page: SettingsPage): Int = when (page) {
    SettingsPage.Profile -> R.string.settings_profile
    SettingsPage.Partner -> R.string.settings_partner
    SettingsPage.Notifications -> R.string.settings_notifications
    SettingsPage.Appearance -> R.string.settings_appearance
    SettingsPage.ChatMedia -> R.string.settings_chatmedia
    SettingsPage.TwinAi -> R.string.settings_twin
    SettingsPage.Privacy -> R.string.settings_privacy
    SettingsPage.Storage -> R.string.settings_storage
    SettingsPage.About -> R.string.settings_about
}

private class SearchEntry(@StringRes val title: Int, @StringRes val keywords: Int, val page: SettingsPage)

/** Same 24 search entries as the web's INDEX. */
private val searchIndex = listOf(
    SearchEntry(R.string.settings_profile, R.string.settings_kw_profile, SettingsPage.Profile),
    SearchEntry(R.string.settings_together_since, R.string.settings_kw_together, SettingsPage.Profile),
    SearchEntry(R.string.settings_birthdays, R.string.settings_kw_birthdays, SettingsPage.Profile),
    SearchEntry(R.string.settings_partner, R.string.settings_kw_partner, SettingsPage.Partner),
    SearchEntry(R.string.settings_remove_partner, R.string.settings_kw_remove, SettingsPage.Partner),
    SearchEntry(R.string.settings_requests_received, R.string.settings_kw_requests, SettingsPage.Partner),
    SearchEntry(R.string.settings_notifications, R.string.settings_kw_notifications, SettingsPage.Notifications),
    SearchEntry(R.string.settings_quiet_hours, R.string.settings_kw_quiet, SettingsPage.Notifications),
    SearchEntry(R.string.settings_appearance, R.string.settings_kw_appearance, SettingsPage.Appearance),
    SearchEntry(R.string.settings_reduce_motion, R.string.settings_kw_motion, SettingsPage.Appearance),
    SearchEntry(R.string.settings_chatmedia, R.string.settings_kw_chatmedia, SettingsPage.ChatMedia),
    SearchEntry(R.string.settings_auto_save, R.string.settings_kw_autosave, SettingsPage.ChatMedia),
    SearchEntry(R.string.settings_voice_playback, R.string.settings_kw_voice, SettingsPage.ChatMedia),
    SearchEntry(R.string.settings_twin, R.string.settings_kw_twin, SettingsPage.TwinAi),
    SearchEntry(R.string.settings_ai_context, R.string.settings_kw_context, SettingsPage.TwinAi),
    SearchEntry(R.string.settings_privacy, R.string.settings_kw_privacy, SettingsPage.Privacy),
    SearchEntry(R.string.settings_app_lock, R.string.settings_kw_lock, SettingsPage.Privacy),
    SearchEntry(R.string.settings_change_pin, R.string.settings_kw_pin, SettingsPage.Privacy),
    SearchEntry(R.string.settings_storage, R.string.settings_kw_storage, SettingsPage.Storage),
    SearchEntry(R.string.settings_export_backup, R.string.settings_kw_export, SettingsPage.Storage),
    SearchEntry(R.string.settings_import_backup, R.string.settings_kw_import, SettingsPage.Storage),
    SearchEntry(R.string.settings_cloud_sync, R.string.settings_kw_cloud, SettingsPage.Storage),
    SearchEntry(R.string.settings_about, R.string.settings_kw_about, SettingsPage.About),
    SearchEntry(R.string.settings_privacy_policy, R.string.settings_kw_legal, SettingsPage.About),
)

@Composable
internal fun SettingsHome(
    state: SettingsUiState,
    notification: NotificationStatus,
    actions: SettingsActions,
    launchers: SettingsLaunchers,
    onAskLogOut: () -> Unit,
    searchFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TwinTheme.spacing.screenEdge)
            .padding(bottom = TwinTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
    ) {
        CoupleHeader(state.profile, onOpen = { actions.onOpenPage(SettingsPage.Profile) }, onRetry = actions::onRetryProfile)
        SearchField(state.query, actions::onQuery, searchFocus)
        if (state.query.isBlank()) {
            SettingsSections(state, notification, actions, launchers, onAskLogOut)
        } else {
            SearchResults(state.query, actions::onOpenPage)
        }
    }
}

@Composable
private fun CoupleHeader(profile: UiState<ProfileInfo>, onOpen: () -> Unit, onRetry: () -> Unit) {
    val colors = TwinTheme.colors
    when (profile) {
        UiState.Loading, UiState.Empty -> GlassCard(modifier = Modifier.fillMaxWidth()) { LoadingSkeleton(lines = 2) }
        is UiState.Error -> GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onRetry) {
            Text(profile.error.settingsText(), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
            Text(
                stringResource(R.string.core_retry),
                style = MaterialTheme.typography.titleSmall,
                color = colors.accent,
                modifier = Modifier.padding(top = TwinTheme.spacing.xs),
            )
        }
        is UiState.Content -> {
            val info = profile.data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TwinTheme.shapes.card)
                    .clickable(role = Role.Button, onClick = onOpen)
                    .padding(vertical = TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
            ) {
                CoupleAvatars(
                    firstName = info.myName,
                    secondName = info.partnerName,
                    size = HeroAvatarSize,
                    firstImageUrl = info.myAvatarUrl,
                    secondImageUrl = info.partnerAvatarUrl,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${info.myName} & ${info.partnerName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                        maxLines = 1,
                    )
                    Text(heroSubtitle(info.anniversary), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.inkFaint)
            }
        }
    }
}

private val HeroAvatarSize = 58.dp

@Composable
private fun heroSubtitle(anniversary: LocalDate?): String {
    if (anniversary == null) return stringResource(R.string.settings_hero_add_anniversary)
    val days = daysTogether(anniversary, LocalDate.now())
    return stringResource(
        R.string.settings_hero_together_since,
        anniversary.asSettingsText(),
        pluralStringResource(R.plurals.settings_days, days, days),
    )
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, focus: FocusRequester) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    BasicTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions.Default,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TwinTheme.shapes.fieldHeight)
                    .clip(shape)
                    .background(colors.glass)
                    .border(1.dp, colors.line, shape)
                    .padding(horizontal = TwinTheme.spacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.inkFaint, modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(stringResource(R.string.settings_search_hint), style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                    }
                    inner()
                }
            }
        },
    )
}

@Composable
private fun SearchResults(query: String, onOpen: (SettingsPage) -> Unit) {
    val colors = TwinTheme.colors
    val hits = searchIndex.filter { matchesSearch(query, stringResource(it.title), stringResource(it.keywords)) }
    if (hits.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_no_results),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.inkFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TwinTheme.spacing.xxl),
        )
        return
    }
    SettingsSection {
        hits.forEach { entry ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onOpen(entry.page) }
                    .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
            ) {
                Text(stringResource(entry.title), style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                Text(stringResource(pageTitle(entry.page)), style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
            }
        }
    }
}

@Composable
private fun SettingsSections(
    state: SettingsUiState,
    notification: NotificationStatus,
    actions: SettingsActions,
    launchers: SettingsLaunchers,
    onAskLogOut: () -> Unit,
) {
    val partnerValue = when ((state.partner.overview as? UiState.Content)?.data?.status) {
        is PartnerStatus.Connected -> R.string.settings_val_connected
        is PartnerStatus.Waiting -> R.string.settings_val_pending
        else -> R.string.settings_val_not_connected
    }
    val notificationValue = if (notification == NotificationStatus.Enabled) R.string.settings_val_on else R.string.settings_val_off

    SettingsSection(title = stringResource(R.string.settings_sec_account)) {
        PageRow(Icons.Rounded.PersonOutline, R.color.settings_ico_profile, SettingsPage.Profile, R.string.settings_profile_sub, actions)
        PageRow(Icons.Rounded.FavoriteBorder, R.color.settings_ico_partner, SettingsPage.Partner, R.string.settings_partner_sub, actions, valueRes = partnerValue)
    }
    SettingsSection(title = stringResource(R.string.settings_sec_preferences)) {
        PageRow(Icons.Rounded.NotificationsNone, R.color.settings_ico_notifications, SettingsPage.Notifications, R.string.settings_notifications_sub, actions, valueRes = notificationValue)
        PageRow(Icons.Rounded.Palette, R.color.settings_ico_appearance, SettingsPage.Appearance, R.string.settings_appearance_sub, actions, valueRes = R.string.settings_val_dark)
        PageRow(Icons.Rounded.ChatBubbleOutline, R.color.settings_ico_chat, SettingsPage.ChatMedia, R.string.settings_chatmedia_sub, actions)
        PageRow(Icons.Rounded.AutoAwesome, R.color.settings_ico_twin, SettingsPage.TwinAi, R.string.settings_twin_sub, actions)
    }
    SettingsSection(title = stringResource(R.string.settings_sec_security)) {
        PageRow(Icons.Rounded.VerifiedUser, R.color.settings_ico_privacy, SettingsPage.Privacy, R.string.settings_privacy_sub, actions)
    }
    SettingsSection(title = stringResource(R.string.settings_sec_data)) {
        PageRow(Icons.Rounded.Storage, R.color.settings_ico_storage, SettingsPage.Storage, R.string.settings_storage_sub, actions)
    }
    SettingsSection(title = stringResource(R.string.settings_sec_support)) {
        SettingsRow(
            icon = Icons.Rounded.ChatBubbleOutline,
            iconTint = colorResource(R.color.settings_ico_partner),
            title = stringResource(R.string.settings_help),
            subtitle = stringResource(R.string.settings_help_sub),
            onClick = launchers.openHelp,
        )
    }
    SettingsSection(title = stringResource(R.string.settings_sec_about)) {
        PageRow(Icons.Rounded.Info, R.color.settings_ico_about, SettingsPage.About, R.string.settings_about_sub, actions)
    }
    SettingsSection {
        SettingsRow(
            icon = Icons.AutoMirrored.Rounded.ExitToApp,
            iconTint = colorResource(R.color.settings_ico_logout),
            title = stringResource(R.string.settings_logout),
            subtitle = stringResource(R.string.settings_logout_sub),
            danger = true,
            showChevron = false,
            onClick = onAskLogOut,
        )
    }
}

@Composable
private fun PageRow(
    icon: ImageVector,
    @androidx.annotation.ColorRes tint: Int,
    page: SettingsPage,
    @StringRes subtitle: Int,
    actions: SettingsActions,
    @StringRes valueRes: Int? = null,
) {
    SettingsRow(
        icon = icon,
        iconTint = colorResource(tint),
        title = stringResource(pageTitle(page)),
        subtitle = stringResource(subtitle),
        value = valueRes?.let { stringResource(it) },
        onClick = { actions.onOpenPage(page) },
    )
}

/** Turns an [AppError] into the text the person reads (the Composable twin of [settingsMessageText]). */
@Composable
internal fun AppError.settingsText(): String = when (this) {
    AppError.Network -> stringResource(R.string.core_error_network)
    is AppError.Unauthorized -> message ?: stringResource(R.string.core_error_unauthorized)
    is AppError.Server -> message ?: stringResource(R.string.core_error_server)
    AppError.Unknown -> stringResource(R.string.core_error_unknown)
}
