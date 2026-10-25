package com.twinhearts.app.feature.getstarted.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.getstarted.ui.components.SlideToStart

private val LogoSize = 52.dp
private val LogoCorner = 16.dp
private val HeartSize = 26.dp
private val SlideMaxWidth = 420.dp

/**
 * First screen for someone who is signed out (web: `#mobileStart` in landing.html).
 * Order: logo + name, "Stay Connected", tagline, couple picture, slide button.
 * It has no data to load, so it has no ViewModel and no loading/error states.
 */
@Composable
fun GetStartedScreen(
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Decorative background: the wine photo, anchored to the bottom like the web page.
        Image(
            painter = painterResource(R.drawable.getstarted_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = TwinTheme.spacing.xl, vertical = TwinTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandRow()
            Hero(modifier = Modifier.padding(top = TwinTheme.spacing.xl))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = TwinTheme.spacing.m),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Image(
                    painter = painterResource(R.drawable.getstarted_couple),
                    contentDescription = stringResource(R.string.getstarted_couple_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
            SlideToStart(
                label = stringResource(R.string.getstarted_slide_label),
                description = stringResource(R.string.getstarted_slide_description),
                onComplete = onSlideComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = SlideMaxWidth),
            )
        }
    }
}

@Composable
private fun BrandRow() {
    val colors = TwinTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.core_logo),
            contentDescription = stringResource(R.string.core_logo),
            modifier = Modifier
                .size(LogoSize)
                .clip(RoundedCornerShape(LogoCorner)),
        )
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.ink,
            )
            Text(
                text = stringResource(R.string.app_tagline).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.ink.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun Hero(modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.getstarted_stay),
            style = MaterialTheme.typography.displayMedium,
            color = colors.pinkSoft,
        )
        Box {
            Text(
                text = stringResource(R.string.getstarted_connected),
                style = MaterialTheme.typography.displayLarge.copy(
                    brush = Brush.verticalGradient(listOf(colors.ink, colors.pinkSoft, colors.orchid)),
                ),
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = HeartSize / 2, y = -HeartSize / 3)
                    .size(HeartSize),
            )
        }
        Text(
            text = stringResource(R.string.getstarted_tagline).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.3.em),
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = TwinTheme.spacing.s),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360)
@Composable
private fun GetStartedPreview() {
    TwinTheme { GetStartedScreen(onSlideComplete = {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360, fontScale = 1.3f)
@Composable
private fun GetStartedLargeFontPreview() {
    TwinTheme { GetStartedScreen(onSlideComplete = {}) }
}
