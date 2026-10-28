package com.twinhearts.app.feature.music.domain

import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.domain.model.SongSort
import java.text.Normalizer

/** Why this file exists: small pure helpers (time text, searching, sorting) shared by the screens. */
object MusicFormat {

    private val accents = Regex("\\p{M}+")
    private val spaces = Regex("\\s+")

    /** 125 -> "2:05". Never negative. */
    fun clock(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }

    fun clockMs(ms: Long): String = clock((ms / 1000L).toInt())

    /** Lower-case, no accents, single spaces: so "Beyoncé" matches "beyonce". */
    fun normalise(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(accents, "")
            .lowercase()
            .replace(spaces, " ")
            .trim()

    fun matches(song: Song, query: String): Boolean {
        val q = normalise(query)
        if (q.isEmpty()) return true
        return normalise("${song.title} ${song.artist} ${song.album}").contains(q)
    }

    fun sorted(songs: List<Song>, sort: SongSort): List<Song> = when (sort) {
        SongSort.Recent -> songs.sortedByDescending { it.createdAt }
        SongSort.Title -> songs.sortedBy { normalise(it.title) }
        SongSort.Artist -> songs.sortedBy { normalise(it.artist) }
        SongSort.MostPlayed -> songs.sortedByDescending { it.myPlayCount }
    }
}
