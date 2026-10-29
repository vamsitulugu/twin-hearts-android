package com.twinhearts.app.feature.globe.ui.globe

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.twinhearts.app.core.design.TwinTheme
import kotlin.math.cos
import kotlin.math.sin

/*
 * Why this file exists: every colour of the globe, map and pins, built only from the app's design
 * tokens (R-50), plus the artwork of the heart pins drawn once and reused.
 */

@Immutable
data class GlobePalette(
    val oceanLight: Color,
    val oceanDeep: Color,
    val land: Color,
    val landVisited: Color,
    val border: Color,
    val graticule: Color,
    val atmosphere: Color,
    val limbShade: Color,
    val star: Color,
    val glow: Color,
    val arcs: List<Color>,
    val me: Color,
    val partner: Color,
    val outline: Color,
    val search: Color,
    val label: Int,
    val labelHalo: Int,
    val pinBody: Int,
    val pinOutline: Int,
    val pinBadgeText: Int,
)

@Composable
fun rememberGlobePalette(): GlobePalette {
    val c = TwinTheme.colors
    return remember(c) {
        GlobePalette(
            oceanLight = lerp(c.background, c.accent, 0.22f),
            oceanDeep = lerp(c.background, c.brand, 0.35f),
            land = lerp(c.card, c.ink, 0.16f),
            landVisited = c.pinkSoft.copy(alpha = 0.55f),
            border = c.ink.copy(alpha = 0.22f),
            graticule = c.ink.copy(alpha = 0.07f),
            atmosphere = c.accent,
            limbShade = c.background.copy(alpha = 0.62f),
            star = c.ink,
            glow = c.pinkSoft,
            arcs = listOf(c.pinkSoft, c.orchid, c.success, c.accent),
            me = c.success,
            partner = c.brand,
            outline = c.ink,
            search = c.accent,
            label = c.ink.toArgb(),
            labelHalo = c.background.toArgb(),
            pinBody = c.brand.toArgb(),
            pinOutline = c.ink.toArgb(),
            pinBadgeText = c.brand.toArgb(),
        )
    }
}

/**
 * The teardrop heart pin with the mood emoji, and the round "N cities" bubble, drawn with Android's
 * Canvas once per look. The same pictures are used on the 3D globe and on the flat map.
 * The artwork is the web app's heartMarkerCanvas / clusterMarkerCanvas, redrawn natively.
 */
class PinArt(private val palette: GlobePalette) {
    private val bitmaps = HashMap<String, Bitmap>()
    private val images = HashMap<String, ImageBitmap>()

    fun pin(emoji: String, count: Int, selected: Boolean): Bitmap =
        bitmaps.getOrPut("p|$emoji|$count|$selected") { drawPin(emoji, count, selected) }

    fun cluster(count: Int): Bitmap = bitmaps.getOrPut("c|$count") { drawCluster(count) }

    fun pinImage(emoji: String, count: Int, selected: Boolean): ImageBitmap =
        images.getOrPut("p|$emoji|$count|$selected") { pin(emoji, count, selected).asImageBitmap() }

    fun clusterImage(count: Int): ImageBitmap = images.getOrPut("c|$count") { cluster(count).asImageBitmap() }

