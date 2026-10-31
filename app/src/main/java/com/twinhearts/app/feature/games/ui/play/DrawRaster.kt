package com.twinhearts.app.feature.games.ui.play

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.twinhearts.app.feature.games.domain.engine.DrawStroke
import com.twinhearts.app.feature.games.domain.engine.DrawTool
import java.util.ArrayDeque

/**
 * Paints saved strokes onto a transparent bitmap. Strokes are stored as 0..1 coordinates, so the
 * same drawing looks the same on both phones. Widths are relative to a 360 px wide board.
 */
internal object DrawRaster {

    private const val BASE_WIDTH = 360f
    private const val HIGHLIGHT_WIDTH = 2.6f
    private const val HIGHLIGHT_ALPHA = 90
    private const val FILL_TOLERANCE = 48
    private const val SAME_COLOR_TOLERANCE = 12

    fun render(strokes: List<DrawStroke>, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = width / BASE_WIDTH
        strokes.forEach { stroke ->
            if (stroke.tool == DrawTool.Fill) {
                if (stroke.pts.size >= 2) {
                    floodFill(bitmap, (stroke.pts[0] * width).toInt(), (stroke.pts[1] * height).toInt(), stroke.color)
                }
            } else {
                canvas.drawPath(pathOf(stroke.pts, width, height), paintOf(stroke, scale))
            }
        }
        return bitmap
    }

    fun widthOf(size: Int, tool: DrawTool, scale: Float): Float =
        size * scale * if (tool == DrawTool.Highlighter) HIGHLIGHT_WIDTH else 1f

    private fun pathOf(pts: List<Float>, width: Int, height: Int): Path {
        val path = Path()
        var i = 0
        while (i + 1 < pts.size) {
            val x = pts[i] * width
            val y = pts[i + 1] * height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            i += 2
        }
        return path
    }

    private fun paintOf(stroke: DrawStroke, scale: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = widthOf(stroke.size, stroke.tool, scale)
        when (stroke.tool) {
            DrawTool.Eraser -> xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            DrawTool.Highlighter -> {
                color = stroke.color
                alpha = HIGHLIGHT_ALPHA
            }
            else -> color = stroke.color
        }
    }

    /** Bucket fill: paints the area of similar colour around the tapped point. */
    private fun floodFill(bitmap: Bitmap, startX: Int, startY: Int, color: Int) {
        val w = bitmap.width
        val h = bitmap.height
        if (startX !in 0 until w || startY !in 0 until h) return
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val target = pixels[startY * w + startX]
        if (close(target, color, SAME_COLOR_TOLERANCE) && (target ushr 24) > 240) return
        val visited = BooleanArray(w * h)
        val queue = ArrayDeque<Int>()
        queue.add(startY * w + startX)
        while (queue.isNotEmpty()) {
            val index = queue.poll() ?: break
            if (visited[index]) continue
            if (!close(pixels[index], target, FILL_TOLERANCE)) continue
            visited[index] = true
            pixels[index] = color
            val x = index % w
            val y = index / w
            if (x + 1 < w) queue.add(index + 1)
            if (x > 0) queue.add(index - 1)
            if (y + 1 < h) queue.add(index + w)
            if (y > 0) queue.add(index - w)
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun close(a: Int, b: Int, tolerance: Int): Boolean {
        val da = (a ushr 24) - (b ushr 24)
        val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
        val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
        val db = (a and 0xFF) - (b and 0xFF)
        return da * da + dr * dr + dg * dg + db * db <= tolerance * tolerance
    }
}
