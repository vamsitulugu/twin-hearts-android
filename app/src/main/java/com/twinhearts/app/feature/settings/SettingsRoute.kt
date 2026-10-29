package com.twinhearts.app.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.settings.ui.SettingsScreen

/**
 * Plugs Settings into the app (R-43). The nine sub-pages live inside this one screen, like the web.
 * Log Out clears the entry screens and returns to Get Started. "Customer Care & Help" is built in a
 * later step (Phase 4), so its rows show the short "built in a later step" note for now.
 */
fun NavGraphBuilder.settingsGraph(navController: NavHostController) {
    composable<Routes.Settings> {
        SettingsScreen(
            onSignedOut = {
                navController.navigate(Routes.GetStarted) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onOpenHelp = { navController.navigate(Routes.Help) },
        )
    }
}
