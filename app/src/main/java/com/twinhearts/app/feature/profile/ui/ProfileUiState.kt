package com.twinhearts.app.feature.profile.ui

import androidx.annotation.StringRes
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.feature.profile.domain.model.ProfileData

/** The six sub-screens of the Profile. The page keeps its own back stack, so Back returns to the screen before. */
enum class ProfileScreenKind { Main, Partner, Settings, Milestones, Love, Story }

/** Loading / content / error of the page (R-41). Empty states live inside the lists (no milestones, no next date). */
sealed interface ProfilePhase {
    data object Loading : ProfilePhase
    data class Content(val data: ProfileData) : ProfilePhase
    data class Failed(val error: AppError) : ProfilePhase
}

data class ProfileUiState(
    val phase: ProfilePhase = ProfilePhase.Loading,
    val stack: List<ProfileScreenKind> = listOf(ProfileScreenKind.Main),
    /** True while a save, upload or request is running, so buttons can show a spinner and not fire twice. */
    val busy: Boolean = false,
    /** True while the new photo is uploading (the old one stays visible). */
    val uploadingAvatar: Boolean = false,
    /** Today's date, set by the ViewModel so every "days left" uses the same day. */
    val todayEpochDay: Long = 0L,
    val nowMs: Long = 0L,
) {
    val screen: ProfileScreenKind get() = stack.last()
}

/** One-off things the screen should show once (R-42). */
sealed interface ProfileEvent {
    data class Message(@StringRes val res: Int, val arg: String? = null) : ProfileEvent

    /** The export text is ready: the screen now asks the person where to save it. */
    data class ExportReady(val json: String) : ProfileEvent

    /** The connection was removed by the second person of the couple: the old app signs them out, so do we. */
    data object SignedOut : ProfileEvent
}
