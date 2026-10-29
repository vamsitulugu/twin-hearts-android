package com.twinhearts.app.feature.vault.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import javax.inject.Inject

/** A picked photo or video: its name, size and type, as the phone reports them. */
class VaultFile(val uri: Uri, val name: String, val size: Long, val mime: String)

/**
 * Reads what the vault needs from the phone's files: a picked item's name and type, and a way to
 * stream it to the server without holding a whole video in memory.
 */
class VaultFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver: ContentResolver get() = context.contentResolver

    /** Name, size and type of [uri]; `null` when it cannot be opened. */
    suspend fun describe(uri: Uri, fallbackMime: String): VaultFile? = withContext(Dispatchers.IO) {
        runCatching {
            var name = ""
            var size = 0L
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val s = c.getColumnIndex(OpenableColumns.SIZE)
                    if (n >= 0) c.getString(n)?.let { name = it }
                    if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                }
            }
            val mime = resolver.getType(uri)?.takeIf { it.contains('/') } ?: fallbackMime
            // The server takes the file ending from the name, so make sure there is one.
            if (!name.contains('.')) {
                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: mime.substringAfter('/')
                name = (name.ifBlank { "vault-upload" }) + "." + ext
            }
            VaultFile(uri, name, size, mime)
        }.getOrNull()
    }

    /** Streams the file to the server; nothing is held in memory (videos can be large). */
    fun body(file: VaultFile): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType? = file.mime.toMediaTypeOrNull()
        override fun contentLength(): Long = if (file.size > 0L) file.size else -1L
        override fun writeTo(sink: BufferedSink) {
            resolver.openInputStream(file.uri)?.use { sink.writeAll(it.source()) }
        }
    }
}
