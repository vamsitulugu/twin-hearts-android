package com.twinhearts.app.feature.chat.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.TwinTextField
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.chat.domain.agoAmount
import com.twinhearts.app.feature.chat.domain.buildChatItems
import com.twinhearts.app.feature.chat.domain.formatTimer
import com.twinhearts.app.feature.chat.domain.model.ChatIdentity
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.PartnerPresence
import com.twinhearts.app.feature.chat.domain.model.PresenceState
import com.twinhearts.app.feature.chat.data.ViewOnceResult
import kotlinx.coroutines.delay
import java.io.File
import java.time.Instant
import java.time.LocalDate

private const val POLL_MS = 3_000L
private const val FULL_REFRESH_EVERY = 4
private const val HEARTBEAT_EVERY = 5
private const val BANNER_MS = 3_000L
private const val MIN_VOICE_SECONDS = 1
private val AvatarSize = 44.dp
private val DotSize = 9.dp
private val ComposerMinHeight = 48.dp

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onCall: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val voice = rememberVoicePlayer()
    val recorder = remember { VoiceRecorder(context.applicationContext) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showStarred by rememberSaveable { mutableStateOf(false) }
    var showScheduled by rememberSaveable { mutableStateOf(false) }
    var showViewOnce by rememberSaveable { mutableStateOf(false) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showReactDialog by rememberSaveable { mutableStateOf(false) }
    var editMessageId by remember { mutableStateOf<ChatMessage?>(null) }
    var editText by rememberSaveable { mutableStateOf("") }
    var deleteMessageId by remember { mutableStateOf<ChatMessage?>(null) }
    var reactMessageId by remember { mutableStateOf<ChatMessage?>(null) }
    var recordingSeconds by remember { mutableStateOf<Int?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // Keep the chat fresh while it is on screen; tell the server we are here (turns single ticks into double).
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.heartbeat(true)
            var tick = 0
            while (true) {
                delay(POLL_MS)
                tick++
                viewModel.poll(full = tick % FULL_REFRESH_EVERY == 0)
                if (tick % HEARTBEAT_EVERY == 0) viewModel.heartbeat(true)
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.heartbeat(false)
        recorder.cancel()
        recordingSeconds = null
    }
    LaunchedEffect(state.banner) {
        if (state.banner != null) {
            delay(BANNER_MS)
            viewModel.showBanner(null)
        }
    }
    LaunchedEffect(recordingSeconds != null) {
        while (recordingSeconds != null) {
            delay(1000)
            recordingSeconds = recordingSeconds?.plus(1)
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.sendPhoto(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = cameraUri
        cameraUri = null
        if (taken && uri != null) viewModel.sendPhoto(uri)
    }
    fun openCamera() {
        val uri = newCameraUri(context)
        if (uri == null) {
            viewModel.showBanner(ChatBanner.Info(R.string.chat_photo_failed))
        } else {
            cameraUri = uri
            camera.launch(uri)
        }
    }
    fun beginRecording() {
        if (recorder.start()) recordingSeconds = 0 else viewModel.showBanner(ChatBanner.Info(R.string.chat_record_failed))
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginRecording() else viewModel.showBanner(ChatBanner.Info(R.string.chat_mic_denied))
    }
    fun finishRecording(send: Boolean) {
        recordingSeconds = null
        if (!send) {
            recorder.cancel()
            return
        }
        val result = recorder.finish()
        when {
            result == null -> viewModel.showBanner(ChatBanner.Info(R.string.chat_record_failed))
            result.seconds < MIN_VOICE_SECONDS -> {
                result.file.delete()
                viewModel.showBanner(ChatBanner.Info(R.string.chat_record_short))
            }
            else -> viewModel.sendVoice(result.file, result.seconds)
        }
    }

    ChatContent(
        state = state,
        voice = voice,
        recordingSeconds = recordingSeconds,
        onBack = onBack,
        onCall = onCall,
        onRetryLoad = viewModel::load,
        onSync = { viewModel.poll(full = true) },
        onSend = viewModel::sendText,
        onGallery = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onCamera = ::openCamera,
        onStartRecording = {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) beginRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
        },
        onFinishRecording = ::finishRecording,
        onReply = viewModel::startReply,
        onCancelReply = viewModel::cancelReply,
        onCopy = { message ->
            message.text?.let {
                clipboard.setText(AnnotatedString(it))
                viewModel.showBanner(ChatBanner.Info(R.string.chat_info_copied))
            }
        },
        onStar = viewModel::toggleStar,
        onRetry = { viewModel.retry(it.clientId) },
        onVoiceFailed = { viewModel.showBanner(ChatBanner.Info(R.string.chat_record_failed)) },
        onOpenSearch = { showSearch = true },
        onOpenStarred = { showStarred = true },
        onOpenScheduled = { viewModel.refreshScheduled(); showScheduled = true },
        onEdit = { message -> editMessageId = message; editText = message.text.orEmpty(); showEditDialog = true },
        onDelete = { message -> deleteMessageId = message; showDeleteDialog = true },
        onReact = { message -> reactMessageId = message; showReactDialog = true },
        onViewOnce = { message ->
            viewModel.openViewOnce(message)
            showViewOnce = true
        },
    )

    if (showSearch) {
        ChatSearchDialog(
            state = state.search,
            partnerName = state.identity?.partnerName.orEmpty(),
            onSearch = viewModel::search,
            onClose = { showSearch = false; viewModel.clearSearch() },
        )
    }
    if (showStarred) {
        StarredDialog(
            messages = state.starred,
            partnerName = state.identity?.partnerName.orEmpty(),
            onUnstar = viewModel::toggleStar,
            onClose = { showStarred = false },
        )
    }
    if (showScheduled) {
        ScheduledDialog(
            scheduled = state.scheduled,
            scheduling = state.scheduling,
            onSchedule = viewModel::schedule,
            onCancel = viewModel::cancelScheduled,
            onClose = { showScheduled = false },
        )
    }
    editMessageId?.let { message ->
        EditDialog(
            message = message,
            onSave = { newText ->
                viewModel.editMessage(message, newText)
                showEditDialog = false
                editMessageId = null
            },
            onClose = { showEditDialog = false; editMessageId = null },
        )
    }
    deleteMessageId?.let { message ->
        DeleteDialog(
            message = message,
            onDelete = { mode ->
                viewModel.deleteMessage(message, mode)
                showDeleteDialog = false
                deleteMessageId = null
            },
            onClose = { showDeleteDialog = false; deleteMessageId = null },
        )
    }
    reactMessageId?.let { message ->
        ReactionDialog(
            message = message,
            onReact = { emoji ->
                viewModel.toggleReaction(message, emoji)
                showReactDialog = false
                reactMessageId = null
            },
            onClose = { showReactDialog = false; reactMessageId = null },
        )
    }
    state.viewOnceResult?.let { result ->
        ViewOnceDialog(
            result = result,
            onClose = { showViewOnce = false; viewModel.showBanner(null) },
        )
    }
}

/** Edit a message I sent: shows the current text and saves on confirm. */
@Composable
private fun EditDialog(
    message: ChatMessage,
    onSave: (String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = TwinTheme.colors
    var text by rememberSaveable { mutableStateOf(message.text.orEmpty()) }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        TwinBackground {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TwinTheme.spacing.screenEdge)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                Text(
                    stringResource(R.string.chat_dialog_edit_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink,
                )
                TwinTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = stringResource(R.string.chat_dialog_edit_hint),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    GlassButton(
                        text = stringResource(R.string.core_cancel),
                        onClick = onClose,
                        style = GlassButtonStyle.Ghost,
                        modifier = Modifier.width(100.dp),
                    )
                    GlassButton(
                        text = stringResource(R.string.chat_dialog_edit_button),
                        onClick = { onSave(text); onClose() },
                        style = GlassButtonStyle.Primary,
                        enabled = text.isNotBlank(),
                        modifier = Modifier.width(100.dp),
                    )
                }
            }
        }
    }
}

/** Delete a message: choose "for me" or "for everyone". */
@Composable
private fun DeleteDialog(
    message: ChatMessage,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = TwinTheme.colors
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        TwinBackground {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TwinTheme.spacing.screenEdge)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                Text(
                    stringResource(R.string.chat_dialog_delete_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink,
                )
                Text(
                    stringResource(R.string.chat_dialog_delete_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                ) {
                    GlassButton(
                        text = stringResource(R.string.chat_menu_delete_for_me),
                        onClick = { onDelete("me"); onClose() },
                        style = GlassButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    GlassButton(
                        text = stringResource(R.string.chat_menu_delete_for_everyone),
                        onClick = { onDelete("everyone"); onClose() },
                        style = GlassButtonStyle.Primary,
                        enabled = message.mine,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Add or remove an emoji reaction on a message. */
@Composable
private fun ReactionDialog(
    message: ChatMessage,
    onReact: (String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = TwinTheme.colors
    val emojis = listOf("❤️", "😍", "😂", "😭", "😡", "👍", "👎", "🎉")
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        TwinBackground {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TwinTheme.spacing.screenEdge)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                Text(
                    stringResource(R.string.chat_reaction_add),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink,
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                ) {
                    items(emojis, key = { it }) { emoji ->
                        val already = message.reactions.contains(emoji)
                        GlassButton(
                            text = "$emoji  ${if (already) stringResource(R.string.chat_menu_unstar) else stringResource(R.string.chat_menu_star)}",
                            onClick = { onReact(emoji); onClose() },
                            style = if (already) GlassButtonStyle.Primary else GlassButtonStyle.Secondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** Shows the view-once media URL after it has been opened (and burned). */
@Composable
private fun ViewOnceDialog(
    result: ViewOnceResult,
    onClose: () -> Unit,
) {
    val colors = TwinTheme.colors
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        TwinBackground {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TwinTheme.spacing.screenEdge)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                Text(
                    stringResource(if (result.alreadyOpen) R.string.chat_view_once_opened else R.string.chat_view_once_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink,
                )
                result.mediaUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = stringResource(R.string.chat_preview_photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(200.dp)
                            .clip(TwinTheme.shapes.small),
                    )
                    Text(
                        url,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                } ?: run {
                    Text(
                        stringResource(R.string.chat_view_once_expired),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted,
                    )
                }
                GlassButton(
                    text = stringResource(R.string.core_close),
                    onClick = onClose,
                    style = GlassButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** A fresh file in the app's cache for the camera to write to; `null` if the file provider is missing. */
private fun newCameraUri(context: Context): Uri? = try {
    val dir = File(context.cacheDir, "chat_camera").apply { mkdirs() }
    val file = File.createTempFile("photo-", ".jpg", dir)
    FileProvider.getUriForFile(context, "${context.packageName}.chatfiles", file)
} catch (e: Exception) {
    null
}

@Composable
internal fun ChatContent(
    state: ChatUiState,
    voice: VoicePlayerState,
    recordingSeconds: Int?,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onRetryLoad: () -> Unit,
    onSync: () -> Unit,
    onSend: (String) -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onStartRecording: () -> Unit,
    onFinishRecording: (Boolean) -> Unit,
    onReply: (ChatMessage) -> Unit,
    onCancelReply: () -> Unit,
    onCopy: (ChatMessage) -> Unit,
    onStar: (ChatMessage) -> Unit,
    onRetry: (ChatMessage) -> Unit,
    onVoiceFailed: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenStarred: () -> Unit,
    onOpenScheduled: () -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit,
    onReact: (ChatMessage) -> Unit,
    onViewOnce: (ChatMessage) -> Unit,
) {
    val partner = state.identity?.partnerName.orEmpty()
    val items = remember(state.messages) { buildChatItems(state.messages, LocalDate.now()) }
    TwinBackground {
        Column(Modifier.fillMaxSize().imePadding()) {
            TwinTopBar(title = stringResource(R.string.chat_title), showLogo = true, onSearchClick = onOpenSearch, onSyncClick = onSync)
            ChatHeader(
                partnerName = partner,
                presence = state.presence,
                onBack = onBack,
                onCall = onCall,
                onStarred = onOpenStarred,
                onSearch = onOpenSearch,
                onScheduled = onOpenScheduled,
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading && state.messages.isEmpty() -> LoadingSkeleton(Modifier.padding(TwinTheme.spacing.l), lines = 6)
                    state.loadError != null && state.messages.isEmpty() ->
                        ErrorState(message = state.loadError.asText(), onRetry = onRetryLoad)
                    state.messages.isEmpty() -> EmptyState(
                        title = stringResource(R.string.chat_empty_title),
                        message = stringResource(R.string.chat_empty_hint, partner.ifBlank { stringResource(R.string.chat_you) }),
                    )
                    else -> MessageList(
                        items = items,
                        allMessages = state.messages,
                        partnerName = partner,
                        voice = voice,
                        onReply = onReply,
                        onCopy = onCopy,
                        onStar = onStar,
                        onRetry = onRetry,
                        onVoiceFailed = onVoiceFailed,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onReact = onReact,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            state.banner?.let { banner ->
                Text(
                    text = banner.asText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (banner is ChatBanner.Failure) TwinTheme.colors.error else TwinTheme.colors.success,
                    modifier = Modifier.padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.xs),
                )
            }
            state.replyingTo?.let { ReplyBar(it, partner, onCancelReply) }
            Composer(
                recordingSeconds = recordingSeconds,
                onSend = onSend,
                onGallery = onGallery,
                onCamera = onCamera,
                onStartRecording = onStartRecording,
                onFinishRecording = onFinishRecording,
            )
        }
    }
}

@Composable
private fun ChatHeader(
    partnerName: String,
    presence: PartnerPresence,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onStarred: () -> Unit,
    onSearch: () -> Unit,
    onScheduled: () -> Unit,
) {
    val colors = TwinTheme.colors
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.glass)
            .border(1.dp, colors.lineSoft)
            .padding(horizontal = TwinTheme.spacing.xs, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.chat_back), tint = colors.ink)
        }
        Box(
            modifier = Modifier.size(AvatarSize).clip(TwinTheme.shapes.circle).background(colors.primaryBrush),
            contentAlignment = Alignment.Center,
        ) {
            Text(partnerName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = colors.ink)
        }
        Column(Modifier.weight(1f).padding(horizontal = TwinTheme.spacing.s)) {
            Text(partnerName, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                Box(
                    Modifier
                        .size(DotSize)
                        .clip(TwinTheme.shapes.circle)
                        .background(if (presence.state == PresenceState.Online) colors.success else colors.inkFaint),
                )
                val last = presence.lastSeen
                Text(
                    text = when (presence.state) {
                        PresenceState.Online -> stringResource(R.string.chat_presence_online)
                        PresenceState.Away -> stringResource(R.string.chat_presence_away)
                        PresenceState.Offline -> if (last != null) agoAmount(last, Instant.now()).asText() else stringResource(R.string.chat_presence_offline)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.inkMuted,
                    maxLines = 1,
                )
            }
        }
        IconButton(onClick = onCall) { Icon(Icons.Rounded.Call, stringResource(R.string.chat_action_call), tint = colors.ink) }
        IconButton(onClick = onCall) { Icon(Icons.Rounded.Videocam, stringResource(R.string.chat_action_video), tint = colors.ink) }
        IconButton(onClick = onStarred) { Icon(Icons.Rounded.StarBorder, stringResource(R.string.chat_action_starred), tint = colors.ink) }
        IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, stringResource(R.string.chat_action_search), tint = colors.ink) }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.chat_action_more), tint = colors.ink) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.chat_menu_scheduled)) }, onClick = { menu = false; onScheduled() })
            }
        }
    }
}

@Composable
private fun ReplyBar(message: ChatMessage, partnerName: String, onCancel: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.glass)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.chat_replying_to, if (message.mine) stringResource(R.string.chat_you) else partnerName),
                style = MaterialTheme.typography.labelMedium,
                color = colors.pinkSoft,
            )
            Text(message.previewText(), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, stringResource(R.string.chat_cancel_reply), tint = colors.ink) }
    }
}

