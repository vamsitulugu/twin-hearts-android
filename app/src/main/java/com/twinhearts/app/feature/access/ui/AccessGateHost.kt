package com.twinhearts.app.feature.access.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.access.domain.AccessAsk
import com.twinhearts.app.feature.access.domain.AccessFeature
import com.twinhearts.app.feature.access.domain.AccessGate
import com.twinhearts.app.feature.access.domain.PassResult
import com.twinhearts.app.feature.access.domain.PendingAsk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccessGateViewModel @Inject constructor(private val gate: AccessGate) : ViewModel() {
    val pending: StateFlow<PendingAsk?> = gate.pending
    val openRewards = gate.openRewards

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun later(p: PendingAsk) { _error.value = null; gate.dismiss(p) }
    fun getCoins(p: PendingAsk) { _error.value = null; gate.goEarnCoins(p) }

    fun unlock(p: PendingAsk) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        viewModelScope.launch {
            val r = gate.unlock(p)
            _busy.value = false
            if (r is PassResult.Failed) _error.value = r.message
        }
    }
}

/**
 * Put this ONCE near the top of the app (next to the Pro gate). It shows the one clear "free ones are
 * gone" sheet when [AccessGate.check] needs it, and opens the Rewards page for "Get coins free".
 */
@Composable
fun AccessGateHost(onGetCoins: () -> Unit, viewModel: AccessGateViewModel = hiltViewModel()) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.openRewards.collect { onGetCoins() } }
    val p = pending ?: return
    AccessSheet(
        info = p.info,
        busy = busy,
        error = error,
        onUnlock = { viewModel.unlock(p) },
        onGetCoins = { viewModel.getCoins(p) },
        onLater = { viewModel.later(p) },
    )
}

private val CoinGold = Color(0xFFF5B83D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessSheet(
    info: AccessAsk,
    busy: Boolean,
    error: String?,
    onUnlock: () -> Unit,
    onGetCoins: () -> Unit,
    onLater: () -> Unit,
) {
    val colors = TwinTheme.colors
    val why = when (info.feature) {
        AccessFeature.Movie -> "You've watched today's free movie."
        AccessFeature.Songs -> "You've used today's ${info.freeTotal} free Sing Together songs."
        AccessFeature.Pins -> "You've dropped today's ${info.freeTotal} free globe pins."
        AccessFeature.Games -> "You've played today's ${info.freeTotal} free games."
    }
    ModalBottomSheet(
        onDismissRequest = onLater,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(info.emoji, fontSize = 38.sp)
            Text(
                info.feature.title,
                color = colors.ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
            )
            Text(
                "$why Free ones refill every day.",
                color = colors.inkMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.glass, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.widthIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("🪙", fontSize = 20.sp)
                    Text("${info.price}", color = CoinGold, fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 4.dp))
                }
                Column {
                    Text(info.perk, color = colors.ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "One tap now. We won't ask again for every ${info.feature.freeNoun} during these ${info.hours} hours.",
                        color = colors.inkMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                if (info.coinsMissing > 0) "You have ${info.balance} 🪙 · need ${info.coinsMissing} more" else "You have ${info.balance} 🪙",
                color = colors.inkMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
            )
            if (error != null) {
                Text(error, color = colors.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
            }
            GlassButton(
                text = if (info.coinsMissing > 0) "Get coins free" else "Unlock for ${info.price} 🪙",
                onClick = if (info.coinsMissing > 0) onGetCoins else onUnlock,
                modifier = Modifier.fillMaxWidth(),
                style = GlassButtonStyle.Primary,
                loading = busy,
            )
            TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Maybe later", color = colors.inkMuted)
            }
            Text(
                "Why coins? They keep Twin Hearts free for everyone. You earn them daily: check-in, Spin & Win, scratch cards and games.",
                color = colors.inkFaint,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            )
            Spacer(Modifier.size(4.dp))
        }
    }
}
