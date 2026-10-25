package com.twinhearts.app.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.design.DesignGalleryScreen

/** Debug builds only: registers the design gallery screen. Release builds get an empty version. */
internal fun NavGraphBuilder.debugGraph(navController: NavHostController) {
    composable<Routes.DesignGallery> {
        DesignGalleryScreen(onBack = { navController.popBackStack() })
    }
}
