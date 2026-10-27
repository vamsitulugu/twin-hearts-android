package com.twinhearts.app.feature.us.ui

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.us.domain.model.CollectionItem
import com.twinhearts.app.feature.us.domain.model.DreamGoal
import com.twinhearts.app.feature.us.domain.model.LoveCounterState

/**
 * One UI state for the whole Us tab container (R-41: loading, content, empty, error).
 */
sealed interface UsScreenState {
    data object Loading : UsScreenState
    data class Content(
        val loveCounter: LoveCounterState,
        val collection: List<CollectionItem>,
        val dreamGoals: List<DreamGoal>,
    ) : UsScreenState
    data object Empty : UsScreenState
    data class Error(val error: AppError) : UsScreenState
}
