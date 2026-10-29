package com.twinhearts.app.feature.chat.ui

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Forward
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.PillSwitch
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.chat.data.PhotoPick
import com.twinhearts.app.feature.chat.domain.CoupleGifts
import com.twinhearts.app.feature.chat.domain.DisappearPresets
import com.twinhearts.app.feature.chat.domain.Stickers
import com.twinhearts.app.feature.chat.domain.agoAmount
import com.twinhearts.app.feature.chat.domain.buildChatItems
import com.twinhearts.app.feature.chat.domain.model.ChatIdentity
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import com.twinhearts.app.feature.chat.domain.model.PartnerPresence
import com.twinhearts.app.feature.chat.domain.model.PresenceState
import com.twinhearts.app.feature.chat.domain.model.SendEffect
import kotlinx.coroutines.delay
import java.io.File
import java.time.Instant
import java.time.LocalDate

/**
 * Why this file exists: the Chat screen. It owns the phone side of things (pickers, camera,
 * microphone, location, sharing, the idle lock) and hands plain state and callbacks to [ChatContent],
 * which only draws. All the logic lives in [ChatViewModel].
 */

private const val POLL_MS = 3_000L
private const val FULL_REFRESH_EVERY = 4
private const val HEARTBEAT_EVERY = 5
private const val BANNER_MS = 3_000L
private const val MIN_VOICE_SECONDS = 1
private const val MAX_PICKS = 10
private const val IDLE_CHECK_MS = 10_000L
private const val MS_PER_MINUTE = 60_000L
private val AutoLockChoices = listOf(0, 1, 5, 15)
private val AvatarSize = 40.dp
private val DotSize = 9.dp
private val MenuWidth = 280.dp

