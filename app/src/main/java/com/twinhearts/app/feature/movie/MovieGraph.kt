package com.twinhearts.app.feature.movie
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.calls.ui.WatchCallOverlay
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
                bubble = { WatchCallOverlay(Modifier.fillMaxSize()) },
            ),
        )
    }
}
