package com.twinhearts.app.feature.music.singtogether.ui

import androidx.lifecycle.ViewModel
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.player.MusicPlayerController
import com.twinhearts.app.feature.music.player.PlayerState
import com.twinhearts.app.feature.music.singtogether.data.SingState
import com.twinhearts.app.feature.music.singtogether.data.SingTogetherEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Why this file exists: the screen's only door to Sing Together (R-40). While the Music page is
 * open the engine listens for the partner's invite; when the page closes it stops.
 */
@HiltViewModel
class SingTogetherViewModel @Inject constructor(
    private val engine: SingTogetherEngine,
    controller: MusicPlayerController,
) : ViewModel() {

    val state: StateFlow<SingState> = engine.state

    /** Song position for the progress bar and the lyric highlight. */
    val player: StateFlow<PlayerState> = controller.state

    init {
        engine.watch()
    }

    override fun onCleared() {
        engine.unwatch()
    }

    fun invite(song: Song, songs: List<Song>) = engine.invite(song, songs)

    fun cancelInvite() = engine.cancelInvite()

    fun accept() = engine.accept()

    fun decline() = engine.decline()

    fun leave() = engine.leave()

    fun togglePlay() = engine.togglePlay()

    fun next() = engine.next()

    fun previous() = engine.previous()

    fun seekTo(positionMs: Long) = engine.seekTo(positionMs)

    fun toggleMic() = engine.toggleMic()

    fun toggleCamera() = engine.toggleCamera()

    fun toggleSpeaker() = engine.toggleSpeaker()

    fun switchCamera() = engine.switchCamera()

    fun sendReaction(emoji: String) = engine.sendReaction(emoji)

    fun consumeNotice() = engine.consumeNotice()
}
