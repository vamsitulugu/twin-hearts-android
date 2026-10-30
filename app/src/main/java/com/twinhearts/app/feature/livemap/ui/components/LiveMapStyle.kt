package com.twinhearts.app.feature.livemap.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.twinhearts.app.core.design.LocalTwinColors
import com.twinhearts.app.core.design.TwinTheme

/**
 * Why this file exists: the Live Map page is neutral (black / grey / white) like Home, Chat and
 * Twin AI, with no pink shading. The shared glass, line, card and button tokens are pink-tinted, so
 * inside Live Map they are swapped for these. Everything under [NeutralLiveMapColors] (cards,
 * buttons, text fields, switches, dialogs) follows automatically; other screens keep the shared
 * theme and nothing in core/ is edited (R-31).
 */
internal object LiveMapStyle {
    /** Page background: the same near-black as Home, Chat, Twin AI and Settings. */
    val Base = Color(0xFF0A0A0C)
}

/** Provides the neutral colour set to everything inside [content]. */
@Composable
internal fun NeutralLiveMapColors(content: @Composable () -> Unit) {
    val base = TwinTheme.colors
    val neutral = remember(base) {
        base.copy(
            background = LiveMapStyle.Base,
            backgroundGlow = LiveMapStyle.Base,
            burgundy = com.twinhearts.app.core.design.TwinNeutral.Glow,
            // Grey glass instead of pink-tinted glass.
            glass = Color.White.copy(alpha = 0.08f),
            line = Color.White.copy(alpha = 0.16f),
            lineSoft = Color.White.copy(alpha = 0.12f),
            line2 = Color.White.copy(alpha = 0.22f),
            card = Color(0xFF1E1E20),
            // Burgundy (same family as the Chat sent bubble) for primary buttons and "on" switches.
            primaryStart = Color(0xFF7A0B44),
            primaryEnd = Color(0xFF5C0834),
            // Light grey for text links, cursor and spinners instead of hot pink.
            accent = Color(0xFFE6E6EA),
            pinkSoft = Color(0xFFE6E6EA),
        )
    }
    CompositionLocalProvider(LocalTwinColors provides neutral, content = content)
}
