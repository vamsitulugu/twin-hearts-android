package com.twinhearts.app.core.notifications

import android.app.Activity
import android.app.Application
import android.os.Bundle

/** Tells whether any screen of the app is visible. Registered once in [com.twinhearts.app.TwinHeartsApp]. */
object AppForeground : Application.ActivityLifecycleCallbacks {

    @Volatile
    private var started = 0

    val isVisible: Boolean get() = started > 0

    override fun onActivityStarted(activity: Activity) { started++ }
    override fun onActivityStopped(activity: Activity) { started = (started - 1).coerceAtLeast(0) }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
