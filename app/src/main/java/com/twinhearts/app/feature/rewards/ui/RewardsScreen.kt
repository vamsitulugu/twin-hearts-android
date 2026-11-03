package com.twinhearts.app.feature.rewards.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.navigation.NavHostController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.rewards.domain.RewardsSummary
import com.twinhearts.app.feature.rewards.domain.ScratchResult
import com.twinhearts.app.feature.rewards.domain.StoreItem

/**
 * Twin Rewards page (web: #page-rewards).
 * Now split into sub-pages (tabs) like other Android app screens:
 *   - Wallet: coins, daily check-in, lucky scratch and couple streak.
 *   - Twin Store: spend coins on themes, frames & decor.
 * Spin & Win lives on its own separate page ([Routes.SpinWheel]) with the coin balance shown above the wheel.
 * Coin history has been removed.
 * Every number comes from the server; this screen only shows it and animates results.
 * On tablets the content is centred in a column no wider than 720 dp.
 */
@Composable
fun RewardsScreen(
    navController: NavHostController,
    viewModel: RewardsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val adPlayer = rememberAdPlayer()
    val scrollState = rememberScrollState()
    // Refresh summary + store items whenever the rewards page opens.
    LaunchedEffect(Unit) {
        viewModel.refresh()
        viewModel.loadStore()
        viewModel.loadEarn()
    }

    val earned = state.justEarned
    if (earned > 0) {
        val text = stringResource(R.string.rewards_toast, earned, state.summary?.streak ?: 0)
        LaunchedEffect(earned) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.consumeEarned()
        }
    }

    TwinBackground {
        Column(Modifier.fillMaxSize()) {
            TwinTopBar(
                title = stringResource(R.string.rewards_title),
                showSidebarButton = true,
                onSyncClick = { viewModel.refresh(); viewModel.loadStore(); viewModel.loadEarn() },
            )
            val summary = state.summary
            val error = state.error
            if (summary == null) {
                if (error != null) {
                    ErrorState(message = error.asText(), onRetry = { viewModel.refresh(); viewModel.loadStore() })
                } else {
                    Column(Modifier.fillMaxSize().padding(TwinTheme.spacing.screenEdge)) { LoadingSkeleton(lines = 4) }
                }
            } else {
                // Tab index survives rotation.
                var tabIndex by rememberSaveable(0) { mutableIntStateOf(0) }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(
                                start = TwinTheme.spacing.screenEdge,
                                end = TwinTheme.spacing.screenEdge,
                                top = TwinTheme.spacing.s,
                                bottom = 120.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SegmentedTabs(
                            options = listOf("Wallet", "Twin Store"),
                            selectedIndex = tabIndex,
                            onSelect = { tabIndex = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        when (tabIndex) {
                            0 -> WalletTab(
                                summary = summary,
                                claiming = state.claiming,
                                spinning = state.spinning,
                                scratching = state.scratching,
                                lastScratchResult = state.lastScratchResult,
                                partnerName = state.partnerName,
                                earnItems = state.earnItems,
                                adPlayer = adPlayer,
                                adBusy = state.adBusy,
                                onWatchAd = { viewModel.watchAdForCoins(adPlayer.show) },
                                onClaim = viewModel::claim,
                                onScratch = viewModel::scratch,
                                onOpenSpinWheel = { navController.navigate(Routes.SpinWheel) },
                            )
                            1 -> TwinStoreTab(
                                balance = summary.balance,
                                storeItems = state.storeItems,
                                unlockedItemIds = state.unlockedItemIds,
                                equipped = state.equipped,
                                storeFilter = state.storeFilter,
                                purchasingItemId = state.purchasingItemId,
                                equippingItemId = state.equippingItemId,
                                onFilterChange = viewModel::setStoreFilter,
                                onBuy = viewModel::buyItem,
                                onEquip = viewModel::equipItem,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletTab(
    summary: RewardsSummary,
    claiming: Boolean,
    spinning: Boolean,
    scratching: Boolean,
    lastScratchResult: ScratchResult?,
    partnerName: String,
    earnItems: List<com.twinhearts.app.feature.rewards.domain.EarnItem>,
    adPlayer: AdPlayer,
    adBusy: Boolean,
    onWatchAd: () -> Unit,
    onClaim: () -> Unit,
    onScratch: () -> Unit,
    onOpenSpinWheel: () -> Unit,
) {
    HeroCard(summary)
    CheckInCard(summary, claiming, onClaim)
    WatchAdCard(summary, adPlayer, adBusy, onWatchAd)
    EarnCard(earnItems)
    ScratchCard(
        summary = summary,
        scratching = scratching,
        lastResult = lastScratchResult,
        onScratch = onScratch,
    )
    CoupleStreakCard(summary, partnerName)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(TwinTheme.shapes.minTouch)
                .clip(TwinTheme.shapes.pill)
                .background(Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep)))
                .clickable(role = Role.Button, onClick = onOpenSpinWheel),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.rewards_spin_title),
                color = RewardsStyle.GoldInk,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.5.sp,
            )
        }
    }
    Text(
        stringResource(R.string.rewards_spin_note_ready),
        style = MaterialTheme.typography.bodySmall,
        color = TwinTheme.colors.inkMuted,
    )
}

@Composable
private fun TwinStoreTab(
    balance: Int,
    storeItems: List<StoreItem>,
    unlockedItemIds: List<String>,
    equipped: Map<String, String>,
    storeFilter: String,
    purchasingItemId: String?,
    equippingItemId: String?,
    onFilterChange: (String) -> Unit,
    onBuy: (StoreItem) -> Unit,
    onEquip: (String, String) -> Unit,
) {
    TwinStoreCard(
        balance = balance,
        storeItems = storeItems,
        unlockedItemIds = unlockedItemIds,
        equipped = equipped,
        storeFilter = storeFilter,
        purchasingItemId = purchasingItemId,
        equippingItemId = equippingItemId,
        onFilterChange = onFilterChange,
        onBuy = onBuy,
        onEquip = onEquip,
    )
}
