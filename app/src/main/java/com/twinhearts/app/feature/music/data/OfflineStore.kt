package com.twinhearts.app.feature.music.data

import android.content.Context
import com.twinhearts.app.feature.music.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** What "Downloaded music" in Storage shows. */
data class OfflineSummary(val count: Int = 0, val bytes: Long = 0L)

/**
 * Why this file exists: "Download for offline" (web `MX.offline`). A copy of the song is saved in the
 * app's private storage (never visible to other apps, removed when the app is uninstalled) and the
 * player uses it instead of the internet link.
 */
@Singleton
class OfflineStore @Inject constructor(
    @ApplicationContext context: Context,
    private val client: OkHttpClient,
) {
    private val dir = File(context.filesDir, "music_offline")

    /** Songs can be large, so downloads skip the app-wide 90 second call limit. */
    private val slowClient: OkHttpClient by lazy {
        client.newBuilder()
            .callTimeout(0, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .build()
    }

    private val _ids = MutableStateFlow(scan())
    /** Ids of the songs that can be played without internet. */
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    private val _busy = MutableStateFlow<Set<String>>(emptySet())
    /** Ids that are downloading right now. */
    val busy: StateFlow<Set<String>> = _busy.asStateFlow()

    private val _summary = MutableStateFlow(summarize())
    val summary: StateFlow<OfflineSummary> = _summary.asStateFlow()

    /** The saved copy, or `null` when this song is not downloaded. */
    fun fileFor(songId: String): File? = File(dir, nameFor(songId)).takeIf { it.isFile && it.length() > 0L }

    /** Downloads [song]. Returns `false` on any failure (no internet, server error, disk full). */
    suspend fun download(song: Song): Boolean {
        if (song.audioUrl.isBlank() || song.id in _busy.value) return false
        _busy.update { it + song.id }
        return try {
            withContext(Dispatchers.IO) { fetch(song) }
        } finally {
            _busy.update { it - song.id }
            refresh()
        }
    }

    fun remove(songId: String) {
        File(dir, nameFor(songId)).delete()
        refresh()
    }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
        refresh()
    }

    private fun fetch(song: Song): Boolean {
        if (!dir.exists() && !dir.mkdirs()) return false
        val target = File(dir, nameFor(song.id))
        val part = File(dir, nameFor(song.id) + ".part")
        return try {
            slowClient.newCall(Request.Builder().url(song.audioUrl).build()).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) return false
                body.byteStream().use { input -> part.outputStream().use { input.copyTo(it) } }
            }
            part.renameTo(target)
        } catch (e: IOException) {
            part.delete()
            false
        } catch (e: IllegalArgumentException) {
            // a link that is not a valid web address
            part.delete()
            false
        }
    }

    private fun refresh() {
        _ids.value = scan()
        _summary.value = summarize()
    }

    private fun scan(): Set<String> =
        dir.listFiles { f -> f.isFile && !f.name.endsWith(".part") && f.length() > 0L }
            ?.map { it.name.removeSuffix(SUFFIX) }?.toSet().orEmpty()

    private fun summarize(): OfflineSummary {
        val files = dir.listFiles { f -> f.isFile && !f.name.endsWith(".part") && f.length() > 0L }.orEmpty()
        return OfflineSummary(files.size, files.sumOf { it.length() })
    }

    /** Song ids can hold any characters, so only safe ones are used in the file name. */
    private fun nameFor(songId: String): String = songId.filter { it.isLetterOrDigit() || it == '-' || it == '_' } + SUFFIX

    private companion object {
        const val SUFFIX = ".audio"
    }
}
