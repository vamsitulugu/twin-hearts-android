package com.twinhearts.app.feature.memories

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.memories.ui.MemoriesScreen

/** Plugs the Memories page into the app (R-43). Opened from the Home "Memories" chip. */
fun NavGraphBuilder.memoriesGraph(navController: NavHostController) {
    composable<Routes.Memories> {
        MemoriesScreen(onBack = { navController.popBackStack() })
    }
}
