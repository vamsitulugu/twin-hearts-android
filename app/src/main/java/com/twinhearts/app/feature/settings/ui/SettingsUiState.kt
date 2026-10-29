package com.twinhearts.app.feature.settings.ui

import android.net.Uri
import androidx.annotation.StringRes
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.settings.domain.model.AppPrefs
import com.twinhearts.app.feature.settings.domain.model.PartnerOverview
import com.twinhearts.app.feature.settings.domain.model.ProfileGender
import com.twinhearts.app.feature.settings.domain.model.ProfileInfo
import com.twinhearts.app.feature.settings.domain.model.SettingsFlag
import com.twinhearts.app.feature.settings.domain.model.SettingsPage
import com.twinhearts.app.feature.settings.domain.model.StorageSummary
import java.time.LocalDate
import java.time.LocalTime

/** Why this file exists: everything the Settings screens draw, in one place (R-41, R-42). */

/** The Profile page's text fields before "Save changes". */
data class ProfileForm(
    val myName: String = "",
    val partnerName: String = "",
    val gender: ProfileGender? = null,
    val anniversary: LocalDate? = null,
    val myBirthday: LocalDate? = null,
    val partnerBirthday: LocalDate? = null,
) {
    companion object {
        fun from(info: ProfileInfo) = ProfileForm(
            myName = info.myName,
            partnerName = info.partnerName,
            gender = info.gender,
            anniversary = info.anniversary,
            myBirthday = info.myBirthday,
            partnerBirthday = info.partnerBirthday,
        )
    }
}

data class PartnerUiState(
    val overview: UiState<PartnerOverview> = UiState.Loading,
    val phone: String = "",
    /** Shown under the phone field, like the web's red line. */
    val problem: SettingsMessage? = null,
    val working: Boolean = false,
)

/** Result line under "Update PIN". */
sealed interface PinResult {
    data object None : PinResult
    data object Working : PinResult
    data object Success : PinResult
    data class Failure(val error: AppError) : PinResult
}

data class PinUiState(
    val current: String = "",
    val new: String = "",
    val result: PinResult = PinResult.None,
)

data class SettingsUiState(
    val page: SettingsPage? = null,
    val query: String = "",
    val profile: UiState<ProfileInfo> = UiState.Loading,
    val form: ProfileForm = ProfileForm(),
    val savingProfile: Boolean = false,
    val uploadingAvatar: Boolean = false,
    val prefs: AppPrefs = AppPrefs(),
    val partner: PartnerUiState = PartnerUiState(),
    val pin: PinUiState = PinUiState(),
    val storage: UiState<StorageSummary> = UiState.Loading,
    /** A backup / restore / photo export / cache clean is running. */
    val storageBusy: Boolean = false,
)

/** A one-off line shown as a toast, or an error text under a field. */
sealed interface SettingsMessage {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : SettingsMessage
    data class Failure(val error: AppError) : SettingsMessage
}

/** One-off events (R-42): they are not state, so they are not replayed after rotation. */
sealed interface SettingsEvent {
    data class Show(val message: SettingsMessage) : SettingsEvent
    data object SignedOut : SettingsEvent
}

/** What the screens can ask the ViewModel to do. Previews use a do-nothing copy. */
interface SettingsActions {
    fun onQuery(query: String)
    fun onOpenPage(page: SettingsPage)
    fun onBack()
    fun onSync()
    fun onRetryProfile()
    fun onLogOut()

    fun onMyName(text: String)
    fun onPartnerName(text: String)
    fun onGender(gender: ProfileGender)
    fun onAnniversary(date: LocalDate?)
    fun onMyBirthday(date: LocalDate?)
    fun onPartnerBirthday(date: LocalDate?)
    fun onSaveProfile()
    fun onAvatarPicked(uri: Uri)

    fun onPhone(text: String)
    fun onSendRequest()
    fun onCancelRequest(requestId: String)
    fun onAccept(requestId: String)
    fun onDecline(requestId: String)
    fun onRemovePartner()
    fun onRetryPartner()

    fun onFlag(flag: SettingsFlag, on: Boolean)
    fun onQuietStart(time: LocalTime)
    fun onQuietEnd(time: LocalTime)

    fun onCurrentPin(text: String)
    fun onNewPin(text: String)
    fun onUpdatePin()

    fun onRetryStorage()
    fun onClearCache()
    fun onExportBackup(target: Uri)
    fun onImportBackup(source: Uri)
    fun onExportPhotos(folder: Uri)
}
