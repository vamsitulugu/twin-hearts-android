package com.twinhearts.app.feature.twinai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.design.TwinTheme

/**
 * Why this file exists: the Twin AI page is neutral (black / grey / white) like Home and Chat, with
 * no pink shading. The shared theme glass, card and glow tokens are pink-tinted, so this page uses
 * these instead. They stay inside Twin AI (R-31); other screens keep the shared theme.
 * Glass is faked with translucent white fills and 1 dp borders, never blur (R-55).
 */
internal object TwinAiStyle {
    /** Page background: the same near-black as Home and Chat, no burgundy glow. */
    val Base = Color(0xFF0A0A0C)

    /** Small round / pill buttons in the header and the quick chips. */
    val GlassFill = Color.White.copy(alpha = 0.08f)

    /** Outline of every glass piece. */
    val GlassBorder = Color.White.copy(alpha = 0.16f)

    /** Cards: frosted grey, a little brighter at the top. */
    val CardBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.13f), Color.White.copy(alpha = 0.07f)))

    /** The typing bar: grey glass. */
    val BarBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.09f)))
    val BarBorder = Color.White.copy(alpha = 0.20f)

    /** The text field inside the bar: slightly lighter grey glass so it reads as a field. */
    val FieldBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.10f)))

    /** Menus and dialogs: solid dark grey. */
    val Menu = Color(0xFF1E1E20)

    /** Selected side of the YOU / BOTH switch: burgundy, same family as the Chat sent bubble. */
    val Burgundy = Brush.linearGradient(listOf(Color(0xFF7A0B44), Color(0xFF5C0834)))

    /** Number badge in "How it works". */
    val Badge = Color.White.copy(alpha = 0.14f)
}

/** Frosted grey card for the Twin AI page (the shared GlassCard is pink-tinted). */
@Composable
internal fun TwinAiCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = TwinTheme.spacing.l,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = TwinTheme.shapes.card
    Column(
        modifier = modifier
            .clip(shape)
            .background(TwinAiStyle.CardBrush)
            .border(1.dp, TwinAiStyle.GlassBorder, shape)
            .padding(contentPadding),
        content = content,
    )
}
