package com.twinhearts.app.feature.calls.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.feature.calls.domain.CallPhase
import kotlinx.coroutines.delay

/**
 * Why this file exists: the call pieces drawn over the Twin Cinema. While my partner is calling it shows a small
 * Accept / Decline card (so the cinema is NOT closed by the full-screen call page); once a call is running it is the
 * same voice bar / video window the Watch Together player uses ([WatchCallOverlay]). Draws nothing when there is no call.
 */
@Composable
fun CinemaCallOverlay(modifier: Modifier = Modifier, viewModel: CallViewModel = hiltViewModel()) {
    val engine = viewModel.engine
    val state by engine.state.collectAsStateWithLifecycle()
    val permissions = remember(state.video) {
        if (state.video) arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA) else arrayOf(Manifest.permission.RECORD_AUDIO)
    }
    var answering by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it } && answering) engine.accept() else engine.decline()
        answering = false
    }
    // The banner host does not clean up inside the cinema, so a call that ended while ringing is cleared here.
    LaunchedEffect(state.phase) {
        if (state.phase == CallPhase.Ended) {
            delay(1_200L)
            engine.acknowledgeEnded()
        }
    }
    Box(modifier.fillMaxSize()) {
        if (state.phase == CallPhase.Incoming) {
            Column(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 10.dp, start = 12.dp, end = 12.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)).background(Color(0xFF0F0F0F).copy(alpha = 0.85f)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(if (state.video) R.string.calls_incoming_video else R.string.calls_incoming_voice),
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
                if (state.partnerName.isNotEmpty()) Text(state.partnerName, color = Color(0xFFCCCCCC), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RoundAction(Icons.Rounded.CallEnd, stringResource(R.string.calls_decline), Color(0xFFDC2626)) { engine.decline() }
                    RoundAction(Icons.Rounded.Call, stringResource(R.string.calls_accept), Color(0xFF16A34A)) {
                        answering = true
                        launcher.launch(permissions)
                    }
                }
            }
        } else {
            WatchCallOverlay(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, fill: Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(24.dp)).background(fill).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
