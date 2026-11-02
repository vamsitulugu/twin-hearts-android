package com.twinhearts.app.feature.rewards.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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

/**
 * Twin Rewards page (web: #page-rewards, rewards.js). Wallet, 7-day check-in, couple streak, coin history.
 * Every number comes from the server; this screen only shows it and asks to claim.
 * On tablets the content is centred in a column no wider than 720 dp (web: two columns on laptops).
 */
@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh(withHistory = true) }

    TwinBackground {
        Column(Modifier.fillMaxSize()) {
            TwinTopBar(
                title = stringResource(R.string.rewards_title),
                showSidebarButton = true,
                onSyncClick = { viewModel.refresh(withHistory = true) },
            )
            val summary = state.summary
            val error = state.error
            if (summary == null) {
                if (error != null) {
                    ErrorState(message = error.asText(), onRetry = { viewModel.refresh(withHistory = true) })
                } else {
                    Column(Modifier.fillMaxSize().padding(TwinTheme.spacing.screenEdge)) { LoadingSkeleton(lines = 4) }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = TwinTheme.spacing.screenEdge, end = TwinTheme.spacing.screenEdge,
                            top = TwinTheme.spacing.s, bottom = 120.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item { HeroCard(summary) }
                        item { CheckInCard(summary, state.claiming, viewModel::claim) }
                        item { CoupleStreakCard(summary, state.partnerName) }
                        item { HistoryCard(state.history) }
                    }
                }
            }
        }
    }
}
