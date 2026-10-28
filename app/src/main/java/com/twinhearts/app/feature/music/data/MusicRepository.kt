package com.twinhearts.app.feature.music.data

import android.net.Uri
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.music.data.mapper.toDomain
import com.twinhearts.app.feature.music.data.mapper.toDomainOrNull
import com.twinhearts.app.feature.music.data.remote.AddSongBody
import com.twinhearts.app.feature.music.data.remote.CoupleBody
import com.twinhearts.app.feature.music.data.remote.CreatePlaylistBody
import com.twinhearts.app.feature.music.data.remote.CreateRecordingBody
import com.twinhearts.app.feature.music.data.remote.EditSongBody
import com.twinhearts.app.feature.music.data.remote.LikeBody
import com.twinhearts.app.feature.music.data.remote.MusicApi
import com.twinhearts.app.feature.music.data.remote.MusicUploadApi
import com.twinhearts.app.feature.music.data.remote.PlaylistSongsBody
import com.twinhearts.app.feature.music.data.remote.RecordingDeleteBody
import com.twinhearts.app.feature.music.data.remote.RecordingPatchBody
import com.twinhearts.app.feature.music.data.remote.RenamePlaylistBody
import com.twinhearts.app.feature.music.data.remote.ViewerBody
import com.twinhearts.app.feature.music.domain.LrcParser
import com.twinhearts.app.feature.music.domain.model.Lyrics
import com.twinhearts.app.feature.music.domain.model.MusicIdentity
import com.twinhearts.app.feature.music.domain.model.MusicLibrary
import com.twinhearts.app.feature.music.domain.model.OnlineTrack
import com.twinhearts.app.feature.music.domain.model.Playlist
import com.twinhearts.app.feature.music.domain.model.PlaylistsResult
import com.twinhearts.app.feature.music.domain.model.Recording
import com.twinhearts.app.feature.music.domain.model.RecordingKind
import com.twinhearts.app.feature.music.domain.model.RecordingMedia
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.domain.model.SongEdit
import com.twinhearts.app.feature.music.domain.model.SongFileInfo
import com.twinhearts.app.feature.music.domain.model.SongVisibility
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

/**
 * Why this file exists: the only place Music talks to the old server. Every call returns an
 * [AppResult] so a ViewModel never sees an exception (R-46).
 */
interface MusicRepository {
    suspend fun identity(): MusicIdentity?

    /** Songs and playlists. A playlists problem never hides the songs. */
    suspend fun library(who: MusicIdentity): AppResult<MusicLibrary>

    // ── songs ──
    suspend fun setLiked(who: MusicIdentity, songId: String, liked: Boolean): AppResult<Unit>
    suspend fun deleteSong(who: MusicIdentity, songId: String): AppResult<Unit>
    suspend fun editSong(who: MusicIdentity, songId: String, edit: SongEdit): AppResult<Unit>

    /** Counts one listen (fire and forget: a failure is not shown). */
    suspend fun recordPlay(songId: String)

    suspend fun lyrics(who: MusicIdentity, song: Song): AppResult<Lyrics>
    suspend fun saveLyrics(who: MusicIdentity, songId: String, text: String): AppResult<Unit>

    suspend fun readSongFile(uri: Uri): SongFileInfo
    fun fileSizeBytes(uri: Uri): Long

    /** Uploads the file, its artwork, then saves the song. Returns null for an "already adding" answer. */
    suspend fun addSongFromDevice(who: MusicIdentity, uri: Uri, info: SongFileInfo, visibility: SongVisibility): AppResult<Song?>

    suspend fun searchOnline(query: String): AppResult<List<OnlineTrack>>
    suspend fun addOnlineTrack(who: MusicIdentity, track: OnlineTrack, visibility: SongVisibility): AppResult<Song?>

    // ── playlists ──
    suspend fun createPlaylist(who: MusicIdentity, name: String, songIds: List<String>): AppResult<Playlist>
    suspend fun renamePlaylist(who: MusicIdentity, id: String, name: String): AppResult<Unit>
    suspend fun deletePlaylist(who: MusicIdentity, id: String): AppResult<Unit>
    suspend fun addToPlaylist(who: MusicIdentity, id: String, songIds: List<String>): AppResult<Unit>
    suspend fun removeFromPlaylist(who: MusicIdentity, id: String, songId: String): AppResult<Unit>

    // ── recordings ──
    suspend fun recordings(who: MusicIdentity): AppResult<List<Recording>>
    suspend fun saveRecording(
        who: MusicIdentity,
        file: File,
        media: RecordingMedia,
        title: String,
        durationSec: Int,
        kind: RecordingKind,
        songId: String?,
    ): AppResult<Recording>
    suspend fun setRecordingFavorite(who: MusicIdentity, id: String, favorite: Boolean): AppResult<Unit>
    suspend fun renameRecording(who: MusicIdentity, id: String, title: String): AppResult<Unit>
    suspend fun deleteRecording(who: MusicIdentity, id: String): AppResult<Unit>
}

