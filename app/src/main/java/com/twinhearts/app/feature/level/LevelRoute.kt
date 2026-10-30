package com.twinhearts.app.feature.level

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.level.ui.LevelScreen
import com.twinhearts.app.core.design.TwinNeutralPage

/** Plugs the Couple Level page into the app (R-43). Needs `Routes.Level`. */
fun NavGraphBuilder.levelGraph(navController: NavHostController) {
    composable<Routes.Level> { TwinNeutralPage {
        LevelScreen(onBack = { if (!navController.popBackStack()) navController.navigate(Routes.Home) })
    }
    }
}