private enum class Sheet {
    None, Search, Starred, Scheduled, Shared, Wallpaper, Nickname, Dnd, Disappear, AutoLock, ClearConfirm,
    Attach, Sticker, Gift, Gif, Poll, Contact, Emoji,
}

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onCall: () -> Unit,
    onVideoCall: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val voice = rememberVoicePlayer()
    val recorder = remember { VoiceRecorder(context.applicationContext) }
    var sheet by rememberSaveable { mutableStateOf(Sheet.None) }
    var text by rememberSaveable { mutableStateOf("") }
    var recordingSeconds by remember { mutableStateOf<Int?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var menuMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var infoMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var reactTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var deleteTargets by remember { mutableStateOf<List<ChatMessage>?>(null) }
    var photoPicks by remember { mutableStateOf<List<PhotoPick>?>(null) }
    var viewer by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var scrollTo by remember { mutableStateOf<Long?>(null) }
    var locked by rememberSaveable { mutableStateOf(false) }
    var lastActive by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lockMinutes = state.settings.autoLockMinutes

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
        if (lockMinutes > 0) locked = true
    }
    LaunchedEffect(lockMinutes) {
        while (lockMinutes > 0) {
            delay(IDLE_CHECK_MS)
            if (System.currentTimeMillis() - lastActive > lockMinutes * MS_PER_MINUTE) locked = true
        }
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
    // The saved draft comes back once, when the chat opens with an empty box.
    LaunchedEffect(state.draft) { if (text.isEmpty() && state.draft.isNotEmpty()) text = state.draft }
    LaunchedEffect(text) {
        delay(DRAFT_MS)
        viewModel.saveDraft(text)
    }
    // Sharing the exported chat as a text file.
    LaunchedEffect(state.exportText) {
        state.exportText?.let { exported ->
            shareExport(context, exported)
            viewModel.exportShown()
        }
    }

    fun openUrl(url: String) = openExternally(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), viewModel)
    fun handlePicked(uris: List<Uri>) {
        val (videos, images) = uris.partition { context.contentResolver.getType(it)?.startsWith("video/") == true }
        videos.forEach { viewModel.sendVideo(it) }
        if (images.isNotEmpty()) photoPicks = images.map { PhotoPick(it) }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICKS)) { uris ->
        if (uris.isNotEmpty()) handlePicked(uris)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = cameraUri
        cameraUri = null
        if (taken && uri != null) photoPicks = listOf(PhotoPick(uri))
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) viewModel.sendFile(uri) }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) viewModel.sendAudioFile(uri) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) viewModel.shareLocation() else viewModel.showBanner(ChatBanner.Info(R.string.chat_location_denied))
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
    val unlock = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            locked = false
            lastActive = System.currentTimeMillis()
        }
    }
    fun requestUnlock() {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val intent = keyguard?.createConfirmDeviceCredentialIntent(null, null)
        if (intent == null) {
            locked = false
        } else {
            unlock.launch(intent)
        }
    }
    fun sendCurrentText(effect: SendEffect?) {
        viewModel.sendText(text, effect)
        text = ""
    }

    val bubbleActions = BubbleActions(
        onTap = { if (state.selected.isNotEmpty()) viewModel.toggleSelect(it) },
        onLongPress = { if (state.selected.isNotEmpty()) viewModel.toggleSelect(it) else menuMessage = it },
        onReply = viewModel::startReply,
        onRetry = { viewModel.retry(it.clientId) },
        onReact = viewModel::react,
        onOpenImages = { urls, index -> viewer = urls to index },
        onOpenViewOnce = viewModel::openViewOnce,
        onOpenUrl = ::openUrl,
        onOpenLocation = { m ->
            val geo = Uri.parse("geo:${m.latitude},${m.longitude}?q=${m.latitude},${m.longitude}")
            openExternally(context, Intent(Intent.ACTION_VIEW, geo), viewModel)
        },
        onVote = viewModel::vote,
        onJump = { scrollTo = it },
        onVoiceFailed = { viewModel.showBanner(ChatBanner.Info(R.string.chat_record_failed)) },
    )
    val composerActions = ComposerActions(
        onTextChange = { text = it },
        onSend = ::sendCurrentText,
        onSchedule = { sheet = Sheet.Scheduled },
        onAttach = { sheet = Sheet.Attach },
        onCamera = ::openCamera,
        onStartRecording = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (granted) beginRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
        },
        onFinishRecording = ::finishRecording,
    )
    val actions = ChatActions(
        onBack = onBack,
        onCall = onCall,
        onVideoCall = onVideoCall,
        onRetryLoad = viewModel::load,
        onSync = { viewModel.poll(full = true) },
        onOpenSearch = { sheet = Sheet.Search },
        onOpenStarred = { sheet = Sheet.Starred },
        onOpenScheduled = { viewModel.refreshScheduled(); sheet = Sheet.Scheduled },
        onOpenShared = { sheet = Sheet.Shared },
        onOpenWallpaper = { sheet = Sheet.Wallpaper },
        onNickname = { sheet = Sheet.Nickname },
        onDnd = { sheet = Sheet.Dnd },
        onDisappear = { sheet = Sheet.Disappear },
        onAutoLock = { sheet = Sheet.AutoLock },
        onExport = viewModel::exportChat,
        onClearChat = { sheet = Sheet.ClearConfirm },
        onToggleMute = viewModel::setMuted,
        onCancelReply = viewModel::cancelReply,
        onCancelEdit = { viewModel.cancelEdit(); text = "" },
        onSelectionClose = viewModel::clearSelection,
        onSelectionReply = { state.selectedMessages.singleOrNull()?.let { viewModel.startReply(it); viewModel.clearSelection() } },
        onSelectionForward = viewModel::forwardSelected,
        onSelectionCopy = {
            val copied = state.selectedMessages.mapNotNull { it.text }.joinToString("\n")
            if (copied.isNotEmpty()) {
                clipboard.setText(AnnotatedString(copied))
                viewModel.showBanner(ChatBanner.Info(R.string.chat_info_copied))
            }
            viewModel.clearSelection()
        },
        onSelectionStar = { state.selectedMessages.forEach(viewModel::toggleStar); viewModel.clearSelection() },
        onSelectionDelete = { deleteTargets = state.selectedMessages },
        onPinnedTap = { scrollTo = it },
        onUnpin = viewModel::togglePin,
        onEffectDone = viewModel::effectDone,
        bubble = bubbleActions,
        composer = composerActions,
    )

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastActive = System.currentTimeMillis()
                    }
                }
            },
    ) {
        ChatContent(
            state = state,
            voice = voice,
            text = text,
            recordingSeconds = recordingSeconds,
            scrollTo = scrollTo,
            onScrolled = { scrollTo = null },
            actions = actions,
        )
        if (locked) LockedOverlay(onUnlock = ::requestUnlock)
    }

    // ───── pop-ups ─────
    val who = state.identity
    val partnerName = who?.partnerName.orEmpty()
    when (sheet) {
        Sheet.None -> Unit
        Sheet.Search -> ChatSearchDialog(
            state = state.search,
            partnerName = partnerName,
            onSearch = viewModel::search,
            onClose = { sheet = Sheet.None; viewModel.clearSearch() },
        )
        Sheet.Starred -> StarredDialog(
            messages = state.starred,
            partnerName = partnerName,
            onUnstar = viewModel::toggleStar,
            onClose = { sheet = Sheet.None },
        )
        Sheet.Scheduled -> ScheduledDialog(
            scheduled = state.scheduled,
            scheduling = state.scheduling,
            initialText = text,
            onSchedule = { t, minutes -> viewModel.schedule(t, minutes); if (t.trim() == text.trim()) text = "" },
            onCancel = viewModel::cancelScheduled,
            onClose = { sheet = Sheet.None },
        )
        Sheet.Shared -> SharedMediaScreen(
            messages = state.messages,
            onOpenImages = { urls, index -> sheet = Sheet.None; viewer = urls to index },
            onOpenUrl = ::openUrl,
            onClose = { sheet = Sheet.None },
        )
        Sheet.Wallpaper -> WallpaperScreen(
            current = state.wallpaper,
            shared = state.wallpaperShared,
            saving = state.wallpaperSaving,
            onMeasure = viewModel::imageSize,
            onCut = viewModel::cutWallpaper,
            onSave = viewModel::saveWallpaper,
            onReset = viewModel::resetWallpaper,
            onClose = { sheet = Sheet.None },
        )
        Sheet.Nickname -> NicknameDialog(state.settings.nickname, onSave = viewModel::setNickname, onDismiss = { sheet = Sheet.None })
        Sheet.Dnd -> DndDialog(state.settings.dndStart, state.settings.dndEnd, onSave = viewModel::setDnd, onDismiss = { sheet = Sheet.None })
        Sheet.Disappear -> ChoiceDialog(
            title = stringResource(R.string.chat_disappear_title),
            labels = disappearLabels(),
            selectedIndex = DisappearPresets.indexOf(state.disappearingSeconds).coerceAtLeast(0),
            onPick = { viewModel.setDisappearing(DisappearPresets[it]) },
            onDismiss = { sheet = Sheet.None },
        )
        Sheet.AutoLock -> ChoiceDialog(
            title = stringResource(R.string.chat_autolock_title),
            labels = AutoLockChoices.map { if (it == 0) stringResource(R.string.chat_value_off) else stringResource(R.string.chat_autolock_minutes, it) },
            selectedIndex = AutoLockChoices.indexOf(lockMinutes).coerceAtLeast(0),
            onPick = { index ->
                val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                if (AutoLockChoices[index] > 0 && keyguard?.isDeviceSecure != true) {
                    viewModel.showBanner(ChatBanner.Info(R.string.chat_autolock_needs_lock))
                } else {
                    viewModel.setAutoLock(AutoLockChoices[index])
                    lastActive = System.currentTimeMillis()
                }
            },
            onDismiss = { sheet = Sheet.None },
        )
        Sheet.ClearConfirm -> ConfirmDialog(
            title = stringResource(R.string.chat_clear_title),
            body = stringResource(R.string.chat_clear_body),
            confirmLabel = stringResource(R.string.chat_clear_confirm),
            onConfirm = viewModel::clearChat,
            onDismiss = { sheet = Sheet.None },
        )
        Sheet.Attach -> AttachSheet(
            actions = AttachActions(
                onPhotos = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                onCamera = ::openCamera,
                onDocument = { documentPicker.launch(arrayOf("*/*")) },
                onAudio = { audioPicker.launch(arrayOf("audio/*")) },
                onLocation = {
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (fine || coarse) {
                        viewModel.shareLocation()
                    } else {
                        locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                },
                onContact = { sheet = Sheet.Contact },
                onPoll = { sheet = Sheet.Poll },
                onSticker = { sheet = Sheet.Sticker },
                onGift = { sheet = Sheet.Gift },
                onGif = { sheet = Sheet.Gif },
                onSchedule = { sheet = Sheet.Scheduled },
                onEmoji = { sheet = Sheet.Emoji },
            ),
            onDismiss = { if (sheet == Sheet.Attach) sheet = Sheet.None },
        )
        Sheet.Sticker -> EmojiCardSheet(
            title = stringResource(R.string.chat_attach_sticker),
            items = Stickers,
            onPick = { viewModel.sendEmojiCard("sticker", it.emoji, it.name) },
            onDismiss = { sheet = Sheet.None },
        )
        Sheet.Gift -> EmojiCardSheet(
            title = stringResource(R.string.chat_attach_gift),
            items = CoupleGifts,
            onPick = { viewModel.sendEmojiCard("gift", it.emoji, it.name) },
            onDismiss = { sheet = Sheet.None },
        )
        Sheet.Gif -> GifSheet(state.gifs, onSearch = viewModel::searchGifs, onPick = viewModel::sendGif, onDismiss = { sheet = Sheet.None })
        Sheet.Poll -> PollSheet(onCreate = viewModel::sendPoll, onDismiss = { sheet = Sheet.None })
        Sheet.Contact -> ContactSheet(onCreate = viewModel::sendContact, onDismiss = { sheet = Sheet.None })
        Sheet.Emoji -> EmojiPickerSheet(
            recent = state.recentEmoji,
            closeOnPick = false,
            onPick = { text += it; viewModel.useEmoji(it) },
            onDismiss = { sheet = Sheet.None },
        )
    }
    menuMessage?.let { target ->
        MessageMenuSheet(
            message = target,
            actions = MessageMenuActions(
                onSelect = { viewModel.toggleSelect(target) },
                onReact = { emoji -> if (emoji == MORE_EMOJI) reactTarget = target else viewModel.react(target, emoji) },
                onReply = { viewModel.startReply(target) },
                onForward = { viewModel.toggleSelect(target); viewModel.forwardSelected() },
                onCopy = {
                    target.text?.let {
                        clipboard.setText(AnnotatedString(it))
                        viewModel.showBanner(ChatBanner.Info(R.string.chat_info_copied))
                    }
                },
                onPin = { viewModel.togglePin(target) },
                onStar = { viewModel.toggleStar(target) },
                onEdit = { viewModel.startEdit(target); text = target.text.orEmpty() },
                onInfo = { infoMessage = target },
                onDeleteEveryone = { deleteTargets = listOf(target) },
                onDeleteMe = { deleteTargets = listOf(target.copy(mine = false)) },
            ),
            onDismiss = { menuMessage = null },
        )
    }
    reactTarget?.let { target ->
        EmojiPickerSheet(
            recent = state.recentEmoji,
            closeOnPick = true,
            onPick = { viewModel.react(target, it) },
            onDismiss = { reactTarget = null },
        )
    }
    infoMessage?.let { MessageInfoDialog(it, onDismiss = { infoMessage = null }) }
    deleteTargets?.let { targets ->
        ConfirmDeleteDialog(
            count = targets.size,
            canEveryone = targets.all { it.mine },
            onConfirm = { everyone -> viewModel.delete(targets, everyone) },
            onDismiss = { deleteTargets = null },
        )
    }
    photoPicks?.let { picks ->
        PhotoPreviewSheet(
            photos = picks,
            onSend = { list, caption, viewOnce -> viewModel.sendPhotos(list, caption, viewOnce) },
            onDismiss = { photoPicks = null },
        )
    }
    viewer?.let { (urls, index) -> ImageViewerDialog(urls, index, onClose = { viewer = null }) }
    state.viewOnce?.let { result ->
        if (result.isVideo) {
            LaunchedEffect(result) {
                openExternally(context, Intent(Intent.ACTION_VIEW, Uri.parse(result.url)), viewModel)
                viewModel.closeViewOnce()
            }
        } else {
            ViewOnceDialog(result, onClose = viewModel::closeViewOnce)
        }
    }
}

