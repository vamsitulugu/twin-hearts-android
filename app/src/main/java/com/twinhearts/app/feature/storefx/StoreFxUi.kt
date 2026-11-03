package com.twinhearts.app.feature.storefx

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twinhearts.app.core.design.LocalTwinColors
import com.twinhearts.app.core.design.TwinColors
import com.twinhearts.app.core.design.TwinTheme

/** The shared palette with the equipped theme's colours put in. Text flips dark on light themes. */
fun TwinColors.withStoreTheme(t: StoreFxTheme): TwinColors {
    val tint = if (t.dark) Color.White else Color.Black
    return copy(
        background = t.bg1,
        backgroundGlow = t.bg2,
        card = t.partner,
        glass = tint.copy(alpha = if (t.dark) 0.08f else 0.06f),
        accent = t.accent,
        primaryStart = t.me1,
        primaryEnd = t.me2,
        ink = t.text,
        inkMuted = t.text.copy(alpha = 0.72f),
        inkFaint = t.text.copy(alpha = 0.50f),
        line = tint.copy(alpha = 0.16f),
        lineSoft = tint.copy(alpha = 0.12f),
        line2 = tint.copy(alpha = 0.22f),
        pinkSoft = t.accent,
        burgundy = t.bg2,
    )
}

/** Wrap a whole page (Chat, Music). With nothing equipped it just shows [content] unchanged. */
@Composable
fun StoreFxThemed(content: @Composable () -> Unit) {
    val theme = StoreFxState.theme
    if (theme == null) {
        content()
    } else {
        val base = TwinTheme.colors
        val colors = remember(base, theme) { base.withStoreTheme(theme) }
        CompositionLocalProvider(LocalTwinColors provides colors, content = content)
    }
}

/** Glow behind an avatar (animated for pulse frames). Put it on the UNclipped outer box. */
fun Modifier.storeFrameGlow(frame: StoreFxCatalog.Frame?): Modifier {
    if (frame == null) return this
    return this.then(Modifier.drawBehind {
        val r = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(listOf(frame.glow.copy(alpha = 0.55f), Color.Transparent), radius = r * 1.45f),
            radius = r * 1.45f,
        )
    })
}

/** Ring colour pair for the avatar border: the frame's outer colour, or [fallback] with no frame. */
fun StoreFxCatalog.Frame?.ringBorder(width: Dp, fallback: Color): BorderStroke =
    BorderStroke(width, this?.outer ?: fallback)

/** Little badge (crown, heart, spark) on the bottom edge of a framed avatar. Place inside the outer Box. */
@Composable
fun StoreFrameBadge(frame: StoreFxCatalog.Frame?, size: Dp, modifier: Modifier = Modifier) {
    if (frame == null) return
    val pulse = if (frame.pulse) {
        val t = rememberInfiniteTransition(label = "frameBadge")
        t.animateFloat(0.9f, 1.15f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "s").value
    } else 1f
    Text(
        text = frame.badge,
        fontSize = (size.value * 0.26f).coerceAtLeast(12f).sp,
        modifier = modifier.scale(pulse),
    )
}

/**
 * "Your Cozy Corner": shows the equipped Virtual Home decor on Home. Draws nothing when no decor is
 * equipped, so Home is unchanged until the couple buys and equips one.
 */
@Composable
fun CozyCornerCard(modifier: Modifier = Modifier) {
    val decor = StoreFxState.decor ?: return
    val colors = TwinTheme.colors
    val t = rememberInfiniteTransition(label = "cozy")
    val flicker by t.animateFloat(0.92f, 1.12f, infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "flicker")
    val sway by t.animateFloat(-4f, 4f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "sway")
    val glowAlpha by t.animateFloat(0.25f, 0.6f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "glow")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(colors.card, colors.card.copy(alpha = 0.7f))))
            .border(1.dp, colors.lineSoft, RoundedCornerShape(20.dp))
            .padding(TwinTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(64.dp)
                    .alpha(glowAlpha)
                    .background(Brush.radialGradient(listOf(decor.glow, Color.Transparent)), CircleShape),
            )
            val motion = when (decor.id) {
                "decor_fireplace" -> Modifier.scale(flicker)
                "decor_teddy_corner" -> Modifier.rotate(sway)
                "decor_love_plant" -> Modifier.scale(1f + (flicker - 1f) * 0.4f)
                else -> Modifier
            }
            Text(decor.emoji, fontSize = 40.sp, modifier = motion)
        }
        Spacer(Modifier.width(TwinTheme.spacing.m))
        Column(Modifier.weight(1f)) {
            Text("Your Cozy Corner", style = MaterialTheme.typography.titleSmall, color = colors.ink)
            Text("${decor.title} · ${decor.line}", style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            if (decor.id == "decor_fairy_lights") {
                Spacer(Modifier.height(TwinTheme.spacing.s))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    repeat(9) { i ->
                        val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(700 + i * 90), RepeatMode.Reverse), label = "light$i")
                        Box(Modifier.size(7.dp).alpha(a).background(decor.glow, CircleShape))
                    }
                }
            }
        }
    }
}
