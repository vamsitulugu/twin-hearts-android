package com.twinhearts.app.feature.cinema.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

/**
 * Why this file exists: lets the Cinema use the app's voice / video calls without importing the Calls feature
 * (R-83). Same idea as MovieCallHooks. CinemaGraph fills it in. The cinema's own Voice / Video buttons talk to
 * these through [CinemaWebView]'s JavaScript bridge; the call window itself is drawn natively by [overlay].
 */
class CinemaCallHooks(
    /** Starts a voice (false) or video (true) call. Asks for the microphone (and camera) first. */
    val start: (video: Boolean) -> Unit,
    /** Hangs up the running call (does nothing when there is none). */
    val end: () -> Unit,
    val toggleMute: () -> Unit,
    val toggleSpeaker: () -> Unit,
    val flipCamera: () -> Unit,
    /** The theatre tells us its id (the web's registerWatchSession); a call is only joined inside the same theatre. */
    val register: (String?) -> Unit,
    val unregister: () -> Unit,
    /** My mic / camera state as JSON for the avatar badge: {"on":1,"muted":false,"level":0.1,"cam":1}. */
    val mic: () -> String,
    /** Draws the call window over the cinema, and the Accept / Decline card when my partner calls. */
    val overlay: @Composable BoxScope.() -> Unit,
)
