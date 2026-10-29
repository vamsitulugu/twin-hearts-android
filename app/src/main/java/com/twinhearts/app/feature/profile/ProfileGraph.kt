package com.twinhearts.app.feature.profile

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.profile.ui.ProfileNavigation
import com.twinhearts.app.feature.profile.ui.ProfileScreen

/** Plugs the Our Profile page into the app (R-43). Other screens are opened only through [Routes] (R-83). */
fun NavGraphBuilder.profileGraph(navController: NavHostController) {
    composable<Routes.Profile> {
        ProfileScreen(
            navigation = ProfileNavigation(
                onBack = { navController.popBackStack() },
                onOpenCalendar = { navController.navigate(Routes.Calendar) },
                onOpenMemories = { navController.navigate(Routes.Collection) },
                onOpenGoals = { navController.navigate(Routes.Us) },
                onOpenChat = { navController.navigate(Routes.Chat) },
                onOpenAppSettings = { navController.navigate(Routes.Settings) },
                onSignedOut = {
                    navController.navigate(Routes.GetStarted) { popUpTo(0) { inclusive = true } }
                },
            ),
        )
    }
}
