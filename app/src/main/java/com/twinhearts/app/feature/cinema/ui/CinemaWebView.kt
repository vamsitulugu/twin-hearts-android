package com.twinhearts.app.feature.cinema.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.twinhearts.app.R
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.cinema.BundledCinema
import com.twinhearts.app.core.cinema.BundledCinemaServer
import com.twinhearts.app.feature.cinema.domain.CinemaLinks

/**
 * Why this file exists: shows the Twin Cinema theatre full screen inside the app. The theatre is bundled in the APK
 * (assets/cinema) and answered by [BundledCinemaServer], so there is no website to open and no link that can go down.
 * It does four jobs the plain WebView does not do by itself:
 *  1. full screen + screen stays awake while the movie runs;
 *  2. only the bundled theatre may open here (everything else is blocked);
 *  3. the cinema's Leave button (a link to [CinemaLinks.BACK_LINK]) closes the screen via [onLeave];
 *  4. "Choose movie file" works (onShowFileChooser), otherwise the button does nothing in a WebView.
 */
@SuppressLint("SetJavaScriptEnabled") // the cinema is a JavaScript app; only its own website is allowed to load
@Composable
internal fun CinemaWebView(url: String, onLeave: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentOnLeave by rememberUpdatedState(onLeave)
    var loaded by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val chooser = remember { FileChooserHolder() }
    val server = remember { BundledCinemaServer(context.applicationContext.assets) }

    val chooserLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // parseResult gives null when the person cancelled; the callback must ALWAYS be answered.
        chooser.finish(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
    }

    val webView = remember(url) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            keepScreenOn = true
            settings.apply {
                javaScriptEnabled = true
                textZoom = 100 // ignore the phone's font size setting
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false // sound and picture start without a tap
                allowFileAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val target = request.url.toString()
                    if (CinemaLinks.isBackLink(target)) {
                        currentOnLeave()
                        return true
                    }
                    return !BundledCinema.isOwnUrl(target) // true = block
                }

                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    server.handle(request) // theatre files come from the APK, never from the network

                override fun onPageStarted(view: WebView, pageUrl: String?, favicon: Bitmap?) {
                    failed = false
                }

                override fun onPageFinished(view: WebView, pageUrl: String?) {
                    loaded = true
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (!request.isForMainFrame) return
                    // Safety net: if the browser tries to open the (never real) back link, that is still "Leave".
                    if (CinemaLinks.isBackLink(request.url.toString())) currentOnLeave() else failed = true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    view: WebView,
                    callback: ValueCallback<Array<Uri>>,
                    params: FileChooserParams,
                ): Boolean {
                    chooser.start(callback)
                    try {
                        chooserLauncher.launch(params.createIntent())
                    } catch (e: ActivityNotFoundException) {
                        chooser.finish(null) // no file picker on this phone: answer "nothing chosen"
                    }
                    return true
                }
            }
            loadUrl(url)
        }
    }

    ImmersiveMode()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            chooser.finish(null)
            webView.stopLoading()
            webView.destroy()
        }
    }

    Box(modifier.fillMaxSize()) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
        if (failed) {
            ErrorState(
                message = stringResource(R.string.core_error_network),
                onRetry = { failed = false; webView.reload() },
                modifier = Modifier.fillMaxSize().background(TwinTheme.colors.background),
            )
        } else if (!loaded) {
            LoadingSkeleton(Modifier.fillMaxSize().background(TwinTheme.colors.background).padding(TwinTheme.spacing.xl))
        }
    }
}

/** Remembers the one "choose a file" request the WebView is waiting on, so it is always answered exactly once. */
private class FileChooserHolder {
    private var callback: ValueCallback<Array<Uri>>? = null

    fun start(next: ValueCallback<Array<Uri>>) {
        callback?.onReceiveValue(null) // an older request that never finished
        callback = next
    }

    fun finish(files: Array<Uri>?) {
        callback?.onReceiveValue(files)
        callback = null
    }
}

/** Hides the status and navigation bars while the cinema is open (swipe from the edge shows them briefly). */
@Composable
private fun ImmersiveMode() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
