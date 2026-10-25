package com.twinhearts.app.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Every colour in the app lives here (R-50). Values come from the web CSS
 * (dashboard-burgundy.css / landing.html). It is a class, not an object, so a light
 * theme can be added later without touching features (R-54).
 */
@Immutable
data class TwinColors(
    val background: Color,
    val backgroundGlow: Color,
    val card: Color,
    val glass: Color,
    val accent: Color,
    val primaryStart: Color,
    val primaryEnd: Color,
    val brand: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val line: Color,
    val lineSoft: Color,
    val neutralGlass: Color,
    val error: Color,
    val success: Color,
    // Get Started / start-screen look
    val pinkSoft: Color,
    val orchid: Color,
    val inkSoft: Color,
    val slideTrack: Color,
    val slideLabel: Color,
    val slideFillStart: Color,
    val slideFillEnd: Color,
    val knobStart: Color,
    val knobEnd: Color,
    val scrim: Color,
) {
    /** Burgundy gradient used by primary buttons and the Twin orb. */
    val primaryBrush: Brush get() = Brush.linearGradient(listOf(primaryStart, primaryEnd))
}

val DarkTwinColors = TwinColors(
    background = Color(0xFF14030E),
    backgroundGlow = Color(0xFF3A0A27),
    card = Color(0xFF400E2A),
    glass = Color.White.copy(alpha = 0.07f),
    accent = Color(0xFFC21A8B),
    primaryStart = Color(0xFFB0127A),
    primaryEnd = Color(0xFF8A0E5F),
    brand = Color(0xFF6B093A),
    ink = Color(0xFFFEF4FA),
    inkMuted = Color.White.copy(alpha = 0.74f),
    inkFaint = Color.White.copy(alpha = 0.52f),
    line = Color(0xFFC21A8B).copy(alpha = 0.20f),
    lineSoft = Color.White.copy(alpha = 0.13f),
    neutralGlass = Color(0xFF8A8A92),
    error = Color(0xFFFF8FAE),
    success = Color(0xFF34D399),
    pinkSoft = Color(0xFFFFB6E6),
    orchid = Color(0xFFD9A8FF),
    inkSoft = Color(0xFFC7A3B8),
    slideTrack = Color.White,
    slideLabel = Color(0xFFA3126F),
    slideFillStart = Color(0xFFFFD3F0),
    slideFillEnd = Color(0xFFFF9FDC),
    knobStart = Color(0xFFC2188A),
    knobEnd = Color(0xFF8F1068),
    scrim = Color(0xFF14030E).copy(alpha = 0.55f),
)
