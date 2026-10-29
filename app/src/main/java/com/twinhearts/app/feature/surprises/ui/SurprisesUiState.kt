package com.twinhearts.app.feature.surprises.ui

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.feature.surprises.domain.model.SurprisesData
import java.time.LocalDate

/** The two tabs of the page (web: `showSurpriseTab('surprises' | 'capsule')`). */
enum class SurprisesTab { Surprises, Capsule }

/** One-off notices the screen shows once (the web's toasts), then reports back as shown. */
sealed interface SurprisesMessage {
    data object Hidden : SurprisesMessage
    data object Sealed : SurprisesMessage
    data object SurpriseDeleted : SurprisesMessage
    data object CapsuleDeleted : SurprisesMessage
    data object NeedTitleAndDate : SurprisesMessage
    data object NeedMessageAndDate : SurprisesMessage
    data class StillLocked(val unlock: LocalDate) : SurprisesMessage
    data class Failed(val error: AppError) : SurprisesMessage
}

/**
 * Everything the Surprises screen draws. The four states of R-41:
 * [loading] with nothing yet, [loadError] with nothing yet, empty lists, and filled lists.
 */
data class SurprisesUiState(
    val loading: Boolean = true,
    /** Set only when the first load failed. A failed refresh keeps the old lists and shows a message. */
    val loadError: AppError? = null,
    val tab: SurprisesTab = SurprisesTab.Surprises,
    val data: SurprisesData = SurprisesData(emptyList(), emptyList()),
    /** Updated on every load, so a page left open overnight unlocks on the new day. */
    val today: LocalDate = LocalDate.now(),
    /** True while a save is running; the forms disable their button. */
    val saving: Boolean = false,
    val message: SurprisesMessage? = null,
)