class MusicRepositoryImpl @Inject constructor(
    private val api: MusicApi,
    private val uploadApi: MusicUploadApi,
    private val files: AudioFileReader,
    private val sessionStore: SessionStore,
) : MusicRepository {

    override suspend fun identity(): MusicIdentity? =
        sessionStore.session.first()?.takeIf { it.coupleId.isNotBlank() }?.let {
            MusicIdentity(it.coupleId, it.role, it.myName, it.partnerName)
        }

    override suspend fun library(who: MusicIdentity): AppResult<MusicLibrary> {
        val songs = when (val r = safeApiCall { api.songs(who.coupleId, who.myRole) }) {
            is AppResult.Success -> r.data.toDomain()
            is AppResult.Failure -> return r
        }
        // Playlists are optional: if the call fails we still show every song.
        val playlists = when (val r = safeApiCall { api.playlists(who.coupleId) }) {
            is AppResult.Success -> PlaylistsResult(r.data.available, r.data.playlists.mapNotNull { it.toDomainOrNull() })
            is AppResult.Failure -> PlaylistsResult(available = false, playlists = emptyList())
        }
        return AppResult.Success(MusicLibrary(songs, playlists))
    }

    // ── songs ──

    override suspend fun setLiked(who: MusicIdentity, songId: String, liked: Boolean): AppResult<Unit> =
        unit { api.like(songId, LikeBody(who.coupleId, who.myRole, liked)) }

    override suspend fun deleteSong(who: MusicIdentity, songId: String): AppResult<Unit> =
        unit { api.deleteSong(songId, ViewerBody(who.coupleId, who.myRole)) }

    override suspend fun editSong(who: MusicIdentity, songId: String, edit: SongEdit): AppResult<Unit> =
        unit {
            api.editSong(
                songId,
                EditSongBody(
                    coupleId = who.coupleId,
                    viewer = who.myRole,
                    title = edit.title,
                    artist = edit.artist,
                    album = edit.album,
                    genre = edit.genre,
                    visibility = edit.visibility.wire,
                ),
            )
        }

    override suspend fun recordPlay(songId: String) {
        val who = identity() ?: return
        safeApiCall { api.recordPlay(songId, ViewerBody(who.coupleId, who.myRole)) }
    }

    override suspend fun lyrics(who: MusicIdentity, song: Song): AppResult<Lyrics> {
        // 1) lyrics typed or pasted by the couple live on the song itself
        if (song.hasLyrics) {
            val full = safeApiCall { api.song(song.id, who.coupleId, who.myRole) }
            if (full is AppResult.Success) {
                val parsed = LrcParser.parse(full.data.lyrics, song.lyricsSource ?: "manual")
                if (!parsed.isEmpty) return AppResult.Success(parsed)
            }
        }
        // 2) otherwise the server's lyrics cache (a pure read, no provider is contacted)
        return when (val r = safeApiCall { api.lyricsStatus(song.id) }) {
            is AppResult.Success ->
                AppResult.Success(if (r.data.state == "cached") LrcParser.parse(r.data.lrc, r.data.provider) else Lyrics.None)
            is AppResult.Failure -> r
        }
    }

    override suspend fun saveLyrics(who: MusicIdentity, songId: String, text: String): AppResult<Unit> =
        unit {
            api.editSong(
                songId,
                EditSongBody(who.coupleId, who.myRole, lyrics = text, lyricsSource = "manual"),
            )
        }

    override suspend fun readSongFile(uri: Uri): SongFileInfo = files.read(uri)

    override fun fileSizeBytes(uri: Uri): Long = files.sizeBytes(uri)

    override suspend fun addSongFromDevice(
        who: MusicIdentity,
        uri: Uri,
        info: SongFileInfo,
        visibility: SongVisibility,
    ): AppResult<Song?> {
        val coupleBody = who.coupleId.toRequestBody("text/plain".toMediaType())
        val audioPart = MultipartBody.Part.createFormData(
            "file",
            files.uploadName(uri),
            files.bodyFor(uri, "application/octet-stream"),
        )
        val audio = when (val r = safeApiCall { uploadApi.uploadAudio(audioPart, coupleBody) }) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return r
        }

        // Artwork is a bonus: if it fails the song is still saved.
        val coverUrl = info.cover?.let { bytes ->
            val part = MultipartBody.Part.createFormData("file", "cover.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
            (safeApiCall { uploadApi.uploadCover(part, coupleBody) } as? AppResult.Success)?.data?.url
        }

        val body = AddSongBody(
            coupleId = who.coupleId,
            uploadedBy = who.myRole,
            title = info.title,
            artist = info.artist,
            audioUrl = audio.url,
            coverUrl = coverUrl,
            durationSec = info.durationSec,
            album = info.album.ifBlank { null },
            genre = info.genre.ifBlank { null },
            year = info.year.ifBlank { null },
            visibility = visibility.wire,
        )
        return when (val r = safeApiCall { api.addSong(body) }) {
            is AppResult.Success -> AppResult.Success(r.data.toDomainOrNull())
            is AppResult.Failure -> r
        }
    }

    override suspend fun searchOnline(query: String): AppResult<List<OnlineTrack>> =
        when (val r = safeApiCall { api.searchOnline(query.trim()) }) {
            is AppResult.Success -> AppResult.Success(r.data.results.map { it.toDomain() })
            is AppResult.Failure -> r
        }

    override suspend fun addOnlineTrack(who: MusicIdentity, track: OnlineTrack, visibility: SongVisibility): AppResult<Song?> {
        val preview = track.previewUrl ?: return AppResult.Failure(AppError.Unknown)
        val body = AddSongBody(
            coupleId = who.coupleId,
            uploadedBy = who.myRole,
            title = track.title,
            artist = track.artist,
            audioUrl = preview,
            coverUrl = track.coverUrl,
            durationSec = track.durationSec.coerceAtMost(PREVIEW_SECONDS),
            album = track.album.ifBlank { null },
            visibility = visibility.wire,
            source = "deezer",
        )
        return when (val r = safeApiCall { api.addSong(body) }) {
            is AppResult.Success -> AppResult.Success(r.data.toDomainOrNull())
            is AppResult.Failure -> r
        }
    }

    // ── playlists ──

    override suspend fun createPlaylist(who: MusicIdentity, name: String, songIds: List<String>): AppResult<Playlist> =
        when (val r = safeApiCall { api.createPlaylist(CreatePlaylistBody(who.coupleId, who.myRole, name, songIds)) }) {
            is AppResult.Success -> r.data.toDomainOrNull()?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> r
        }

    override suspend fun renamePlaylist(who: MusicIdentity, id: String, name: String): AppResult<Unit> =
        unit { api.renamePlaylist(id, RenamePlaylistBody(who.coupleId, name)) }

    override suspend fun deletePlaylist(who: MusicIdentity, id: String): AppResult<Unit> =
        unit { api.deletePlaylist(id, CoupleBody(who.coupleId)) }

    override suspend fun addToPlaylist(who: MusicIdentity, id: String, songIds: List<String>): AppResult<Unit> =
        unit { api.addToPlaylist(id, PlaylistSongsBody(who.coupleId, who.myRole, songIds)) }

    override suspend fun removeFromPlaylist(who: MusicIdentity, id: String, songId: String): AppResult<Unit> =
        unit { api.removeFromPlaylist(id, songId, CoupleBody(who.coupleId)) }

    // ── recordings ──

    override suspend fun recordings(who: MusicIdentity): AppResult<List<Recording>> =
        when (val r = safeApiCall { api.recordings(who.coupleId) }) {
            is AppResult.Success -> AppResult.Success(r.data.mapNotNull { it.toDomainOrNull() })
            is AppResult.Failure -> r
        }

    override suspend fun saveRecording(
        who: MusicIdentity,
        file: File,
        media: RecordingMedia,
        title: String,
        durationSec: Int,
        kind: RecordingKind,
        songId: String?,
    ): AppResult<Recording> {
        val isVideo = media == RecordingMedia.Video
        val mime = if (isVideo) "video/mp4" else "audio/mp4"
        val part = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody(mime.toMediaType()))
        val text = "text/plain".toMediaType()
        val uploaded = when (
            val r = safeApiCall {
                uploadApi.uploadRecording(
                    part,
                    who.coupleId.toRequestBody(text),
                    title.toRequestBody(text),
                    (if (isVideo) "video" else "audio").toRequestBody(text),
                )
            }
        ) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return r
        }
        val path = uploaded.path ?: return AppResult.Failure(AppError.Unknown)
        val body = CreateRecordingBody(
            coupleId = who.coupleId,
            userId = who.myRole,
            title = title,
            mediaType = if (isVideo) "video" else "audio",
            mediaUrl = uploaded.url,
            storagePath = path,
            durationSec = durationSec,
            kind = if (kind == RecordingKind.Karaoke) "karaoke" else "voice",
            songId = songId,
        )
        return when (val r = safeApiCall { api.createRecording(body) }) {
            is AppResult.Success -> r.data.toDomainOrNull()?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Unknown)
            is AppResult.Failure -> r
        }
    }

    override suspend fun setRecordingFavorite(who: MusicIdentity, id: String, favorite: Boolean): AppResult<Unit> =
        unit { api.patchRecording(id, RecordingPatchBody(who.coupleId, who.myRole, isFavorite = favorite)) }

    override suspend fun renameRecording(who: MusicIdentity, id: String, title: String): AppResult<Unit> =
        unit { api.patchRecording(id, RecordingPatchBody(who.coupleId, who.myRole, title = title)) }

    override suspend fun deleteRecording(who: MusicIdentity, id: String): AppResult<Unit> =
        unit { api.deleteRecording(id, RecordingDeleteBody(who.coupleId, who.myRole)) }

    /** Runs a call whose answer we do not need. */
    private suspend fun unit(block: suspend () -> Any): AppResult<Unit> =
        when (val r = safeApiCall { block() }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> r
        }

    private companion object {
        /** Deezer previews are 30 seconds long. */
        const val PREVIEW_SECONDS = 30
    }
}
