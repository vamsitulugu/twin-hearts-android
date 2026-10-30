package com.twinhearts.app.feature.fights.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.fights.domain.FightMath
import com.twinhearts.app.feature.fights.ui.components.ActivitiesView
import com.twinhearts.app.feature.fights.ui.components.FightDetailSheet
import com.twinhearts.app.feature.fights.ui.components.FightRow
import com.twinhearts.app.feature.fights.ui.components.LogActions
import com.twinhearts.app.feature.fights.ui.components.LogFightButton
import com.twinhearts.app.feature.fights.ui.components.LogWizard
import com.twinhearts.app.feature.fights.ui.components.MetricTiles
import com.twinhearts.app.feature.fights.ui.components.NoFightsYet
import com.twinhearts.app.feature.fights.ui.components.RecentHeader
import com.twinhearts.app.feature.fights.ui.components.ReconnectRow
import com.twinhearts.app.feature.fights.ui.components.ResolutionRateCard
import java.time.LocalDate

/**
 * Why this file exists: the Fight Log page. Three in-page screens (dashboard, Log Fight wizard,
 * Reconnect Activities) share it, exactly like the web page; Back steps out of them before leaving.
 */
@Composable
fun FightsScreen(
    onBack: () -> Unit,
    onOpenTwin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FightsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FightsEvent.Message -> Toast.makeText(context, event.id, Toast.LENGTH_SHORT).show()
                FightsEvent.OpenTwin -> onOpenTwin()
            }
        }
    }

    BackHandler(enabled = state.view != FightView.Dashboard) { viewModel.handleBack() }

    val pending = { Toast.makeText(context, R.string.fights_msg_pending, Toast.LENGTH_SHORT).show() }
    val scroll = rememberScrollState()
    LaunchedEffect(state.view, state.step) { scroll.scrollTo(0) }

    Column(modifier.fillMaxSize()) {
        TwinTopBar(
            title = stringResource(R.string.fights_title),
            onBackClick = { if (!viewModel.handleBack()) onBack() },
            onMenuClick = pending,
            onSearchClick = pending,
            onSyncClick = viewModel::load,
        )
        when (val load = state.load) {
            FightsLoad.Loading -> Column(Modifier.fillMaxSize().padding(TwinTheme.spacing.l), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                LoadingSkeleton(lines = 4)
            }
            is FightsLoad.Error -> ErrorState(message = load.error.asText(), onRetry = viewModel::load)
            is FightsLoad.Ready -> {
                val today = remember { LocalDate.now() }
                val fights = load.fights
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
                    verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                ) {
                    when (state.view) {
                        FightView.Dashboard -> {
                            val inRange = FightMath.inRange(fights, today, state.range)
                            ResolutionRateCard(
                                stats = FightMath.stats(inRange),
                                range = state.range,
                                onRange = viewModel::setRange,
                                onInfo = { Toast.makeText(context, R.string.fights_rate_info_text, Toast.LENGTH_LONG).show() },
                            )
                            MetricTiles(fights, state.range, today)
                            LogFightButton(viewModel::openLog)
                            val sorted = FightMath.recentFirst(fights)
                            RecentHeader(state.showAll, canToggle = sorted.size > 3, onToggle = viewModel::toggleShowAll)
                            if (sorted.isEmpty()) {
                                NoFightsYet(viewModel::openActivities)
                            } else {
                                (if (state.showAll) sorted else sorted.take(3)).forEach { fight ->
                                    FightRow(fight, onClick = { viewModel.openDetail(fight) })
                                }
                            }
                            ReconnectRow(viewModel::openActivities)
                        }
                        FightView.Log -> LogWizard(
                            form = state.form,
                            step = state.step,
                            titleMissing = state.titleMissing,
                            saving = state.saving,
                            actions = remember(viewModel) {
                                LogActions(
                                    onBack = { viewModel.handleBack() },
                                    onSaveDraft = viewModel::saveDraft,
                                    onStep = viewModel::goStep,
                                    onNext = viewModel::next,
                                    onForm = viewModel::onForm,
                                    onCategory = viewModel::pickCategory,
                                    onIntensity = viewModel::pickIntensity,
                                    onStarter = viewModel::pickStarter,
                                    onStatus = viewModel::pickStatus,
                                    onFeeling = viewModel::toggleFeeling,
                                    onApproach = viewModel::toggleApproach,
                                )
                            },
                        )
                        FightView.Activities -> ActivitiesView(
                            filter = state.activityFilter,
                            onFilter = viewModel::setActivityFilter,
                            onBack = { viewModel.handleBack() },
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    state.detail?.let { fight ->
        FightDetailSheet(
            fight = fight,
            myRole = state.myRole,
            partnerName = state.partnerName,
            confirmDelete = state.confirmDelete,
            onDismiss = viewModel::closeDetail,
            onResolve = viewModel::resolveDetail,
            onAskAi = viewModel::askAi,
            onDelete = viewModel::askDelete,
            onConfirmDelete = viewModel::confirmDelete,
            onCancelDelete = viewModel::cancelDelete,
        )
    }
}
