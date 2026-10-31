package com.twinhearts.app.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.notifications.NotificationDefaults
import com.twinhearts.app.core.notifications.NotificationPrefs
import com.twinhearts.app.core.notifications.NotificationType
import com.twinhearts.app.core.notifications.PushTokenRegistrar
import com.twinhearts.app.core.notifications.TwinChannel
import com.twinhearts.app.feature.settings.ui.components.SettingsNote
import com.twinhearts.app.feature.settings.ui.components.SettingsSection
import com.twinhearts.app.feature.settings.ui.components.SettingsSwitchRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings > Notifications, second half: the master switch, the lock-screen preview switch and one
 * switch for every kind of notification the app can send (grouped like the phone's channels).
 * It talks to [NotificationPrefs] itself, so the rest of Settings stays unchanged.
 */
@HiltViewModel
class NotificationPrefsViewModel @Inject constructor(
    private val prefs: NotificationPrefs,
    registrar: PushTokenRegistrar,
) : ViewModel() {
    val master: StateFlow<Boolean> = prefs.masterOn.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val preview: StateFlow<Boolean> = prefs.showPreview.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val enabled: StateFlow<Map<NotificationType, Boolean>> =
        prefs.enabledMap.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val pushConfigured: Boolean = registrar.isConfigured

    fun setMaster(on: Boolean) = viewModelScope.launch { prefs.setMaster(on) }
    fun setPreview(on: Boolean) = viewModelScope.launch { prefs.setPreview(on) }
    fun setType(type: NotificationType, on: Boolean) = viewModelScope.launch { prefs.setType(type, on) }
}

@Composable
internal fun NotificationTypesSection(vm: NotificationPrefsViewModel = hiltViewModel()) {
    val master by vm.master.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val enabled by vm.enabled.collectAsStateWithLifecycle()
    val tint = colorResource(R.color.settings_ico_notifications)

    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        if (!vm.pushConfigured) SettingsNote(stringResource(R.string.notif_settings_push_off))
        SettingsSection {
            SettingsSwitchRow(
                icon = if (master) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                iconTint = tint,
                title = stringResource(R.string.notif_settings_master),
                subtitle = stringResource(R.string.notif_settings_master_sub),
                checked = master,
                onCheckedChange = vm::setMaster,
            )
            SettingsSwitchRow(
                icon = if (preview) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                iconTint = tint,
                title = stringResource(R.string.notif_settings_preview),
                subtitle = stringResource(R.string.notif_settings_preview_sub),
                checked = preview,
                onCheckedChange = vm::setPreview,
            )
        }
        SettingsNote(stringResource(R.string.notif_settings_quiet_note))
        TwinChannel.entries.forEach { channel ->
            val types = NotificationType.entries.filter { it.channel == channel }
            SettingsSection(title = stringResource(channel.title)) {
                types.forEach { type ->
                    SettingsSwitchRow(
                        icon = channelIcon(channel),
                        iconTint = tint,
                        title = stringResource(type.label),
                        // An example of how it looks, e.g. "Your partner sent you a hug 🤗".
                        subtitle = stringResource(NotificationDefaults.title(type), stringResource(R.string.notif_partner_fallback)),
                        checked = enabled[type] ?: type.defaultOn,
                        onCheckedChange = { vm.setType(type, it) },
                    )
                }
            }
        }
    }
}

private fun channelIcon(channel: TwinChannel) = when (channel) {
    TwinChannel.MESSAGES -> Icons.Rounded.ChatBubbleOutline
    TwinChannel.CALLS -> Icons.Rounded.Call
    TwinChannel.LOVE -> Icons.Rounded.Favorite
    TwinChannel.LOCATION -> Icons.Rounded.LocationOn
    TwinChannel.REMINDERS -> Icons.Rounded.Alarm
    TwinChannel.ACTIVITY -> Icons.Rounded.Bolt
    TwinChannel.UPDATES -> Icons.Rounded.Update
}
