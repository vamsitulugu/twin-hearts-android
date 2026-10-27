package com.twinhearts.app.feature.twinai

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.twinai.ui.TwinAiScreen

/** Plugs Twin AI into the app (R-43). It uses the existing `Routes.TwinAi` (the bottom-bar orb). */
fun NavGraphBuilder.twinAiGraph(navController: NavHostController) {
    composable<Routes.TwinAi> {
        TwinAiScreen(
            onBack = { navController.navigate(Routes.Home) { launchSingleTop = true } },
        )
    }
}
