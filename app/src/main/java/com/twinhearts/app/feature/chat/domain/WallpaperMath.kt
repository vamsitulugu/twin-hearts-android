package com.twinhearts.app.feature.chat.domain

import kotlin.math.max

/**
 * Why this file exists: the "Move and scale" wallpaper screen is pure geometry. Keeping it here
 * (no Android classes) means it can be unit tested.
 */

/** The part of the picture that is visible, in the picture's own pixels. */
data class CropRect(val left: Float, val top: Float, val width: Float, val height: Float)

/** Smallest zoom at which the picture still covers the whole [viewW] x [viewH] stage. */
fun minCoverScale(natW: Float, natH: Float, viewW: Float, viewH: Float): Float =
    max(viewW / natW, viewH / natH)

/** Keeps the picture covering the stage: no empty edge can show. Returns the corrected offset. */
fun clampOffset(offset: Float, natSize: Float, scale: Float, viewSize: Float): Float {
    val scaled = natSize * scale
    val min = viewSize - scaled
    return if (scaled <= viewSize) (viewSize - scaled) / 2f else offset.coerceIn(min, 0f)
}

/** The visible window when the picture is drawn at [scale] with its top-left corner at ([offsetX], [offsetY]). */
fun visibleCrop(natW: Float, natH: Float, viewW: Float, viewH: Float, scale: Float, offsetX: Float, offsetY: Float): CropRect {
    val width = (viewW / scale).coerceAtMost(natW)
    val height = (viewH / scale).coerceAtMost(natH)
    val left = (-offsetX / scale).coerceIn(0f, natW - width)
    val top = (-offsetY / scale).coerceIn(0f, natH - height)
    return CropRect(left, top, width, height)
}
