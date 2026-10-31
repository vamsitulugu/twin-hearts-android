package com.twinhearts.app.core.design

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import com.twinhearts.app.core.common.rememberReduceMotion

/** One bottom-bar entry. [key] is whatever the navigation layer uses to identify the tab. */
@Immutable
data class BottomBarItem(
    val key: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val isOrb: Boolean = false,
    /** Filled icon shown while the tab is selected (the web bar uses line icons, filled when active). */
    val selectedIcon: ImageVector = icon,
)

/**
 * Floating glass bottom bar, matched to the web app: dark translucent pill, light border,
 * line icons (filled + soft highlight when active) and the raised "Twin" orb in the middle.
 * Translucent fill + border instead of blur (R-55). Pads itself for the gesture bar.
 */
@Composable
fun TwinBottomBar(
    items: List<BottomBarItem>,
    selectedKey: Any?,
    onItemClick: (BottomBarItem) -> Unit,
    modifier: Modifier = Modifier,
    /** Fills the whole bar slot (also the strip beside the orb). Leave transparent unless the page behind is a flat colour. */
    slotFill: Color = Color.Transparent,
) {
    val colors = TwinTheme.colors
    // The bar's top edge dips in a U around the orb, so the orb stands in its own cradle.
    val barShape = remember { NotchedBarShape() }
    Box(modifier = modifier.fillMaxWidth().background(slotFill)) {
        // Black backing plate: hides whatever scrolls behind the bar and the gesture area, keeps a
        // small gap around the bar, and its two top corners curve up to match the bar's curve.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = PlateTopOffset)
                .background(Color.Black, RoundedCornerShape(topStart = PlateCorner, topEnd = PlateCorner)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp)
                .padding(top = 28.dp, bottom = TwinTheme.spacing.s),
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                // Glass: see-through white sheen + bright top edge. No .shadow() here: it clips its
                // children to the bar shape, which cut the top of the orb.
                // Web .glass-nav: dark tint (rgba(18,8,14,.42) + backdrop blur) so content never shows
                // through as clear; no blur here (R-55), so the tint is stronger. Then the white sheen.
                .background(Color(0xFF12080E).copy(alpha = 0.92f), barShape)
                // Glass sheen: bright at the top edge, fading down, with a faint lift at the bottom
                // so the pill reads as a curved pane of glass.
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.White.copy(alpha = 0.26f),
                        0.35f to Color.White.copy(alpha = 0.09f),
                        0.70f to Color.White.copy(alpha = 0.03f),
                        1.00f to Color.White.copy(alpha = 0.10f),
                    ),
                    barShape,
                )
                // Specular rim: strong light on the top edge, soft on the bottom edge.
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        0.00f to Color.White.copy(alpha = 0.45f),
                        0.50f to Color.White.copy(alpha = 0.10f),
                        1.00f to Color.White.copy(alpha = 0.22f),
                    ),
                    barShape,
                )
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                BarItem(
                    item = item,
                    selected = item.key == selectedKey,
                    onClick = { onItemClick(item) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
        }
    }
}

private val PlateTopOffset = 22.dp // 6 dp above the bar's top edge (the bar starts 28 dp down)
private val PlateCorner = 40.dp

/**
 * The bar pill (62 dp tall, 30 dp corners) with a round cut-out in the top edge, centred on the orb.
 * The cut-out is a circle drawn around the orb's centre (8 dp below the bar's top edge) and is only
 * [NotchRadius] - orb radius (about 4 dp) bigger than the orb, so a thin line of space follows the orb.
 * The two top corners of the cut-out are softened with a tiny curve.
 */
