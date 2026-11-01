package com.twinhearts.app.core.appconfig

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.twinhearts.app.R

/**
 * Shows the update the admin published. "Required" cannot be closed (not even with Back);
 * "optional / recommended" has a Later button.
 */
@Composable
fun UpdateGate(update: UpdateInfo?) {
    if (update == null) return
    val context = LocalContext.current
    var dismissed by rememberSaveable(update.version) { mutableStateOf(false) }
    if (!update.required && dismissed) return

    AlertDialog(
        onDismissRequest = { if (!update.required) dismissed = true },
        properties = DialogProperties(
            dismissOnBackPress = !update.required,
            dismissOnClickOutside = !update.required,
        ),
        title = { Text(update.title.ifBlank { stringResource(R.string.update_title) }) },
        text = { Text(update.message.ifBlank { stringResource(R.string.update_message, update.version) }) },
        confirmButton = {
            TextButton(onClick = {
                val url = update.url
                if (!url.isNullOrBlank()) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }) { Text(stringResource(R.string.update_now)) }
        },
        dismissButton = if (update.required) null else {
            { TextButton(onClick = { dismissed = true }) { Text(stringResource(R.string.update_later)) } }
        },
    )
}
