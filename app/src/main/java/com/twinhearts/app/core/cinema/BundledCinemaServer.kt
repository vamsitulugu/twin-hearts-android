package com.twinhearts.app.core.cinema

import android.content.res.AssetManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.FilterInputStream
import java.io.InputStream

/**
 * Why this file exists: a WebView cannot load `file:///android_asset/` pages properly (ES modules, fetch() of models and
 * audio and CORS all break). This answers every request for the app's own virtual host ([BundledCinema.HOST]) straight
 * from the APK's assets, with the right content type and "Range" support (the ad video and sounds seek with it).
 * Use it from `WebViewClient.shouldInterceptRequest`; it returns null for every other address so the WebView carries on.
 *
 * Big media (glb, mp3, mp4...) are stored uncompressed (see `androidResources.noCompress` in app/build.gradle.kts) so the
 * exact size is known and any byte range can be read directly. Small compressed files (js, css, html) are read whole.
 */
class BundledCinemaServer(private val assets: AssetManager) {

    fun handle(request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url
        if (!uri.host.equals(BundledCinema.HOST, ignoreCase = true)) return null
        val path = BundledCinema.assetPath(uri.path) ?: return null // other private paths (e.g. /leave) belong to the screen itself
        val mime = BundledCinema.mimeFor(path)
        val encoding = if (BundledCinema.isText(mime)) "utf-8" else null
        val baseHeaders = mapOf("Access-Control-Allow-Origin" to "*", "Accept-Ranges" to "bytes")

        // Fast path: uncompressed file, exact length, any range.
        val afd = try {
            assets.openFd(path)
        } catch (e: FileNotFoundException) {
            null // missing OR stored compressed: the fallback below tells them apart
        }
        if (afd != null) {
            val total = afd.length
            val header = request.requestHeaders.entries.firstOrNull { it.key.equals("Range", ignoreCase = true) }?.value
            val range = BundledCinema.parseRange(header, total)
            if (range == null) {
                return WebResourceResponse(mime, encoding, 200, "OK", baseHeaders + ("Content-Length" to total.toString()), afd.createInputStream())
            }
            if (range.first > range.last || range.first >= total) {
                afd.close()
                return WebResourceResponse(mime, encoding, 416, "Range Not Satisfiable", baseHeaders + ("Content-Range" to "bytes */$total"), ByteArrayInputStream(ByteArray(0)))
            }
            val stream = afd.createInputStream()
            skipFully(stream, range.first)
            val length = range.last - range.first + 1
            val headers = baseHeaders + mapOf(
                "Content-Range" to "bytes ${range.first}-${range.last}/$total",
                "Content-Length" to length.toString(),
            )
            return WebResourceResponse(mime, encoding, 206, "Partial Content", headers, LimitedInputStream(stream, length))
        }

        // Compressed small file: read it whole.
        val bytes = try {
            assets.open(path).use { it.readBytes() }
        } catch (e: FileNotFoundException) {
            return WebResourceResponse("text/plain", "utf-8", 404, "Not Found", baseHeaders, ByteArrayInputStream("Not found".toByteArray()))
        }
        return WebResourceResponse(mime, encoding, 200, "OK", baseHeaders + ("Content-Length" to bytes.size.toString()), ByteArrayInputStream(bytes))
    }

    private fun skipFully(stream: InputStream, count: Long) {
        var left = count
        while (left > 0) {
            val skipped = stream.skip(left)
            if (skipped <= 0) break
            left -= skipped
        }
    }

    /** Stops reading after [limit] bytes (the end of the requested range). */
    private class LimitedInputStream(source: InputStream, private var left: Long) : FilterInputStream(source) {
        override fun read(): Int {
            if (left <= 0) return -1
            val b = super.read()
            if (b >= 0) left--
            return b
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (left <= 0) return -1
            val n = super.read(buffer, offset, minOf(length.toLong(), left).toInt())
            if (n > 0) left -= n
            return n
        }

        override fun available(): Int = minOf(super.available().toLong(), left).toInt()
    }
}
