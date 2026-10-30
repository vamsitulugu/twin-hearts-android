package com.twinhearts.app.feature.games

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.games.ui.GamesScreen
import com.twinhearts.app.core.design.TwinNeutralPage

/** Adds the Games page to the navigation graph (opened from the Home "Games" chip). */
fun NavGraphBuilder.gamesGraph(navController: NavHostController) {
    composable<Routes.Games> { TwinNeutralPage {
        GamesScreen(onBack = { navController.popBackStack() })
    }
    }
}
