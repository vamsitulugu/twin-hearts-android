package com.twinhearts.app.feature.profile.highlights.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.twinhearts.app.feature.profile.highlights.domain.PickedClip
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.source
import javax.inject.Inject

/**
 * Why this file exists: videos can be 100 MB, so a clip is streamed from the phone to the server
 * (never loaded into memory), and the upload bar gets real progress from how many bytes went out.
 */
class ClipFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver get() = context.contentResolver

    /** Reads the name, size and type of a picked or recorded file. Null when it cannot be opened. */
    suspend fun describe(uri: String): PickedClip? = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = Uri.parse(uri)
            var name = DEFAULT_NAME
            var size = 0L
            resolver.query(parsed, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameAt = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeAt = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameAt >= 0) cursor.getString(nameAt)?.let { name = it }
                    if (sizeAt >= 0 && !cursor.isNull(sizeAt)) size = cursor.getLong(sizeAt)
                }
            }
            PickedClip(uri, name, resolver.getType(parsed) ?: UNKNOWN_MIME, size)
        }.getOrNull()
    }

    /** Length of a video in whole seconds; 0 for photos or when it cannot be read. */
    suspend fun durationSeconds(clip: PickedClip): Int = withContext(Dispatchers.IO) {
        if (!clip.isVideo) return@withContext 0
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(clip.uri))
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            (ms / MS_PER_SECOND).toInt()
        } catch (e: RuntimeException) {
            0
        } finally {
            retriever.release()
        }
    }

    /** A request body that streams [clip] and reports 0..1 through [onProgress]. */
    fun body(clip: PickedClip, onProgress: (Float) -> Unit): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType? = clip.mime.toMediaTypeOrNull()
        override fun contentLength(): Long = if (clip.sizeBytes > 0L) clip.sizeBytes else -1L
        override fun writeTo(sink: BufferedSink) {
            resolver.openInputStream(Uri.parse(clip.uri))?.use { input ->
                val source = input.source()
                val buffer = Buffer()
                var sent = 0L
                while (true) {
                    val read = source.read(buffer, CHUNK_BYTES)
                    if (read == -1L) break
                    sink.write(buffer, read)
                    sent += read
                    if (clip.sizeBytes > 0L) onProgress((sent.toFloat() / clip.sizeBytes).coerceAtMost(1f))
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_NAME = "clip"
        const val UNKNOWN_MIME = "application/octet-stream"
        const val MS_PER_SECOND = 1000L
        const val CHUNK_BYTES = 64L * 1024
    }
}
