package com.twinhearts.app.feature.us.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.us.ui.screens.CollectionScreen
import com.twinhearts.app.feature.us.ui.screens.DreamGoalsScreen
import com.twinhearts.app.feature.us.ui.screens.LoveCounterScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon

/**
 * Tab container for the Us feature: Love Counter · Collection · Dream Goals.
 * Stateless — state comes from [UsViewModel], events go back via callbacks.
 */
@Composable
fun UsScreen(
    onDreamGoalAdded: () -> Unit,
    viewModel: UsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TwinTheme.colors
    var tabIndex by remember { mutableIntStateOf(0) }

    when (state) {
        is UsScreenState.Loading -> {
            TwinBackground {
                Column(
                    modifier = Modifier.fillMaxSize(),
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
                    message = state.error.asText(),
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
                    UsTabs(selectedIndex = tabIndex, onTabClick = { tabIndex = it })

                    when (tabIndex) {
                        0 -> LoveCounterScreen(state.loveCounter)
                        1 -> CollectionScreen(state.collection)
                        else -> DreamGoalsScreen(state.dreamGoals, onDreamGoalAdded = onDreamGoalAdded)
                    }
                }
            }
        }
    }
}

/** Simple segmented tab row matching the web tab style. */
@Composable
private fun UsTabs(
    selectedIndex: Int,
    onTabClick: (Int) -> Unit,
) {
    val colors = TwinTheme.colors
    val labels = listOf("Love Counter", "Collection", "Dream Goals")
    val icons = listOf(
        Icons.Rounded.Favorite,
        Icons.Rounded.CollectionBookmark,
        Icons.Rounded.Flag,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabClick(index) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .background(if (selected) colors.accent else Color.Unspecified)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = null,
                        tint = if (selected) colors.ink else colors.inkMuted,
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) colors.ink else colors.inkMuted,
                    )
                }
            }
        }
    }
}
