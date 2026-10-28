package com.twinhearts.app.feature.study

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.study.ui.StudyScreen

/** Plugs the Study page into the app (R-43). Opened from Home (Explore Together list). */
fun NavGraphBuilder.studyGraph(navController: NavHostController) {
    composable<Routes.Study> {
        StudyScreen(onBack = { navController.popBackStack() })
    }
}
