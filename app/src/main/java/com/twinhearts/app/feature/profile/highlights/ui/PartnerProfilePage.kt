package com.twinhearts.app.feature.profile.highlights.ui

import com.twinhearts.app.feature.storefx.StoreFxState
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.profile.highlights.domain.HighlightsOwner
import com.twinhearts.app.feature.profile.ui.components.ProfileAvatar

private val RING_GAP = 6.dp
private val RING_WIDTH = 3.dp
private val PARTNER_PHOTO = 112.dp
private const val SPIN_MS = 4000

// Pink, blue and purple, closing back on pink so the sweep has no seam.
private val RING_COLORS = listOf(Color(0xFFFF4FA3), Color(0xFF7C5CFF), Color(0xFF3DA5FF), Color(0xFFC13BFF), Color(0xFFFF4FA3))

/**
 * Wraps a round picture with a slowly turning pink · blue · purple ring. It stands still when the phone
 * has animations turned off (Settings › Developer / Accessibility › Remove animations).
 */
@Composable
fun PartnerRing(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val animate = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val angle = if (animate) {
        val transition = rememberInfiniteTransition(label = "partnerRing")
        val turning by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(SPIN_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "partnerRingAngle",
        )
        turning
    } else {
        0f
    }
    Box(
        modifier
            .drawBehind {
                val stroke = RING_WIDTH.toPx()
                rotate(angle, pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(RING_COLORS, center = Offset(size.width / 2f, size.height / 2f)),
                        // Drawn just outside the picture, so the layout (and my side of the card) does not move.
                        radius = size.minDimension / 2f + RING_GAP.toPx() + stroke / 2f,
                        style = Stroke(stroke),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Partner profile: their photo on top (tap to see it large), their name, and below it their highlights.
 * Opens when you tap my partner's picture on Our Profile.
 */
@Composable
fun PartnerProfilePage(imageUrl: String?, name: String, onViewAvatar: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = TwinTheme.spacing.screenEdge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        PartnerRing(Modifier.padding(top = TwinTheme.spacing.l)) {
            ProfileAvatar(imageUrl, stringResource(R.string.profile_view_partner_photo), onViewAvatar.takeIf { !imageUrl.isNullOrBlank() }, size = PARTNER_PHOTO, frame = StoreFxState.partnerFrame)
        }
        Text(name, style = MaterialTheme.typography.headlineSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            stringResource(R.string.profile_hl_section_title),
            style = MaterialTheme.typography.titleSmall,
            color = colors.inkMuted,
            modifier = Modifier.fillMaxWidth(),
        )
        HighlightsSection(owner = HighlightsOwner.Partner)
    }
}
