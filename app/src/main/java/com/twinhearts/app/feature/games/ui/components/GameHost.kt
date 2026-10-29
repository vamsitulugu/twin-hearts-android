package com.twinhearts.app.feature.games.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.games.domain.model.Players
import com.twinhearts.app.feature.games.ui.play.GameContent
import com.twinhearts.app.feature.games.ui.play.GameEvent
import com.twinhearts.app.feature.games.ui.play.SyncedGameViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * The frame every game sits in: top bar, players strip, loading/error states, sounds, toasts,
 * prize pop-ups, and the "poll only while the screen is on" loop.
 *
 * [alongside] runs next to the sync loop while the screen is visible (turn clocks).
 */
@Composable
fun <S : Any> GameHost(
    viewModel: SyncedGameViewModel<S>,
    title: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    alongside: suspend () -> Unit = {},
    content: @Composable ColumnScope.(state: S, players: Players) -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val feedback = rememberGameFeedback()
    val snackbar = remember { SnackbarHostState() }
    val prizes = remember { mutableStateListOf<com.twinhearts.app.feature.games.ui.play.Prize>() }

    BackHandler(onBack = onExit)

    LaunchedEffect(viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            coroutineScope {
                launch { viewModel.run() }
                launch { alongside() }
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is GameEvent.Cue -> feedback.play(event.cue)
                is GameEvent.ShowPrize -> prizes.add(event.prize)
                is GameEvent.Message -> snackbar.showSnackbar(context.getString(event.res, *event.args.toTypedArray()))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TwinTopBar(title = title, onBackClick = onExit)
            when (val current = ui) {
                UiState.Loading, UiState.Empty -> LoadingSkeleton(modifier = Modifier.padding(TwinTheme.spacing.screenEdge))
                is UiState.Error -> ErrorState(message = current.error.asText(), onRetry = viewModel::retry)
                is UiState.Content -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = TwinTheme.spacing.screenEdge)
                        .navigationBarsPadding(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(TwinTheme.spacing.m),
                ) {
                    PlayersStrip(current.data.players)
                    content(current.data.state, current.data.players)
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }

    prizes.firstOrNull()?.let { prize ->
        PrizeDialog(prize = prize, onDismiss = { prizes.removeAt(0) })
    }
}
