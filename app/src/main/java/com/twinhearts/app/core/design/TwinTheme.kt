package com.twinhearts.app.core.design

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. Features never type raw dp numbers for gaps (R-50). */
@Immutable
data class TwinSpacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val screenEdge: Dp = 20.dp,
)

/** Shapes and fixed sizes shared by all components. */
@Immutable
data class TwinShapes(
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
    val card: RoundedCornerShape = RoundedCornerShape(20.dp),
    val field: RoundedCornerShape = RoundedCornerShape(16.dp),
    val small: RoundedCornerShape = RoundedCornerShape(12.dp),
    val circle: androidx.compose.ui.graphics.Shape = CircleShape,
    val buttonHeight: Dp = 52.dp,
    val fieldHeight: Dp = 54.dp,
    val minTouch: Dp = 48.dp,
)

val LocalTwinColors = staticCompositionLocalOf { DarkTwinColors }
val LocalTwinSpacing = staticCompositionLocalOf { TwinSpacing() }
val LocalTwinShapes = staticCompositionLocalOf { TwinShapes() }

/** Short access to the design tokens: `TwinTheme.colors.accent`, `TwinTheme.spacing.l` ... */
object TwinTheme {
    val colors: TwinColors @Composable @ReadOnlyComposable get() = LocalTwinColors.current
    val spacing: TwinSpacing @Composable @ReadOnlyComposable get() = LocalTwinSpacing.current
    val shapes: TwinShapes @Composable @ReadOnlyComposable get() = LocalTwinShapes.current
}

/** Wraps the app. Dark only for now (R-54); the tokens are ready for a light set later. */
@Composable
fun TwinTheme(content: @Composable () -> Unit) {
    val c = DarkTwinColors
    val scheme = darkColorScheme(
        primary = c.primaryStart,
        onPrimary = c.ink,
        secondary = c.accent,
        onSecondary = c.ink,
        background = c.background,
        onBackground = c.ink,
        surface = c.card,
        onSurface = c.ink,
        onSurfaceVariant = c.inkMuted,
        outline = c.line,
        error = c.error,
    )
    CompositionLocalProvider(
        LocalTwinColors provides c,
        LocalTwinSpacing provides TwinSpacing(),
        LocalTwinShapes provides TwinShapes(),
    ) {
        MaterialTheme(colorScheme = scheme, typography = TwinTypography, content = content)
    }
}
