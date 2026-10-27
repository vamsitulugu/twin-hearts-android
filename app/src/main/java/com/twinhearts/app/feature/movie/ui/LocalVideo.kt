package com.twinhearts.app.feature.movie.ui

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A video the person picked from their own phone (Direct Device Video mode). */
data class LocalVideo(val uri: String, val title: String, val durationSec: Int, val sizeBytes: Long = 0, val mime: String = "")

/**
 * Why this file exists: the room needs the movie's name and length so both phones can check they
 * picked the same file (the server allows 3 seconds of difference). Returns null when the file is not a video.
 */
suspend fun readLocalVideo(context: Context, uri: Uri): LocalVideo? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri)
    if (mime == null || !mime.startsWith("video/")) return@withContext null
    // Keep access after the app restarts, so a half-watched movie can resume.
    runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    var name: String? = null
    var size = 0L
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            name = c.getString(0)
            if (!c.isNull(1)) size = c.getLong(1)
        }
    }
    val retriever = MediaMetadataRetriever()
    val millis = try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } catch (e: RuntimeException) {
        null
    } finally {
        retriever.release()
    }
    if (millis == null || millis <= 0) return@withContext null
    LocalVideo(uri.toString(), (name ?: "Movie").substringBeforeLast('.'), (millis / 1000).toInt(), size, mime)
}
