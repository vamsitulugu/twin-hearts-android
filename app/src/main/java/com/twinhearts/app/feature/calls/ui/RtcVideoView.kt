package com.twinhearts.app.feature.calls.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/** Why this file exists: draws one WebRTC video track. The sink is removed before the track can be freed. */
@Composable
fun RtcVideoView(track: VideoTrack?, egl: EglBase.Context?, mirror: Boolean, modifier: Modifier = Modifier, overlay: Boolean = false) {
    if (egl == null) return
    var renderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(egl, null)
                setEnableHardwareScaler(true)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setMirror(mirror)
                setZOrderMediaOverlay(overlay)
                renderer = this
            }
        },
        modifier = modifier,
        onRelease = { it.release() },
    )
    DisposableEffect(track, renderer) {
        val view = renderer
        if (track != null && view != null) track.addSink(view)
        onDispose { if (track != null && view != null) runCatching { track.removeSink(view) } }
    }
}
