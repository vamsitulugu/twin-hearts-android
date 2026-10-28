package com.twinhearts.app.feature.movie

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.calls.ui.CallBubble
import com.twinhearts.app.feature.calls.ui.rememberCallStarter
import com.twinhearts.app.feature.movie.ui.MovieCallHooks
import com.twinhearts.app.feature.movie.ui.MovieScreen

/** Plugs Watch Together into the app (R-43). Opened from the Home "Movie" chip. */
fun NavGraphBuilder.movieGraph(navController: NavHostController) {
    composable<Routes.Movie> {
        MovieScreen(
            onBack = { navController.popBackStack() },
            callHooks = MovieCallHooks(
                start = rememberCallStarter(),
                bubble = { CallBubble(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = CALL_TOP_DP.dp, start = 12.dp)) },
            ),
        )
    }
}

private const val CALL_TOP_DP = 64
