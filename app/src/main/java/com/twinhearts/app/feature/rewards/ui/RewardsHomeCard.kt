package com.twinhearts.app.feature.rewards.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    GlassCard(modifier.fillMaxWidth(), onClick = onOpen, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinIcon()
            Spacer(Modifier.width(7.dp))
            Column {
                Text("$shown", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = colors.ink)
                Text(stringResource(R.string.rewards_gold_coins), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
            }
            Spacer(Modifier.width(14.dp))
            FlameIcon()
            Spacer(Modifier.width(5.dp))
            Column {
                Text("${s.streak}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = colors.ink)
                Text(stringResource(R.string.rewards_day_streak), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
            }
            Spacer(Modifier.weight(1f))
            ClaimButton(
                claimed = s.checkedInToday, claiming = state.claiming, coins = s.todayCoins,
                onClick = viewModel::claim,
            )
        }
    }
}
