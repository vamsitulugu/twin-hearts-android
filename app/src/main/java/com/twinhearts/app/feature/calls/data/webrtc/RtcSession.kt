package com.twinhearts.app.feature.calls.data.webrtc

import android.content.Context
import com.twinhearts.app.feature.calls.domain.IceServerInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Why this file exists: one phone call = one [RtcSession]. It owns the WebRTC objects (the
 * connection, the microphone, the camera) and hides the callback style behind simple
 * suspend functions, so [com.twinhearts.app.feature.calls.data.CallEngine] reads like a story.
 *
 * The signalling format (offer / answer / ice JSON) is the same one the web app uses, so a call
 * between the web app and this app works in both directions.
 */
class RtcSession(
    context: Context,
    private val factory: PeerConnectionFactory,
    egl: EglBase,
    servers: List<IceServerInfo>,
    val video: Boolean,
    private val listener: Listener,
) {
    interface Listener {
        fun onLocalCandidate(candidate: IceCandidate)
        fun onIceState(state: PeerConnection.IceConnectionState)
        fun onRemoteVideo(track: VideoTrack)
    }

    private val appContext = context.applicationContext
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var cameraTrack: VideoTrack? = null
    private var capturer: CameraVideoCapturer? = null
    private var surfaceHelper: SurfaceTextureHelper? = null
    private var closed = false

    private val observer = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            if (state != null) listener.onIceState(state)
        }
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
        override fun onIceCandidate(candidate: IceCandidate?) {
            if (candidate != null) listener.onLocalCandidate(candidate)
        }
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
        override fun onAddStream(stream: MediaStream?) = Unit
        override fun onRemoveStream(stream: MediaStream?) = Unit
        override fun onDataChannel(channel: DataChannel?) = Unit
        override fun onRenegotiationNeeded() = Unit
        override fun onTrack(transceiver: RtpTransceiver?) {
            val track = transceiver?.receiver?.track()
            if (track is VideoTrack) listener.onRemoteVideo(track)
        }
    }

    private val peer: PeerConnection

    /** My own camera picture, for the small preview. `null` on voice calls. */
    val localVideo: VideoTrack? get() = cameraTrack

    val hasRemoteDescription: Boolean get() = peer.remoteDescription != null
    val waitingForAnswer: Boolean get() = peer.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER

    init {
        val iceServers = servers.map { s ->
            PeerConnection.IceServer.builder(s.urls).apply {
                s.username?.let { setUsername(it) }
                s.credential?.let { setPassword(it) }
            }.createIceServer()
        }
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peer = requireNotNull(factory.createPeerConnection(config, observer)) { "Could not create the connection" }
        startLocalMedia(egl)
    }

    private fun startLocalMedia(egl: EglBase) {
        val aSource = factory.createAudioSource(MediaConstraints())
        val aTrack = factory.createAudioTrack(AUDIO_ID, aSource)
        audioSource = aSource
        audioTrack = aTrack
        peer.addTrack(aTrack, listOf(STREAM_ID))
        if (!video) return

        val enumerator = Camera2Enumerator(appContext)
        val name = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: return
        val cap = enumerator.createCapturer(name, null) ?: return
        val helper = SurfaceTextureHelper.create("TwinCapture", egl.eglBaseContext)
        val vSource = factory.createVideoSource(cap.isScreencast)
        cap.initialize(helper, appContext, vSource.capturerObserver)
        cap.startCapture(CAPTURE_WIDTH, CAPTURE_HEIGHT, CAPTURE_FPS)
        val vTrack = factory.createVideoTrack(VIDEO_ID, vSource)
        peer.addTrack(vTrack, listOf(STREAM_ID))
        capturer = cap
        surfaceHelper = helper
        videoSource = vSource
        cameraTrack = vTrack
    }

    suspend fun createOffer(): SessionDescription = suspendCancellableCoroutine { cont ->
        peer.createOffer(
            object : SimpleSdp() {
                override fun onCreateSuccess(description: SessionDescription?) {
                    if (description != null) cont.resume(description)
                    else cont.resumeWithException(IllegalStateException("Empty offer"))
                }
                override fun onCreateFailure(error: String?) =
                    cont.resumeWithException(IllegalStateException(error ?: "Offer failed"))
            },
            MediaConstraints(),
        )
    }

    suspend fun createAnswer(): SessionDescription = suspendCancellableCoroutine { cont ->
        peer.createAnswer(
            object : SimpleSdp() {
                override fun onCreateSuccess(description: SessionDescription?) {
                    if (description != null) cont.resume(description)
                    else cont.resumeWithException(IllegalStateException("Empty answer"))
                }
                override fun onCreateFailure(error: String?) =
                    cont.resumeWithException(IllegalStateException(error ?: "Answer failed"))
            },
            MediaConstraints(),
        )
    }

    suspend fun setLocal(description: SessionDescription) = setDescription { peer.setLocalDescription(it, description) }

    suspend fun setRemote(description: SessionDescription) = setDescription { peer.setRemoteDescription(it, description) }

    private suspend fun setDescription(call: (SdpObserver) -> Unit) = suspendCancellableCoroutine<Unit> { cont ->
        call(
            object : SimpleSdp() {
                override fun onSetSuccess() = cont.resume(Unit)
                override fun onSetFailure(error: String?) =
                    cont.resumeWithException(IllegalStateException(error ?: "Could not apply the description"))
            },
        )
    }

    fun addRemoteCandidate(candidate: IceCandidate) {
        peer.addIceCandidate(candidate)
    }

    fun setMicEnabled(enabled: Boolean) {
        audioTrack?.setEnabled(enabled)
    }

    /** A switched-off camera sends black frames, so the partner sees that the picture is paused. */
    fun setCameraEnabled(enabled: Boolean) {
        cameraTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        capturer?.switchCamera(null)
    }

    /** Frees the camera, microphone and connection. Safe to call twice. */
    fun close() {
        if (closed) return
        closed = true
        runCatching { capturer?.stopCapture() }
        capturer?.dispose()
        surfaceHelper?.dispose()
        runCatching { peer.close() }
        cameraTrack?.dispose()
        videoSource?.dispose()
        audioTrack?.dispose()
        audioSource?.dispose()
        peer.dispose()
    }

    /** [SdpObserver] with empty bodies, so each call only overrides what it needs. */
    private abstract class SimpleSdp : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }

    private companion object {
        const val STREAM_ID = "twin-stream"
        const val AUDIO_ID = "twin-audio"
        const val VIDEO_ID = "twin-video"
        const val CAPTURE_WIDTH = 960
        const val CAPTURE_HEIGHT = 540
        const val CAPTURE_FPS = 30
    }
}
