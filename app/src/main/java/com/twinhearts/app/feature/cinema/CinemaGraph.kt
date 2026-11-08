package com.twinhearts.app.feature.cinema

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.cinema.ui.CinemaScreen

/**
 * Plugs the Cinema into the app (R-43). Opened from the Home "Cinema" chip (I am the host) or from the
 * invite dialog (I am the partner, `sessionId` set). Leaving the cinema simply goes back one screen.
 */
fun NavGraphBuilder.cinemaGraph(navController: NavHostController) {
    composable<Routes.Cinema> {
        CinemaScreen(onClose = { navController.popBackStack() })
    }
}
