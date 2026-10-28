package com.twinhearts.app.feature.music.domain

import com.twinhearts.app.feature.music.domain.model.LibraryScope
import com.twinhearts.app.feature.music.domain.model.Playlist
import com.twinhearts.app.feature.music.domain.model.Recording
import com.twinhearts.app.feature.music.domain.model.RecordingFilter
import com.twinhearts.app.feature.music.domain.model.RecordingMedia
import com.twinhearts.app.feature.music.domain.model.RecordingOwner
import com.twinhearts.app.feature.music.domain.model.Song

/**
 * Why this file exists: the rules for which songs and recordings each list shows. They match
 * `Lib.*` in the web app's `public/music/core.js`, and live here (not in a screen) so they can be tested.
 */
object LibraryQueries {

    /** Full songs only: the 30-second Deezer previews live under "Featured". */
    fun full(songs: List<Song>): List<Song> = songs.filterNot { it.isPreview }

    fun forScope(scope: LibraryScope, songs: List<Song>, myRole: String): List<Song> = when (scope) {
        LibraryScope.All -> full(songs)
        LibraryScope.Mine -> full(songs).filter { it.isMine(myRole) }
        LibraryScope.Partner -> full(songs).filterNot { it.isMine(myRole) }
        LibraryScope.Liked -> full(songs).filter { it.liked }
        LibraryScope.Featured -> songs.filter { it.isPreview }.sortedByDescending { it.createdAt }
        LibraryScope.Recent -> songs.filter { it.myLastPlayed != null }.sortedByDescending { it.myLastPlayed }
    }

    fun inPlaylist(playlist: Playlist, songs: List<Song>): List<Song> {
        val byId = songs.associateBy { it.id }
        return playlist.songIds.mapNotNull { byId[it] }
    }

    /** Songs that can be sung along to: they need lyrics. */
    fun singable(songs: List<Song>): List<Song> = full(songs).filter { it.hasLyrics }

    fun filterRecordings(
        all: List<Recording>,
        filter: RecordingFilter,
        owner: RecordingOwner,
        myRole: String,
        query: String = "",
    ): List<Recording> = all
        .filter { r ->
            when (filter) {
                RecordingFilter.Audio -> r.media == RecordingMedia.Audio
                RecordingFilter.Video -> r.media == RecordingMedia.Video
                RecordingFilter.Favorites -> r.favorite
            }
        }
        .filter { r ->
            when (owner) {
                RecordingOwner.Everyone -> true
                RecordingOwner.Mine -> r.userRole == myRole
                RecordingOwner.Partner -> r.userRole != myRole
            }
        }
        .filter { r -> query.isBlank() || MusicFormat.normalise(r.title).contains(MusicFormat.normalise(query.trim())) }
        .sortedByDescending { it.createdAt }
}
