package com.twinhearts.app.feature.us.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.us.ui.screens.CollectionScreen
import com.twinhearts.app.feature.us.ui.screens.DreamGoalsScreen
import com.twinhearts.app.feature.us.ui.screens.LoveCounterScreen

/**
 * Tab container for the Us feature: Love Counter · Collection · Dream Goals.
 * State comes from [UsViewModel], events go back via callbacks.
 */
@Composable
fun UsScreen(
    onDreamGoalAdded: () -> Unit,
    viewModel: UsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tabIndex by remember { mutableIntStateOf(0) }

    // `state` is a delegated property, so it cannot be smart-cast: take a local copy.
    when (val current = state) {
        is UsScreenState.Loading -> {
            TwinBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(TwinTheme.spacing.screenEdge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    LoadingSkeleton(lines = 3)
                }
            }
        }
        is UsScreenState.Error -> {
            TwinBackground {
                ErrorState(
                    message = current.error.asText(),
                    onRetry = { viewModel.refresh() },
                )
            }
        }
        is UsScreenState.Empty -> {
            TwinBackground {
                EmptyState(
                    title = stringResource(R.string.core_nothing_yet),
                    message = "Open the Love Counter, Collection or Dream Goals tab to get started.",
                )
            }
        }
        is UsScreenState.Content -> {
            TwinBackground {
                Column(modifier = Modifier.fillMaxSize()) {
                    SegmentedTabs(
                        options = listOf("Love Counter", "Collection", "Dream Goals"),
                        selectedIndex = tabIndex,
                        onSelect = { tabIndex = it },
                        modifier = Modifier.padding(
                            horizontal = TwinTheme.spacing.l,
                            vertical = TwinTheme.spacing.s,
                        ),
                    )

                    when (tabIndex) {
                        0 -> LoveCounterScreen(current.loveCounter)
                        1 -> CollectionScreen(current.collection)
                        else -> DreamGoalsScreen(current.dreamGoals, onDreamGoalAdded = onDreamGoalAdded)
                    }
                }
            }
        }
    }
}
