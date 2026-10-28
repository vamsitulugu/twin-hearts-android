package com.twinhearts.app.feature.livemap.data.share

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Starts and stops the background sharing service. A small class of its own so ViewModels can be tested without Android. */
@Singleton
class ShareLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun start() = LiveShareService.start(context)
    fun stop() = LiveShareService.stop(context)
}
