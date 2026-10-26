package com.twinhearts.app.feature.home.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.twinhearts.app.feature.home.domain.CARD_PHOTO_HEIGHT
import com.twinhearts.app.feature.home.domain.CARD_PHOTO_WIDTH
import com.twinhearts.app.feature.home.domain.CropRect
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/** A photo opened for editing. [width] and [height] are in pixels after turning it upright. */
class LoadedPhoto(val width: Int, val height: Int, internal val payload: Any)

sealed interface PhotoLoadResult {
    data class Loaded(val photo: LoadedPhoto) : PhotoLoadResult
    data object NotAnImage : PhotoLoadResult
    data object TooLarge : PhotoLoadResult
    data object Unreadable : PhotoLoadResult
}

/**
 * Opens a picked picture and cuts the chosen 16:9 part out of it.
 * It is an interface so the editor's ViewModel can be tested without a real phone.
 */
interface CardPhotoProcessor {
    suspend fun load(uri: String): PhotoLoadResult

    /** The part [rect] of [photo] as a 1280x720 JPEG, or null when it could not be made. */
    suspend fun render(photo: LoadedPhoto, rect: CropRect): ByteArray?
}

private const val MAX_FILE_BYTES = 30L * 1024 * 1024 // same limit as the web editor
private const val MAX_SIDE_PX = 3072 // big enough for 4x zoom, small enough for low-memory phones
private const val JPEG_QUALITY = 88

class AndroidCardPhotoProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
) : CardPhotoProcessor {

    override suspend fun load(uri: String): PhotoLoadResult = withContext(Dispatchers.IO) {
        runCatching { loadBlocking(Uri.parse(uri)) }.getOrDefault(PhotoLoadResult.Unreadable)
    }

    private fun loadBlocking(uri: Uri): PhotoLoadResult {
        val resolver = context.contentResolver
        val type = resolver.getType(uri)
        if (type != null && !type.startsWith("image/")) return PhotoLoadResult.NotAnImage
        val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        if (size > MAX_FILE_BYTES) return PhotoLoadResult.TooLarge

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return PhotoLoadResult.Unreadable

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return PhotoLoadResult.Unreadable

        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val upright = decoded.turnUpright(orientation)
        return PhotoLoadResult.Loaded(LoadedPhoto(upright.width, upright.height, upright))
    }

    override suspend fun render(photo: LoadedPhoto, rect: CropRect): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val source = photo.payload as Bitmap
            val left = rect.left.toInt().coerceIn(0, source.width - 1)
            val top = rect.top.toInt().coerceIn(0, source.height - 1)
            val width = rect.width.toInt().coerceIn(1, source.width - left)
            val height = rect.height.toInt().coerceIn(1, source.height - top)
            val piece = Bitmap.createBitmap(source, left, top, width, height)
            val output = Bitmap.createScaledBitmap(piece, CARD_PHOTO_WIDTH, CARD_PHOTO_HEIGHT, true)
            ByteArrayOutputStream().use { stream ->
                output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
                stream.toByteArray()
            }
        }.getOrNull()
    }
}

/** Phones often store photos sideways plus a rotation tag; browsers apply it, so we do too. */
private fun Bitmap.turnUpright(orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
