package com.twinhearts.app.feature.movie.ui.components

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.movie.domain.formatRuntime
import com.twinhearts.app.feature.movie.domain.model.ChatMessage
import com.twinhearts.app.feature.movie.domain.model.MovieReaction
import com.twinhearts.app.feature.movie.domain.model.WatchRoom
import com.twinhearts.app.feature.movie.ui.MovieCallHooks
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max

/**
 * Why this file exists: the cinema screen. It plays the movie with Media3 and keeps it in step with
 * the partner using the shared room (same rules as the web app): jump when 2 s or more apart, nudge
 * the speed by 3 % when 0.5 to 2 s apart. It only reads the room; actions go out through [onAction].
 * The look (round buttons, status pill, red timeline, six icon buttons, reactions, chat toast) is in
 * PlayerChrome.kt and the call windows are in WatchCallOverlay.kt; this file decides when to show them.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    room: WatchRoom,
    streamUrl: String?,
    chat: List<ChatMessage>,
    serverNowMs: () -> Long,
    onAction: (playing: Boolean, positionSec: Double) -> Unit,
    onSendChat: (text: String, positionSec: Double) -> Unit,
    onEnd: (positionSec: Double) -> Unit,
    reactions: List<MovieReaction>,
    onReaction: (String) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOn: (Boolean) -> Unit,
    isCloud: Boolean,
    cloudSizeText: String?,
    callHooks: MovieCallHooks?,
    partnerName: String,
    subtitle: SubtitleChoice?,
    onSubtitle: (SubtitleChoice?) -> Unit,
) {
    val context = LocalContext.current
    val colors = TwinTheme.colors
    val density = LocalDensity.current
    val latestRoom by rememberUpdatedState(room)
    val player = remember { ExoPlayer.Builder(context).build() }
    var positionSec by remember { mutableDoubleStateOf(0.0) }
    var scrubbing by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var touches by remember { mutableIntStateOf(0) }
    var showEnd by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var composing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var syncedPill by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showReactions by remember { mutableStateOf(false) }
    var fit by remember { mutableStateOf(ScreenFit.Fit) }
    var fullscreen by remember { mutableStateOf(false) }
    var modeLabel by remember { mutableStateOf<String?>(null) }
    var modeLabelKey by remember { mutableIntStateOf(0) }
    var sleepMin by remember { mutableIntStateOf(0) }
    var inPip by remember { mutableStateOf(false) }
    var overlayPx by remember { mutableIntStateOf(0) }
    var everPlayed by remember { mutableStateOf(room.playing) }
    var chatToast by remember { mutableStateOf<String?>(null) }
    val chatQueue = remember { mutableStateListOf<String>() }
    // Messages already in the list when the player opens are old; only new ones pop up.
    val seenChat = remember { mutableSetOf<String>().apply { chat.forEach { add(it.id) } } }
    val chatFocus = remember { FocusRequester() }
    val activity = remember(context) { context.findActivity() }
    val pipAvailable = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val durationSec = (room.myDurationSec ?: 1.0).coerceAtLeast(1.0)
    val noCallText = stringResource(R.string.movie_toast_call_unavailable)

    val exo = player
    ImmersiveEffect()
    KeepScreenOnEffect(keepScreenOn && room.playing)
    DisposableEffect(player) { onDispose { player.release() } }
    // Leaving the cinema always gives the phone its rotation back.
    DisposableEffect(activity) { onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED } }

    fun flashMode(text: String) {
        modeLabel = text
        modeLabelKey++
    }

    // Fullscreen = landscape. While already fullscreen the same button cycles Fit, Fill, Zoom (like the web page).
    fun exitFullscreen() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        fullscreen = false
        fit = ScreenFit.Fit
    }
    fun onFullscreenTap() {
        if (!fullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            fullscreen = true
            fit = ScreenFit.Fit
        } else {
            fit = fit.next()
        }
        flashMode(context.getString(fit.label))
        touches++
    }
    BackHandler { if (fullscreen) exitFullscreen() else showEnd = true }

    LaunchedEffect(modeLabelKey) {
        if (modeLabel == null) return@LaunchedEffect
        delay(MODE_LABEL_MS)
        modeLabel = null
    }
    // Show the .srt/.vtt file even when the movie has no language tag of its own.
    LaunchedEffect(player) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setPreferredTextLanguage(SUBTITLE_LANGUAGE)
            .setSelectUndeterminedTextLanguage(true)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
    }
    // Picture in Picture: hide the controls while the small window is up.
    DisposableEffect(activity) {
        val host = activity as? ComponentActivity
        val listener = Consumer<PictureInPictureModeChangedInfo> { inPip = it.isInPictureInPictureMode }
        host?.addOnPictureInPictureModeChangedListener(listener)
        onDispose { host?.removeOnPictureInPictureModeChangedListener(listener) }
    }
    val subsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val choice = readSubtitle(context, uri)
        if (choice == null) {
            Toast.makeText(context, R.string.movie_toast_subs_failed, Toast.LENGTH_SHORT).show()
        } else {
            onSubtitle(choice)
            Toast.makeText(context, R.string.movie_toast_subs_loaded, Toast.LENGTH_SHORT).show()
        }
    }
    // Sleep timer: pauses the movie for both of you, like the web page.
    LaunchedEffect(sleepMin) {
        if (sleepMin <= 0) return@LaunchedEffect
        delay(sleepMin * MS_PER_MINUTE)
        sleepMin = 0
        if (player.playWhenReady) {
            player.playWhenReady = false
            onAction(false, player.currentPosition / MS)
            Toast.makeText(context, R.string.movie_toast_sleep, Toast.LENGTH_LONG).show()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    Toast.makeText(context, R.string.movie_finished, Toast.LENGTH_SHORT).show()
                    onEnd(durationSec)
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Open the movie, then jump to where the room says we should be (also covers rejoining mid-movie).
    LaunchedEffect(streamUrl, subtitle) {
        val url = streamUrl ?: return@LaunchedEffect
        // Adding or removing subtitles reloads the movie; keep the same spot and play state.
        val reload = player.mediaItemCount > 0
        val startMs = if (reload) player.currentPosition else (latestRoom.expectedPositionSec(serverNowMs()) * 1000).toLong()
        val playing = if (reload) player.playWhenReady else latestRoom.playing
        player.setMediaItem(buildMediaItem(url, subtitle), startMs)
        player.prepare()
        player.playWhenReady = playing
    }

    // Someone (the partner, or this phone's own echo) played, paused or jumped.
    LaunchedEffect(room.actionSeq, room.playing) {
        if (room.playing) everPlayed = true
        if (streamUrl == null) return@LaunchedEffect
        val expected = room.expectedPositionSec(serverNowMs())
        if (abs(player.currentPosition / MS - expected) > SEEK_TOLERANCE_SEC) player.seekTo((expected * MS).toLong())
        player.playWhenReady = room.playing
    }

    // Small drift correction once a second. It never writes to the server.
    LaunchedEffect(streamUrl) {
        while (true) {
            delay(DRIFT_EVERY_MS)
            val r = latestRoom
            if (streamUrl == null || !r.playing || scrubbing) continue
            val expected = r.expectedPositionSec(serverNowMs())
            val diff = expected - player.currentPosition / MS
            when {
                abs(diff) < 0.5 -> player.setPlaybackSpeed(1f)
                abs(diff) < 2.0 -> player.setPlaybackSpeed(if (diff > 0) 1.03f else 0.97f)
                else -> {
                    player.setPlaybackSpeed(1f)
                    player.seekTo((expected * MS).toLong())
                }
            }
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (!scrubbing) positionSec = player.currentPosition / MS
            delay(POSITION_EVERY_MS)
        }
    }
    // Controls fade after 4 s, but never while a sheet, dialog or the message bar is open (web "controlsLocked").
    val locked = composing || showMore || showEnd || showLog
    LaunchedEffect(controlsVisible, touches, locked) {
        if (controlsVisible && !locked) {
            delay(HIDE_AFTER_MS)
            controlsVisible = false
        }
    }
    // Partner messages pop up one at a time above the timeline.
    LaunchedEffect(chat) {
        chat.forEach { m ->
            if (seenChat.add(m.id) && !m.fromMe) {
                chatQueue.add(m.text)
                controlsVisible = true
                touches++
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val next = chatQueue.firstOrNull()
            if (next == null) {
                delay(CHAT_IDLE_MS)
                continue
            }
            chatToast = next
            delay(CHAT_SHOW_MS)
            chatToast = null
            delay(CHAT_GAP_MS)
            chatQueue.removeAt(0)
        }
    }
    LaunchedEffect(syncedPill) {
        if (syncedPill) {
            delay(PILL_MS)
            syncedPill = false
        }
    }
    LaunchedEffect(composing) {
        if (composing) {
            delay(FOCUS_DELAY_MS)
            runCatching { chatFocus.requestFocus() }
        }
    }

    fun togglePlay() {
        val play = !player.playWhenReady
        player.playWhenReady = play
        onAction(play, player.currentPosition / MS)
    }

    fun seekTo(sec: Double) {
        val target = sec.coerceIn(0.0, durationSec)
        player.seekTo((target * MS).toLong())
        positionSec = target
        onAction(player.playWhenReady, target)
    }

    fun sendDraft() {
        val text = draft.trim()
        if (text.isEmpty()) return
        onSendChat(text, player.currentPosition / MS)
        // Own message shows as a toast too, like the web page. The echo from the server is ignored (fromMe).
        chatQueue.add(text)
        draft = ""
    }

    fun startCall(video: Boolean) {
        if (callHooks == null) Toast.makeText(context, noCallText, Toast.LENGTH_SHORT).show() else callHooks.start(video)
        touches++
    }

    // The pill under the title: "Paused", "Paused by Partner" or "✓ In Sync" (same texts as the web page).
    val pill: PillState? = when {
        syncedPill -> PillState(stringResource(R.string.movie_in_sync), PillKind.Good)
        !room.playing && everPlayed -> PillState(
            stringResource(if (room.updatedByMe) R.string.movie_pill_paused else R.string.movie_pill_paused_partner), PillKind.Warn,
        )
        else -> null
    }
    val overlayDp = with(density) { overlayPx.toDp() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { useController = false; this.player = exo } },
            update = { view -> view.resizeMode = fit.resizeMode() },
            modifier = Modifier.fillMaxSize(),
        )
        if (streamUrl == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = colors.accent)
                Text(stringResource(R.string.movie_loading_video), color = colors.inkMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = TwinTheme.spacing.m))
            }
        }
        if (inPip) return@Box
        // Tap anywhere to show or hide the controls.
        Box(
            Modifier.fillMaxSize().tapNoRipple {
                controlsVisible = !controlsVisible
                showReactions = false
                touches++
            },
        )
        ModeLabel(modeLabel, Modifier.align(Alignment.Center))
        ReactionsLayer(reactions)
        // A message floats above the timeline, clear of the bottom block (the web page measures the same gap).
        ChatToast(chatToast, Modifier.align(Alignment.BottomCenter).padding(bottom = overlayDp + CHAT_GAP_DP.dp, start = 40.dp, end = 40.dp))
        if (controlsVisible) {
            PlayerTopBar(
                title = room.myTitle.orEmpty(),
                pill = pill,
                fullscreen = fullscreen,
                onBack = { if (fullscreen) exitFullscreen() else showEnd = true },
                onMore = { showMore = true },
                onFullscreen = ::onFullscreenTap,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            PlayerCenterControls(
                playing = room.playing,
                landscape = landscape,
                onBack10 = { seekTo(player.currentPosition / MS - SKIP_SEC); touches++ },
                onPlayPause = { togglePlay(); touches++ },
                onForward10 = { seekTo(player.currentPosition / MS + SKIP_SEC); touches++ },
                modifier = Modifier.align(Alignment.Center),
            )
            // 😊 sits at the right edge just above the timeline; the five emoji open to its left.
            ReactionColumn(
                open = showReactions,
                onToggle = { showReactions = !showReactions; touches++ },
                onPick = { onReaction(it); showReactions = false },
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = max(overlayDp.value - REACTION_BELOW_TIMELINE_DP, MIN_REACTION_BOTTOM_DP).dp),
            )
            PlayerBottomOverlay(
                positionSec = positionSec,
                durationSec = durationSec,
                onScrub = { scrubbing = true; positionSec = it; touches++ },
                onScrubDone = { scrubbing = false; seekTo(positionSec) },
                actions = {
                    PlayerSecondaryRow(
                        onSync = {
                            player.seekTo((latestRoom.expectedPositionSec(serverNowMs()) * MS).toLong())
                            player.setPlaybackSpeed(1f)
                            syncedPill = true
                            touches++
                        },
                        onMessage = { composing = true; touches++ },
                        onLog = { showLog = true },
                        onVoice = { startCall(false) },
                        onVideo = { startCall(true) },
                        onEnd = { showEnd = true },
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { overlayPx = it.height },
            )
        }
        if (composing) {
            PlayerChatComposer(
                draft = draft,
                onDraft = { draft = it },
                onSend = ::sendDraft,
                onClose = { composing = false; touches++ },
                focus = chatFocus,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        // The call windows draw over everything, independent of the controls fading (like the web page).
        callHooks?.bubble?.invoke(this)
    }
    if (showEnd) EndDialog(onConfirm = { showEnd = false; onEnd(player.currentPosition / MS) }, onDismiss = { showEnd = false })
    if (showLog) ChatLogSheet(chat, partnerName, onDismiss = { showLog = false })
    if (showMore) {
        PlayerMoreSheet(
            info = MovieInfo(
                title = room.myTitle.orEmpty(),
                lengthText = formatRuntime(durationSec),
                cloud = isCloud,
                sizeText = cloudSizeText,
                subtitles = subtitle?.name,
            ),
            fit = fit,
            sleepMinutes = sleepMin,
            keepScreenOn = keepScreenOn,
            pipAvailable = pipAvailable,
            onSubtitles = {
                if (subtitle != null) {
                    onSubtitle(null)
                    Toast.makeText(context, R.string.movie_toast_subs_off, Toast.LENGTH_SHORT).show()
                } else {
                    subsPicker.launch(arrayOf("*/*"))
                }
            },
            onFit = { fit = fit.next(); flashMode(context.getString(fit.label)) },
            onPip = {
                showMore = false
                val params = PictureInPictureParams.Builder().setAspectRatio(Rational(PIP_W, PIP_H)).build()
                val entered = runCatching { activity?.enterPictureInPictureMode(params) == true }.getOrDefault(false)
                if (!entered) Toast.makeText(context, R.string.movie_toast_pip_failed, Toast.LENGTH_SHORT).show()
            },
            onSleep = { sleepMin = nextSleepStep(sleepMin) },
            onKeepScreenOn = onKeepScreenOn,
            onEnd = { showMore = false; showEnd = true },
            onDismiss = { showMore = false },
        )
    }
}

