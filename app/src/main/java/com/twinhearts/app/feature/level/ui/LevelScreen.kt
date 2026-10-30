package com.twinhearts.app.feature.level.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.level.domain.LevelState
import kotlinx.coroutines.launch

/**
 * Couple Level page (web: page-level, renderLevelPage in index.html). Shows the couple's XP from
 * the shared state: level ring, next unlock, locked and earned badges, and how to earn XP.
 * It only reads; XP is added by the other features (Chat, habits, fights and so on).
 */
@Composable
fun LevelScreen(
    onBack: () -> Unit,
    viewModel: LevelViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TwinBackground {
        Column(Modifier.fillMaxSize()) {
            TwinTopBar(
                title = stringResource(R.string.level_title),
                onBackClick = onBack,
                onSyncClick = viewModel::load,
            )
            when (val current = state) {
                LevelUiState.Loading -> Column(Modifier.fillMaxSize().padding(TwinTheme.spacing.screenEdge)) { LoadingSkeleton(lines = 4) }
                is LevelUiState.Error -> ErrorState(message = current.error.asText(), onRetry = viewModel::load)
                is LevelUiState.Content -> LevelContent(current.level)
            }
        }
    }
}

@Composable
private fun LevelContent(level: LevelState) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = TwinTheme.spacing.screenEdge, end = TwinTheme.spacing.screenEdge, top = TwinTheme.spacing.s, bottom = BOTTOM_SPACE),
        verticalArrangement = Arrangement.spacedBy(CARD_GAP),
    ) {
        item { HeroCard(level) { index -> scope.launch { listState.animateScrollToItem(index) } } }
        item { NextUnlockCard(level) }
        item { LockedBadgesCard(level) }
        item { EarnedBadgesCard(level) }
        item { EarnXpCard() }
    }
}

private val CARD_GAP = 14.dp
private val BOTTOM_SPACE = 96.dp
