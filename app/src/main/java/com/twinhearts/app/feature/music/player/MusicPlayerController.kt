package com.twinhearts.app.feature.music.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.twinhearts.app.feature.music.data.MusicRepository
import com.twinhearts.app.feature.music.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: ONE player for the whole app, so music keeps playing when the person
 * leaves the Music page, turns the screen off or opens the lock-screen player. The Screen only
 * reads [state] and calls the small functions below; it never touches ExoPlayer.
 *
 * The [MusicPlaybackService] shares this same ExoPlayer, which is what shows the notification.
 */
@Singleton
class MusicPlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Emits a song id every time a listen was counted, so the library can refresh "Recently Played". */
    private val _played = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val played: SharedFlow<String> = _played.asSharedFlow()

    /** Emits once when a track cannot be played (bad link, no internet). */
    private val _failed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val failed: SharedFlow<Unit> = _failed.asSharedFlow()

    /** Songs by id, so the queue can be rebuilt from the player's own item list. */
    private val known = mutableMapOf<String, Song>()
    private var ticker: Job? = null
    private var sleepJob: Job? = null
    private var sessionConnected = false

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync()
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) countListen()
        }

        override fun onPlayerError(error: PlaybackException) {
            _failed.tryEmit(Unit)
        }
    }

    /** The shared player. Created on first use. */
    val player: ExoPlayer by lazy {
        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        ExoPlayer.Builder(context)
            .setAudioAttributes(attributes, true) // pause for phone calls, duck for navigation voice
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .build()
            .also { it.addListener(listener) }
    }

    // ── starting playback ──

    /** Plays [songs] from the one with id [startId]. With [shuffle] the order is mixed. */
    fun play(songs: List<Song>, startId: String, shuffle: Boolean = false) {
        val playable = songs.filter { it.audioUrl.isNotBlank() }
        if (playable.isEmpty()) return
        playable.forEach { known[it.id] = it }
        val start = playable.indexOfFirst { it.id == startId }.coerceAtLeast(0)
        player.shuffleModeEnabled = shuffle
        player.setMediaItems(playable.map(::toItem), start, 0L)
        player.prepare()
        player.play()
        connectSession()
    }

    /** "Play" / "Shuffle" on a list. Shuffle starts from a random song. */
    fun playList(songs: List<Song>, shuffle: Boolean) {
        val playable = songs.filter { it.audioUrl.isNotBlank() }
        if (playable.isEmpty()) return
        val first = if (shuffle) playable.random() else playable.first()
        play(playable, first.id, shuffle)
    }

    fun playNext(song: Song) = enqueue(song, next = true)

    fun addToQueue(song: Song) = enqueue(song, next = false)

    private fun enqueue(song: Song, next: Boolean) {
        if (song.audioUrl.isBlank()) return
        if (!_state.value.hasSong) {
            play(listOf(song), song.id)
            return
        }
        known[song.id] = song
        val index = if (next) player.currentMediaItemIndex + 1 else player.mediaItemCount
        player.addMediaItem(index, toItem(song))
    }

    // ── controls ──

    fun togglePlay() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() = player.seekToNextMediaItem()

    /** Like every music app: within the first seconds go to the previous song, else restart this one. */
    fun previous() = player.seekToPrevious()

    fun seekTo(positionMs: Long) = player.seekTo(positionMs.coerceAtLeast(0L))

    fun seekToQueueIndex(index: Int) {
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0L)
            player.play()
        }
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Stops playback and empties the queue (used when the current song is deleted). */
    fun stop() {
        player.stop()
        player.clearMediaItems()
        setSleepTimer(null)
        sync()
    }

    /** Removes one song from the queue (for example, after it was deleted from the library). */
    fun remove(songId: String) {
        for (i in player.mediaItemCount - 1 downTo 0) {
            if (player.getMediaItemAt(i).mediaId == songId) player.removeMediaItem(i)
        }
        known.remove(songId)
        if (player.mediaItemCount == 0) stop()
    }

    /** Pauses playback after [minutes]. `null` cancels the timer. */
    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null) {
            sleepJob = null
            _state.update { it.copy(sleepAtMs = null) }
            return
        }
        val wakeAt = System.currentTimeMillis() + minutes * 60_000L
        _state.update { it.copy(sleepAtMs = wakeAt) }
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            player.pause()
            _state.update { it.copy(sleepAtMs = null) }
        }
    }

    /** Lets the library update a song's heart/title in the queue without restarting playback. */
    fun refreshKnown(songs: List<Song>) {
        if (known.isEmpty()) return
        songs.forEach { if (known.containsKey(it.id)) known[it.id] = it }
        sync()
    }

    // ── internals ──

    private fun toItem(song: Song): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album.ifBlank { null })
            .setArtworkUri(song.coverUrl?.let { Uri.parse(it) })
            .build()
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(song.audioUrl)
            .setMediaMetadata(meta)
            .build()
    }

    private fun sync() {
        val p = player
        val queue = (0 until p.mediaItemCount).mapNotNull { known[p.getMediaItemAt(it).mediaId] }
        val current = p.currentMediaItem?.mediaId?.let { known[it] }
        _state.update {
            it.copy(
                queue = queue,
                current = current,
                isPlaying = p.isPlaying,
                isBuffering = p.playbackState == Player.STATE_BUFFERING,
                positionMs = p.currentPosition.coerceAtLeast(0L),
                durationMs = p.duration.takeIf { d -> d != C.TIME_UNSET } ?: 0L,
                shuffle = p.shuffleModeEnabled,
                repeat = when (p.repeatMode) {
                    Player.REPEAT_MODE_ALL -> RepeatKind.All
                    Player.REPEAT_MODE_ONE -> RepeatKind.One
                    else -> RepeatKind.Off
                },
            )
        }
        if (p.isPlaying) startTicker() else stopTicker()
    }

    /** While a song plays, the progress bar and synced lyrics need a position several times a second. */
    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                val p = player
                _state.update {
                    it.copy(
                        positionMs = p.currentPosition.coerceAtLeast(0L),
                        durationMs = p.duration.takeIf { d -> d != C.TIME_UNSET } ?: it.durationMs,
                    )
                }
                delay(TICK_MS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun countListen() {
        val id = player.currentMediaItem?.mediaId ?: return
        // Recordings and search previews are not library songs, so there is nothing to count.
        if (id.startsWith(RECORDING_PREFIX) || id.startsWith(PREVIEW_PREFIX)) return
        scope.launch {
            repository.recordPlay(id)
            _played.emit(id)
        }
    }

    /**
     * Connecting a MediaController to the service is what makes Android start it as a foreground
     * service, which keeps the music alive in the background and draws the notification.
     */
    private fun connectSession() {
        if (sessionConnected) return
        sessionConnected = true
        val token = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        MediaController.Builder(context, token).buildAsync()
    }

    companion object {
        private const val TICK_MS = 300L

        /** Ids of the temporary songs made for a recording or an online preview. */
        const val RECORDING_PREFIX = "rec_"
        const val PREVIEW_PREFIX = "deezer_"
    }
}
