package com.twinhearts.app.core.notifications

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Why this file exists: a tiny "something just arrived" bus between the push service (core) and
 * screens that want to react at once. The Hug / Touch / Miss You popup listens here, so it opens the
 * moment the push lands instead of waiting for its next check.
 */
object NotificationEvents {
    private val _love = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Fires when a Hug / Touch / Miss You push reaches this phone. */
    val love: SharedFlow<Unit> = _love.asSharedFlow()

    fun loveArrived() {
        _love.tryEmit(Unit)
    }
}
