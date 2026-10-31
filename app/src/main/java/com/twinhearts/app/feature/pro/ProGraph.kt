package com.twinhearts.app.feature.pro

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.twinhearts.app.core.design.TwinNeutralPage
import com.twinhearts.app.core.navigation.Routes

/** Plugs the Pro page into the app (R-43). Opened from the side menu or from any locked feature. */
fun NavGraphBuilder.proGraph(navController: NavHostController) {
    composable<Routes.Pro> { entry ->
        val args = entry.toRoute<Routes.Pro>()
        TwinNeutralPage {
            ProScreen(feature = args.feature, onBack = { navController.popBackStack() })
        }
    }
}
