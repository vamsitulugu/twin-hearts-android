package com.twinhearts.app.core.common

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** Settings > Appearance > "Reduce motion". TwinApp provides it once for the whole app. */
val LocalReduceMotionSetting = staticCompositionLocalOf { false }

/**
 * True when the phone's "remove animations" setting is on OR the person turned on "Reduce motion"
 * in Settings. Decorative loops must stop then (R-55).
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    val system = remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    return system || LocalReduceMotionSetting.current
}
