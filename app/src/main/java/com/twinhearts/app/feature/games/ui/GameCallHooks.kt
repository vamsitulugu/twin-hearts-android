package com.twinhearts.app.feature.games.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Why this file exists: lets Games show the face-to-face video call without importing the Calls
 * feature (R-83). Same idea as MovieCallHooks. GamesGraph fills it in; GameHost (the frame every
 * game sits in) reads it, so all 8 games get the call without any game screen changing.
 */
class GameCallHooks(
    /** Starts a video call (asks for camera + mic first). Does nothing if a call is already running. */
    val startVideo: () -> Unit,
    /** Ends the running call. */
    val hangUp: () -> Unit,
    /** True while a call is ringing or running. Read inside a composable so the button updates. */
    val inCall: @Composable () -> Boolean,
    /** Draws the small draggable call window over the game. Draws nothing when there is no call. */
    val overlay: @Composable BoxScope.() -> Unit,
)

/** Null when Games is opened without call support (previews, tests): the call button is then hidden. */
val LocalGameCallHooks = staticCompositionLocalOf<GameCallHooks?> { null }
