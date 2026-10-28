package com.twinhearts.app.feature.music

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.music.ui.MusicScreen

/** Plugs Music into the app (R-43). Opened from the Home "Music" chip. */
fun NavGraphBuilder.musicGraph(navController: NavHostController) {
    composable<Routes.Music> {
        MusicScreen(onBack = { navController.popBackStack() })
    }
}