private const val DRAFT_MS = 600L

/** Shown while the chat is locked; the phone's own screen lock opens it again. */
@Composable
private fun LockedOverlay(onUnlock: () -> Unit) {
    val colors = TwinTheme.colors
    TwinBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(TwinTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🔒", style = MaterialTheme.typography.displayLarge)
            Text(stringResource(R.string.chat_locked_title), style = MaterialTheme.typography.titleLarge, color = colors.ink)
            Text(stringResource(R.string.chat_locked_hint), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
            GlassButton(text = stringResource(R.string.chat_unlock), onClick = onUnlock)
        }
    }
}

/** Opens a link, map or video in another app; tells the person when none can open it. */
private fun openExternally(context: Context, intent: Intent, viewModel: ChatViewModel) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        viewModel.showBanner(ChatBanner.Info(R.string.chat_no_app))
    }
}

/** Saves the exported chat as a text file in the cache and offers it to the share sheet. */
private fun shareExport(context: Context, exported: String) {
    val uri = try {
        val dir = File(context.cacheDir, "chat_export").apply { mkdirs() }
        val file = File(dir, "chat-export.txt")
        file.writeText(exported)
        FileProvider.getUriForFile(context, "${context.packageName}.chatfiles", file)
    } catch (e: java.io.IOException) {
        null
    } ?: return
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
        // Nothing can take a text file on this phone; there is nothing more to do.
    }
}

