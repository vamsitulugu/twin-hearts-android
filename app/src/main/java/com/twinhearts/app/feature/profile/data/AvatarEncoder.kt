package com.twinhearts.app.feature.profile.data

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

/** Turns a picked photo into a small upright JPEG. An interface so ViewModels can be tested without Android. */
interface AvatarEncoder {
    /** [uri] is the text form of the picked photo's address. Null when it cannot be read as a picture. */
    suspend fun toJpeg(uri: String): ByteArray?
}

/**
 * Why this file exists: phone photos are 5-10 MB, but a profile picture only needs about 800 px.
 * The server also refuses files over 10 MB, so we shrink first and the upload is fast on mobile data.
 */
class AndroidAvatarEncoder @Inject constructor(
    @ApplicationContext private val context: Context,
) : AvatarEncoder {

    override suspend fun toJpeg(uri: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val source = Uri.parse(uri)
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val decoded = resolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null

            val rotation = resolver.openInputStream(source)?.use { exifDegrees(ExifInterface(it)) } ?: 0f
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
        const val MAX_SIDE = 800
        const val QUALITY = 88
    }
}
