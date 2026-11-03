package com.twinhearts.app.feature.ads.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.ads.data.AdsManager
import com.twinhearts.app.feature.ads.di.rememberAdsManager
import com.twinhearts.app.feature.ads.domain.AdRules
import com.twinhearts.app.feature.rewards.data.RewardsRepository
import com.twinhearts.app.feature.rewards.domain.PendingAdReward
import com.twinhearts.app.feature.rewards.ui.AdScratchDialog
import com.twinhearts.app.feature.rewards.ui.RewardsStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the person is told after tapping the card. */
sealed interface EarnPromptMessage {
    data class Earned(val coins: Int) : EarnPromptMessage
    data object NotEarned : EarnPromptMessage
    data object TryLater : EarnPromptMessage
}

/**
 * Why this file exists: the coin side of the small "watch an ad, earn coins" card. Same server flow as the
 * Rewards screen (ad/start, play the rewarded ad, ad/claim): the server pays, nothing is added here by guess.
 */
@HiltViewModel
class EarnPromptViewModel @Inject constructor(
    private val rewards: RewardsRepository,
    private val ads: AdsManager,
) : ViewModel() {

    private val _working = MutableStateFlow(false)
    val working: StateFlow<Boolean> = _working.asStateFlow()

    private val _messages = MutableSharedFlow<EarnPromptMessage>(extraBufferCapacity = 2)
    val messages: SharedFlow<EarnPromptMessage> = _messages.asSharedFlow()

    /** The ad was watched; a scratch card is on screen and the coins are paid only when it is scratched. */
    private val _pending = MutableStateFlow<PendingAdReward?>(null)
    val pending: StateFlow<PendingAdReward?> = _pending.asStateFlow()

    /** Called by the scratch card once it is scratched. Returns the coins the server paid, or null if it could not. */
    suspend fun claim(): Int? {
        val reward = _pending.value ?: return null
        return when (val claim = rewards.adClaim(reward.token)) {
            is AppResult.Success -> claim.data.earned
            is AppResult.Failure -> null
        }
    }

    /** The scratch card was closed. */
    fun finish() {
        _pending.value = null
        _working.value = false
    }

    fun watch(activity: Activity) {
        if (_working.value) return
        _working.value = true
        ads.onEarnPromptClosed() // the card goes away now; the next one is a full interval after this ad
        viewModelScope.launch {
            when (val ticket = rewards.adStart("coins")) {
                is AppResult.Failure -> {
                    _working.value = false
                    _messages.tryEmit(EarnPromptMessage.TryLater)
                }
                is AppResult.Success -> ads.showRewarded(
                    activity,
                    // Not paid yet: the scratch card opens first (see claim()).
                    onEarned = { _pending.value = PendingAdReward(ticket.data.token, ticket.data.coins) },
                    onFail = {
                        _working.value = false
                        _messages.tryEmit(EarnPromptMessage.NotEarned)
                    },
                )
            }
        }
    }
}

/**
 * Why this file exists: every few minutes a small card slides in at the top of the screen: "Watch a short ad,
 * earn coins". It is shown only on calm pages (never over chat, calls, the map or a movie), only when a rewarded
 * ad is loaded, goes away by itself after a few seconds, and has no daily limit. One instance in TwinApp.
 */
@Composable
fun EarnCoinsPrompt(modifier: Modifier = Modifier, viewModel: EarnPromptViewModel = hiltViewModel()) {
    val ads = rememberAdsManager()
    val context = LocalContext.current
    val visible by ads.earnPromptVisible.collectAsStateWithLifecycle()
    val working by viewModel.working.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()

    pending?.let { reward ->
        AdScratchDialog(coins = reward.coins, onClaim = viewModel::claim, onClose = viewModel::finish)
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                is EarnPromptMessage.Earned -> if (message.coins > 0) context.getString(R.string.earn_prompt_earned, message.coins) else null
                EarnPromptMessage.NotEarned -> context.getString(R.string.rewards_ad_failed)
                EarnPromptMessage.TryLater -> context.getString(R.string.earn_prompt_try_later)
            }
            if (text != null) Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
    // Nobody tapped it: it leaves by itself.
    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        delay(AdRules.EARN_PROMPT_VISIBLE_MILLIS)
        ads.onEarnPromptClosed()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        GlassCard(
            modifier = Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            contentPadding = 12.dp,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("📺", fontSize = 22.sp, modifier = Modifier.size(34.dp).wrapContentSize(Alignment.Center))
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.earn_prompt_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TwinTheme.colors.ink,
                    )
                    Text(
                        stringResource(R.string.earn_prompt_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = TwinTheme.colors.inkMuted,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(R.string.earn_prompt_watch),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RewardsStyle.GoldInk,
                    modifier = Modifier
                        .clip(TwinTheme.shapes.pill)
                        .background(Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep)))
                        .clickable(enabled = !working) { (context.findActivity())?.let(viewModel::watch) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
                Text(
                    "✕",
                    color = TwinTheme.colors.inkMuted,
                    modifier = Modifier
                        .clip(TwinTheme.shapes.pill)
                        .clickable { ads.onEarnPromptClosed() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }
}
