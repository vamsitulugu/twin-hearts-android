package com.twinhearts.app.feature.home.domain

/** Size of the saved card picture (same as the web editor). */
const val CARD_PHOTO_WIDTH = 1280
const val CARD_PHOTO_HEIGHT = 720
const val CARD_PHOTO_MAX_ZOOM = 4f

private const val CARD_ASPECT = 16f / 9f

/**
 * Which part of the picked photo is shown: [zoom] (1 = the photo just covers the 16:9 frame) and
 * the centre of the visible window in photo pixels.
 */
data class CropState(val zoom: Float, val centerX: Float, val centerY: Float)

/** The visible window in photo pixels. */
data class CropRect(val left: Float, val top: Float, val width: Float, val height: Float)

/**
 * Pure maths for the 16:9 crop editor (no Android classes, so it is unit-tested).
 * The web editor does the same with a translate + scale; here we keep the visible window instead,
 * which does not depend on the screen size.
 */
object CropMath {

    private fun windowWidth(imageW: Int, imageH: Int, zoom: Float): Float {
        val base = if (imageW.toFloat() / imageH >= CARD_ASPECT) imageH * CARD_ASPECT else imageW.toFloat()
        return base / zoom
    }

    /** Photo fully centred, no zoom. */
    fun initial(imageW: Int, imageH: Int) = CropState(1f, imageW / 2f, imageH / 2f)

    /** Keeps the window inside the photo and the zoom inside 1..4. */
    fun clamp(state: CropState, imageW: Int, imageH: Int): CropState {
        val zoom = state.zoom.coerceIn(1f, CARD_PHOTO_MAX_ZOOM)
        val w = windowWidth(imageW, imageH, zoom)
        val h = w / CARD_ASPECT
        return CropState(
            zoom = zoom,
            centerX = state.centerX.coerceIn(w / 2f, imageW - w / 2f),
            centerY = state.centerY.coerceIn(h / 2f, imageH - h / 2f),
        )
    }

    fun rect(state: CropState, imageW: Int, imageH: Int): CropRect {
        val s = clamp(state, imageW, imageH)
        val w = windowWidth(imageW, imageH, s.zoom)
        val h = w / CARD_ASPECT
        return CropRect(s.centerX - w / 2f, s.centerY - h / 2f, w, h)
    }

    /** Dragging the picture right by [dx] stage pixels moves the window left. */
    fun pan(state: CropState, dx: Float, dy: Float, stageWidthPx: Float, imageW: Int, imageH: Int): CropState {
        if (stageWidthPx <= 0f) return state
        val scale = windowWidth(imageW, imageH, state.zoom) / stageWidthPx
        return clamp(state.copy(centerX = state.centerX - dx * scale, centerY = state.centerY - dy * scale), imageW, imageH)
    }

    /**
     * Zooms to [newZoom] keeping the point under the finger fixed.
     * [focusX] / [focusY] are 0..1 positions inside the frame (0.5, 0.5 = centre).
     */
    fun zoomTo(state: CropState, newZoom: Float, focusX: Float, focusY: Float, imageW: Int, imageH: Int): CropState {
        val old = clamp(state, imageW, imageH)
        val zoom = newZoom.coerceIn(1f, CARD_PHOTO_MAX_ZOOM)
        val oldW = windowWidth(imageW, imageH, old.zoom)
        val oldH = oldW / CARD_ASPECT
        val pointX = old.centerX - oldW / 2f + focusX * oldW
        val pointY = old.centerY - oldH / 2f + focusY * oldH
        val newW = windowWidth(imageW, imageH, zoom)
        val newH = newW / CARD_ASPECT
        return clamp(CropState(zoom, pointX - focusX * newW + newW / 2f, pointY - focusY * newH + newH / 2f), imageW, imageH)
    }
}