/** A fresh file in the app's cache for the camera to write to; `null` if the file provider is missing. */
private fun newCameraUri(context: Context): Uri? = try {
    val dir = File(context.cacheDir, "chat_camera").apply { mkdirs() }
    val file = File.createTempFile("photo-", ".jpg", dir)
    FileProvider.getUriForFile(context, "${context.packageName}.chatfiles", file)
} catch (e: java.io.IOException) {
    null
} catch (e: IllegalArgumentException) {
    null
}

/** Marker the menu uses for "more reactions…"; the screen turns it into the full emoji picker. */
internal const val MORE_EMOJI = "…"

/** Everything the screen can be asked to do, so [ChatContent] stays a plain function of state. */
internal class ChatActions(
    val onBack: () -> Unit = {},
    val onCall: () -> Unit = {},
    val onVideoCall: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onSync: () -> Unit = {},
    val onOpenSearch: () -> Unit = {},
    val onOpenStarred: () -> Unit = {},
    val onOpenScheduled: () -> Unit = {},
    val onOpenShared: () -> Unit = {},
    val onOpenWallpaper: () -> Unit = {},
    val onNickname: () -> Unit = {},
    val onDnd: () -> Unit = {},
    val onDisappear: () -> Unit = {},
    val onAutoLock: () -> Unit = {},
    val onExport: () -> Unit = {},
    val onClearChat: () -> Unit = {},
    val onToggleMute: (Boolean) -> Unit = {},
    val onCancelReply: () -> Unit = {},
    val onCancelEdit: () -> Unit = {},
    val onSelectionClose: () -> Unit = {},
    val onSelectionReply: () -> Unit = {},
    val onSelectionForward: () -> Unit = {},
    val onSelectionCopy: () -> Unit = {},
    val onSelectionStar: () -> Unit = {},
    val onSelectionDelete: () -> Unit = {},
    val onPinnedTap: (Long) -> Unit = {},
    val onUnpin: (ChatMessage) -> Unit = {},
    val onEffectDone: () -> Unit = {},
    val bubble: BubbleActions = BubbleActions.None,
    val composer: ComposerActions = ComposerActions({}, {}, {}, {}, {}, {}, {}),
)

