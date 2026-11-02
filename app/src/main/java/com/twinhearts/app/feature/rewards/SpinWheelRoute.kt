package com.twinhearts.app.feature.rewards

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.design.TwinNeutralPage
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.rewards.ui.SpinWheelScreen

/** Plugs the separate Spin & Win page into the app (R-43). Needs `Routes.SpinWheel`. */
fun NavGraphBuilder.spinWheelGraph(navController: NavHostController) {
    composable<Routes.SpinWheel> {
        TwinNeutralPage {
            SpinWheelScreen()
        }
    }
}
