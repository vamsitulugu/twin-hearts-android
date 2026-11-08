package com.twinhearts.app.feature.cinema3d

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.cinema3d.ui.Cinema3dSpikeScreen

/** Plugs the C1 spike page into the app (R-43). It is a developer test page, not part of the normal menus. */
fun NavGraphBuilder.cinema3dGraph(navController: NavHostController) {
    composable<Routes.Cinema3dSpike> {
        Cinema3dSpikeScreen(onBack = { navController.popBackStack() })
    }
}