@Composable
private fun Composer(
    recordingSeconds: Int?,
    onSend: (String) -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onStartRecording: () -> Unit,
    onFinishRecording: (Boolean) -> Unit,
) {
    val colors = TwinTheme.colors
    var text by rememberSaveable { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
    ) {
        if (recordingSeconds != null) {
            IconButton(onClick = { onFinishRecording(false) }) {
                Icon(Icons.Rounded.Close, stringResource(R.string.chat_recording_cancel), tint = colors.error)
            }
            Text(
                text = "${stringResource(R.string.chat_recording_label)}  ${formatTimer(recordingSeconds)}",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            RoundAction(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.chat_recording_send)) { onFinishRecording(true) }
        } else {
            IconButton(onClick = onGallery) { Icon(Icons.Rounded.Image, stringResource(R.string.chat_attach), tint = colors.ink) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = ComposerMinHeight)
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.glass)
                    .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
                    .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) {
                    Text(stringResource(R.string.chat_hint), style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.pinkSoft),
                    maxLines = 5,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Default),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (text.isBlank()) {
                IconButton(onClick = onCamera) { Icon(Icons.Rounded.CameraAlt, stringResource(R.string.chat_camera), tint = colors.ink) }
                RoundAction(Icons.Rounded.Mic, stringResource(R.string.chat_record), onStartRecording)
            } else {
                RoundAction(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.chat_send)) {
                    onSend(text)
                    text = ""
                }
            }
        }
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(TwinTheme.shapes.minTouch)
            .clip(TwinTheme.shapes.circle)
            .background(TwinTheme.colors.primaryBrush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = TwinTheme.colors.ink)
    }
}

