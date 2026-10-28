package com.twinhearts.app.feature.movie.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

/**
 * Why this file exists: lets the app plug calls into the movie player without the movie code
 * importing the Calls feature (R-83). The graph file fills this in.
 */
class MovieCallHooks(
    /** Starts a voice (false) or video (true) call. */
    val start: (video: Boolean) -> Unit,
    /** Draws the small call window over the movie. */
    val bubble: @Composable BoxScope.() -> Unit,
)
