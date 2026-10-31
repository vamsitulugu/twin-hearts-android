package com.twinhearts.app.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Why this file exists: the Movie and Music pages define the look of the whole app. Their glass, burgundy
 * button and page background are written once here, so every page uses exactly the same pieces.
 * Glass = a light-to-clear gradient fill plus a bright-to-faint 1 dp edge. No real blur (R-55).
 */
object TwinLook {
    /** Page background: near-black. */
    val Base = Color(0xFF0A0A0C)

    /** Very dark wine that tints the top of every page. */
    val Glow = Color(0xFF2A0617)

    /** Burgundy for the active tab, "See all" links, hearts and progress. */
    val Accent = Color(0xFFC2185B)

    val GlassFill = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f)))
    val GlassEdge = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f)))
    /** Dark glass for anything floating over a map: a smoky black tint so text stays readable on bright tiles. */
    val DarkGlassFill = Brush.verticalGradient(listOf(Color(0xFF16161A).copy(alpha = 0.78f), Color(0xFF0A0A0C).copy(alpha = 0.64f)))
    val DarkGlassEdge = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f)))
    val WineFill = Brush.linearGradient(listOf(Color(0xFF9A1156), Color(0xFF4F0629)))
    val WineEdge = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.08f)))
}

/** Frosted grey glass (same as the Music and Movie pages). */
fun Modifier.twinGlass(shape: Shape): Modifier =
    this.clip(shape).background(TwinLook.GlassFill).border(1.dp, TwinLook.GlassEdge, shape)

/** Burgundy glass for the active / primary thing. */
fun Modifier.twinWine(shape: Shape): Modifier =
    this.clip(shape).background(TwinLook.WineFill).border(1.dp, TwinLook.WineEdge, shape)

/** Full page: near-black with the soft wine glow at the top (what TwinBackground paints on Movie and Music). */
fun Modifier.twinPage(): Modifier = this
    .fillMaxSize()
    .background(TwinLook.Base)
    .drawBehind {
        drawRect(
            Brush.verticalGradient(
                0f to TwinLook.Glow.copy(alpha = 0.92f),
                0.30f to TwinLook.Base.copy(alpha = 0.55f),
                0.62f to Color.Transparent,
            ),
        )
    }


/** Dark smoky glass for cards, banners and buttons that float over a map. */
fun Modifier.twinGlassDark(shape: Shape): Modifier =
    this.clip(shape).background(TwinLook.DarkGlassFill).border(1.dp, TwinLook.DarkGlassEdge, shape)

/** When true, [GlassCard] draws dark glass. Map pages turn this on for everything inside them. */
val LocalDarkGlass = compositionLocalOf { false }
