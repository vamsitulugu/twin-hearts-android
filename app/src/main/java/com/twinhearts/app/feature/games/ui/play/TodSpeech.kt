package com.twinhearts.app.feature.games.ui.play

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Returns a function that reads text aloud (the "🔊 Read" button). Cleans up on leave. */
@Composable
fun rememberSpeaker(): (String) -> Unit {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    val engine = remember {
        var created: TextToSpeech? = null
        created = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                created?.language = Locale.getDefault()
                ready = true
            }
        }
        created
    }
    DisposableEffect(engine) {
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }
    return { text -> if (ready) engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tod") }
}

/**
 * Returns a function that opens the phone's speech dialog and hands the spoken words to
 * [onResult]. [onUnavailable] runs when the phone has no speech service.
 */
@Composable
fun rememberVoiceInput(onUnavailable: () -> Unit, onResult: (String) -> Unit): () -> Unit {
    val latestResult = rememberUpdatedState(onResult)
    val latestUnavailable = rememberUpdatedState(onUnavailable)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!heard.isNullOrBlank()) latestResult.value(heard)
        }
    }
    return {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            latestUnavailable.value()
        }
    }
}
