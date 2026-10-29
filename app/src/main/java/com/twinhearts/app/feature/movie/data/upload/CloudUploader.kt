package com.twinhearts.app.feature.movie.data.upload

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.movie.data.remote.CompleteBody
import com.twinhearts.app.feature.movie.data.remote.MovieApi
import com.twinhearts.app.feature.movie.data.remote.PartEtagDto
import com.twinhearts.app.feature.movie.data.remote.PartsBody
import com.twinhearts.app.feature.movie.data.remote.StartUploadBody
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** What the person picked to upload. Built from the phone file by the screen. */
data class UploadRequest(val uri: String, val title: String, val sizeBytes: Long, val mime: String, val durationSec: Double)

enum class UploadProblem { BadType, Network, Server }

sealed interface UploadState {
    data object Idle : UploadState
    data class Running(val title: String, val sentBytes: Long, val totalBytes: Long, val bytesPerSec: Long, val finishing: Boolean) : UploadState
    data class Done(val title: String) : UploadState
    /** [message] is the server's own friendly text (for example "Not enough shared storage left"). */
    data class Failed(val problem: UploadProblem, val message: String? = null) : UploadState
}

/**
 * Why this file exists: it sends one big movie file to the shared cloud storage in 16 MB pieces
 * (the same steps as the web app). The pieces go straight to storage through short-lived links the
 * server hands out, so the movie never passes through the server and no secret is in the app (R-60).
 * It lives for the whole app run, and [UploadService] keeps the process alive, so leaving the movie screen or closing the app does not stop the upload.
 */
