package com.twinhearts.app.feature.movie

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.movie.ui.MovieScreen

/** Plugs Watch Together into the app (R-43). Opened from the Home "Movie" chip. */
fun NavGraphBuilder.movieGraph(navController: NavHostController) {
    composable<Routes.Movie> {
        MovieScreen(onBack = { navController.popBackStack() })
    }
}
