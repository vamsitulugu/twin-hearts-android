package com.twinhearts.app.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.twinhearts.app.BuildConfig
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.settings.domain.model.SettingsFlag
import com.twinhearts.app.feature.settings.ui.components.SettingsNote
import com.twinhearts.app.feature.settings.ui.components.SettingsRow
import com.twinhearts.app.feature.settings.ui.components.SettingsSection
import com.twinhearts.app.feature.settings.ui.components.SettingsSwitchRow
import com.twinhearts.app.feature.settings.ui.components.SettingsTimePickerDialog
import com.twinhearts.app.feature.settings.ui.components.SettingsValueRow
import com.twinhearts.app.feature.settings.ui.components.SubPageHeader
import com.twinhearts.app.feature.settings.ui.components.asSettingsText
import androidx.compose.foundation.layout.imePadding

/** Why this file exists: the smaller Settings sub-pages (web `#setSub-notifications` ... `#setSub-about`). */

@Composable
private fun SubPage(
    @StringRes title: Int,
    @StringRes subtitle: Int,
    actions: SettingsActions,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TwinTheme.spacing.screenEdge)
            .padding(bottom = TwinTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
    ) {
        SubPageHeader(stringResource(title), stringResource(subtitle), actions::onBack)
        content()
    }
}

// ───────────── Notifications ─────────────

private enum class QuietField { From, To }

@Composable
internal fun NotificationsPage(
    state: SettingsUiState,
    notification: NotificationStatus,
    actions: SettingsActions,
    launchers: SettingsLaunchers,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf<QuietField?>(null) }
    val status = when (notification) {
        NotificationStatus.Enabled -> R.string.settings_notif_enabled
        NotificationStatus.Blocked -> R.string.settings_notif_blocked
        NotificationStatus.CanAsk -> R.string.settings_notif_tap
    }
    SubPage(R.string.settings_notifications, R.string.settings_notifications_head, actions, modifier) {
        SettingsSection {
            SettingsSwitchRow(
                icon = Icons.Rounded.NotificationsNone,
                iconTint = colorResource(R.color.settings_ico_notifications),
                title = stringResource(R.string.settings_push),
                subtitle = stringResource(status),
                checked = notification == NotificationStatus.Enabled,
                onCheckedChange = { launchers.toggleNotifications() },
            )
        }
        SettingsSection(title = stringResource(R.string.settings_quiet_hours)) {
            SettingsValueRow(stringResource(R.string.settings_from), state.prefs.quietStart.asSettingsText()) { editing = QuietField.From }
            SettingsValueRow(stringResource(R.string.settings_to), state.prefs.quietEnd.asSettingsText()) { editing = QuietField.To }
        }
    }
    when (editing) {
        QuietField.From -> SettingsTimePickerDialog(state.prefs.quietStart, onDismiss = { editing = null }, onPicked = {
            actions.onQuietStart(it)
            editing = null
        })
        QuietField.To -> SettingsTimePickerDialog(state.prefs.quietEnd, onDismiss = { editing = null }, onPicked = {
            actions.onQuietEnd(it)
            editing = null
        })
        null -> Unit
    }
}

// ───────────── Appearance ─────────────

@Composable
internal fun AppearancePage(state: SettingsUiState, actions: SettingsActions, modifier: Modifier = Modifier) {
    SubPage(R.string.settings_appearance, R.string.settings_appearance_head, actions, modifier) {
        SettingsSection(title = stringResource(R.string.settings_theme)) {
            Column(modifier = Modifier.padding(TwinTheme.spacing.m)) {
                // Dark is the only theme for now (README R-54). Light and System arrive with their own task.
                SegmentedTabs(
                    options = listOf(
                        stringResource(R.string.settings_theme_dark),
                        stringResource(R.string.settings_theme_light),
                        stringResource(R.string.settings_theme_system),
                    ),
                    selectedIndex = 0,
                    onSelect = {},
                )
            }
        }
        SettingsNote(stringResource(R.string.settings_theme_note))
        SettingsSection {
            SettingsSwitchRow(
                icon = Icons.Rounded.AutoFixHigh,
                iconTint = colorResource(R.color.settings_ico_appearance),
                title = stringResource(R.string.settings_reduce_motion),
                subtitle = stringResource(R.string.settings_reduce_motion_sub),
                checked = state.prefs.reduceMotion,
                onCheckedChange = { actions.onFlag(SettingsFlag.ReduceMotion, it) },
            )
        }
    }
}

// ───────────── Chat & Media, Twin AI ─────────────

