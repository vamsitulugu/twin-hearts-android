package com.twinhearts.app.feature.music.singtogether.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme

/**
 * Why this file exists: asks for the camera and the microphone only when a singing room is about to
 * open, with a short reason first (README §9). The room opens even if the answer is "no": the web
 * app then joins "audio-lyrics only", and so do we.
 */
internal class SingAccess(val ask: (onReady: () -> Unit) -> Unit)

@Composable
internal fun rememberSingAccess(partnerName: String): SingAccess {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var rationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // Allowed or not, the room opens; the engine checks what it may use.
        pending?.invoke()
        pending = null
    }

    if (rationale) {
        val colors = TwinTheme.colors
        AlertDialog(
            onDismissRequest = { rationale = false; pending = null },
            containerColor = colors.card,
            title = { Text(stringResource(R.string.music_together_access_title), style = MaterialTheme.typography.titleLarge, color = colors.ink) },
            text = {
                Text(
                    stringResource(R.string.music_together_access_message, partnerName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    rationale = false
                    launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                }) { Text(stringResource(R.string.music_together_access_allow), color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = {
                    rationale = false
                    pending?.invoke()
                    pending = null
                }) { Text(stringResource(R.string.music_together_access_skip), color = colors.inkMuted) }
            },
        )
    }

    return remember(context) {
        SingAccess { onReady ->
            if (hasBoth(context)) {
                onReady()
            } else {
                pending = onReady
                rationale = true
            }
        }
    }
}

private fun hasBoth(context: Context): Boolean =
    listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