// ───────────── previews ─────────────

private val previewWho = ChatIdentity("c", "user1", "Vamsi", "Likitha")

private fun previewMessage(id: Long, mine: Boolean, text: String, minutesAgo: Long = 5) = ChatMessage(
    id = id, clientId = "m$id", mine = mine, kind = com.twinhearts.app.feature.chat.domain.model.MessageKind.Text,
    text = text, mediaUrl = null, createdAt = Instant.now().minusSeconds(minutesAgo * 60),
)

@Composable
private fun PreviewHost(state: ChatUiState, fontScale: Float = 1f) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, fontScale),
    ) {
        TwinTheme {
            ChatContent(
                state, VoicePlayerState(), null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 720)
@Composable
private fun ChatPreview() {
    PreviewHost(
        ChatUiState(
            identity = previewWho, loading = false,
            messages = listOf(
                previewMessage(1, false, "Hlooo maa😘", 30),
                previewMessage(2, true, "Hlooo maaa🥹", 20),
                previewMessage(3, false, "Padukundhama🥺, eyes pain vastundhi maa", 10),
            ),
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 720)
@Composable
private fun ChatEmptyPreview() = PreviewHost(ChatUiState(identity = previewWho, loading = false))

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 720)
@Composable
private fun ChatErrorPreview() = PreviewHost(ChatUiState(identity = previewWho, loading = false, loadError = AppError.Network))

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 720, fontScale = 1.3f)
@Composable
private fun ChatLargeFontPreview() = PreviewHost(
    ChatUiState(identity = previewWho, loading = false, messages = listOf(previewMessage(1, false, "Hlooo maa😘"), previewMessage(2, true, "Hi 💕"))),
    fontScale = 1.3f,
)
