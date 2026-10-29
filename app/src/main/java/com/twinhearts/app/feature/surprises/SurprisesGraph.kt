package com.twinhearts.app.feature.surprises

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.surprises.ui.SurprisesScreen

/** Plugs Surprises into the app (R-43). Opened from the Home "Surprises" tile. */
fun NavGraphBuilder.surprisesGraph(navController: NavHostController) {
    composable<Routes.Surprises> {
        SurprisesScreen(onBack = { navController.popBackStack() })
    }
}
