package com.twinhearts.app.feature.chat.data

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.twinhearts.app.feature.chat.domain.CropRect
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.math.min

/** A file the person picked: its display name, size and type. */
class PickedFile(val uri: Uri, val name: String, val size: Long, val mime: String)

/**
 * Why this file exists: everything Chat needs from the phone's files and location that is not a
 * network call: reading a picked file's name and size, streaming it to the server without loading
 * it into memory, cutting the wallpaper, and asking for one GPS fix.
 */
class ChatFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver: ContentResolver get() = context.contentResolver

    /** Where the cut wallpaper of this couple is kept on the phone (survives cache clearing). */
    fun wallpaperFile(coupleId: String): File = File(context.filesDir, "chat_wallpaper_${coupleId.filter { it.isLetterOrDigit() }}.jpg")

    /** Name, size and type of [uri]; `null` when it cannot be opened. */
    suspend fun describe(uri: Uri): PickedFile? = withContext(Dispatchers.IO) {
        runCatching {
            var name = "file"
            var size = 0L
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val s = c.getColumnIndex(OpenableColumns.SIZE)
                    if (n >= 0) c.getString(n)?.let { name = it }
                    if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                }
            }
            PickedFile(uri, name, size, resolver.getType(uri) ?: "application/octet-stream")
        }.getOrNull()
    }

    /**
     * Gives a document the type the server accepts for documents. Android often reports a
     * generic or wrong type for Office files; the server rejects anything not on its list, so the
     * file ending decides instead.
     */
    fun asDocument(file: PickedFile): PickedFile {
        val mime = DOCUMENT_TYPES[file.name.substringAfterLast('.', "").lowercase()] ?: return file
        return PickedFile(file.uri, file.name, file.size, mime)
    }

    /** Streams the file to the server; nothing is held in memory (videos can be large). */
    fun body(file: PickedFile): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType? = file.mime.toMediaTypeOrNull()
        override fun contentLength(): Long = if (file.size > 0L) file.size else -1L
        override fun writeTo(sink: BufferedSink) {
            resolver.openInputStream(file.uri)?.use { sink.writeAll(it.source()) }
        }
    }

    /** Width and height of the picture as it is shown (phone-camera turning already applied), or `null`. */
    suspend fun imageSize(uri: Uri): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            if (exifDegrees(uri) % STRAIGHT == 0f) bounds.outWidth to bounds.outHeight else bounds.outHeight to bounds.outWidth
        }.getOrNull()
    }

    /**
     * Cuts [crop] (in the units of [imageSize]) out of the picture at [uri] and saves it as a JPEG.
     * The output is at most [MAX_WALLPAPER_SIDE] on its long side. `null` when anything fails.
     */
    suspend fun saveWallpaper(uri: Uri, crop: CropRect, target: File): File? = withContext(Dispatchers.IO) {
        runCatching {
            val (shownW, _) = imageSize(uri) ?: return@runCatching null
            var sample = 1
            while (min(crop.width, crop.height) / (sample * 2) >= MAX_WALLPAPER_SIDE) sample *= 2
            val raw = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val degrees = exifDegrees(uri)
            val decoded = if (degrees == 0f) {
                raw
            } else {
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(degrees) }, true)
            }
            val ratio = decoded.width.toFloat() / shownW
            val x = (crop.left * ratio).toInt().coerceIn(0, decoded.width - 1)
            val y = (crop.top * ratio).toInt().coerceIn(0, decoded.height - 1)
            val w = (crop.width * ratio).toInt().coerceIn(1, decoded.width - x)
            val h = (crop.height * ratio).toInt().coerceIn(1, decoded.height - y)
            val cut = Bitmap.createBitmap(decoded, x, y, w, h)
            val longest = maxOf(cut.width, cut.height)
            val out = if (longest > MAX_WALLPAPER_SIDE) {
                val f = MAX_WALLPAPER_SIDE.toFloat() / longest
                Bitmap.createScaledBitmap(cut, (cut.width * f).toInt().coerceAtLeast(1), (cut.height * f).toInt().coerceAtLeast(1), true)
            } else {
                cut
            }
            target.parentFile?.mkdirs()
            ByteArrayOutputStream().use { bytes ->
                out.compress(Bitmap.CompressFormat.JPEG, WALLPAPER_QUALITY, bytes)
                target.writeBytes(bytes.toByteArray())
            }
            target
        }.getOrNull()
    }

    private fun exifDegrees(uri: Uri): Float = runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
    }.getOrNull() ?: 0f

    /** One GPS fix (latitude, longitude), or `null`. The caller must already hold the location permission. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Pair<Double, Double>? = withContext(Dispatchers.Main) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return@withContext null
        val last = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < FRESH_FIX_MS) return@withContext last.latLng()
        val fresh = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            suspendCancellableCoroutine<Location?> { cont ->
                runCatching {
                    manager.getCurrentLocation(providers.first(), null, ContextCompat.getMainExecutor(context)) { cont.resume(it) }
                }.onFailure { cont.resume(null) }
            }
        } else {
            null
        }
        (fresh ?: last)?.latLng()
    }

    private fun Location.latLng() = latitude to longitude

    private companion object {
        val DOCUMENT_TYPES = mapOf(
            "pdf" to "application/pdf",
            "doc" to "application/msword",
            "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls" to "application/vnd.ms-excel",
            "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "ppt" to "application/vnd.ms-powerpoint",
            "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "txt" to "text/plain",
            "md" to "text/plain",
            "log" to "text/plain",
            "csv" to "text/csv",
            "json" to "application/json",
            "rtf" to "application/rtf",
            "zip" to "application/zip",
        )

        const val MAX_WALLPAPER_SIDE = 1600
        const val WALLPAPER_QUALITY = 85
        const val FRESH_FIX_MS = 120_000L
        const val STRAIGHT = 180f
    }
}
