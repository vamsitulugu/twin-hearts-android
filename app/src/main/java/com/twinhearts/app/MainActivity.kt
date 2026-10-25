package com.twinhearts.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.navigation.TwinApp
import dagger.hilt.android.AndroidEntryPoint

/** The single Activity (R-05). Draws edge-to-edge and hosts the whole Compose app. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent bars with light icons: the app background is always dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            TwinTheme {
                TwinApp()
            }
        }
    }
}
