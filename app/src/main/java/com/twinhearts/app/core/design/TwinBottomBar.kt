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
) {
    val colors = TwinTheme.colors
    val barShape = RoundedCornerShape(30.dp)
    Box(
        modifier = modifier
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

@Composable
private fun BarItem(
    item: BottomBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val tint = if (selected) Color.White else Color.White.copy(alpha = 0.52f)
    Column(
        modifier = modifier
            .padding(vertical = 5.dp, horizontal = 2.dp)
            .then(
                if (item.isOrb) {
                    Modifier // never clip the orb: it floats above the bar
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
                        .offset(y = (-14).dp),
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
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            modifier = Modifier.padding(top = if (item.isOrb) 2.dp else 1.dp),
        )
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
