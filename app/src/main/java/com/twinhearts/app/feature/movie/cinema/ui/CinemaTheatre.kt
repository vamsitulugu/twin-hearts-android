package com.twinhearts.app.feature.movie.cinema.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.twinhearts.app.feature.movie.cinema.domain.CinemaLaunch
import java.io.FilterInputStream
import java.io.InputStream

/**
 * Why this file exists: the Twin Cinema 3D theatre is a Three.js web page, so on Android it runs in a full-screen WebView
 * on top of Watch Together. (README R-01 says "no WebView screens": this is the one approved exception, because a native
 * 3D theatre was tried and reverted, and it is the same theatre the web app uses so both phones can sit together.)
 *
 * Two private addresses on a virtual host are answered by the app itself, nothing else:
 *  - /twin-movie : streams the movie file this phone picked (with Range support), so the theatre never asks to choose it again;
 *  - /leave      : "Leave theatre" in the cinema goes here and closes this screen.
 */
private const val VIRTUAL_HOST = "appassets.androidplatform.net"
private const val MOVIE_PATH = "/twin-movie"
private const val LEAVE_PATH = "/leave"

private fun theatreUrl(launch: CinemaLaunch): String {
    val sep = if ('#' in launch.url) "&" else "#"
    val back = Uri.encode("https://$VIRTUAL_HOST$LEAVE_PATH")
    val movie = launch.local?.let { "&mv=" + Uri.encode("https://$VIRTUAL_HOST$MOVIE_PATH") + "&mn=" + Uri.encode(it.title) }.orEmpty()
    return launch.url + sep + "b=" + back + movie
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CinemaTheatre(launch: CinemaLaunch, onClose: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        keepScreenOn = true
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val u = request.url
                                if (u.host == VIRTUAL_HOST && u.path == LEAVE_PATH) {
                                    onClose()
                                    return true
                                }
                                return false
                            }

                            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                val u = request.url
                                val local = launch.local
                                if (local == null || u.host != VIRTUAL_HOST || u.path != MOVIE_PATH) return null
                                return streamMovie(ctx, Uri.parse(local.uri), request.requestHeaders)
                            }
                        }
                        loadUrl(theatreUrl(launch))
                    }
                },
                onRelease = { web ->
                    web.stopLoading()
                    web.destroy()
                },
            )
        }
    }
}

/** Answers the movie request straight from the phone's file (content://). Handles "Range: bytes=a-b" so seeking works. */
private fun streamMovie(context: Context, uri: Uri, headers: Map<String, String>): WebResourceResponse? {
    val resolver = context.contentResolver
    val afd = runCatching { resolver.openAssetFileDescriptor(uri, "r") }.getOrNull() ?: return null
    val total = afd.length
    if (total <= 0) {
        afd.close()
        return null
    }
    val mime = resolver.getType(uri)?.takeIf { it.startsWith("video/") } ?: "video/mp4"
    val range = (headers["Range"] ?: headers["range"])?.let { Regex("""bytes=(\d*)-(\d*)""").matchEntire(it.trim()) }
    var start = 0L
    var end = total - 1
    if (range != null) {
        val a = range.groupValues[1]
        val b = range.groupValues[2]
        when {
            a.isNotEmpty() -> {
                start = a.toLong()
                if (b.isNotEmpty()) end = minOf(b.toLong(), total - 1)
            }
            b.isNotEmpty() -> start = maxOf(0L, total - b.toLong())    // "bytes=-500": the last 500 bytes
        }
    }
    if (start > end || start >= total) {
        afd.close()
        return null
    }
    val length = end - start + 1
    val raw = afd.createInputStream()
    var left = start
    while (left > 0) {
        val skipped = raw.skip(left)
        if (skipped <= 0) break
        left -= skipped
    }
    val reply = mutableMapOf(
        "Accept-Ranges" to "bytes",
        "Content-Length" to length.toString(),
        "Access-Control-Allow-Origin" to "*",
        "Cache-Control" to "no-store",
    )
    if (range != null) reply["Content-Range"] = "bytes $start-$end/$total"
    return WebResourceResponse(
        mime, null, if (range != null) 206 else 200, if (range != null) "Partial Content" else "OK",
        reply, LimitedInputStream(raw, length),
    )
}

/** Gives out at most [limit] bytes, then ends (so a Range answer is exactly as long as its Content-Length says). */
private class LimitedInputStream(source: InputStream, private var limit: Long) : FilterInputStream(source) {
    override fun read(): Int {
        if (limit <= 0) return -1
        val b = super.read()
        if (b >= 0) limit--
        return b
    }

    override fun read(buffer: ByteArray, off: Int, len: Int): Int {
        if (limit <= 0) return -1
        val n = super.read(buffer, off, minOf(len.toLong(), limit).toInt())
        if (n > 0) limit -= n
        return n
    }
}