@Composable
internal fun ChatContent(
    state: ChatUiState,
    voice: VoicePlayerState,
    text: String,
    recordingSeconds: Int?,
    scrollTo: Long?,
    onScrolled: () -> Unit,
    actions: ChatActions,
) {
    val partner = state.settings.nickname.ifBlank { state.identity?.partnerName.orEmpty() }
    val items = remember(state.messages) { buildChatItems(state.messages, LocalDate.now()) }
    Box(Modifier.fillMaxSize()) {
        TwinBackground {
            Box(Modifier.fillMaxSize()) {
                WallpaperLayer(state.wallpaper, Modifier.fillMaxSize())
                Column(Modifier.fillMaxSize().imePadding()) {
                    TwinTopBar(title = stringResource(R.string.chat_title), showLogo = true, showSidebarButton = true, onSearchClick = actions.onOpenSearch, onSyncClick = actions.onSync)
                    if (state.selected.isNotEmpty()) {
                        SelectionBar(state, actions)
                    } else {
                        ChatHeader(partner, state, actions)
                    }
                    state.pinned.lastOrNull()?.let { PinnedBar(it, state.pinned.size, actions) }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            state.loading && state.messages.isEmpty() -> LoadingSkeleton(Modifier.padding(TwinTheme.spacing.l), lines = 6)
                            state.loadError != null && state.messages.isEmpty() ->
                                ErrorState(message = state.loadError.asText(), onRetry = actions.onRetryLoad)
                            state.messages.isEmpty() -> EmptyState(
                                title = stringResource(R.string.chat_empty_title),
                                message = stringResource(R.string.chat_empty_hint, partner.ifBlank { stringResource(R.string.chat_you) }),
                            )
                            else -> MessageList(
                                items = items,
                                allMessages = state.messages,
                                partnerName = partner,
                                voice = voice,
                                selected = state.selected,
                                actions = actions.bubble,
                                scrollTo = scrollTo,
                                onScrolled = onScrolled,
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
                    state.editing?.let { EditBar(it, actions.onCancelEdit) }
                    state.replyingTo?.let { ReplyBar(it, partner, actions.onCancelReply) }
                    Composer(text, recordingSeconds, editing = state.editing != null, actions = actions.composer)
                }
            }
        }
        state.effect?.let { EffectOverlay(it, actions.onEffectDone) }
    }
}

@Composable
private fun ChatHeader(partnerName: String, state: ChatUiState, actions: ChatActions) {
    val colors = TwinTheme.colors
    val presence = state.presence
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.glass)
            .border(1.dp, colors.lineSoft)
            .padding(horizontal = TwinTheme.spacing.xs, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
    ) {
        IconButton(onClick = actions.onBack, modifier = Modifier.size(HeaderButtonSize)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.chat_back), tint = colors.ink)
        }
        Box(
            modifier = Modifier.size(AvatarSize).clip(TwinTheme.shapes.circle).background(colors.primaryBrush),
            contentAlignment = Alignment.Center,
        ) {
            Text(partnerName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = colors.ink)
        }
        Column(Modifier.weight(1f).padding(horizontal = TwinTheme.spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                Text(partnerName, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (state.settings.muted) Icon(Icons.Rounded.VolumeOff, stringResource(R.string.chat_muted), tint = colors.inkMuted, modifier = Modifier.size(DotSize + DotSize))
            }
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
        HeaderButton(Icons.Rounded.Call, stringResource(R.string.chat_action_call), actions.onCall)
        HeaderButton(Icons.Rounded.Videocam, stringResource(R.string.chat_action_video), actions.onVideoCall)
        HeaderButton(Icons.Rounded.StarBorder, stringResource(R.string.chat_action_starred), actions.onOpenStarred)
        HeaderButton(Icons.Rounded.Search, stringResource(R.string.chat_action_search), actions.onOpenSearch)
        Box {
            HeaderButton(Icons.Rounded.MoreVert, stringResource(R.string.chat_action_more)) { menu = true }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.width(MenuWidth)) {
                MenuSwitch(stringResource(R.string.chat_menu_mute), state.settings.muted) { actions.onToggleMute(it) }
                MenuRow(stringResource(R.string.chat_menu_shared)) { menu = false; actions.onOpenShared() }
                MenuRow(stringResource(R.string.chat_menu_wallpaper)) { menu = false; actions.onOpenWallpaper() }
                MenuRow(stringResource(R.string.chat_menu_scheduled)) { menu = false; actions.onOpenScheduled() }
                MenuRow(stringResource(R.string.chat_menu_nickname)) { menu = false; actions.onNickname() }
                MenuRow(stringResource(R.string.chat_menu_export)) { menu = false; actions.onExport() }
                MenuRow(stringResource(R.string.chat_menu_clear), danger = true) { menu = false; actions.onClearChat() }
                MenuValue(stringResource(R.string.chat_menu_disappearing), disappearValue(state.disappearingSeconds)) { menu = false; actions.onDisappear() }
                MenuValue(
                    stringResource(R.string.chat_menu_autolock),
                    if (state.settings.autoLockMinutes > 0) stringResource(R.string.chat_autolock_minutes, state.settings.autoLockMinutes) else stringResource(R.string.chat_value_off),
                ) { menu = false; actions.onAutoLock() }
                MenuValue(
                    stringResource(R.string.chat_menu_dnd),
                    if (state.settings.dndStart != null && state.settings.dndEnd != null) "${state.settings.dndStart}–${state.settings.dndEnd}" else stringResource(R.string.chat_value_off),
                ) { menu = false; actions.onDnd() }
            }
        }
    }
}

