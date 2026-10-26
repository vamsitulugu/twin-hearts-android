package com.twinhearts.app.feature.home

import com.twinhearts.app.feature.home.domain.CARD_PHOTO_MAX_ZOOM
import com.twinhearts.app.feature.home.domain.CropMath
import com.twinhearts.app.feature.home.domain.CropState
import org.junit.Assert.assertEquals
import org.junit.Test

class CropMathTest {
    private val delta = 0.01f

    @Test
    fun wide16by9Photo_fillsTheFrame_atZoom1() {
        val rect = CropMath.rect(CropMath.initial(1600, 900), 1600, 900)
        assertEquals(0f, rect.left, delta)
        assertEquals(0f, rect.top, delta)
        assertEquals(1600f, rect.width, delta)
        assertEquals(900f, rect.height, delta)
    }

    @Test
    fun squarePhoto_isCutToTheMiddleStrip() {
        val rect = CropMath.rect(CropMath.initial(1000, 1000), 1000, 1000)
        assertEquals(1000f, rect.width, delta)
        assertEquals(562.5f, rect.height, delta)
        assertEquals(218.75f, rect.top, delta)
    }

    @Test
    fun zoom2_showsHalfTheWidth_inTheMiddle() {
        val state = CropMath.zoomTo(CropMath.initial(1600, 900), 2f, 0.5f, 0.5f, 1600, 900)
        val rect = CropMath.rect(state, 1600, 900)
        assertEquals(800f, rect.width, delta)
        assertEquals(400f, rect.left, delta)
        assertEquals(225f, rect.top, delta)
    }

    @Test
    fun zoomKeepsThePointUnderTheFinger() {
        val state = CropMath.zoomTo(CropMath.initial(1600, 900), 2f, 0f, 0f, 1600, 900)
        val rect = CropMath.rect(state, 1600, 900)
        assertEquals(0f, rect.left, delta)
        assertEquals(0f, rect.top, delta)
    }

    @Test
    fun zoom_isLimitedTo4() {
        val state = CropMath.zoomTo(CropMath.initial(1600, 900), 10f, 0.5f, 0.5f, 1600, 900)
        assertEquals(CARD_PHOTO_MAX_ZOOM, state.zoom, delta)
    }

    @Test
    fun draggingRight_movesTheWindowLeft() {
        val start = CropState(zoom = 2f, centerX = 800f, centerY = 450f)
        // Window is 800 px wide on a 400 px frame: 1 frame pixel = 2 photo pixels.
        val moved = CropMath.pan(start, dx = 50f, dy = 0f, stageWidthPx = 400f, imageW = 1600, imageH = 900)
        assertEquals(700f, moved.centerX, delta)
    }

    @Test
    fun pan_neverLeavesThePhoto() {
        val start = CropState(zoom = 2f, centerX = 800f, centerY = 450f)
        val moved = CropMath.pan(start, dx = 100_000f, dy = 100_000f, stageWidthPx = 400f, imageW = 1600, imageH = 900)
        assertEquals(400f, moved.centerX, delta)
        assertEquals(225f, moved.centerY, delta)
    }

    @Test
    fun pan_withUnknownFrameWidth_changesNothing() {
        val start = CropState(zoom = 2f, centerX = 800f, centerY = 450f)
        assertEquals(start, CropMath.pan(start, 10f, 10f, 0f, 1600, 900))
    }
}
