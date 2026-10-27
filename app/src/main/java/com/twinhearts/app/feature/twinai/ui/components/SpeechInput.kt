package com.twinhearts.app.feature.twinai.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.twinhearts.app.R

/**
 * Why this file exists: the mic inside BOTH answers. Opens the phone's speech dialog once and
 * passes the words back (the web's "Speak your side"). Returns the function to call on tap.
 */
@Composable
fun rememberSpeechInput(onText: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val prompt = stringResource(R.string.twinai_both_mic)
    val latest = rememberUpdatedState(onText)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !heard.isNullOrBlank()) latest.value(heard)
    }
    return {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        try { launcher.launch(intent) } catch (e: ActivityNotFoundException) { onUnavailable() }
    }
}