/** Small round glass button of the chat header (small enough that all five fit on a narrow phone). */
@Composable
private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Box(
        modifier = Modifier
            .size(HeaderButtonSize)
            .clip(TwinTheme.shapes.circle)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.circle)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = colors.ink, modifier = Modifier.size(HeaderIconSize))
    }
}

private val HeaderButtonSize = 36.dp
private val HeaderIconSize = 20.dp

@Composable
private fun MenuRow(label: String, danger: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = if (danger) TwinTheme.colors.error else TwinTheme.colors.ink, style = MaterialTheme.typography.bodyMedium) },
        onClick = onClick,
    )
}

@Composable
private fun MenuValue(label: String, value: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = TwinTheme.colors.ink, style = MaterialTheme.typography.bodyMedium) },
        trailingIcon = { Text(value, color = TwinTheme.colors.pinkSoft, style = MaterialTheme.typography.labelLarge) },
        onClick = onClick,
    )
}

@Composable
private fun MenuSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = TwinTheme.colors.ink, style = MaterialTheme.typography.bodyMedium) },
        trailingIcon = { PillSwitch(checked = checked, onCheckedChange = onChange) },
        onClick = { onChange(!checked) },
    )
}

@Composable
private fun disappearValue(seconds: Int): String = when {
    seconds <= 0 -> stringResource(R.string.chat_value_off)
    seconds < DAY_SECONDS -> stringResource(R.string.chat_disappear_value_hours, seconds / HOUR_SECONDS)
    else -> stringResource(R.string.chat_disappear_value_days, seconds / DAY_SECONDS)
}

