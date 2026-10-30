package com.twinhearts.app.feature.globe

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.globe.ui.GlobeScreen
import com.twinhearts.app.core.design.TwinNeutralPage

/** Plugs the Memory Globe into the app (R-43). It opens from the Home "Globe" chip. */
fun NavGraphBuilder.globeGraph(navController: NavHostController) {
    composable<Routes.Globe> { TwinNeutralPage {
        GlobeScreen(onBack = { navController.popBackStack() })
    }
    }
}
