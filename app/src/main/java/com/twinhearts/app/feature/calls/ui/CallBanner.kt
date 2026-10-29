package com.twinhearts.app.feature.calls.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.feature.calls.data.CallState
import com.twinhearts.app.feature.calls.domain.CallPhase
import kotlinx.coroutines.delay

/**
 * Why this file exists: going back from the call page does not end the call, but it used to hide it
 * with no way to return. While a call is ringing or running and the call page is not on screen, a
 * slim bar (like WhatsApp's green "Tap to return to call") sits at the top of every screen. Tapping
 * it opens the call page again. The screen below is pushed down by the bar.
 */
@Composable
fun CallBannerHost(
    onCallScreen: Boolean,
    onOpen: (video: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CallViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val engine = viewModel.engine
    val state by engine.state.collectAsStateWithLifecycle()
    // A call that ends while the call page is closed would stay "ended" for ever: clear it here.
    LaunchedEffect(state.phase, onCallScreen) {
        if (state.phase == CallPhase.Ended && !onCallScreen) {
            delay(ENDED_SHOW_MS)
            engine.acknowledgeEnded()
        }
    }
    val show = state.phase != CallPhase.Idle && !onCallScreen
    Column(modifier.fillMaxSize()) {
        if (show) CallBanner(state, onClick = { onOpen(state.video) })
        // The bar already covers the status bar, so the screen below must not pad for it again.
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (show) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier),
        ) { content() }
    }
}

@Composable
private fun CallBanner(state: CallState, onClick: () -> Unit) {
    val ended = state.phase == CallPhase.Ended
    val label = when (state.phase) {
        CallPhase.Calling -> stringResource(R.string.calls_banner_calling, state.partnerName)
        CallPhase.Incoming -> stringResource(R.string.calls_banner_incoming)
        CallPhase.Connecting -> stringResource(R.string.calls_banner_connecting)
        CallPhase.Ended -> stringResource(R.string.calls_ended)
        else -> stringResource(R.string.calls_banner_return)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (ended) Color(0xFF3F3F46) else Color(0xFF16A34A))
            .statusBarsPadding()
            .clickable(enabled = !ended, onClick = onClick)
            .height(BAR_HEIGHT.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(if (state.video) Icons.Rounded.Videocam else Icons.Rounded.Call, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (state.phase == CallPhase.Connected) {
            Text(
                "%s  %d:%02d".format(state.partnerName, state.seconds / 60, state.seconds % 60),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
            )
        }
    }
}

private const val BAR_HEIGHT = 36
private const val ENDED_SHOW_MS = 1_500L
