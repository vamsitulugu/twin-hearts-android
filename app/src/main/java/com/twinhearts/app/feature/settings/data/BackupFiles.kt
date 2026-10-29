package com.twinhearts.app.feature.settings.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Base64
import coil.imageLoader
import com.twinhearts.app.core.data.CacheDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

/**
 * Why this file exists: the Storage & Data page works with files the person picks in the system file
 * picker (Storage Access Framework). That needs no storage permission, on any Android version.
 */
class BackupFiles @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    /** Writes [text] into the file the person chose. `false` when the phone refused. */
    suspend fun write(target: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(target, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
        }.getOrDefault(false)
    }

    /** Reads a backup file. `null` when it cannot be opened or is bigger than [MAX_BACKUP_BYTES]. */
    suspend fun read(source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(source)?.use { readLimited(it) }?.toString(Charsets.UTF_8.name())
        }.getOrNull()
    }

    private fun readLimited(input: InputStream): ByteArrayOutputStream? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER)
        var total = 0
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > MAX_BACKUP_BYTES) return null
            out.write(buffer, 0, n)
        }
        return out
    }

    /** Saves each picture into the chosen folder as `TwinHearts-memory-N.jpg` (the web's file names). Returns how many worked. */
    suspend fun savePhotos(folder: Uri, urls: List<String>): Int = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
        var saved = 0
        urls.forEachIndexed { index, url ->
            val bytes = fetch(url) ?: return@forEachIndexed
            val document = runCatching {
                DocumentsContract.createDocument(resolver, parent, "image/jpeg", "TwinHearts-memory-${index + 1}.jpg")
            }.getOrNull() ?: return@forEachIndexed
            val ok = runCatching { resolver.openOutputStream(document)?.use { it.write(bytes) } != null }.getOrDefault(false)
            if (ok) saved++
        }
        saved
    }

    /** Old photos were saved inside the state as `data:` text; newer ones are web addresses. */
    private fun fetch(url: String): ByteArray? = runCatching {
        if (url.startsWith("data:")) {
            val comma = url.indexOf(',')
            if (comma < 0) null else Base64.decode(url.substring(comma + 1), Base64.DEFAULT)
        } else {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (response.isSuccessful) response.body?.bytes() else null
            }
        }
    }.getOrNull()

    private companion object {
        const val MAX_BACKUP_BYTES = 40 * 1024 * 1024
        const val BUFFER = 8 * 1024
    }
}

/** Clears what can be rebuilt: downloaded pictures (Coil) and the small cache table. Never user data. */
class CacheCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cacheDao: CacheDao,
) {
    suspend fun clear() {
        withContext(Dispatchers.IO) {
            val loader = context.imageLoader
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
            cacheDao.clear()
        }
    }
}
