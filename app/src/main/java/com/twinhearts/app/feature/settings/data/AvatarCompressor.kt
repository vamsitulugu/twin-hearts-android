package com.twinhearts.app.feature.settings.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max

/** Why this file exists: a profile photo only needs ~1000 px; this makes it small and upright before upload. */
class AvatarCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** `null` when the file cannot be opened as a picture. */
    suspend fun toJpeg(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null

            val rotation = resolver.openInputStream(uri)?.use { exifDegrees(ExifInterface(it)) } ?: 0f
            val upright = if (rotation == 0f) {
                decoded
            } else {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
            }
            val longest = max(upright.width, upright.height)
            val sized = if (longest > MAX_SIDE) {
                val ratio = MAX_SIDE.toFloat() / longest
                Bitmap.createScaledBitmap(upright, (upright.width * ratio).toInt(), (upright.height * ratio).toInt(), true)
            } else {
                upright
            }
            ByteArrayOutputStream().use { out ->
                sized.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
                out.toByteArray()
            }
        }.getOrNull()
    }

    private fun exifDegrees(exif: ExifInterface): Float = when (
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    private companion object {
        const val MAX_SIDE = 1024
        const val QUALITY = 88
    }
}
