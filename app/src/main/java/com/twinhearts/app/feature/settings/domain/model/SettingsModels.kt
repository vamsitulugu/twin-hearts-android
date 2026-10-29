package com.twinhearts.app.feature.settings.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Why this file exists: plain Kotlin types the Settings screens use (no Android, no JSON). */

/** The nine sub-pages of Settings (the web's `SettingsUI` SUBS list). No page = the home list. */
enum class SettingsPage { Profile, Partner, Notifications, Appearance, ChatMedia, TwinAi, Privacy, Storage, About }

/** "I am" on the Profile page. [wire] is the word the server stores in `profile.u1.gender`. */
enum class ProfileGender(val wire: String) {
    Female("female"),
    Male("male"),
    ;

    companion object {
        fun fromWire(value: String?): ProfileGender? = entries.firstOrNull { it.wire == value }
    }
}

/** What the Profile page and the couple header show. */
data class ProfileInfo(
    val myName: String,
    val partnerName: String,
    val gender: ProfileGender?,
    val anniversary: LocalDate?,
    val myBirthday: LocalDate?,
    val partnerBirthday: LocalDate?,
    val myAvatarUrl: String?,
    val partnerAvatarUrl: String?,
)

/** What "Save changes" sends. Blank names become "You" / "Partner" first, like the web. */
data class ProfileEdit(
    val myName: String,
    val partnerName: String,
    val gender: ProfileGender?,
    val anniversary: LocalDate?,
    val myBirthday: LocalDate?,
    val partnerBirthday: LocalDate?,
    val avatarUrl: String?,
) {
    fun normalized(): ProfileEdit = copy(
        myName = myName.trim().ifBlank { DEFAULT_MY_NAME },
        partnerName = partnerName.trim().ifBlank { DEFAULT_PARTNER_NAME },
    )

    companion object {
        const val DEFAULT_MY_NAME = "You"
        const val DEFAULT_PARTNER_NAME = "Partner"
    }
}

/** Where the link with the partner stands (answer of `/api/partner/status`). */
sealed interface PartnerStatus {
    data object NotConnected : PartnerStatus
    data class Connected(val partnerName: String) : PartnerStatus
    data class Waiting(
        val requestId: String,
        val receiverName: String,
        val receiverPhone: String,
        val sentAt: Instant?,
    ) : PartnerStatus

    /** Someone invited me; the invitation itself is in [PartnerOverview.incoming]. */
    data class InvitationReceived(val senderName: String) : PartnerStatus
}

/** An invitation somebody sent to me. */
data class IncomingRequest(
    val id: String,
    val senderName: String,
    val senderPhone: String,
    val senderAvatarUrl: String?,
    val sentAt: Instant?,
)

data class PartnerOverview(
    val status: PartnerStatus,
    val incoming: List<IncomingRequest>,
)

/** After "Remove partner": the web signs a user2 account out; a user1 account stays. */
enum class RemoveOutcome { StaySignedIn, MustSignOut }

/** Result of choosing a backup file to restore. */
enum class ImportOutcome { Restored, InvalidFile }

enum class StorageKind { Photos, Voice, Chat, Notes, AppData }

data class StorageUsage(val kind: StorageKind, val bytes: Long)

data class StorageSummary(
    val categories: List<StorageUsage>,
    val totalBytes: Long,
    val capacityBytes: Long,
) {
    /** 0..100, like the ring on the web page. */
    val percent: Int
        get() = if (capacityBytes <= 0) 0 else Math.round(totalBytes * 100.0 / capacityBytes).toInt().coerceIn(0, 100)

    val availableBytes: Long get() = (capacityBytes - totalBytes).coerceAtLeast(0)
}

/** The switches that are only yes/no. Each has its own key in `AppPrefKeys`. */
enum class SettingsFlag { ReduceMotion, AutoSavePhotos, AutoPlayVoice, AiUseContext, AppLock }

/** Everything the Settings screens save on this phone. */
data class AppPrefs(
    val reduceMotion: Boolean = false,
    val autoSavePhotos: Boolean = false,
    val autoPlayVoice: Boolean = false,
    val aiUseContext: Boolean = true,
    val appLock: Boolean = false,
    val quietStart: LocalTime = DEFAULT_QUIET_START,
    val quietEnd: LocalTime = DEFAULT_QUIET_END,
    val lastBackup: String? = null,
) {
    fun isOn(flag: SettingsFlag): Boolean = when (flag) {
        SettingsFlag.ReduceMotion -> reduceMotion
        SettingsFlag.AutoSavePhotos -> autoSavePhotos
        SettingsFlag.AutoPlayVoice -> autoPlayVoice
        SettingsFlag.AiUseContext -> aiUseContext
        SettingsFlag.AppLock -> appLock
    }

    companion object {
        // The web's quiet-hours inputs show 22:00 and 08:00 until the person changes them.
        val DEFAULT_QUIET_START: LocalTime = LocalTime.of(22, 0)
        val DEFAULT_QUIET_END: LocalTime = LocalTime.of(8, 0)
    }
}
