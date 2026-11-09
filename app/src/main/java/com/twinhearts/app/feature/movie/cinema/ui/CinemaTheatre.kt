package com.twinhearts.app.feature.movie.cinema.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.twinhearts.app.core.cinema.BundledCinema
import com.twinhearts.app.core.cinema.BundledCinemaServer
import com.twinhearts.app.feature.movie.cinema.domain.CinemaLaunch
import java.io.FilterInputStream
import java.io.InputStream

/**
 * Why this file exists: the Twin Cinema 3D theatre is a Three.js web page, so on Android it runs in a full-screen WebView
 * on top of Watch Together. The page is bundled inside the app (assets/cinema, see core/cinema/BundledCinema.kt): no website is loaded. (README R-01 says "no WebView screens": this is the one approved exception, because a native
 * 3D theatre was tried and reverted, and it is the same theatre the web app uses so both phones can sit together.)
 *
 * Two private addresses on a virtual host are answered by the app itself, nothing else:
 *  - /twin-movie : streams the movie file this phone picked (with Range support), so the theatre never asks to choose it again;
 *  - /leave      : "Leave theatre" in the cinema goes here and closes this screen.
 *
 * While the theatre is open the phone is locked to LANDSCAPE and the window is truly full screen (status + navigation bars hidden,
 * drawn edge to edge, even into the camera cut-out) — no black bar at the top, and the cinema's bottom panel sits on the real edge.
 */
private const val VIRTUAL_HOST = BundledCinema.HOST
private const val MOVIE_PATH = "/twin-movie"
private const val LEAVE_PATH = "/leave"

private fun theatreUrl(launch: CinemaLaunch): String {
    val back = Uri.encode("https://$VIRTUAL_HOST$LEAVE_PATH")
    val movie = launch.local?.let { "&mv=" + Uri.encode("https://$VIRTUAL_HOST$MOVIE_PATH") + "&mn=" + Uri.encode(it.title) }.orEmpty()
    // Only the room settings (the # part) of the server's link are used; the theatre itself is opened from the APK.
    val base = BundledCinema.localUrl(launch.url) ?: BundledCinema.ENTRY
    return base + (if ('#' in base) "&" else "#") + "b=" + back + movie
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CinemaTheatre(launch: CinemaLaunch, onClose: () -> Unit) {
    val context = LocalContext.current
    LandscapeLock()
    val chooser = remember { FileChooser() }
    val server = remember { BundledCinemaServer(context.applicationContext.assets) }
    val chooserLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        chooser.finish(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
    }
    DisposableEffect(Unit) { onDispose { chooser.finish(null) } }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        FullScreenDialogWindow()
        // No systemBarsPadding(): that was the black strip at the top. Only the camera cut-out is kept clear so no button hides under it.
        Box(Modifier.fillMaxSize().background(Color.Black).displayCutoutPadding()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        setBackgroundColor(android.graphics.Color.BLACK)
                        keepScreenOn = true
                        overScrollMode = android.view.View.OVER_SCROLL_NEVER
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        // "Choose movie file" only has to work when this phone has no movie (the normal flow never needs it).
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                                chooser.start(callback)
                                try {
                                    chooserLauncher.launch(params.createIntent())
                                } catch (e: Exception) {
                                    chooser.finish(null)
                                }
                                return true
                            }
                        }
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
                                if (local != null && u.host == VIRTUAL_HOST && u.path == MOVIE_PATH) {
                                    return streamMovie(ctx, Uri.parse(local.uri), request.requestHeaders)
                                }
                                return server.handle(request) // theatre files come from the APK
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

/** Landscape while the theatre is open (the 3D theatre is made for it); the old orientation comes back when it closes. */
@Composable
private fun LandscapeLock() {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        val previous = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { activity?.requestedOrientation = previous }
    }
}

/** Makes the dialog's own window cover the whole screen: edge to edge, bars hidden (swipe from an edge shows them for a moment), cut-out used. */
@Composable
private fun FullScreenDialogWindow() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            window.attributes = lp
        }
        val controller = WindowInsetsControllerCompat(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        view.post { controller.hide(WindowInsetsCompat.Type.systemBars()) } // once the dialog window is attached, hide again (some phones ignore the first call)
    }
}

/** The one "choose a file" request the WebView is waiting on; it is always answered exactly once. */
private class FileChooser {
    private var callback: ValueCallback<Array<Uri>>? = null

    fun start(next: ValueCallback<Array<Uri>>) {
        callback?.onReceiveValue(null)
        callback = next
    }

    fun finish(files: Array<Uri>?) {
        callback?.onReceiveValue(files)
        callback = null
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Answers the movie request straight from the phone's file (content://). Handles "Range: bytes=a-b" so seeking works. */
private fun streamMovie(context: Context, uri: Uri, headers: Map<String, String>): WebResourceResponse? {
    val resolver = context.contentResolver
    val afd = runCatching { resolver.openAssetFileDescriptor(uri, "r") }.getOrNull() ?: return null
    var total = afd.length
    if (total <= 0) total = afd.declaredLength
    if (total <= 0) {
        total = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c -> if (c.moveToFirst()) c.getLong(0) else -1L } ?: -1L
        }.getOrDefault(-1L)
    }
    if (total <= 0) total = runCatching { afd.parcelFileDescriptor.statSize }.getOrDefault(-1L)
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
        "Access-Control-Expose-Headers" to "Content-Range, Content-Length, Accept-Ranges",   // the theatre reads Content-Range to upload this file once (auto-share)
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
