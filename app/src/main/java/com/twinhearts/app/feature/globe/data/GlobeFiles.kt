package com.twinhearts.app.feature.globe.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import javax.inject.Inject

/*
 * Why this file exists: turns a photo or video the person picked into an upload, streaming it from
 * the phone so a big video is never loaded into memory.
 */
class GlobeFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver: ContentResolver get() = context.contentResolver

    /** The multipart "file" part for [uriText], or null when the file cannot be opened. */
    suspend fun part(uriText: String, kind: MediaKind): MultipartBody.Part? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(uriText)
            var name = if (kind == MediaKind.Video) "video.mp4" else "photo.jpg"
            var size = -1L
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val s = c.getColumnIndex(OpenableColumns.SIZE)
                    if (n >= 0) c.getString(n)?.let { name = it }
                    if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                }
            }
            val mime = resolver.getType(uri) ?: if (kind == MediaKind.Video) "video/mp4" else "image/jpeg"
            val body = object : RequestBody() {
                override fun contentType(): MediaType? = mime.toMediaTypeOrNull()
                override fun contentLength(): Long = size
                override fun writeTo(sink: BufferedSink) {
                    resolver.openInputStream(uri)?.use { sink.writeAll(it.source()) }
                }
            }
            MultipartBody.Part.createFormData("file", name, body)
        }.getOrNull()
    }
}
