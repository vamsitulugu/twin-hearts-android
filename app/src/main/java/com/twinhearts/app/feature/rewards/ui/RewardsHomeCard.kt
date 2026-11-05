package com.twinhearts.app.feature.rewards.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme

/**
 * Slim rewards strip for the top of Home (web: #rwStrip): coins, streak and the claim button.
 * Tapping the card opens the Rewards page. Shows nothing until the server has answered, so a
 * failed request leaves Home exactly as it was.
 */
@Composable
fun RewardsHomeCard(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RewardsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Refresh once whenever Home comes back on screen (no timers, no polling).
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.refresh() }
    }
    val earned = state.justEarned
    if (earned > 0) {
        val text = stringResource(R.string.rewards_toast, earned, state.summary?.streak ?: 0)
        LaunchedEffect(earned) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.consumeEarned()
        }
    }
    val s = state.summary ?: return
    val colors = TwinTheme.colors
    val shown = rememberCountedBalance(s.balance)
    val bump = rememberCoinBump(s.balance)

    // Big "font size" / "display size" settings must never break the strip: every stat gets an equal
    // weighted slot, labels stay on one line (ellipsis instead of letter-by-letter wrapping), and from
    // a 1.3x font scale up the three stats stack icon-above-text so nothing is squeezed.
    val stacked = LocalDensity.current.fontScale >= 1.3f
    val spinValue = if (s.spinsAvailable > 0) "1" else "0"
    val spinLabel = stringResource(if (s.spinsAvailable > 0) R.string.rewards_spin_ready else R.string.rewards_spun)

    GlassCard(modifier.fillMaxWidth(), onClick = onOpen, contentPadding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RewardStat(
                icon = { CoinIcon(modifier = Modifier.graphicsLayer { scaleX = bump; scaleY = bump }) },
                value = "$shown",
                label = stringResource(R.string.rewards_gold_coins),
                stacked = stacked,
                modifier = Modifier.weight(1f),
            )
            RewardStat(
                icon = { FlameIcon() },
                value = "${s.streak}",
                label = stringResource(R.string.rewards_day_streak),
                stacked = stacked,
                modifier = Modifier.weight(1f),
            )
            RewardStat(
                icon = { WheelIcon() },
                value = spinValue,
                label = spinLabel,
                stacked = stacked,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RewardStat(
    icon: @Composable () -> Unit,
    value: String,
    label: String,
    stacked: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val texts: @Composable () -> Unit = {
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.ink,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.inkMuted,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (stacked) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(Modifier.height(4.dp))
            texts()
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f, fill = false)) { texts() }
        }
    }
}
