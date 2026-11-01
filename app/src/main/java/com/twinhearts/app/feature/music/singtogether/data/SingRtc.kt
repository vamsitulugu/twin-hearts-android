package com.twinhearts.app.feature.music.singtogether.data

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Why this file exists: one singing room = one [SingRtc]. It owns the WebRTC objects (connection,
 * microphone, camera) and turns the callback style into simple suspend functions.
 *
 * Unlike a phone call, the room must open even when the camera or microphone is not allowed
 * (the web app then joins "audio-lyrics only"). So [camera] and [mic] are switches, and a missing
 * one is replaced by a receive-only line so the partner's picture and voice still arrive.
 */
class SingRtc(
    context: Context,
    private val factory: PeerConnectionFactory,
    private val egl: EglBase,
    servers: List<PeerConnection.IceServer>,
    camera: Boolean,
    mic: Boolean,
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
    private var remoteAudio: AudioTrack? = null
    private var speakerOn = true
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
            when (val track = transceiver?.receiver?.track()) {
                is VideoTrack -> listener.onRemoteVideo(track)
                is AudioTrack -> {
                    remoteAudio = track
                    track.setEnabled(speakerOn)
                }
                else -> Unit
            }
        }
    }

    private val peer: PeerConnection

    val localVideo: VideoTrack? get() = cameraTrack
    val hasCamera: Boolean get() = cameraTrack != null
    val hasMic: Boolean get() = audioTrack != null
    val hasRemoteDescription: Boolean get() = peer.remoteDescription != null
    val waitingForAnswer: Boolean get() = peer.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER

    init {
        val config = PeerConnection.RTCConfiguration(servers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peer = requireNotNull(factory.createPeerConnection(config, observer)) { "Could not create the connection" }
        if (mic) {
            val source = factory.createAudioSource(MediaConstraints())
            val track = factory.createAudioTrack(AUDIO_ID, source)
            audioSource = source
            audioTrack = track
            peer.addTrack(track, listOf(STREAM_ID))
        } else {
            receiveOnly(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO)
        }
        if (!(camera && startCamera())) receiveOnly(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO)
    }

    private fun receiveOnly(type: MediaStreamTrack.MediaType) {
        peer.addTransceiver(
            type,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY),
        )
    }

    private fun startCamera(): Boolean {
        val enumerator = Camera2Enumerator(appContext)
        val name = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: return false
        val cap = enumerator.createCapturer(name, null) ?: return false
        val helper = SurfaceTextureHelper.create("SingCapture", egl.eglBaseContext)
        val source = factory.createVideoSource(cap.isScreencast)
        cap.initialize(helper, appContext, source.capturerObserver)
        cap.startCapture(CAPTURE_WIDTH, CAPTURE_HEIGHT, CAPTURE_FPS)
        val track = factory.createVideoTrack(VIDEO_ID, source)
        peer.addTrack(track, listOf(STREAM_ID))
        capturer = cap
        surfaceHelper = helper
        videoSource = source
        cameraTrack = track
        return true
    }

    /** [restartIce] asks both sides to find a new network path (after the link dropped). */
    suspend fun createOffer(restartIce: Boolean): SessionDescription = suspendCancellableCoroutine { cont ->
        val constraints = MediaConstraints().apply {
            if (restartIce) mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
        }
        peer.createOffer(
            object : SimpleSdp() {
                override fun onCreateSuccess(description: SessionDescription?) {
                    if (description != null) cont.resume(description)
                    else cont.resumeWithException(IllegalStateException("Empty offer"))
                }
                override fun onCreateFailure(error: String?) =
                    cont.resumeWithException(IllegalStateException(error ?: "Offer failed"))
            },
            constraints,
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

    fun restartIce() {
        peer.restartIce()
    }

    fun setMicEnabled(enabled: Boolean) {
        audioTrack?.setEnabled(enabled)
    }

    /** A switched-off camera sends black frames, so the partner sees that the picture is paused. */
    fun setCameraEnabled(enabled: Boolean) {
        cameraTrack?.setEnabled(enabled)
    }

    /** "Speaker off" silences the partner's voice (the song itself is not touched). */
    fun setSpeakerEnabled(enabled: Boolean) {
        speakerOn = enabled
        remoteAudio?.setEnabled(enabled)
    }

    fun switchCamera(onDone: (Boolean) -> Unit) {
        val cap = capturer
        if (cap == null) {
            onDone(false)
            return
        }
        cap.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontCamera: Boolean) = onDone(true)
            override fun onCameraSwitchError(errorDescription: String?) = onDone(false)
        })
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

    private abstract class SimpleSdp : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }

    private companion object {
        const val STREAM_ID = "twin-sing-stream"
        const val AUDIO_ID = "twin-sing-audio"
        const val VIDEO_ID = "twin-sing-video"
        const val CAPTURE_WIDTH = 960
        const val CAPTURE_HEIGHT = 540
        const val CAPTURE_FPS = 30
    }
}
