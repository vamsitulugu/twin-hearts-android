package com.twinhearts.app.feature.memories.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import javax.inject.Inject

/** A file the person picked or the camera made: name, size and type. */
class PickedMedia(val uri: Uri, val name: String, val size: Long, val mime: String)

/** Reads a picked file's details and streams it to the server without loading it into memory. */
class MemoryFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver: ContentResolver get() = context.contentResolver

    suspend fun describe(uri: Uri): PickedMedia? = withContext(Dispatchers.IO) {
        runCatching {
            var name = "memory"
            var size = 0L
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val s = c.getColumnIndex(OpenableColumns.SIZE)
                    if (n >= 0) c.getString(n)?.let { name = it }
                    if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                }
            }
            PickedMedia(uri, name, size, resolver.getType(uri) ?: "application/octet-stream")
        }.getOrNull()
    }

    /** Streams the file; videos can be large. */
    fun body(file: PickedMedia, mimeOverride: String? = null): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType? = (mimeOverride ?: file.mime).toMediaTypeOrNull()
        override fun contentLength(): Long = if (file.size > 0L) file.size else -1L
        override fun writeTo(sink: BufferedSink) {
            resolver.openInputStream(file.uri)?.use { sink.writeAll(it.source()) }
        }
    }
}
