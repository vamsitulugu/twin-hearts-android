package com.twinhearts.app.feature.movie.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.Forward10
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.movie.domain.formatClock
import com.twinhearts.app.feature.movie.domain.model.ChatMessage
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
    val durationSec = (room.myDurationSec ?: 1.0).coerceAtLeast(1.0)

    val exo = player
    ImmersiveEffect()
    BackHandler { showEnd = true }
    DisposableEffect(player) { onDispose { player.release() } }
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
    LaunchedEffect(streamUrl) {
        val url = streamUrl ?: return@LaunchedEffect
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.seekTo((latestRoom.expectedPositionSec(serverNowMs()) * 1000).toLong())
        player.playWhenReady = latestRoom.playing
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
            modifier = Modifier.fillMaxSize(),
        )
        if (streamUrl == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = colors.accent)
                Text(stringResource(R.string.movie_loading_video), color = colors.inkMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = TwinTheme.spacing.m))
            }
        }
        // Tap anywhere to show or hide the controls.
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                controlsVisible = !controlsVisible
                touches++
            },
        )
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
                Text(room.myTitle.orEmpty(), color = colors.ink, style = MaterialTheme.typography.titleMedium, maxLines = 1)
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
}

@Composable
private fun SmallAction(label: Int, modifier: Modifier, onClick: () -> Unit) {
    GlassButton(stringResource(label), onClick, modifier, style = GlassButtonStyle.Secondary)
}

/** Hides the system bars and keeps the screen on while a movie plays. */
@Composable
private fun ImmersiveEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
        }
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
