package com.twinhearts.app.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Why this file exists: one shared copy of the "neutral" page look used by Home, Chat, Twin AI,
 * Settings and Live Map (near-black page, grey glass cards, burgundy buttons and switches, no pink).
 * Pages that do not have their own style file are wrapped in [TwinNeutralPage] from their nav graph.
 */
object TwinNeutral {
    /** Page background shared by all restyled pages. */
    val Base = Color(0xFF0A0A0C)
}

/** Swaps the pink-tinted tokens for grey glass + burgundy inside [content], on a flat near-black page. */
@Composable
fun TwinNeutralPage(content: @Composable () -> Unit) {
    val base = TwinTheme.colors
    val neutral = remember(base) {
        base.copy(
            background = TwinNeutral.Base,
            backgroundGlow = TwinNeutral.Base,
            burgundy = TwinNeutral.Base,
            glass = Color.White.copy(alpha = 0.08f),
            line = Color.White.copy(alpha = 0.16f),
            lineSoft = Color.White.copy(alpha = 0.12f),
            line2 = Color.White.copy(alpha = 0.22f),
            card = Color(0xFF1E1E20),
            primaryStart = Color(0xFF7A0B44),
            primaryEnd = Color(0xFF5C0834),
            accent = Color(0xFFE6E6EA),
            pinkSoft = Color(0xFFE6E6EA),
        )
    }
    val scheme = MaterialTheme.colorScheme.copy(
        primary = neutral.primaryStart,
        secondary = neutral.accent,
        background = neutral.background,
        surface = neutral.card,
        surfaceContainer = neutral.card,
        surfaceContainerHigh = neutral.card,
        surfaceContainerHighest = neutral.card,
        surfaceContainerLow = neutral.card,
        surfaceContainerLowest = neutral.background,
        outline = neutral.line,
    )
    CompositionLocalProvider(LocalTwinColors provides neutral) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
            Box(Modifier.fillMaxSize().background(TwinNeutral.Base)) { content() }
        }
    }
}
