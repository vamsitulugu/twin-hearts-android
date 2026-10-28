package com.twinhearts.app.feature.music.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.twinhearts.app.feature.music.domain.model.SongFileInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: reads a song file the person picked (its name, size and embedded tags such
 * as title, artist and artwork) and turns it into an upload body, without loading the whole file
 * into memory. The web app does the same in `metadata-service.js`.
 */
@Singleton
class AudioFileReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Display name of the picked file, e.g. "Perfect.mp3". */
    fun displayName(uri: Uri): String = query(uri, OpenableColumns.DISPLAY_NAME) ?: "song"

    fun sizeBytes(uri: Uri): Long = query(uri, OpenableColumns.SIZE)?.toLongOrNull() ?: -1L

    /** Tags from the file. Missing tags fall back to the file name, never to blank. */
    suspend fun read(uri: Uri): SongFileInfo = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            fun tag(key: Int) = retriever.extractMetadata(key)?.trim().orEmpty()
            val fileTitle = displayName(uri).substringBeforeLast('.')
            SongFileInfo(
                title = tag(MediaMetadataRetriever.METADATA_KEY_TITLE).ifEmpty { fileTitle },
                artist = tag(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    .ifEmpty { tag(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST) },
                album = tag(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                genre = tag(MediaMetadataRetriever.METADATA_KEY_GENRE),
                year = tag(MediaMetadataRetriever.METADATA_KEY_YEAR),
                durationSec = ((tag(MediaMetadataRetriever.METADATA_KEY_DURATION).toLongOrNull() ?: 0L) / 1000L).toInt(),
                cover = retriever.embeddedPicture,
            )
        } catch (e: RuntimeException) {
            // Some files have no readable tags at all. The file name is still a fine title.
            SongFileInfo(displayName(uri).substringBeforeLast('.'), "", "", "", "", 0, null)
        } finally {
            retriever.release()
        }
    }

    /** The file name the server needs: it checks the extension, so make sure it has a valid one. */
    fun uploadName(uri: Uri): String {
        val name = displayName(uri)
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext in AUDIO_EXTENSIONS) return name
        val fromMime = when (context.contentResolver.getType(uri)) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/mp4", "audio/x-m4a", "audio/m4a" -> "m4a"
            "audio/aac" -> "aac"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/ogg", "application/ogg" -> "ogg"
            else -> "mp3"
        }
        return "${name.substringBeforeLast('.')}.$fromMime"
    }

    /** Streams the file to the network; nothing is copied to memory or to a temp file. */
    fun bodyFor(uri: Uri, mime: String): RequestBody {
        val type: MediaType = mime.toMediaType()
        val length = sizeBytes(uri)
        return object : RequestBody() {
            override fun contentType(): MediaType = type
            override fun contentLength(): Long = length
            override fun writeTo(sink: BufferedSink) {
                context.contentResolver.openInputStream(uri)?.use { sink.writeAll(it.source()) }
            }
        }
    }

    private fun query(uri: Uri, column: String): String? = try {
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: RuntimeException) {
        null
    }

    companion object {
        /** Same list as `AUDIO_EXT_TYPES` in routes/media.js. */
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus", "webm", "mp4")

        /** The server's limit for one song (routes/media.js, 60 MB). */
        const val MAX_SONG_BYTES = 60L * 1024L * 1024L
    }
}