private const val DAY_SECONDS = 86_400
private const val HOUR_SECONDS = 3_600

/** The header while messages are selected: close, count, and the actions that fit. */
@Composable
private fun SelectionBar(state: ChatUiState, actions: ChatActions) {
    val colors = TwinTheme.colors
    val picked = state.selectedMessages
    val hasText = picked.any { !it.text.isNullOrBlank() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.glass)
            .border(1.dp, colors.lineSoft)
            .padding(horizontal = TwinTheme.spacing.xs, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onSelectionClose) { Icon(Icons.Rounded.Close, stringResource(R.string.chat_close), tint = colors.ink) }
        Text(stringResource(R.string.chat_selected_count, picked.size), style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
        if (picked.size == 1) IconButton(onClick = actions.onSelectionReply) { Icon(Icons.Rounded.Reply, stringResource(R.string.chat_menu_reply), tint = colors.ink) }
        IconButton(onClick = actions.onSelectionForward) { Icon(Icons.Rounded.Forward, stringResource(R.string.chat_menu_forward), tint = colors.ink) }
        if (hasText) IconButton(onClick = actions.onSelectionCopy) { Icon(Icons.Rounded.ContentCopy, stringResource(R.string.chat_menu_copy), tint = colors.ink) }
        IconButton(onClick = actions.onSelectionStar) { Icon(Icons.Rounded.StarBorder, stringResource(R.string.chat_menu_star), tint = colors.ink) }
        IconButton(onClick = actions.onSelectionDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.chat_menu_delete_me), tint = colors.error) }
    }
}

