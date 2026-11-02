package com.twinhearts.app.feature.profile.highlights.ui

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.feature.profile.highlights.domain.HighlightCategory
import com.twinhearts.app.feature.profile.highlights.domain.HighlightsOwner
import com.twinhearts.app.feature.profile.highlights.domain.HighlightsSnapshot
import com.twinhearts.app.feature.profile.highlights.domain.PickedClip

/** Loading / content / error of the row (R-41). An empty list is "Ready" and shows the hint text. */
sealed interface HighlightsPhase {
    data object Loading : HighlightsPhase
    data class Ready(val snapshot: HighlightsSnapshot) : HighlightsPhase
    data class Failed(val error: AppError) : HighlightsPhase
}

/**
 * Clips chosen but not sent yet; the add sheet is open while this is not null.
 * [preselectId] = the person came from "Add more clips" of that highlight.
 * [lockedHighlightId] = an earlier try already created a highlight, so the rest must join it.
 */
data class PendingUpload(
    val clips: List<PickedClip>,
    val preselectId: String? = null,
    val lockedHighlightId: String? = null,
)

data class UploadProgress(val done: Int, val total: Int, val fraction: Float)

/** One-off notices shown as a toast; the screen clears them with `messageShown()`. */
sealed interface HighlightsMessage {
    data object Saved : HighlightsMessage
    data object EditSaved : HighlightsMessage
    data object Deleted : HighlightsMessage
    data object ClipDeleted : HighlightsMessage
    data object Skipped : HighlightsMessage
    data object TooBig : HighlightsMessage
    data object NotMedia : HighlightsMessage
    data class Failed(val error: AppError) : HighlightsMessage
}

data class HighlightsUiState(
    val phase: HighlightsPhase = HighlightsPhase.Loading,
    /** The selected category chip of each row (absent = All). */
    val filters: Map<HighlightsOwner, HighlightCategory> = emptyMap(),
    val pending: PendingUpload? = null,
    val progress: UploadProgress? = null,
    val uploadError: AppError? = null,
    /** True while a rename or delete runs, so buttons show a spinner and cannot fire twice. */
    val busy: Boolean = false,
    val message: HighlightsMessage? = null,
)
