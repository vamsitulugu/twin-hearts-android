package com.twinhearts.app.core.sound

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** One small switch for every app sound (coins, scratch, spin, ads, unlocks). Remembered on this phone. */
@Composable
fun SoundToggle(modifier: Modifier = Modifier) {
    val on by TwinSound.enabledFlow.collectAsState()
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = { TwinSound.toggle() },
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = if (on) "Sound effects on. Tap to turn off" else "Sound effects off. Tap to turn on" },
        ) {
            Text(if (on) "🔊 Sounds on" else "🔇 Sounds off", style = MaterialTheme.typography.labelLarge)
        }
    }
}
