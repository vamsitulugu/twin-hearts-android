package com.twinhearts.app.feature.music.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.feature.music.data.OfflineSummary
import com.twinhearts.app.feature.music.domain.MixKind
import com.twinhearts.app.feature.music.domain.model.LibraryScope
import com.twinhearts.app.feature.music.domain.model.MusicSettings
import com.twinhearts.app.feature.music.domain.model.Lyrics
import com.twinhearts.app.feature.music.domain.model.MusicIdentity
import com.twinhearts.app.feature.music.domain.model.MusicLibrary
import com.twinhearts.app.feature.music.domain.model.OnlineTrack
import com.twinhearts.app.feature.music.domain.model.SongSort

/*
 * Why this file exists: the state shapes of the Music page. One state object per ViewModel, so a
 * screen is a plain function of its state (R-42).
 */

enum class MusicTab { Music, Recordings }

/** A screen pushed on top of the Music home. Back pops it (the web app does the same with MX.nav). */
sealed interface MusicPage {
    data class Songs(val scope: LibraryScope) : MusicPage
    data class PlaylistDetail(val id: String) : MusicPage
    data object Search : MusicPage
    data object OnlineSearch : MusicPage
    data object KaraokeList : MusicPage
    data object Settings : MusicPage
    data class Mix(val kind: MixKind) : MusicPage
    data class Lyrics(val songId: String) : MusicPage
    data class Sing(val songId: String) : MusicPage
}

/** Progress of "Add songs" while files upload one by one. */
data class AddProgress(val done: Int, val total: Int, val currentName: String)

data class OnlineUiState(
    val query: String = "",
    val results: UiState<List<OnlineTrack>>? = null,
    val addingId: String? = null,
    val addedIds: Set<String> = emptySet(),
)

data class MusicUiState(
    val who: MusicIdentity? = null,
    val tab: MusicTab = MusicTab.Music,
    val library: UiState<MusicLibrary> = UiState.Loading,
    val syncing: Boolean = false,
    val sort: SongSort = SongSort.Recent,
    val pages: List<MusicPage> = emptyList(),
    val playerOpen: Boolean = false,
    val query: String = "",
    val adding: AddProgress? = null,
    val online: OnlineUiState = OnlineUiState(),
    val lyrics: UiState<Lyrics> = UiState.Loading,
    val lyricsSaving: Boolean = false,
    val settings: MusicSettings = MusicSettings(),
    /** Songs saved for offline listening, songs downloading now, and the total for Storage. */
    val offlineIds: Set<String> = emptySet(),
    val downloading: Set<String> = emptySet(),
    val offline: OfflineSummary = OfflineSummary(),
) {
    val page: MusicPage? get() = pages.lastOrNull()
}

/** One-off things to tell the person (toast-like). Never kept in state (R-42). */
sealed interface MusicEvent {
    data class Message(@StringRes val res: Int, val args: List<Any> = emptyList()) : MusicEvent
    data class Plural(@PluralsRes val res: Int, val count: Int) : MusicEvent
    data class Failure(val error: AppError) : MusicEvent
}
