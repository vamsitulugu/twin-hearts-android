package com.twinhearts.app.feature.surprises.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.surprises.data.remote.DeleteMediaBody
import com.twinhearts.app.feature.surprises.data.remote.SurprisesApi
import com.twinhearts.app.feature.surprises.domain.classifyMime
import com.twinhearts.app.feature.surprises.domain.model.MediaKind
import com.twinhearts.app.feature.surprises.domain.model.PickedMedia
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max

/** A file the server has stored: where to open it, and where to delete it later. */
data class UploadedMedia(val url: String, val path: String?, val kind: MediaKind)

/**
 * Why this file exists: sends the file attached to a surprise to the server. Photos are shrunk to
 * a 1600 px JPEG first (phone photos are 5-10 MB). Videos and audio are streamed from the phone,
 * never held in memory.
 */
class MediaUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: SurprisesApi,
) {
    private val resolver get() = context.contentResolver

    suspend fun upload(media: PickedMedia, session: Session): AppResult<UploadedMedia> {
        val kind = classifyMime(media.mime) ?: return AppResult.Failure(AppError.Unknown)
        val uri = Uri.parse(media.uri)
        val coupleId = session.coupleId.toRequestBody(TEXT)
        return when (kind) {
            MediaKind.Image -> {
                val jpeg = toJpeg(uri) ?: return AppResult.Failure(AppError.Unknown)
                val part = MultipartBody.Part.createFormData("file", "surprise.jpg", jpeg.toRequestBody(JPEG))
                safeApiCall { api.uploadMedia(part, coupleId) }.map { UploadedMedia(it.url, it.path, kind) }
            }
            MediaKind.Video -> {
                val part = MultipartBody.Part.createFormData("file", fileName(media, "surprise.mp4"), streamBody(uri, media))
                safeApiCall { api.uploadMedia(part, coupleId) }.map { UploadedMedia(it.url, it.path, kind) }
            }
            MediaKind.Audio -> {
                val part = MultipartBody.Part.createFormData("file", fileName(media, "surprise.m4a"), streamBody(uri, media))
                val role = session.role.toRequestBody(TEXT)
                safeApiCall { api.uploadVoice(part, coupleId, role) }.map { UploadedMedia(it.url, it.path, kind) }
            }
        }
    }

    /** Deleting is a tidy-up only: a failure leaves an unused file in storage, so the result is ignored. */
    suspend fun discard(path: String, kind: MediaKind, coupleId: String) {
        val body = DeleteMediaBody(path, coupleId)
        safeApiCall { if (kind == MediaKind.Audio) api.deleteVoice(body) else api.deleteMedia(body) }
    }

    private fun fileName(media: PickedMedia, fallback: String): String = media.name.ifBlank { fallback }

    private fun streamBody(uri: Uri, media: PickedMedia): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType? = media.mime.toMediaTypeOrNull()
        override fun contentLength(): Long = if (media.sizeBytes > 0L) media.sizeBytes else -1L
        override fun writeTo(sink: BufferedSink) {
            resolver.openInputStream(uri)?.use { sink.writeAll(it.source()) }
        }
    }

    /** Upright JPEG, longest side at most [MAX_SIDE] px. Null when the file cannot be read as a picture. */
    private suspend fun toJpeg(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
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
        const val MAX_SIDE = 1600
        const val QUALITY = 85
        val TEXT = "text/plain".toMediaType()
        val JPEG = "image/jpeg".toMediaType()
    }
}