    private fun drawPin(emoji: String, count: Int, selected: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(PIN_PX, PIN_PX, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.scale(SCALE, SCALE)
        val cx = BASE / 2f
        val tipY = BASE - 8f
        val r = HEAD_R
        val headCy = tipY - r * 1.55f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Soft shadow where the pin touches the ground.
        cv.save()
        cv.translate(cx, tipY + 2f)
        cv.scale(1f, 0.28f)
        paint.shader = RadialGradient(0f, 0f, r * 0.9f, SHADOW, 0x00000000, Shader.TileMode.CLAMP)
        cv.drawCircle(0f, 0f, r * 0.9f, paint)
        cv.restore()

        // Glow around the head (bigger when the pin is selected).
        val glowR = r * if (selected) 2.0f else 1.6f
        val glowColor = palette.pinBody
        val glowAlpha = if (selected) 0x8C else 0x66
        paint.shader = RadialGradient(
            cx, headCy, glowR,
            (glowAlpha shl 24) or (glowColor and 0x00FFFFFF), glowColor and 0x00FFFFFF, Shader.TileMode.CLAMP,
        )
        cv.drawCircle(cx, headCy, glowR, paint)
        paint.shader = null

        // The teardrop.
        val body = Path()
        body.arcTo(RectF(cx - r, headCy - r, cx + r, headCy + r), 189f, 162f, true)
        body.quadTo(cx + r * 0.95f, headCy + r * 0.75f, cx, tipY)
        body.quadTo(cx - r * 0.95f, headCy + r * 0.75f, cx - r * cos(Math.toRadians(9.0)).toFloat(), headCy - r * sin(Math.toRadians(9.0)).toFloat())
        body.close()
        paint.style = Paint.Style.FILL
        paint.color = palette.pinBody
        cv.drawPath(body, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = withAlpha(palette.pinOutline, OUTLINE_ALPHA)
        cv.drawPath(body, paint)

        // The mood emoji on the head.
        val text = Paint(Paint.ANTI_ALIAS_FLAG)
        text.textAlign = Paint.Align.CENTER
        text.textSize = r * 1.05f
        val fm = text.fontMetrics
        cv.drawText(emoji, cx, headCy - (fm.ascent + fm.descent) / 2f, text)

        // Number badge when a city has more than one memory.
        if (count > 1) {
            val bx = cx + r * 0.78f
            val by = headCy - r * 0.85f
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(palette.pinOutline, BADGE_ALPHA)
            cv.drawCircle(bx, by, BADGE_R, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = palette.pinBody
            cv.drawCircle(bx, by, BADGE_R, paint)
            text.color = palette.pinBadgeText
            text.typeface = Typeface.DEFAULT_BOLD
            text.textSize = 13f
            val tm = text.fontMetrics
            cv.drawText(count.toString(), bx, by - (tm.ascent + tm.descent) / 2f, text)
        }
        return bmp
    }

    private fun drawCluster(count: Int): Bitmap {
        val bmp = Bitmap.createBitmap(CLUSTER_PX, CLUSTER_PX, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.scale(SCALE, SCALE)
        val mid = CLUSTER_BASE / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val glowColor = palette.pinBody
        paint.shader = RadialGradient(
            mid, mid, mid * 0.95f,
            (0x8C shl 24) or (glowColor and 0x00FFFFFF), glowColor and 0x00FFFFFF, Shader.TileMode.CLAMP,
        )
        cv.drawCircle(mid, mid, mid * 0.95f, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = palette.pinBody
        cv.drawCircle(mid, mid, CLUSTER_R, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = withAlpha(palette.pinOutline, OUTLINE_ALPHA)
        cv.drawCircle(mid, mid, CLUSTER_R, paint)
        val text = Paint(Paint.ANTI_ALIAS_FLAG)
        text.textAlign = Paint.Align.CENTER
        text.typeface = Typeface.DEFAULT_BOLD
        text.textSize = 20f
        text.color = palette.pinOutline
        val fm = text.fontMetrics
        cv.drawText(count.toString(), mid, mid - (fm.ascent + fm.descent) / 2f, text)
        return bmp
    }

    private fun withAlpha(argb: Int, alpha: Int): Int = (alpha shl 24) or (argb and 0x00FFFFFF)

    companion object {
        /** Drawn on a 96 x 96 grid like the web canvas, rendered at 1.5x for sharp edges. */
        private const val BASE = 96
        private const val SCALE = 1.5f
        const val PIN_PX = 144
        private const val CLUSTER_BASE = 80
        const val CLUSTER_PX = 120
        private const val HEAD_R = 24f
        private const val BADGE_R = 12f
        private const val CLUSTER_R = 22f
        private const val SHADOW = 0x73000000
        private const val OUTLINE_ALPHA = 0xE6
        private const val BADGE_ALPHA = 0xF7

        /** Where the pin's tip sits inside its picture, as a fraction of the height. */
        const val TIP_FRACTION = 88f / 96f
    }
}
