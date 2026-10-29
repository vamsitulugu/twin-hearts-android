package com.twinhearts.app.feature.settings.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.settings.domain.model.AppPrefs
import com.twinhearts.app.feature.settings.domain.model.IncomingRequest
import com.twinhearts.app.feature.settings.domain.model.PartnerOverview
import com.twinhearts.app.feature.settings.domain.model.PartnerStatus
import com.twinhearts.app.feature.settings.domain.model.ProfileGender
import com.twinhearts.app.feature.settings.domain.model.ProfileInfo
import com.twinhearts.app.feature.settings.domain.model.SettingsFlag
import com.twinhearts.app.feature.settings.domain.model.SettingsPage
import com.twinhearts.app.feature.settings.domain.model.StorageKind
import com.twinhearts.app.feature.settings.domain.model.StorageSummary
import com.twinhearts.app.feature.settings.domain.model.StorageUsage
import java.time.LocalDate
import java.time.LocalTime

/** Why this file exists: Android Studio previews of every Settings page and state (R-57). */

private object NoActions : SettingsActions {
    override fun onQuery(query: String) = Unit
    override fun onOpenPage(page: SettingsPage) = Unit
    override fun onBack() = Unit
    override fun onSync() = Unit
    override fun onRetryProfile() = Unit
    override fun onLogOut() = Unit
    override fun onMyName(text: String) = Unit
    override fun onPartnerName(text: String) = Unit
    override fun onGender(gender: ProfileGender) = Unit
    override fun onAnniversary(date: LocalDate?) = Unit
    override fun onMyBirthday(date: LocalDate?) = Unit
    override fun onPartnerBirthday(date: LocalDate?) = Unit
    override fun onSaveProfile() = Unit
    override fun onAvatarPicked(uri: Uri) = Unit
    override fun onPhone(text: String) = Unit
    override fun onSendRequest() = Unit
    override fun onCancelRequest(requestId: String) = Unit
    override fun onAccept(requestId: String) = Unit
    override fun onDecline(requestId: String) = Unit
    override fun onRemovePartner() = Unit
    override fun onRetryPartner() = Unit
    override fun onFlag(flag: SettingsFlag, on: Boolean) = Unit
    override fun onQuietStart(time: LocalTime) = Unit
    override fun onQuietEnd(time: LocalTime) = Unit
    override fun onCurrentPin(text: String) = Unit
    override fun onNewPin(text: String) = Unit
    override fun onUpdatePin() = Unit
    override fun onRetryStorage() = Unit
    override fun onClearCache() = Unit
    override fun onExportBackup(target: Uri) = Unit
    override fun onImportBackup(source: Uri) = Unit
    override fun onExportPhotos(folder: Uri) = Unit
}

private val sampleInfo = ProfileInfo(
    myName = "Vamsi",
    partnerName = "Likitha",
    gender = ProfileGender.Male,
    anniversary = LocalDate.of(2025, 10, 12),
    myBirthday = LocalDate.of(1999, 3, 4),
    partnerBirthday = null,
    myAvatarUrl = null,
    partnerAvatarUrl = null,
)

private val sampleForm = ProfileForm.from(sampleInfo)

private val sampleStorage = StorageSummary(
    categories = listOf(
        StorageUsage(StorageKind.Photos, 48_000_000),
        StorageUsage(StorageKind.Voice, 0),
        StorageUsage(StorageKind.Chat, 0),
        StorageUsage(StorageKind.Notes, 12_400),
        StorageUsage(StorageKind.AppData, 180_000),
    ),
    totalBytes = 48_192_400,
    capacityBytes = 500L * 1024 * 1024,
)

private val sampleState = SettingsUiState(
    profile = UiState.Content(sampleInfo),
    form = sampleForm,
    partner = PartnerUiState(overview = UiState.Content(PartnerOverview(PartnerStatus.Connected("Likitha"), emptyList()))),
    storage = UiState.Content(sampleStorage),
)

@Composable
private fun Host(content: @Composable () -> Unit) {
    TwinTheme { TwinBackground { content() } }
}

private const val BG = 0xFF14030E

