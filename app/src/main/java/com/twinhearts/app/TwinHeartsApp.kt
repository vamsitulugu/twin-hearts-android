package com.twinhearts.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application class. Its only job is to start Hilt (dependency injection) for the whole app. */
@HiltAndroidApp
class TwinHeartsApp : Application()
