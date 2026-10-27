package com.twinhearts.app.feature.movie.ui.components

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.movie.domain.formatClock
import com.twinhearts.app.feature.movie.domain.formatRuntime
import com.twinhearts.app.feature.movie.domain.model.ChatMessage
import com.twinhearts.app.feature.movie.domain.model.MovieReaction
import com.twinhearts.app.feature.movie.domain.model.WatchRoom
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Why this file exists: the cinema screen. It plays the movie with Media3 and keeps it in step with
 * the partner using the shared room (same rules as the web app): jump when 2 s or more apart, nudge
 * the speed by 3 % when 0.5 to 2 s apart. It only reads the room; actions go out through [onAction].
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
) {
    val context = LocalContext.current
    val colors = TwinTheme.colors
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
    var floating by remember { mutableStateOf<ChatMessage?>(null) }
    var showMore by remember { mutableStateOf(false) }
    var showReactions by remember { mutableStateOf(false) }
    var fit by remember { mutableStateOf(ScreenFit.Fit) }
    var sleepMin by remember { mutableIntStateOf(0) }
    var subs by remember { mutableStateOf<SubtitleChoice?>(null) }
    var inPip by remember { mutableStateOf(false) }
    val activity = remember(context) { context.findActivity() }
    val pipAvailable = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) }
    val durationSec = (room.myDurationSec ?: 1.0).coerceAtLeast(1.0)

    val exo = player
    ImmersiveEffect()
    KeepScreenOnEffect(keepScreenOn && room.playing)
    BackHandler { showEnd = true }
    DisposableEffect(player) { onDispose { player.release() } }
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
            subs = choice
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
    LaunchedEffect(streamUrl, subs) {
        val url = streamUrl ?: return@LaunchedEffect
        // Adding or removing subtitles reloads the movie; keep the same spot and play state.
        val reload = player.mediaItemCount > 0
        val startMs = if (reload) player.currentPosition else (latestRoom.expectedPositionSec(serverNowMs()) * 1000).toLong()
        val playing = if (reload) player.playWhenReady else latestRoom.playing
        player.setMediaItem(buildMediaItem(url, subs), startMs)
        player.prepare()
        player.playWhenReady = playing
    }

    // Someone (the partner, or this phone's own echo) played, paused or jumped.
    LaunchedEffect(room.actionSeq, room.playing) {
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
    LaunchedEffect(controlsVisible, touches) {
        if (controlsVisible && !composing) {
            delay(HIDE_AFTER_MS)
            controlsVisible = false
        }
    }
    LaunchedEffect(chat.lastOrNull()?.id) {
        val last = chat.lastOrNull() ?: return@LaunchedEffect
        if (last.fromMe) return@LaunchedEffect
        floating = last
        delay(FLOAT_MS)
        floating = null
    }
    LaunchedEffect(syncedPill) {
        if (syncedPill) {
            delay(PILL_MS)
            syncedPill = false
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
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                controlsVisible = !controlsVisible
                showReactions = false
                touches++
            },
        )
        ReactionsLayer(reactions)
        floating?.let {
            Text(
                it.text, color = colors.ink, style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterStart).padding(TwinTheme.spacing.l)
                    .background(colors.scrim, TwinTheme.shapes.pill).padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
            )
        }
        if (syncedPill) {
            Text(
                stringResource(R.string.movie_in_sync), color = colors.success, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = TwinTheme.spacing.xxl)
                    .background(colors.scrim, TwinTheme.shapes.pill).padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
            )
        }
        if (controlsVisible) {
            Row(
                Modifier.align(Alignment.TopStart).fillMaxWidth().statusBarsPadding().padding(TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton({ showEnd = true }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.movie_back), tint = colors.ink)
                }
                Text(room.myTitle.orEmpty(), color = colors.ink, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.weight(1f))
                IconButton({ showReactions = !showReactions; touches++ }) {
                    Icon(Icons.Rounded.EmojiEmotions, stringResource(R.string.movie_reactions), tint = colors.ink)
                }
                IconButton({ showMore = true }) {
                    Icon(Icons.Rounded.MoreVert, stringResource(R.string.movie_more_title), tint = colors.ink)
                }
            }
            if (showReactions) {
                ReactionBar(
                    onPick = { onReaction(it); showReactions = false },
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = REACTION_BAR_TOP_DP.dp, end = TwinTheme.spacing.s),
                )
            }
            Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xl), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ seekTo(player.currentPosition / MS - SKIP_SEC) }) {
                    Icon(Icons.Rounded.Replay10, stringResource(R.string.movie_back_10), Modifier.size(40.dp), tint = colors.ink)
                }
                IconButton({ togglePlay() }, Modifier.size(72.dp)) {
                    Icon(
                        if (room.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        stringResource(if (room.playing) R.string.movie_pause else R.string.movie_play),
                        Modifier.size(56.dp), tint = colors.ink,
                    )
                }
                IconButton({ seekTo(player.currentPosition / MS + SKIP_SEC) }) {
                    Icon(Icons.Rounded.Forward10, stringResource(R.string.movie_forward_10), Modifier.size(40.dp), tint = colors.ink)
                }
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatClock(positionSec), color = colors.ink, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = positionSec.toFloat().coerceIn(0f, durationSec.toFloat()),
                        onValueChange = { scrubbing = true; positionSec = it.toDouble(); touches++ },
                        onValueChangeFinished = { scrubbing = false; seekTo(positionSec) },
                        valueRange = 0f..durationSec.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent, inactiveTrackColor = colors.line),
                        modifier = Modifier.weight(1f).padding(horizontal = TwinTheme.spacing.s),
                    )
                    Text(formatClock(durationSec), color = colors.inkMuted, style = MaterialTheme.typography.labelMedium)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    SmallAction(R.string.movie_sync, Modifier.weight(1f)) {
                        player.seekTo((latestRoom.expectedPositionSec(serverNowMs()) * MS).toLong())
                        player.setPlaybackSpeed(1f)
                        syncedPill = true
                    }
                    SmallAction(R.string.movie_message, Modifier.weight(1f)) { composing = true }
                    SmallAction(R.string.movie_log, Modifier.weight(1f)) { showLog = true }
                    SmallAction(R.string.movie_end, Modifier.weight(1f)) { showEnd = true }
                }
            }
        }
        if (composing) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(colors.scrim).navigationBarsPadding().padding(TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TwinTextField(
                    value = draft, onValueChange = { draft = it }, label = stringResource(R.string.movie_chat_hint),
                    modifier = Modifier.weight(1f),
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send,
                    onImeAction = { onSendChat(draft, player.currentPosition / MS); draft = ""; composing = false },
                )
                GlassButton(
                    stringResource(R.string.movie_send),
                    { onSendChat(draft, player.currentPosition / MS); draft = ""; composing = false },
                    Modifier.padding(start = TwinTheme.spacing.s),
                )
            }
        }
    }
    if (showEnd) EndDialog(onConfirm = { showEnd = false; onEnd(player.currentPosition / MS) }, onDismiss = { showEnd = false })
    if (showLog) ChatLogSheet(chat, onDismiss = { showLog = false })
    if (showMore) {
        PlayerMoreSheet(
            info = MovieInfo(
                title = room.myTitle.orEmpty(),
                lengthText = formatRuntime(durationSec),
                cloud = isCloud,
                sizeText = cloudSizeText,
                subtitles = subs?.name,
            ),
            fit = fit,
            sleepMinutes = sleepMin,
            keepScreenOn = keepScreenOn,
            pipAvailable = pipAvailable,
            onSubtitles = {
                if (subs != null) {
                    subs = null
                    Toast.makeText(context, R.string.movie_toast_subs_off, Toast.LENGTH_SHORT).show()
                } else {
                    subsPicker.launch(arrayOf("*/*"))
                }
            },
            onFit = { fit = fit.next() },
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

/** A subtitle file the person chose on this phone. It is never uploaded; each phone loads its own. */
private data class SubtitleChoice(val uri: Uri, val name: String, val mime: String)

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

/** Media3 reads .srt and .vtt directly (the web page has to convert .srt first). Anything else is refused. */
@OptIn(UnstableApi::class)
private fun readSubtitle(context: Context, uri: Uri): SubtitleChoice? {
    val name = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: return null
    val lower = name.lowercase()
    val mime = when {
        lower.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
        lower.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        else -> return null
    }
    return SubtitleChoice(uri, name, mime)
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

@Composable
private fun SmallAction(label: Int, modifier: Modifier, onClick: () -> Unit) {
    GlassButton(stringResource(label), onClick, modifier, style = GlassButtonStyle.Secondary)
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
private const val FLOAT_MS = 5000L
private const val PILL_MS = 2000L
private const val MS_PER_MINUTE = 60_000L
private const val SUBTITLE_LANGUAGE = "en"
private const val PIP_W = 16
private const val PIP_H = 9
private const val REACTION_BAR_TOP_DP = 56
