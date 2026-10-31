package com.twinhearts.app.core.design

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import kotlinx.coroutines.delay

private val SplashBackground = Color(0xFF14030E)

/** The system splash icon area is 288 dp wide; splash_brand.png (logo + name) is drawn for exactly that size. */
private val SplashBrandSize = 288.dp

/** How long the logo and name stay on screen in total, counted from when the app started opening. */
const val SPLASH_MIN_VISIBLE_MILLIS = 2_800L

/**
 * Start screen. Logo and "Twin Hearts" are one picture (splash_brand.png), shown together from the
 * very first frame. The Android system splash shows the same picture at the same place, so there is
 * no blank page and no jump. It stays until [SPLASH_MIN_VISIBLE_MILLIS] after [startedAtUptimeMillis],
 * then fades out and calls [onFinished].
 *
 * [onFirstFrame] is called once this screen has drawn, so the system splash can be removed.
 */
@Composable
fun TwinSplash(
    startedAtUptimeMillis: Long,
    onFirstFrame: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        withFrameNanos { }   // wait until the first frame is on screen
        onFirstFrame()
        val remaining = SPLASH_MIN_VISIBLE_MILLIS - (SystemClock.uptimeMillis() - startedAtUptimeMillis)
        if (remaining > 0) delay(remaining)
        exitAlpha.animateTo(0f, tween(300))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAlpha.value }
            .background(SplashBackground),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.splash_brand),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(SplashBrandSize),
        )
    }
}
