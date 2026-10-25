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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.twinhearts.app.core.common.rememberReduceMotion

/** One bottom-bar entry. [key] is whatever the navigation layer uses to identify the tab. */
@Immutable
data class BottomBarItem(
    val key: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val isOrb: Boolean = false,
)

/**
 * Floating bottom bar: Home, Chat, centre "Twin" orb, Map, Settings.
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
    val barShape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(colors.card.copy(alpha = 0.94f), barShape)
                .border(1.dp, colors.line, barShape),
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
    val tint = if (selected) colors.pinkSoft else colors.inkFaint
    Column(
        modifier = modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (item.isOrb) {
            TwinOrb(icon = item.icon)
        } else {
            Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}

/** The raised centre button. It breathes slowly, unless the phone asks for no animations (R-55). */
@Composable
private fun TwinOrb(icon: ImageVector) {
    val colors = TwinTheme.colors
    val reduceMotion = rememberReduceMotion()
    val pulse = if (reduceMotion) {
        null
    } else {
        rememberInfiniteTransition(label = "twinOrb").animateFloat(
            initialValue = 1f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "twinOrbScale",
        )
    }
    Box(
        modifier = Modifier
            .offset(y = (-10).dp)
            .size(44.dp)
            .graphicsLayer {
                val scale = pulse?.value ?: 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(TwinTheme.shapes.circle)
            .background(colors.primaryBrush)
            .border(1.dp, Color.White.copy(alpha = 0.25f), TwinTheme.shapes.circle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(24.dp))
    }
}

/** Default icon for the orb, so callers do not need to import it. */
val TwinOrbIcon: ImageVector get() = Icons.Rounded.AutoAwesome