@OptIn(UnstableApi::class)
private fun buildMediaItem(url: String, subs: SubtitleChoice?): MediaItem {
    val builder = MediaItem.Builder().setUri(url)
    if (subs != null) {
        builder.setSubtitleConfigurations(
            listOf(
                MediaItem.SubtitleConfiguration.Builder(subs.uri)
                    .setMimeType(subs.mime)
                    .setLanguage(SUBTITLE_LANGUAGE)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build(),
            ),
        )
    }
    return builder.build()
}

@OptIn(UnstableApi::class)
private fun ScreenFit.resizeMode(): Int = when (this) {
    ScreenFit.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    ScreenFit.Fill -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    ScreenFit.Zoom -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
}

/** Keeps the screen awake only while the movie plays and the switch is on (like the web wake lock). */
@Composable
private fun KeepScreenOnEffect(on: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, on) {
        view.keepScreenOn = on
        onDispose { view.keepScreenOn = false }
    }
}

/** Hides the system bars while a movie plays. */
@Composable
private fun ImmersiveEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val MS = 1000.0
private const val SEEK_TOLERANCE_SEC = 0.7
private const val SKIP_SEC = 10.0
private const val DRIFT_EVERY_MS = 1000L
private const val POSITION_EVERY_MS = 500L
private const val HIDE_AFTER_MS = 4000L
private const val PILL_MS = 2000L
private const val MS_PER_MINUTE = 60_000L
private const val MODE_LABEL_MS = 1000L
private const val CHAT_SHOW_MS = 3500L
private const val CHAT_GAP_MS = 300L
private const val CHAT_IDLE_MS = 150L
private const val FOCUS_DELAY_MS = 50L
private const val CHAT_GAP_DP = 16
/**
 * The bottom block is 20 dp padding + 28 dp timeline + 10 dp gap + 48 dp buttons + 14 dp padding. The timeline top is
 * therefore (block height - 20) above the screen bottom, and the web page keeps the emoji button 12 dp above that.
 */
private const val REACTION_BELOW_TIMELINE_DP = 8f
private const val MIN_REACTION_BOTTOM_DP = 112f
private const val PIP_W = 16
private const val PIP_H = 9
