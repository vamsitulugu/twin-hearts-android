package com.twinhearts.app.feature.myspace

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.myspace.ui.MySpaceScreen
import com.twinhearts.app.core.design.TwinNeutralPage

/** Plugs the My Space page into the app (R-43). Opened from the Home "My Space" tile. */
fun NavGraphBuilder.mySpaceGraph(navController: NavHostController) {
    composable<Routes.MySpace> { TwinNeutralPage {
        MySpaceScreen(onBack = { navController.popBackStack() })
    }
    }
}
