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

    // Phase 5: Music (library, player, lyrics, karaoke, recordings), opened from the Home "Music" chip
    @Serializable data object Music

    // Phase 5: Watch Together (movies), opened from the Home "Movie" chip
    @Serializable data object Movie

    // Phase 3: Us tab container (Love Counter · Collection · Dream Goals)
    @Serializable data object Us

    // Calendar page, opened from Home ("Calendar ›" and the Plans list)
    @Serializable data object Calendar

    // Phase 2.3: voice / video call screen ([incoming] true = already ringing)
    @Serializable data class Call(val video: Boolean, val incoming: Boolean = false)

    // Debug builds only (the screen itself lives in src/debug)
    @Serializable data object DesignGallery
}