@Singleton
class CloudUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: MovieApi,
    private val sessionStore: SessionStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // A plain client on purpose: the signed links must not get extra headers, and a part can take minutes.
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state.asStateFlow()

    private var job: Job? = null

    val isRunning: Boolean get() = job?.isActive == true

    fun start(request: UploadRequest) {
        if (isRunning) return
        // Set first, so the notification service (started next) already sees a running upload.
        _state.value = UploadState.Running(request.title, 0, request.sizeBytes, 0, finishing = false)
        job = scope.launch { upload(request) }
        // A foreground service keeps the upload going when the app is closed or the screen is off.
        runCatching { ContextCompat.startForegroundService(context, Intent(context, UploadService::class.java)) }
    }

    /** Stops the upload; the work below then tells the server to throw away the unfinished pieces. */
    fun cancel() { job?.cancel() }

    /** Clears a finished or failed message so the pane goes back to the normal list. */
    fun dismiss() { if (!isRunning) _state.value = UploadState.Idle }

    private suspend fun upload(request: UploadRequest) {
        val who = sessionStore.session.first()
        if (who == null) {
            _state.value = UploadState.Failed(UploadProblem.Server)
            return
        }
        if (request.mime !in ALLOWED_TYPES) {
            _state.value = UploadState.Failed(UploadProblem.BadType)
            return
        }
        val uri = Uri.parse(request.uri)
        var movieId: String? = null
        val sent = AtomicLong(0)
        val startedAt = SystemClock.elapsedRealtime()
        _state.value = UploadState.Running(request.title, 0, request.sizeBytes, 0, finishing = false)
        try {
            val init = checked {
                api.startUpload(who.coupleId, who.userId, StartUploadBody(request.title, request.sizeBytes, request.mime, request.durationSec))
            }
            movieId = init.movieId
            val id = init.movieId
            val etags = arrayOfNulls<String>(init.totalParts)
            coroutineScope {
                val ticker = launch {
                    while (true) {
                        val bytes = sent.get()
                        val seconds = ((SystemClock.elapsedRealtime() - startedAt) / MS).coerceAtLeast(1.0)
                        _state.value = UploadState.Running(request.title, bytes, request.sizeBytes, (bytes / seconds).toLong(), finishing = false)
                        delay(TICK_MS)
                    }
                }
                (1..init.totalParts).chunked(PARALLEL_PARTS).forEach { batch ->
                    val links = checked { api.signParts(who.coupleId, id, who.userId, PartsBody(batch)).urls }
                    batch.map { n ->
                        async {
                            etags[n - 1] = sendPart(uri, n, init.partSize, request.sizeBytes, links[n.toString()], sent) {
                                // A link can expire or fail: ask the server for a fresh one for just this part.
                                checked { api.signParts(who.coupleId, id, who.userId, PartsBody(listOf(n))).urls[n.toString()] }
                            }
                        }
                    }.awaitAll()
                }
                ticker.cancel()
            }
            _state.value = UploadState.Running(request.title, request.sizeBytes, request.sizeBytes, 0, finishing = true)
            val parts = etags.mapIndexed { i, tag -> PartEtagDto(i + 1, tag.orEmpty()) }
            checked { api.completeUpload(who.coupleId, id, who.userId, CompleteBody(parts)) }
            _state.value = UploadState.Done(request.title)
        } catch (e: CancellationException) {
            abort(who.coupleId, who.userId, movieId)
            _state.value = UploadState.Idle
            throw e
        } catch (e: UploadException) {
            abort(who.coupleId, who.userId, movieId)
            _state.value = UploadState.Failed(e.problem, e.serverMessage)
        } catch (e: IOException) {
            abort(who.coupleId, who.userId, movieId)
            _state.value = UploadState.Failed(UploadProblem.Network)
        }
    }

    /** Sends one piece, trying again up to [ATTEMPTS] times with a growing pause. Returns its ETag (the receipt). */
    private suspend fun sendPart(
        uri: Uri, number: Int, partSize: Long, total: Long, firstLink: String?, sent: AtomicLong,
        freshLink: suspend () -> String?,
    ): String {
        val offset = (number - 1) * partSize
        val length = minOf(partSize, total - offset)
        var link = firstLink
        var attempt = 0
        while (true) {
            var partSent = 0L
            try {
                val url = link ?: throw IOException("No upload link")
                val body = SliceBody(context, uri, offset, length) { bytes ->
                    partSent += bytes
                    sent.addAndGet(bytes)
                }
                val response = http.newCall(Request.Builder().url(url).put(body).build()).await()
                response.use {
                    if (!it.isSuccessful) throw IOException("Storage answered ${it.code}")
                    return it.header("ETag") ?: throw IOException("Storage sent no receipt")
                }
            } catch (e: IOException) {
                sent.addAndGet(-partSent)
                attempt++
                if (attempt >= ATTEMPTS) throw e
                delay(minOf(MAX_BACKOFF_MS, BASE_BACKOFF_MS shl attempt))
                link = runCatching { freshLink() }.getOrNull() ?: link
            }
        }
    }

    /** Tells the server to drop the unfinished pieces, even when this job was cancelled. */
    private suspend fun abort(coupleId: String, userId: String, movieId: String?) {
        if (movieId == null) return
        withContext(NonCancellable) { safeApiCall { api.abortUpload(coupleId, movieId, userId) } }
    }

    private suspend fun <T> checked(block: suspend () -> T): T = when (val r = safeApiCall(block)) {
        is AppResult.Success -> r.data
        is AppResult.Failure -> throw when (val e = r.error) {
            is AppError.Server -> UploadException(UploadProblem.Server, e.message)
            is AppError.Unauthorized -> UploadException(UploadProblem.Server, e.message)
            else -> UploadException(UploadProblem.Network, null)
        }
    }

    private class UploadException(val problem: UploadProblem, val serverMessage: String?) : Exception()

    private companion object {
        val ALLOWED_TYPES = setOf("video/mp4", "video/webm", "video/x-m4v", "video/quicktime")
        const val PARALLEL_PARTS = 3
        const val ATTEMPTS = 5
        const val BASE_BACKOFF_MS = 500L
        const val MAX_BACKOFF_MS = 15_000L
        const val TICK_MS = 500L
        const val MS = 1000.0
    }
}

/** A window of the movie file: bytes [offset, offset + length). Reads it again from the start if OkHttp retries. */
private class SliceBody(
    private val context: Context,
    private val uri: Uri,
    private val offset: Long,
    private val length: Long,
    private val onBytes: (Long) -> Unit,
) : RequestBody() {
    override fun contentType(): MediaType? = null

    override fun contentLength(): Long = length

    override fun writeTo(sink: BufferedSink) {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: throw IOException("Cannot open the movie file")
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
            // Some cloud-backed files cannot jump; then we read and drop the bytes before the window.
            if (runCatching { input.channel.position(offset) }.isFailure) {
                var toSkip = offset
                while (toSkip > 0) {
                    val skipped = input.skip(toSkip)
                    if (skipped <= 0) throw IOException("Cannot read the movie file")
                    toSkip -= skipped
                }
            }
            val buffer = ByteArray(BUFFER_BYTES)
            var left = length
            while (left > 0) {
                val n = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                if (n < 0) throw IOException("The movie file ended early")
                sink.write(buffer, 0, n)
                left -= n
                onBytes(n.toLong())
            }
        }
    }

    private companion object {
        const val BUFFER_BYTES = 64 * 1024
    }
}

/** Runs an OkHttp call without blocking a thread, and cancels it when the coroutine is cancelled. */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            cont.resume(response)
        }
    })
}