private class NotchedBarShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = with(density) {
        val w = size.width
        val h = size.height
        val r = 30.dp.toPx()
        val cx = w / 2f
        val nr = NotchRadius.toPx()
        val cy = NotchCenterY.toPx()
        val shoulder = 5.dp.toPx()   // length of the soft corner on the top edge
        val fillet = 3.dp.toPx()     // how far below the top edge the circle starts
        val relY = cy - fillet       // start height of the circle, relative to its centre
        val half = sqrt(nr * nr - relY * relY)
        val theta = atan2(relY, half)                  // radians
        val thetaDeg = Math.toDegrees(theta.toDouble()).toFloat()
        val k = shoulder * 0.5f
        // direction the outline travels at the left start point of the arc (down and slightly outward)
        val dx = sin(Math.PI + theta.toDouble()).toFloat()
        val dy = -cos(Math.PI + theta.toDouble()).toFloat()
        val leftX = cx - half
        val rightX = cx + half
        Path().apply {
            moveTo(r, 0f)
            lineTo(leftX - shoulder, 0f)
            cubicTo(leftX - shoulder * 0.45f, 0f, leftX - dx * k, fillet - dy * k, leftX, fillet)
            arcTo(Rect(cx - nr, cy - nr, cx + nr, cy + nr), 180f + thetaDeg, -(180f + 2f * thetaDeg), false)
            // direction at the right end of the arc (up and slightly inward) is (-sin t, -cos t)
            cubicTo(
                rightX - sin(theta) * k, fillet - cos(theta) * k,
                rightX + shoulder * 0.45f, 0f,
                rightX + shoulder, 0f,
            )
            lineTo(w - r, 0f)
            arcTo(Rect(w - 2 * r, 0f, w, 2 * r), -90f, 90f, false)
            lineTo(w, h - r)
            arcTo(Rect(w - 2 * r, h - 2 * r, w, h), 0f, 90f, false)
            lineTo(r, h)
            arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false)
            lineTo(0f, r)
            arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
            close()
        }.let { Outline.Generic(it) }
    }
}

// Orb is 60 dp (radius 30): 34 dp leaves a ~4 dp line of space around it.
private val NotchRadius = 34.dp
private val NotchCenterY = 8.dp

// Without the "Twin" label the orb's 24 dp slot is centred in the bar: its centre is 31 dp below the
// bar's top edge (5 dp item padding + 14 dp + 12 dp). This moves the orb's centre up to NotchCenterY.
private val OrbOffsetY = NotchCenterY - 31.dp

@Composable
private fun BarItem(
    item: BottomBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val tint = if (selected) Color.White else Color.White.copy(alpha = 0.52f)
    val label = stringResource(item.labelRes)
    Column(
        modifier = modifier
            .padding(vertical = 5.dp, horizontal = 2.dp)
            .then(
                if (item.isOrb) {
                    // never clip the orb: it floats above the bar. No visible label, but screen
                    // readers still announce it.
                    Modifier.semantics { contentDescription = label }
                } else {
                    Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (selected) colors.navActive else Color.Transparent)
                },
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (item.isOrb) {
            // Same 24 dp slot as the other icons; the orb floats above the bar from inside it.
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                TwinOrb(
                    selected = selected,
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .offset(y = OrbOffsetY),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .width(46.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (!item.isOrb) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = tint,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

/**
 * The raised centre button: dark glass ring around the animated energy sphere (same as the web orb).
 * The ring breathes slowly, unless the phone asks for no animations (R-55).
 */
@Composable
private fun TwinOrb(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val reduceMotion = rememberReduceMotion()
    val pulse = if (reduceMotion) {
        null
    } else {
        rememberInfiniteTransition(label = "twinOrb").animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "twinOrbScale",
        )
    }
    Box(
        modifier = modifier
            .size(60.dp)
            .graphicsLayer {
                val scale = pulse?.value ?: 1f
                scaleX = scale
                scaleY = scale
            }
            .shadow(12.dp, TwinTheme.shapes.circle, clip = false, ambientColor = colors.accent, spotColor = colors.accent)
            .clip(TwinTheme.shapes.circle)
            .background(Color(0xFF120A0E))
            .border(1.dp, Color.White.copy(alpha = if (selected) 0.40f else 0.22f), TwinTheme.shapes.circle)
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        EnergyOrb(size = 50.dp)
    }
}

/** Default icon for the orb, so callers do not need to import it. */
val TwinOrbIcon: ImageVector get() = Icons.Rounded.AutoAwesome
