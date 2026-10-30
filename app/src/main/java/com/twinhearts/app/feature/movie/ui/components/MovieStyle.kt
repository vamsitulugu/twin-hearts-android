package com.twinhearts.app.feature.movie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.twinhearts.app.core.design.LocalTwinColors
import com.twinhearts.app.core.design.TwinTheme

/**
 * Why this file exists: the Movie page now matches Home, Chat, Twin AI, Settings and Live Map:
 * near-black page, grey glass cards, burgundy buttons and "on" switches, no pink shading. The
 * shared glass, line, card and button tokens are pink-tinted, so inside this page they are swapped
 * for these. Everything under [MovieStyled] (cards, buttons, fields, switches, sheets, dialogs)
 * follows automatically; other screens keep the shared theme and nothing in core/ is edited (R-31).
 */
internal object MovieStyle {
    /** Page background: the same near-black as the other restyled pages. */
    val Base = Color(0xFF0A0A0C)

    /** Burgundy used for the active tab, "See all" links, hearts and progress (same as Music). */
    val Accent = Color(0xFFC2185B)

    /** Very dark wine that tints the top of the page so the glass has something to sit on (same as Music). */
    val Glow = Color(0xFF2A0617)
}

/** Neutral colour set for everything inside [content]. */
@Composable
private fun NeutralMovieColors(content: @Composable () -> Unit) {
    val base = TwinTheme.colors
    val neutral = remember(base) {
        base.copy(
            background = MovieStyle.Base,
            backgroundGlow = MovieStyle.Base,
            // TwinBackground paints a soft wine glow from this colour at the top of the page.
            burgundy = MovieStyle.Glow,
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
        MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes, content = content)
    }
}

/** Wraps a whole page: neutral colours plus the flat near-black background. */
@Composable
internal fun MovieStyled(content: @Composable () -> Unit) {
    NeutralMovieColors {
        Box(Modifier.fillMaxSize().background(MovieStyle.Base)) { content() }
    }
}
