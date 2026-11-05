package com.twinhearts.app.feature.calls.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.twinhearts.app.feature.calls.data.GameVoiceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Why this file exists: gives the Games graph the app-wide [GameVoiceEngine] (R-42: state lives outside composables). */
@HiltViewModel
class GameVoiceViewModel @Inject constructor(val engine: GameVoiceEngine) : ViewModel()

/** The two actions that need the microphone, each asking for permission first. */
class GameVoiceActions(val start: () -> Unit, val accept: () -> Unit)

/**
 * Asks for the microphone (only when it is needed), then calls or answers.
 * Saying no tells the engine, which declines a ringing call and shows "needs the microphone".
 */
@Composable
fun rememberGameVoiceActions(viewModel: GameVoiceViewModel = hiltViewModel()): GameVoiceActions {
    val engine = viewModel.engine
    var answering by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when {
            !granted -> engine.permissionDenied()
            answering -> engine.accept()
            else -> engine.start()
        }
    }
    return remember(engine, launcher) {
        GameVoiceActions(
            start = { answering = false; launcher.launch(Manifest.permission.RECORD_AUDIO) },
            accept = { answering = true; launcher.launch(Manifest.permission.RECORD_AUDIO) },
        )
    }
}
