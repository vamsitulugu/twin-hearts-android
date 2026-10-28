package com.twinhearts.app.core.navigation

import kotlinx.serialization.Serializable

/**
 * Every screen address in the app (R-05, R-83). Features navigate with these objects and never
 * import each other's classes. Add a new route only through a Change Request after Phase 0.
 */
object Routes {
    // Phase 1: entry
    @Serializable data object GetStarted
    @Serializable data object SignIn
    @Serializable data object SignUp
    @Serializable data object ForgotPassword
    @Serializable data object Pairing

    // Phase 2: the five bottom-bar screens
    @Serializable data object Home
    @Serializable data object Chat
    @Serializable data object TwinAi
    @Serializable data object LiveMap
    @Serializable data object Settings

    // Phase 2: opened from Home / Settings
    @Serializable data object Profile

    // Phase 5: Watch Together (movies), opened from the Home "Movie" chip
    @Serializable data object Movie

    // Meet Planner, opened from the Home "Meet Planner" chip
    @Serializable data object MeetPlanner

    // Debug builds only (the screen itself lives in src/debug)
    @Serializable data object DesignGallery
}
