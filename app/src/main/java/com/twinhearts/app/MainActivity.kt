package com.twinhearts.app

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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

    // Razorpay reports the payment result here; the Pro screen listens through the bridge.
    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        paymentBridge.success(data?.orderId, razorpayPaymentId ?: data?.paymentId, data?.signature)
    }

    override fun onPaymentError(code: Int, response: String?, data: PaymentData?) {
        paymentBridge.error(code, response)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash: the logo stays on screen for a short moment (about 1 second) while the app opens.
        val splashStart = SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition { SystemClock.uptimeMillis() - splashStart < SPLASH_MIN_MILLIS }
        // Transparent bars with light icons: the app background is always dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        Checkout.preload(applicationContext) // opens the payment window faster later
        setContent {
            TwinTheme {
                TwinApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Ads: checks Pro first, then consent, then starts the ad SDK. Does nothing for Pro couples.
        adsManager.onAppStart(this)
    }

    private companion object {
        const val SPLASH_MIN_MILLIS = 1_000L
    }
}
