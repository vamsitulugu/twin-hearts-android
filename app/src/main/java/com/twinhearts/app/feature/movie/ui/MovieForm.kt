package com.twinhearts.app.feature.movie.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.twinhearts.app.feature.movie.domain.model.MovieOwner
import com.twinhearts.app.feature.movie.domain.model.WatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Everything the Add Movie / Edit Movie form collects. [poster] is new JPEG bytes, or null to keep the old poster. */
data class MovieForm(
    val title: String,
    val year: String,
    val genre: String?,
    val status: WatchStatus,
    val stars: Int,
    val owner: MovieOwner,
    val review: String,
    val scene: String,
    val poster: ByteArray?,
)

/**
 * Reads a gallery picture and shrinks it to a poster-sized JPEG (long side 900 px) so the upload is small.
 * Returns null when the file cannot be read as a picture.
 */
suspend fun readPosterJpeg(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= POSTER_MAX_SIDE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return@runCatching null
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, POSTER_QUALITY, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }.getOrNull()
}

private const val POSTER_MAX_SIDE = 900
private const val POSTER_QUALITY = 85
