package com.twinhearts.app.core.appconfig

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTheme

/**
 * The "new version" card the admin dashboard controls (Releases page).
 *
 *  - Shown every time the app is opened, for as long as the installed version is older than the
 *    published one. It stops by itself once the person installs the new version (the server then
 *    answers "no update").
 *  - Update now = opens the download link, and the card is closed for this visit.
 *  - Later / Cancel = closes it for this visit; it comes back the next time the app is opened.
 *  - "Required" updates cannot be closed (no Later / Cancel, Back does nothing).
 *
 * "This visit" = until the app process is started again: the state is remembered across screen
 * rotation, but a new launch starts fresh.
 */
@Composable
fun UpdateGate(update: UpdateInfo?) {
    if (update == null) return
    val context = LocalContext.current
    var dismissed by rememberSaveable(update.version) { mutableStateOf(false) }
    if (!update.required && dismissed) return

    val close = { if (!update.required) dismissed = true }
    val download = {
        val url = update.url
        if (!url.isNullOrBlank()) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        close()
    }

    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(
            dismissOnBackPress = !update.required,
            dismissOnClickOutside = !update.required,
            usePlatformDefaultWidth = false,
        ),
    ) {
        UpdateCard(update = update, onUpdate = download, onLater = close, onCancel = close)
    }
}

@Composable
private fun UpdateCard(update: UpdateInfo, onUpdate: () -> Unit, onLater: () -> Unit, onCancel: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 28.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line2, shape)
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(colors.primaryBrush)
                .padding(horizontal = 12.dp, vertical = 5.dp),
        ) {
            Text(
                text = stringResource(R.string.update_version_label, update.version),
                color = colors.ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = update.title.ifBlank { stringResource(R.string.update_title) },
            color = colors.ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = update.message.ifBlank { stringResource(R.string.update_message, update.version) },
            color = colors.inkMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        if (update.required) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.update_required_hint),
                color = colors.inkFaint,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        GlassButton(
            text = stringResource(R.string.update_now),
            onClick = onUpdate,
            modifier = Modifier.fillMaxWidth(),
            style = GlassButtonStyle.Primary,
        )
        if (!update.required) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassButton(
                    text = stringResource(R.string.update_later),
                    onClick = onLater,
                    modifier = Modifier.weight(1f),
                    style = GlassButtonStyle.Secondary,
                )
                GlassButton(
                    text = stringResource(R.string.update_cancel),
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    style = GlassButtonStyle.Ghost,
                )
            }
        }
    }
}
