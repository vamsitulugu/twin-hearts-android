package com.twinhearts.app.feature.pairing

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.pairing.ui.PairingScreen

/** Plugs the Pairing screen into the app (R-43). Continue / Skip opens Home and clears the entry screens. */
fun NavGraphBuilder.pairingGraph(navController: NavHostController) {
    composable<Routes.Pairing> {
        PairingScreen(
            onContinue = {
                navController.navigate(Routes.Home) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
    }
}