@Composable
private fun PinnedBar(message: ChatMessage, count: Int, actions: ChatActions) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.glass)
            .clickable { actions.onPinnedTap(message.id) }
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Icon(Icons.Rounded.PushPin, null, tint = colors.pinkSoft)
        Column(Modifier.weight(1f)) {
            Text(
                if (count > 1) stringResource(R.string.chat_pinned_many, count) else stringResource(R.string.chat_pinned),
                style = MaterialTheme.typography.labelMedium,
                color = colors.pinkSoft,
            )
            Text(message.previewText(), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = { actions.onUnpin(message) }) { Icon(Icons.Rounded.Close, stringResource(R.string.chat_menu_unpin), tint = colors.ink) }
    }
}

// ───────────── previews ─────────────

private val previewWho = ChatIdentity("c", "user1", "Vamsi", "Likitha")

private fun previewMessage(id: Long, mine: Boolean, text: String, minutesAgo: Long = 5) = ChatMessage(
    id = id, clientId = "m$id", mine = mine, kind = MessageKind.Text,
    text = text, mediaUrl = null, createdAt = Instant.now().minusSeconds(minutesAgo * 60),
)

@Composable
private fun PreviewHost(state: ChatUiState, fontScale: Float = 1f) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        TwinTheme {
            ChatContent(state, VoicePlayerState(), "", null, null, {}, ChatActions())
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 720)
@Composable
private fun ChatPreview() {
    PreviewHost(
        ChatUiState(
            identity = previewWho, loading = false,
            presence = PartnerPresence(PresenceState.Online, null),
            messages = listOf(
                previewMessage(1, false, "Hlooo maa😘", 30),
                previewMessage(2, true, "Hlooo maaa🥹", 20),
                previewMessage(3, true, "*Bold* and _italic_ with https://example.com", 18),
                previewMessage(4, false, "Padukundhama🥺, eyes pain vastundhi maa", 10),
            ),
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 720)
@Composable
private fun ChatLoadingPreview() = PreviewHost(ChatUiState(identity = previewWho, loading = true))

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
