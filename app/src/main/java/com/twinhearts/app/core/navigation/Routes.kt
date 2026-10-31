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

    /** Global search, opened from the Home search bar (pages, features, messages, page content). */
    @Serializable data object Search

    /** Pro page (Razorpay). [feature] is the locked feature that sent the person here, or null from the menu. */
    @Serializable data class Pro(val feature: String? = null)

    // Phase 5: Watch Together (movies), opened from the Home "Movie" chip
    @Serializable data object Movie

    // Meet Planner, opened from the Home "Meet Planner" chip
    @Serializable data object MeetPlanner

    // Fight Log, opened from the Home "Fight Log" shortcut
    @Serializable data object Fights

    // Pages opened from Home and Chat (restored: these were missing from the repo copy)
    /** The Us page. [tab]: 0 = Love Counter, 1 = Collection, 2 = Dream Goals. */
    @Serializable data class Us(val tab: Int = 0)
    @Serializable data object Collection
    @Serializable data object Money
    @Serializable data object Calendar
    @Serializable data object Music
    @Serializable data object Study
    @Serializable data object Level
    @Serializable data object Places

    // Pages merged from the Phase 2-6 feature zips
    @Serializable data object Memories
    @Serializable data object BucketList
    @Serializable data object PeriodTracker
    @Serializable data object Help
    @Serializable data object MySpace
    @Serializable data object Vault
    @Serializable data object Surprises
    @Serializable data object Globe
    @Serializable data object Games
    @Serializable data object DailyRoutine

    /** The call page. [incoming] is true when my partner is calling me. */
    @Serializable data class Call(val video: Boolean = false, val incoming: Boolean = false)

    // Debug builds only (the screen itself lives in src/debug)
    @Serializable data object DesignGallery
}
