package com.twinhearts.app.feature.home.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Why this file exists: the picture card draws dark and white overlays on top of a photo.
 * These values are copied from the web CSS (`dashboard.css`: `.dh-shade`, `.dh-cap`, `.dh-act`,
 * `dashboard-widgets.css`: `.dh-edit`, `.dh-menu-pop`). They are not theme colours, so they stay
 * inside Home. See SHARED_CHANGES.md (CHANGE REQUEST: card overlay tokens).
 */
internal object HomeCardStyle {
    val Shade74 = Color.Black.copy(alpha = 0.74f)
    val Shade32 = Color.Black.copy(alpha = 0.32f)
    val Shade22 = Color.Black.copy(alpha = 0.22f)
    val Chip = Color.Black.copy(alpha = 0.34f)
    val ChipBorder = Color.White.copy(alpha = 0.20f)
    val ActionFill = Color.White.copy(alpha = 0.17f)
    val ActionBorder = Color.White.copy(alpha = 0.30f)
    val CardBorder = Color.White.copy(alpha = 0.14f)
    val CardBase = Color(0xFF270719)
    val MenuFill = Color(0xFF1E0616).copy(alpha = 0.86f)
    val MenuBorder = Color.White.copy(alpha = 0.22f)
    val StageBase = Color.Black
    val EditorBase = Color(0xFF13090F)

    // Web "neutral glass" (index.html :root --g2/--border/--border2): white, never pink.
    val GlassFill = Color.White.copy(alpha = 0.08f)
    val GlassBorder = Color.White.copy(alpha = 0.18f)
    val GlassBorderSoft = Color.White.copy(alpha = 0.10f)
    val ChipFill = Color.White.copy(alpha = 0.13f)
    val ChipBorder2 = Color.White.copy(alpha = 0.24f)
    val SearchFill = Color.White.copy(alpha = 0.15f)
    val SearchBorder = Color.White.copy(alpha = 0.28f)
    val QuoteAccent = Color(0xFFFFD0EA)
    val Hero = Color(0xFF6B093A)
    val Sheet = Color(0xFF0A0A0C)
    val SheetBorder = Color.White.copy(alpha = 0.13f)
    val SheetRadius = 30.dp

    // Gray glass for Coming up + Mood cards: white sheen over the black sheet reads as frosted gray.
    val GrayGlass = androidx.compose.ui.graphics.Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.07f)),
    )
    val GrayGlassBorder = Color.White.copy(alpha = 0.18f)

    val CardRadius = 24.dp
    val MenuRadius = 14.dp
    val ActionHeight = 46.dp
    val EditButtonSize = 36.dp
}
