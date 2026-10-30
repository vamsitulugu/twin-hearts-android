package com.twinhearts.app.feature.movie.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.twinhearts.app.feature.movie.ui.components.MovieGlassCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.movie.domain.formatBytes
import com.twinhearts.app.feature.movie.domain.model.RoomStatus
import com.twinhearts.app.feature.movie.ui.components.CountdownContent
import com.twinhearts.app.feature.movie.ui.components.HistorySheet
import com.twinhearts.app.feature.movie.ui.components.InviteDialog
import com.twinhearts.app.feature.movie.ui.components.LobbyContent
import com.twinhearts.app.feature.movie.ui.components.MovieTabsContent
import com.twinhearts.app.feature.movie.ui.components.PlayerScreen
import kotlinx.coroutines.flow.merge
import com.twinhearts.app.feature.movie.ui.components.MovieStyled

/**
 * Why this file exists: the one Watch Together screen. It decides what to show from the room:
 * movie tabs, the lobby, the countdown or the player. All state lives in the two ViewModels (R-42).
 */
@Composable
private fun MovieScreenContent(
    onBack: () -> Unit,
    callHooks: MovieCallHooks? = null,
    watchVm: WatchViewModel = hiltViewModel(),
    browseVm: BrowseViewModel = hiltViewModel(),
) {
    val state by watchVm.state.collectAsStateWithLifecycle()
    val upload by watchVm.upload.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // "Change" only hides the lobby on this phone so the partner's choice is not wiped.
    var changing by rememberSaveable { mutableStateOf(false) }

    // Polling only runs while the screen is visible, so the battery is safe in the background (R-66).
    LaunchedEffect(watchVm) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { watchVm.runLoop() }
    }
    LaunchedEffect(watchVm, browseVm) {
        merge(watchVm.messages, browseVm.messages).collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    // Sentences the server wrote for the user are shown as they are.
    LaunchedEffect(watchVm) {
        watchVm.notes.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    TwinBackground(Modifier.fillMaxSize()) {
        when (state.stage) {
            WatchStage.Watching -> state.room?.let { room ->
                PlayerScreen(
                    room = room,
                    streamUrl = state.streamUrl,
                    chat = state.chat,
                    serverNowMs = watchVm::serverNowMs,
                    onAction = watchVm::sendAction,
                    onSendChat = watchVm::sendChat,
                    onEnd = watchVm::endMovie,
                    reactions = state.reactions,
                    onReaction = watchVm::sendReaction,
                    keepScreenOn = state.keepScreenOn,
                    onKeepScreenOn = watchVm::setKeepScreenOn,
                    callHooks = callHooks,
                    isCloud = room.sharedMovieId != null,
                    cloudSizeText = (state.cloud as? UiState.Content)?.data?.movies
                        ?.firstOrNull { it.id == room.sharedMovieId }?.sizeBytes?.let(::formatBytes),
                )
            }
            WatchStage.Countdown -> CountdownContent(state.countdown)
            WatchStage.Setup -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Header(onBack = onBack, onHistory = { showHistory = true; watchVm.loadHistory() })
                val error = state.error
                val room = state.room
                when {
                    state.loading -> LoadingSkeleton(Modifier.padding(TwinTheme.spacing.screenEdge))
                    error != null -> ErrorState(message = error.asText(), onRetry = watchVm::retry)
                    room != null && room.myTitle != null && room.status != RoomStatus.Ended && !changing ->
                        LobbyContent(
                            room = room,
                            busy = state.busy,
                            onReady = { watchVm.setReady(true) },
                            onStart = watchVm::requestStart,
                            onCancelStart = watchVm::cancelStart,
                            onCancelMovie = watchVm::cancelMovie,
                            onChange = { changing = true; tab = WATCH_TAB },
                        )
                    else -> MovieTabsContent(
                        tab = tab,
                        onTab = { tab = it },
                        browseVm = browseVm,
                        cloud = state.cloud,
                        upload = upload,
                        onLoadCloud = watchVm::loadCloud,
                        onPickLocal = { uri, title, sec -> changing = false; watchVm.pickLocalMovie(uri, title, sec) },
                        onPickShared = { movie -> changing = false; watchVm.pickSharedMovie(movie) },
                        onUpload = watchVm::uploadMovie,
                        onCancelUpload = watchVm::cancelUpload,
                        onDismissUpload = watchVm::dismissUpload,
                        onDeleteShared = watchVm::deleteShared,
                    )
                }
            }
        }
    }

    if (state.inviteVisible && state.stage == WatchStage.Setup) {
        InviteDialog(
            movieTitle = state.room?.partnerTitle ?: state.room?.myTitle.orEmpty(),
            matches = state.room?.moviesMatch == true,
            onAccept = watchVm::acceptInvite,
            onDecline = watchVm::cancelStart,
        )
    }
    if (showHistory) {
        HistorySheet(
            history = state.history,
            onDismiss = { showHistory = false },
            onRetry = watchVm::loadHistory,
            onDelete = watchVm::deleteHistory,
            onClearAll = watchVm::clearHistory,
        )
    }
}

private const val WATCH_TAB = 3

@Composable
private fun Header(onBack: () -> Unit, onHistory: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MovieGlassCircle(44.dp, onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.movie_back), tint = TwinTheme.colors.ink) }
        Column(Modifier.weight(1f).padding(horizontal = TwinTheme.spacing.s), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.movie_title),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = SerifFamily,
                color = TwinTheme.colors.ink,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
        MovieGlassCircle(44.dp, onHistory) { Icon(Icons.Rounded.History, stringResource(R.string.movie_history), tint = TwinTheme.colors.ink) }
    }
}

/** Public entry: the page body wrapped in the shared near-black glass look. */
@Composable
fun MovieScreen(
    onBack: () -> Unit,
    callHooks: MovieCallHooks? = null,
    watchVm: WatchViewModel = hiltViewModel(),
    browseVm: BrowseViewModel = hiltViewModel(),
) {
    MovieStyled { MovieScreenContent(onBack = onBack, callHooks = callHooks, watchVm = watchVm, browseVm = browseVm) }
}