@Preview(showBackground = true, backgroundColor = BG, heightDp = 900, widthDp = 360)
@Composable
private fun HomePreview() = Host {
    SettingsHome(sampleState, NotificationStatus.Enabled, NoActions, SettingsLaunchers(), {}, FocusRequester())
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 900, widthDp = 360, fontScale = 1.3f)
@Composable
private fun HomeLargeFontPreview() = Host {
    SettingsHome(sampleState, NotificationStatus.Blocked, NoActions, SettingsLaunchers(), {}, FocusRequester())
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 500, widthDp = 360)
@Composable
private fun HomeLoadingPreview() = Host {
    SettingsHome(SettingsUiState(), NotificationStatus.CanAsk, NoActions, SettingsLaunchers(), {}, FocusRequester())
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 500, widthDp = 360)
@Composable
private fun HomeErrorPreview() = Host {
    SettingsHome(SettingsUiState(profile = UiState.Error(AppError.Network)), NotificationStatus.CanAsk, NoActions, SettingsLaunchers(), {}, FocusRequester())
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 500, widthDp = 360)
@Composable
private fun SearchPreview() = Host {
    SettingsHome(sampleState.copy(query = "pin"), NotificationStatus.Enabled, NoActions, SettingsLaunchers(), {}, FocusRequester())
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 900, widthDp = 360)
@Composable
private fun ProfilePreview() = Host { ProfilePage(sampleState, NoActions, SettingsLaunchers()) }

@Preview(showBackground = true, backgroundColor = BG, heightDp = 400, widthDp = 360)
@Composable
private fun ProfileLoadingPreview() = Host { ProfilePage(SettingsUiState(), NoActions, SettingsLaunchers()) }

@Preview(showBackground = true, backgroundColor = BG, heightDp = 400, widthDp = 360)
@Composable
private fun ProfileErrorPreview() = Host {
    ProfilePage(SettingsUiState(profile = UiState.Error(AppError.Network)), NoActions, SettingsLaunchers())
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 700, widthDp = 360)
@Composable
private fun PartnerConnectedPreview() = Host { PartnerPage(sampleState, NoActions) }

private val sampleRequest = IncomingRequest("r1", "Ravi", "9876543210", null, null)

@Preview(showBackground = true, backgroundColor = BG, heightDp = 800, widthDp = 360)
@Composable
private fun PartnerNotConnectedPreview() = Host {
    val overview = PartnerOverview(PartnerStatus.NotConnected, listOf(sampleRequest))
    PartnerPage(sampleState.copy(partner = PartnerUiState(overview = UiState.Content(overview), phone = "98765")), NoActions)
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 700, widthDp = 360)
@Composable
private fun PartnerWaitingPreview() = Host {
    val overview = PartnerOverview(PartnerStatus.Waiting("r2", "Anu", "9123456780", null), emptyList())
    PartnerPage(sampleState.copy(partner = PartnerUiState(overview = UiState.Content(overview))), NoActions)
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 400, widthDp = 360)
@Composable
private fun PartnerErrorPreview() = Host {
    PartnerPage(sampleState.copy(partner = PartnerUiState(overview = UiState.Error(AppError.Network))), NoActions)
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 500, widthDp = 360)
@Composable
private fun NotificationsPreview() = Host {
    NotificationsPage(sampleState, NotificationStatus.CanAsk, NoActions, SettingsLaunchers())
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 500, widthDp = 360)
@Composable
private fun AppearancePreview() = Host { AppearancePage(sampleState.copy(prefs = AppPrefs(reduceMotion = true)), NoActions) }

@Preview(showBackground = true, backgroundColor = BG, heightDp = 400, widthDp = 360)
@Composable
private fun ChatMediaPreview() = Host { ChatMediaPage(sampleState, NoActions) }

@Preview(showBackground = true, backgroundColor = BG, heightDp = 300, widthDp = 360)
@Composable
private fun TwinAiPreview() = Host { TwinAiPage(sampleState, NoActions) }

@Preview(showBackground = true, backgroundColor = BG, heightDp = 700, widthDp = 360)
@Composable
private fun PrivacyPreview() = Host {
    PrivacyPage(sampleState.copy(pin = PinUiState("1357", "2468", PinResult.Failure(AppError.Server(401, "Current PIN is wrong")))), NoActions)
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 1000, widthDp = 360)
@Composable
private fun StoragePreview() = Host {
    StoragePage(sampleState, NoActions, SettingsLaunchers(), pendingRestore = null, onRestoreHandled = {})
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 600, widthDp = 360)
@Composable
private fun StorageLoadingPreview() = Host {
    StoragePage(SettingsUiState(), NoActions, SettingsLaunchers(), pendingRestore = null, onRestoreHandled = {})
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 500, widthDp = 360)
@Composable
private fun StorageErrorPreview() = Host {
    StoragePage(SettingsUiState(storage = UiState.Error(AppError.Network)), NoActions, SettingsLaunchers(), pendingRestore = null, onRestoreHandled = {})
}

@Preview(showBackground = true, backgroundColor = BG, heightDp = 500, widthDp = 360)
@Composable
private fun AboutPreview() = Host { AboutPage(NoActions, SettingsLaunchers()) }
