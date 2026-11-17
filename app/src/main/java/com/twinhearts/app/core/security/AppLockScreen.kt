package com.twinhearts.app.core.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twinhearts.app.R

/** Covers the whole app (and swallows every tap) until the phone's lock is passed. */
@Composable
fun AppLockScreen(onUnlock: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF0B0B10))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_lock_title), color = Color.White, fontSize = 20.sp)
        Text(stringResource(R.string.app_lock_hint), color = Color(0xFFAAAAAA), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))
        Button(onClick = onUnlock) { Text(stringResource(R.string.app_lock_unlock)) }
    }
}
