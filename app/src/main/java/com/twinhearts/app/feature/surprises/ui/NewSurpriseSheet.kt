package com.twinhearts.app.feature.surprises.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.surprises.domain.MAX_AUDIO_MB
import com.twinhearts.app.feature.surprises.domain.MAX_VIDEO_MB
import com.twinhearts.app.feature.surprises.domain.MediaProblem
import com.twinhearts.app.feature.surprises.domain.checkPickedMedia
import com.twinhearts.app.feature.surprises.domain.model.PickedMedia
import com.twinhearts.app.feature.surprises.domain.model.SurpriseKind
import java.time.LocalDate

/** Keeps a picked file across a screen rotation (the sheet's own state would otherwise be lost). */
private val PickedMediaSaver = listSaver<PickedMedia?, Any>(
    save = { media -> if (media == null) emptyList() else listOf(media.uri, media.name, media.mime, media.sizeBytes) },
    restore = { list ->
        if (list.size < 4) null else PickedMedia(list[0] as String, list[1] as String, list[2] as String, list[3] as Long)
    },
)

/**
 * The web's "New Surprise" modal: type, title, message, optional file, unlock date, "Hide it Away".
 * Checking the title and date is the ViewModel's job (it shows the web's own message).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSurpriseSheet(
    today: LocalDate,
    saving: Boolean,
    onSave: (SurpriseKind, String, String, PickedMedia?, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    val context = LocalContext.current
    var kindName by rememberSaveable { mutableStateOf(SurpriseKind.Gift.name) }
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var unlockText by rememberSaveable { mutableStateOf("") }
    var media by rememberSaveable(stateSaver = PickedMediaSaver) { mutableStateOf<PickedMedia?>(null) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    val unlock = runCatching { LocalDate.parse(unlockText) }.getOrNull()
    val kind = SurpriseKind.entries.firstOrNull { it.name == kindName } ?: SurpriseKind.Gift

    fun accept(uri: Uri?) {
        if (uri == null) return
        val picked = readPickedMedia(context, uri)
        val problem = picked?.let(::checkPickedMedia)
        val text = when {
            picked == null -> context.getString(R.string.surprises_toast_file_unreadable)
            problem == MediaProblem.Unsupported -> context.getString(R.string.surprises_toast_file_unsupported)
            problem == MediaProblem.TooBig -> context.getString(R.string.surprises_toast_file_too_big, MAX_VIDEO_MB, MAX_AUDIO_MB)
            else -> null
        }
        if (text != null) Toast.makeText(context, text, Toast.LENGTH_LONG).show() else media = picked
    }

    val photoVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { accept(it) }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { accept(it) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(
                text = stringResource(R.string.surprises_form_title),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = SerifFamily,
                color = colors.ink,
            )

            Text(stringResource(R.string.surprises_form_type), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                SurpriseKind.entries.forEach { option ->
                    TwinFilterChip(
                        label = "${option.emoji} ${stringResource(option.labelRes)}",
                        selected = option == kind,
                        onClick = { kindName = option.name },
                    )
                }
            }

            TwinTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.surprises_form_name),
                placeholder = stringResource(R.string.surprises_form_name_hint),
            )
            SurpriseTextArea(
                value = message,
                onValueChange = { message = it },
                label = stringResource(R.string.surprises_form_message),
                placeholder = stringResource(R.string.surprises_form_message_hint),
            )

            Text(stringResource(R.string.surprises_form_attach), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
            val attached = media
            if (attached == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    GlassButton(
                        text = stringResource(R.string.surprises_form_attach_photo_video),
                        onClick = {
                            photoVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                        style = GlassButtonStyle.Secondary,
                        leadingIcon = Icons.Rounded.Image,
                        modifier = Modifier.weight(1f),
                    )
                    GlassButton(
                        text = stringResource(R.string.surprises_form_attach_audio),
                        onClick = { audioPicker.launch("audio/*") },
                        style = GlassButtonStyle.Secondary,
                        leadingIcon = Icons.Rounded.Mic,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                ) {
                    Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = colors.accent)
                    Text(
                        text = attached.name.ifBlank { attached.mime },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { media = null }) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.surprises_form_remove_attachment), tint = colors.inkMuted)
                    }
                }
            }

            SurpriseDateField(
                label = stringResource(R.string.surprises_form_date),
                date = unlock,
                onClick = { showDate = true },
            )

            GlassButton(
                text = stringResource(R.string.surprises_form_save),
                onClick = { onSave(kind, title, message, media, unlock) },
                loading = saving,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().padding(bottom = TwinTheme.spacing.m),
            )
        }
    }

    if (showDate) {
        SurpriseDatePicker(
            initial = unlock,
            minDate = today,
            onPick = {
                unlockText = it.toString()
                showDate = false
            },
            onDismiss = { showDate = false },
        )
    }
}
