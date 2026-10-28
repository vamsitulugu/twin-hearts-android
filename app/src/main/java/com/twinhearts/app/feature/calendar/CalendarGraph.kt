package com.twinhearts.app.feature.calendar

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.twinhearts.app.core.navigation.Routes
import com.twinhearts.app.feature.calendar.ui.CalendarScreen

/** Plugs the Calendar page into the app (R-43). Opened from Home ("Calendar ›" and the Plans list). */
fun NavGraphBuilder.calendarGraph(navController: NavHostController) {
    composable<Routes.Calendar> {
        CalendarScreen(onBack = { navController.popBackStack() })
    }
}
