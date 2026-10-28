package com.twinhearts.app.feature.music.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.music.data.AudioFileReader
import com.twinhearts.app.feature.music.data.MusicRepository
import com.twinhearts.app.feature.music.domain.model.Lyrics
import com.twinhearts.app.feature.music.domain.model.MusicIdentity
import com.twinhearts.app.feature.music.domain.model.MusicLibrary
import com.twinhearts.app.feature.music.domain.model.OnlineTrack
import com.twinhearts.app.feature.music.domain.model.Playlist
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.domain.model.SongEdit
import com.twinhearts.app.feature.music.domain.model.SongSort
import com.twinhearts.app.feature.music.domain.model.SongVisibility
import com.twinhearts.app.feature.music.player.MusicPlayerController
import com.twinhearts.app.feature.music.player.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Why this file exists: runs the Music tab: the library, pushed pages, likes, playlists, adding
 * songs, online search and lyrics. Playback itself is in [MusicPlayerController]; this class only
 * forwards the taps. Recordings have their own [RecordingsViewModel].
 */
@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val controller: MusicPlayerController,
) : ViewModel() {

    private val _state = MutableStateFlow(MusicUiState())
    val state: StateFlow<MusicUiState> = _state.asStateFlow()

    /** What is playing now. Straight from the shared player. */
    val player: StateFlow<PlayerState> = controller.state

    private val eventChannel = Channel<MusicEvent>(Channel.BUFFERED)
    val events: Flow<MusicEvent> = eventChannel.receiveAsFlow()

    private var who: MusicIdentity? = null

    init {
        refresh()
        viewModelScope.launch { controller.played.collect { loadLibrary(silent = true) } }
        viewModelScope.launch { controller.failed.collect { send(MusicEvent.Message(R.string.music_msg_cannot_play)) } }
    }

    // ── loading ──

    /** The Sync button, pull to refresh, and the first load. */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(syncing = true) }
            if (who == null) who = repository.identity()
            val identity = who
            if (identity == null) {
                _state.update { it.copy(library = UiState.Error(AppError.Unauthorized()), syncing = false) }
                return@launch
            }
            _state.update { it.copy(who = identity) }
            loadLibrary(silent = false)
        }
    }

    private suspend fun loadLibrary(silent: Boolean) {
        val identity = who ?: return
        when (val r = repository.library(identity)) {
            is AppResult.Success -> {
                controller.refreshKnown(r.data.songs)
                _state.update { it.copy(library = libraryState(r.data), syncing = false) }
            }
            is AppResult.Failure -> {
                val hasContent = _state.value.library is UiState.Content || _state.value.library is UiState.Empty
                // A failed background refresh must not wipe a screen that already has songs.
                if (hasContent) {
                    if (!silent) send(MusicEvent.Failure(r.error))
                    _state.update { it.copy(syncing = false) }
                } else {
                    _state.update { it.copy(library = UiState.Error(r.error), syncing = false) }
                }
            }
        }
    }

    private fun libraryState(library: MusicLibrary): UiState<MusicLibrary> =
        if (library.songs.isEmpty()) UiState.Empty else UiState.Content(library)

    private fun currentLibrary(): MusicLibrary? = (_state.value.library as? UiState.Content)?.data

    // ── navigation inside Music ──

    fun setTab(tab: MusicTab) = _state.update { it.copy(tab = tab) }

    fun open(page: MusicPage) {
        _state.update { it.copy(pages = it.pages + page, query = if (page == MusicPage.Search) "" else it.query) }
        if (page is MusicPage.Lyrics) loadLyrics(page.songId)
        if (page is MusicPage.Sing) loadLyrics(page.songId)
    }

    /** Back button: closes the player, then pops pages. Returns false when Music itself should close. */
    fun back(): Boolean {
        val s = _state.value
        return when {
            s.playerOpen -> { _state.update { it.copy(playerOpen = false) }; true }
            s.pages.isNotEmpty() -> { _state.update { it.copy(pages = it.pages.dropLast(1)) }; true }
            s.tab != MusicTab.Music -> { _state.update { it.copy(tab = MusicTab.Music) }; true }
            else -> false
        }
    }

    fun openPlayer() = _state.update { it.copy(playerOpen = true) }

    fun closePlayer() = _state.update { it.copy(playerOpen = false) }

    fun setSort(sort: SongSort) = _state.update { it.copy(sort = sort) }

    fun setQuery(text: String) = _state.update { it.copy(query = text) }

    // ── playback (forwarded) ──

    fun play(list: List<Song>, song: Song) = controller.play(list, song.id)

    fun playAll(list: List<Song>, shuffle: Boolean) {
        if (list.isEmpty()) send(MusicEvent.Message(R.string.music_msg_nothing_to_play)) else controller.playList(list, shuffle)
    }

    fun playNext(song: Song) {
        controller.playNext(song)
        send(MusicEvent.Message(R.string.music_msg_play_next))
    }

    fun addToQueue(song: Song) {
        controller.addToQueue(song)
        send(MusicEvent.Message(R.string.music_msg_added_queue))
    }

    fun togglePlay() = controller.togglePlay()
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seekTo(ms: Long) = controller.seekTo(ms)
    fun toggleShuffle() = controller.toggleShuffle()
    fun cycleRepeat() = controller.cycleRepeat()
    fun playQueueIndex(index: Int) = controller.seekToQueueIndex(index)
    fun setSleepTimer(minutes: Int?) = controller.setSleepTimer(minutes)

    // ── songs ──

    fun toggleLike(song: Song) {
        val identity = who ?: return
        val next = !song.liked
        updateSong(song.id) { it.copy(liked = next) }
        viewModelScope.launch {
            val r = repository.setLiked(identity, song.id, next)
            if (r is AppResult.Failure) {
                updateSong(song.id) { it.copy(liked = !next) }
                send(MusicEvent.Message(R.string.music_msg_like_failed))
            }
        }
    }

    fun deleteSong(song: Song) {
        val identity = who ?: return
        viewModelScope.launch {
            when (val r = repository.deleteSong(identity, song.id)) {
                is AppResult.Success -> {
                    controller.remove(song.id)
                    removeSongLocally(song.id)
                    send(MusicEvent.Message(R.string.music_msg_song_deleted))
                }
                is AppResult.Failure -> send(MusicEvent.Failure(r.error))
            }
        }
    }

    fun editSong(song: Song, edit: SongEdit) {
        val identity = who ?: return
        if (edit.title.isBlank()) {
            send(MusicEvent.Message(R.string.music_msg_title_empty))
            return
        }
        viewModelScope.launch {
            when (val r = repository.editSong(identity, song.id, edit)) {
                is AppResult.Success -> {
                    updateSong(song.id) {
                        it.copy(
                            title = edit.title.trim(),
                            artist = edit.artist.trim().ifEmpty { "Unknown Artist" },
                            album = edit.album.trim(),
                            genre = edit.genre.trim(),
                            visibility = edit.visibility,
                        )
                    }
                    send(MusicEvent.Message(R.string.music_msg_saved))
                }
                is AppResult.Failure -> send(MusicEvent.Failure(r.error))
            }
        }
    }

    /** Uploads the picked files one after another, reading each file's tags first. */
    fun addFromDevice(uris: List<Uri>) {
        val identity = who ?: return
        if (uris.isEmpty() || _state.value.adding != null) return
        viewModelScope.launch {
            var added = 0
            uris.forEachIndexed { index, uri ->
                val info = repository.readSongFile(uri)
                _state.update { it.copy(adding = AddProgress(index, uris.size, info.title)) }
                if (repository.fileSizeBytes(uri) > AudioFileReader.MAX_SONG_BYTES) {
                    send(MusicEvent.Message(R.string.music_msg_too_big, listOf(info.title)))
                    return@forEachIndexed
                }
                when (val r = repository.addSongFromDevice(identity, uri, info, SongVisibility.Both)) {
                    is AppResult.Success -> added++
                    is AppResult.Failure -> send(MusicEvent.Failure(r.error))
                }
            }
            _state.update { it.copy(adding = null) }
            if (added > 0) {
                send(MusicEvent.Plural(R.plurals.music_msg_added_songs, added))
                loadLibrary(silent = true)
            }
        }
    }

    // ── playlists ──

    fun createPlaylist(name: String, songIds: List<String> = emptyList(), after: (Playlist) -> Unit = {}) {
        val identity = who ?: return
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            when (val r = repository.createPlaylist(identity, clean, songIds)) {
                is AppResult.Success -> {
                    updatePlaylists { it + r.data.copy(songIds = songIds) }
                    send(MusicEvent.Message(R.string.music_msg_playlist_created, listOf(clean)))
                    after(r.data)
                }
                is AppResult.Failure -> send(MusicEvent.Failure(r.error))
            }
        }
    }

    fun renamePlaylist(playlist: Playlist, name: String) {
        val identity = who ?: return
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            when (val r = repository.renamePlaylist(identity, playlist.id, clean)) {
                is AppResult.Success -> updatePlaylists { list -> list.map { if (it.id == playlist.id) it.copy(name = clean) else it } }
                is AppResult.Failure -> send(MusicEvent.Failure(r.error))
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        val identity = who ?: return
        viewModelScope.launch {
            when (val r = repository.deletePlaylist(identity, playlist.id)) {
                is AppResult.Success -> {
                    updatePlaylists { list -> list.filterNot { it.id == playlist.id } }
                    _state.update { s ->
                        s.copy(pages = s.pages.filterNot { it is MusicPage.PlaylistDetail && it.id == playlist.id })
                    }
                }
                is AppResult.Failure -> send(MusicEvent.Failure(r.error))
            }
        }
    }

    fun addToPlaylist(playlist: Playlist, songIds: List<String>) {
        val identity = who ?: return
        viewModelScope.launch {
            when (val r = repository.addToPlaylist(identity, playlist.id, songIds)) {
                is AppResult.Success -> {
                    updatePlaylists { list ->
                        list.map { if (it.id == playlist.id) it.copy(songIds = (it.songIds + songIds).distinct()) else it }
                    }
                    send(MusicEvent.Message(R.string.music_msg_added_to, listOf(playlist.name)))
                }
                is AppResult.Failure -> send(MusicEvent.Failure(r.error))
            }
        }
    }

    fun removeFromPlaylist(playlist: Playlist, songId: String) {
        val identity = who ?: return
        viewModelScope.launch {
            when (val r = repository.removeFromPlaylist(identity, playlist.id, songId)) {
                is AppResult.Success -> updatePlaylists { list ->
                    list.map { if (it.id == playlist.id) it.copy(songIds = it.songIds - songId) else it }
                }
                is AppResult.Failure -> send(MusicEvent.Failure(r.error))
            }
        }
    }

    // ── search online (Deezer through the server) ──

    fun setOnlineQuery(text: String) = _state.update { it.copy(online = it.online.copy(query = text)) }

    fun searchOnline() {
        val q = _state.value.online.query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(online = it.online.copy(results = UiState.Loading)) }
            val result = when (val r = repository.searchOnline(q)) {
                is AppResult.Success -> if (r.data.isEmpty()) UiState.Empty else UiState.Content(r.data)
                is AppResult.Failure -> UiState.Error(r.error)
            }
            _state.update { it.copy(online = it.online.copy(results = result)) }
        }
    }

    /** Plays the 30-second preview without saving it. */
    fun previewOnline(track: OnlineTrack) {
        val url = track.previewUrl ?: return
        val song = Song(
            id = track.id, title = track.title, artist = track.artist, album = track.album, genre = "", year = "",
            audioUrl = url, coverUrl = track.coverUrl, durationSec = track.durationSec,
            visibility = SongVisibility.Both, uploadedBy = "", createdAt = null, liked = false, partnerLiked = false,
            myPlayCount = 0, myLastPlayed = null, hasLyrics = false, lyricsSource = null,
        )
        controller.play(listOf(song), song.id)
    }

    fun addOnline(track: OnlineTrack) {
        val identity = who ?: return
        if (_state.value.online.addingId != null) return
        viewModelScope.launch {
            _state.update { it.copy(online = it.online.copy(addingId = track.id)) }
            when (val r = repository.addOnlineTrack(identity, track, SongVisibility.Both)) {
                is AppResult.Success -> {
                    _state.update { it.copy(online = it.online.copy(addingId = null, addedIds = it.online.addedIds + track.id)) }
                    send(MusicEvent.Message(R.string.music_msg_online_added, listOf(track.title)))
                    loadLibrary(silent = true)
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(online = it.online.copy(addingId = null)) }
                    send(MusicEvent.Failure(r.error))
                }
            }
        }
    }

    // ── lyrics ──

    fun loadLyrics(songId: String) {
        val identity = who ?: return
        val song = currentLibrary()?.songs?.firstOrNull { it.id == songId } ?: return
        viewModelScope.launch {
            _state.update { it.copy(lyrics = UiState.Loading) }
            val result = when (val r = repository.lyrics(identity, song)) {
                is AppResult.Success -> if (r.data.isEmpty) UiState.Empty else UiState.Content(r.data)
                is AppResult.Failure -> UiState.Error(r.error)
            }
            _state.update { it.copy(lyrics = result) }
        }
    }

    /** Saves pasted or imported lyrics (either partner may do this), then shows them. */
    fun saveLyrics(songId: String, text: String) {
        val identity = who ?: return
        if (text.isBlank()) {
            send(MusicEvent.Message(R.string.music_msg_paste_lyrics_first))
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(lyricsSaving = true) }
            when (val r = repository.saveLyrics(identity, songId, text)) {
                is AppResult.Success -> {
                    updateSong(songId) { it.copy(hasLyrics = true, lyricsSource = "manual") }
                    _state.update { it.copy(lyricsSaving = false) }
                    loadLyrics(songId)
                    send(MusicEvent.Message(R.string.music_msg_saved))
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(lyricsSaving = false) }
                    send(MusicEvent.Failure(r.error))
                }
            }
        }
    }

    // ── helpers ──

    private fun updateSong(id: String, change: (Song) -> Song) {
        _state.update { s ->
            val content = s.library as? UiState.Content ?: return@update s
            val songs = content.data.songs.map { if (it.id == id) change(it) else it }
            controller.refreshKnown(songs)
            s.copy(library = UiState.Content(content.data.copy(songs = songs)))
        }
    }

    private fun removeSongLocally(id: String) {
        _state.update { s ->
            val content = s.library as? UiState.Content ?: return@update s
            val songs = content.data.songs.filterNot { it.id == id }
            val playlists = content.data.playlists.copy(
                playlists = content.data.playlists.playlists.map { it.copy(songIds = it.songIds - id) },
            )
            s.copy(library = if (songs.isEmpty()) UiState.Empty else UiState.Content(MusicLibrary(songs, playlists)))
        }
    }

    private fun updatePlaylists(change: (List<Playlist>) -> List<Playlist>) {
        _state.update { s ->
            val content = s.library as? UiState.Content ?: return@update s
            val lists = content.data.playlists
            s.copy(library = UiState.Content(content.data.copy(playlists = lists.copy(playlists = change(lists.playlists)))))
        }
    }

    private fun send(event: MusicEvent) {
        eventChannel.trySend(event)
    }
}
