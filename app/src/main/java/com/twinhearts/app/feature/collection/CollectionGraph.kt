package com.twinhearts.app.feature.collection

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.collection.ui.CollectionScreen

/** Plugs the Collection page into the app (R-43). Opened from the Home "Collection" shortcut. */
fun NavGraphBuilder.collectionGraph(navController: NavHostController) {
    composable<Routes.Collection> {
        CollectionScreen(onBack = { navController.popBackStack() })
    }
}
