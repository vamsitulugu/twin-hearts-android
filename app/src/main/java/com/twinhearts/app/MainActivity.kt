package com.twinhearts.app

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.twinhearts.app.core.design.TwinSplash
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.navigation.TwinApp
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.twinhearts.app.core.pro.PaymentBridge
import com.twinhearts.app.feature.ads.data.AdsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** The single Activity (R-05). Draws edge-to-edge and hosts the whole Compose app. */
@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    @Inject lateinit var paymentBridge: PaymentBridge
    @Inject lateinit var adsManager: AdsManager

    private var splashStart = 0L
    @Volatile private var splashReady = false

    // Razorpay reports the payment result here; the Pro screen listens through the bridge.
    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        paymentBridge.success(data?.orderId, razorpayPaymentId ?: data?.paymentId, data?.signature)
    }

    override fun onPaymentError(code: Int, response: String?, data: PaymentData?) {
        paymentBridge.error(code, response)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash: the system splash shows the logo instantly (no blank page). It stays until the
        // Compose splash (TwinSplash) has drawn its first frame, then hands over at the same place.
        splashStart = SystemClock.uptimeMillis()
        splashReady = savedInstanceState != null // a restored app skips the splash
        val systemSplash = installSplashScreen()
        systemSplash.setKeepOnScreenCondition {
            !splashReady && SystemClock.uptimeMillis() - splashStart < SPLASH_HANDOVER_TIMEOUT_MILLIS
        }
        systemSplash.setOnExitAnimationListener { it.remove() } // no extra exit animation: instant hand-over
        // Transparent bars with light icons: the app background is always dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        Checkout.preload(applicationContext) // opens the payment window faster later
        setContent {
            TwinTheme {
                var showSplash by rememberSaveable { mutableStateOf(savedInstanceState == null) }
                Box(Modifier.fillMaxSize()) {
                    TwinApp()
                    if (showSplash) {
                        TwinSplash(
                            startedAtUptimeMillis = splashStart,
                            onFirstFrame = { splashReady = true },
                            onFinished = { showSplash = false },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Ads: checks Pro first, then consent, then starts the ad SDK. Does nothing for Pro couples.
        adsManager.onAppStart(this)
    }


    private companion object {
        /** Safety: never keep the system splash longer than this if the Compose splash is late. */
        const val SPLASH_HANDOVER_TIMEOUT_MILLIS = 1_500L
    }
}
