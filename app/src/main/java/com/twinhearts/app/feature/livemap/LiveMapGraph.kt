package com.twinhearts.app.feature.livemap

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.livemap.ui.LiveMapScreen

/** Plugs the Live Map into the app (R-43). It is the "Map" tab of the bottom bar. */
fun NavGraphBuilder.liveMapGraph(navController: NavHostController) {
    composable<Routes.LiveMap> {
        LiveMapScreen(
            onBack = { navController.popBackStack() },
            onOpenSettings = { navController.navigate(Routes.Settings) },
        )
    }
}
