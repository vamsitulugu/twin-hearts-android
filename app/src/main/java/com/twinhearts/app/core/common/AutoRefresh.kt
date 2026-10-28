package com.twinhearts.app.core.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

private const val DEFAULT_INTERVAL_MS = 30_000L

/**
 * Calls [onRefresh] every ~30 seconds while the screen is visible (not in the background), so a
 * change the partner made shows up without leaving and re-opening the page. Silent: it must not
 * show a spinner or an error.
 */
@Composable
fun AutoRefresh(intervalMillis: Long = DEFAULT_INTERVAL_MS, onRefresh: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(onRefresh)
    LaunchedEffect(owner, intervalMillis) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(intervalMillis)
                latest()
            }
        }
    }
}
