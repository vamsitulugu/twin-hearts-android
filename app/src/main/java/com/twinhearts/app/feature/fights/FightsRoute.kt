package com.twinhearts.app.feature.fights

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.fights.ui.FightsScreen
import com.twinhearts.app.core.design.TwinNeutralPage

/** Plugs Fight Log into the app (R-43). Needs `Routes.Fights` (see SHARED_CHANGES_fights.md). */
fun NavGraphBuilder.fightsGraph(navController: NavHostController) {
    composable<Routes.Fights> { TwinNeutralPage {
        FightsScreen(
            onBack = {
                if (!navController.popBackStack()) navController.navigate(Routes.Home)
            },
            onOpenTwin = { navController.navigate(Routes.TwinAi) },
        )
    }
    }
}