@Composable
internal fun ChatMediaPage(state: SettingsUiState, actions: SettingsActions, modifier: Modifier = Modifier) {
    SubPage(R.string.settings_chatmedia, R.string.settings_chatmedia_head, actions, modifier) {
        SettingsSection {
            SettingsSwitchRow(
                icon = Icons.Rounded.Image,
                iconTint = colorResource(R.color.settings_ico_chat),
                title = stringResource(R.string.settings_auto_save),
                subtitle = stringResource(R.string.settings_auto_save_sub),
                checked = state.prefs.autoSavePhotos,
                onCheckedChange = { actions.onFlag(SettingsFlag.AutoSavePhotos, it) },
            )
            SettingsSwitchRow(
                icon = Icons.Rounded.VolumeUp,
                iconTint = colorResource(R.color.settings_ico_chat),
                title = stringResource(R.string.settings_voice_playback),
                subtitle = stringResource(R.string.settings_voice_playback_sub),
                checked = state.prefs.autoPlayVoice,
                onCheckedChange = { actions.onFlag(SettingsFlag.AutoPlayVoice, it) },
            )
        }
    }
}

@Composable
internal fun TwinAiPage(state: SettingsUiState, actions: SettingsActions, modifier: Modifier = Modifier) {
    SubPage(R.string.settings_twin, R.string.settings_twin_head, actions, modifier) {
        SettingsSection {
            SettingsSwitchRow(
                icon = Icons.Rounded.AutoAwesome,
                iconTint = colorResource(R.color.settings_ico_twin),
                title = stringResource(R.string.settings_ai_context),
                subtitle = stringResource(R.string.settings_ai_context_sub),
                checked = state.prefs.aiUseContext,
                onCheckedChange = { actions.onFlag(SettingsFlag.AiUseContext, it) },
            )
        }
    }
}

// ───────────── Privacy & Security ─────────────

@Composable
internal fun PrivacyPage(state: SettingsUiState, actions: SettingsActions, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val pin = state.pin
    SubPage(R.string.settings_privacy, R.string.settings_privacy_head, actions, modifier) {
        SettingsSection {
            SettingsSwitchRow(
                icon = Icons.Rounded.Lock,
                iconTint = colorResource(R.color.settings_ico_privacy),
                title = stringResource(R.string.settings_app_lock),
                subtitle = stringResource(R.string.settings_app_lock_sub),
                checked = state.prefs.appLock,
                onCheckedChange = { actions.onFlag(SettingsFlag.AppLock, it) },
            )
        }
        SettingsSection(title = stringResource(R.string.settings_change_pin)) {
            Column(
                modifier = Modifier.padding(TwinTheme.spacing.l),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                TwinTextField(
                    value = pin.current,
                    onValueChange = actions::onCurrentPin,
                    label = stringResource(R.string.settings_current_pin),
                    placeholder = stringResource(R.string.settings_pin_dots),
                    isPassword = true,
                    keyboardType = KeyboardType.NumberPassword,
                    maxLength = 4,
                )
                TwinTextField(
                    value = pin.new,
                    onValueChange = actions::onNewPin,
                    label = stringResource(R.string.settings_new_pin),
                    placeholder = stringResource(R.string.settings_pin_four_digits),
                    isPassword = true,
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                    onImeAction = actions::onUpdatePin,
                    maxLength = 4,
                )
                GlassButton(
                    text = stringResource(R.string.settings_update_pin),
                    onClick = actions::onUpdatePin,
                    style = GlassButtonStyle.Secondary,
                    loading = pin.result == PinResult.Working,
                    modifier = Modifier.fillMaxWidth(),
                )
                val resultText = when (val result = pin.result) {
                    PinResult.None, PinResult.Working -> null
                    PinResult.Success -> stringResource(R.string.settings_pin_updated)
                    is PinResult.Failure -> stringResource(R.string.settings_pin_failed, result.error.settingsText())
                }
                if (resultText != null) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (pin.result == PinResult.Success) colors.success else colors.error,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ───────────── App Info ─────────────

@Composable
internal fun AboutPage(actions: SettingsActions, launchers: SettingsLaunchers, modifier: Modifier = Modifier) {
    SubPage(R.string.settings_about, R.string.settings_about_head, actions, modifier) {
        SettingsSection {
            SettingsRow(
                icon = Icons.Rounded.Info,
                iconTint = colorResource(R.color.settings_ico_logout),
                title = stringResource(R.string.settings_app_name),
                subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                showChevron = false,
            )
            SettingsRow(
                icon = Icons.Rounded.ChatBubbleOutline,
                iconTint = colorResource(R.color.settings_ico_partner),
                title = stringResource(R.string.settings_help),
                subtitle = stringResource(R.string.settings_help_about_sub),
                onClick = launchers.openHelp,
            )
            SettingsRow(
                icon = Icons.Rounded.VerifiedUser,
                iconTint = colorResource(R.color.settings_ico_privacy),
                title = stringResource(R.string.settings_privacy_policy),
                subtitle = stringResource(R.string.settings_privacy_policy_sub),
                onClick = launchers.openPrivacyPolicy,
            )
        }
    }
}
