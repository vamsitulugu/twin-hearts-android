package com.twinhearts.app.feature.music.ui

import com.twinhearts.app.core.pro.rememberProGate
import com.twinhearts.app.core.design.LocalOpenSidebar
import com.twinhearts.app.feature.music.ui.components.MusicStyle
import com.twinhearts.app.feature.music.ui.components.MusicTabs
import com.twinhearts.app.feature.music.ui.components.MusicHeader
import androidx.compose.material.icons.rounded.Sync
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.rounded.Tune
import com.twinhearts.app.feature.music.domain.Milestones
import com.twinhearts.app.feature.music.domain.SmartMixes
import java.time.Instant
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.music.domain.LibraryQueries
import com.twinhearts.app.feature.music.domain.MusicFormat
import com.twinhearts.app.feature.music.domain.model.LibraryScope
import com.twinhearts.app.feature.music.domain.model.Playlist
import com.twinhearts.app.feature.music.domain.model.RecordingKind
import com.twinhearts.app.feature.music.domain.model.RecordingMedia
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.domain.model.SongSort
import com.twinhearts.app.feature.music.ui.components.AddToPlaylistSheet
import com.twinhearts.app.feature.music.ui.components.ConfirmDialog
import com.twinhearts.app.feature.music.ui.components.EditSongDialog
import com.twinhearts.app.feature.music.ui.components.PickSongsSheet
import com.twinhearts.app.feature.music.ui.components.SleepTimerSheet
import com.twinhearts.app.feature.music.ui.components.SongDetailsDialog
import com.twinhearts.app.feature.music.ui.components.SongMenuActions
import com.twinhearts.app.feature.music.ui.components.SongMenuSheet
import com.twinhearts.app.feature.music.ui.components.TextPromptDialog
import com.twinhearts.app.feature.music.ui.components.MusicStyled

/** Every sheet or dialog the Music page can open. Only ids are kept, so the data is always fresh. */
private sealed interface MusicDialog {
    data class SongMenu(val songId: String, val playlistId: String? = null) : MusicDialog
    data class Details(val songId: String) : MusicDialog
    data class Edit(val songId: String) : MusicDialog
    data class DeleteSong(val songId: String) : MusicDialog
    data class AddToPlaylist(val songId: String) : MusicDialog
    data class NewPlaylist(val thenAddSongId: String?) : MusicDialog
    data class RenamePlaylist(val playlistId: String) : MusicDialog
    data class DeletePlaylist(val playlistId: String) : MusicDialog
    data class PickSongs(val playlistId: String) : MusicDialog
    data object Queue : MusicDialog
    data object Sleep : MusicDialog
}

/**
 * Why this file exists: the Music page. It owns the top bar, the two tabs, the pushed pages,
 * the mini player, and opens every sheet and dialog. Follows the web app's `music.html` shell.
 */
@Composable
private fun MusicScreenContent(
    onBack: () -> Unit,
    viewModel: MusicViewModel = hiltViewModel(),
    recordingsViewModel: RecordingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val rec by recordingsViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var dialog by remember { mutableStateOf<MusicDialog?>(null) }
    var take by remember { mutableStateOf<Take>(Take.Idle) }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    var failure by remember { mutableStateOf<AppError?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    // Server sentences are shown as they are (R-46).
    val failureText = failure?.asText()
    LaunchedEffect(failure) {
        failureText?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        failure = null
    }
    LaunchedEffect(viewModel) { viewModel.events.collect { showEvent(context, it) { e -> failure = e } } }
    LaunchedEffect(recordingsViewModel) { recordingsViewModel.events.collect { showEvent(context, it) { e -> failure = e } } }

    NotificationRationale(playing = player.hasSong)

    val mic = rememberMicAccess()
    val captureVideo = rememberVideoCapture { file, seconds ->
        take = Take.Naming(file, seconds, RecordingMedia.Video, RecordingKind.Voice, null, context.getString(R.string.music_rec_default_video))
    }
    val pickSongs = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addFromDevice(uris)
    }
    val proGate = rememberProGate()
    val proFeature = stringResource(R.string.pro_f_music)
    val openPicker = { proGate.require(proFeature) { pickSongs.launch(arrayOf("audio/*")) } }

    BackHandler { if (!viewModel.back()) onBack() }

    val library = (state.library as? UiState.Content)?.data
    val songs = library?.songs.orEmpty()
    val playlists = library?.playlists?.playlists.orEmpty()
    val who = state.who ?: rec.who
    val role = who?.myRole.orEmpty()
    val partner = who?.partnerName.orEmpty().ifBlank { stringResource(R.string.music_partner_fallback) }
    val youLabel = stringResource(R.string.music_rec_you)
    val fullSongs = LibraryQueries.full(songs)
    val currentId = player.current?.id

    fun songById(id: String): Song? = songs.firstOrNull { it.id == id }
    fun playlistById(id: String): Playlist? = playlists.firstOrNull { it.id == id }
    fun listFor(song: Song): List<Song> = if (song.isPreview) listOf(song) else fullSongs

    fun pauseIfPlaying() {
        if (player.isPlaying) viewModel.togglePlay()
    }

    fun menuActions(song: Song, playlistId: String?): SongMenuActions = SongMenuActions(
        onPlay = { dialog = null; viewModel.play(listFor(song), song) },
        onPlayNext = { dialog = null; viewModel.playNext(song) },
        onAddToQueue = { dialog = null; viewModel.addToQueue(song) },
        onToggleLike = { dialog = null; viewModel.toggleLike(song) },
        onAddToPlaylist = { dialog = MusicDialog.AddToPlaylist(song.id) },
        onSing = { dialog = null; viewModel.open(MusicPage.Sing(song.id)) },
        onLyrics = { dialog = null; viewModel.open(MusicPage.Lyrics(song.id)) },
        onDetails = { dialog = MusicDialog.Details(song.id) },
        onShare = {
            dialog = null
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.music_share_text, song.title, song.artist))
            }
            context.startActivity(Intent.createChooser(send, context.getString(R.string.music_action_share)))
        },
        onEdit = { dialog = MusicDialog.Edit(song.id) },
        onDelete = { dialog = MusicDialog.DeleteSong(song.id) },
        onRemoveFromPlaylist = playlistId?.let { pid ->
            { dialog = null; playlistById(pid)?.let { viewModel.removeFromPlaylist(it, song.id) } }
        },
        downloaded = song.id in state.offlineIds,
        downloading = song.id in state.downloading,
        onToggleDownload = { dialog = null; viewModel.toggleDownload(song) },
    )

    val page = state.page
    val title = when (page) {
        null -> stringResource(R.string.music_title)
        is MusicPage.Songs -> when (page.scope) {
            LibraryScope.All -> stringResource(R.string.music_page_all)
            LibraryScope.Mine -> stringResource(R.string.music_tile_mine)
            LibraryScope.Partner -> stringResource(R.string.music_page_partner, partner)
            LibraryScope.Liked -> stringResource(R.string.music_tile_liked)
            LibraryScope.Featured -> stringResource(R.string.music_tile_featured)
            LibraryScope.Recent -> stringResource(R.string.music_recent_title)
        }
        is MusicPage.PlaylistDetail -> playlistById(page.id)?.name ?: stringResource(R.string.music_playlists_title)
        MusicPage.Search -> stringResource(R.string.music_page_search)
        MusicPage.OnlineSearch -> stringResource(R.string.music_quick_search_online)
        MusicPage.KaraokeList -> stringResource(R.string.music_tile_karaoke)
        is MusicPage.Lyrics -> stringResource(R.string.music_action_lyrics)
        is MusicPage.Sing -> stringResource(R.string.music_action_sing)
        MusicPage.Settings -> stringResource(R.string.music_settings_title)
        is MusicPage.Mix -> mixName(page.kind, partner)
    }

    TwinBackground(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                if (page == null) {
                    MusicHeader(
                        title = title,
                        onMenu = LocalOpenSidebar.current,
                        onSearch = { viewModel.open(MusicPage.Search) },
                        onMore = { menuOpen = true },
                        menu = {
                            HomeMenu(
                                expanded = menuOpen,
                                sort = state.sort,
                                onDismiss = { menuOpen = false },
                                onAddSongs = { menuOpen = false; openPicker() },
                                onSearchOnline = { menuOpen = false; viewModel.open(MusicPage.OnlineSearch) },
                                onSort = { menuOpen = false; viewModel.setSort(it) },
                                onSleep = { menuOpen = false; dialog = MusicDialog.Sleep },
                                onSettings = { menuOpen = false; viewModel.open(MusicPage.Settings) },
                                onRefresh = { menuOpen = false; viewModel.refresh(); recordingsViewModel.refresh() },
                            )
                        },
                    )
                } else {
                    TwinTopBar(
                        title = title,
                        onBackClick = { if (!viewModel.back()) onBack() },
                    )
                }
                Box(Modifier.weight(1f)) {
                    when (page) {
                        null -> Column(Modifier.fillMaxSize()) {
                            MusicTabs(
                                options = listOf(stringResource(R.string.music_tab_music), stringResource(R.string.music_tab_recordings)),
                                selectedIndex = state.tab.ordinal,
                                onSelect = { viewModel.setTab(MusicTab.entries[it]) },
                                modifier = Modifier
                                    .padding(horizontal = TwinTheme.spacing.screenEdge)
                                    .padding(top = TwinTheme.spacing.xs, bottom = TwinTheme.spacing.s),
                            )
                            if (state.tab == MusicTab.Music) {
                                MusicHome(
                                    state = state,
                                    currentSongId = currentId,
                                    partnerName = partner,
                                    actions = MusicHomeActions(
                                        onRetry = viewModel::refresh,
                                        onAddSongs = openPicker,
                                        onOpenScope = { viewModel.open(MusicPage.Songs(it)) },
                                        onKaraoke = { viewModel.open(MusicPage.KaraokeList) },
                                        onPlayAll = { shuffle -> viewModel.playAll(fullSongs, shuffle) },
                                        onSearch = { viewModel.open(MusicPage.Search) },
                                        onSearchOnline = { viewModel.open(MusicPage.OnlineSearch) },
                                        onPlay = { song, list -> viewModel.play(list, song) },
                                        onSongMenu = { dialog = MusicDialog.SongMenu(it.id) },
                                        onOpenPlaylist = { viewModel.open(MusicPage.PlaylistDetail(it.id)) },
                                        onNewPlaylist = { dialog = MusicDialog.NewPlaylist(null) },
                                        onOpenMix = { viewModel.open(MusicPage.Mix(it)) },
                                    ),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                RecordingsTab(
                                    state = rec,
                                    partnerName = partner,
                                    currentSongId = currentId,
                                    actions = RecordingsActions(
                                        onRetry = recordingsViewModel::refresh,
                                        onRecordAudio = {
                                            mic.ask {
                                                pauseIfPlaying()
                                                if (recordingsViewModel.startTake()) take = Take.Recording(null)
                                                else Toast.makeText(context, R.string.music_mic_failed, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        onRecordVideo = { pauseIfPlaying(); captureVideo() },
                                        onFilter = recordingsViewModel::setFilter,
                                        onOwner = recordingsViewModel::setOwner,
                                        onPlay = { r ->
                                            if (r.media == RecordingMedia.Video) {
                                                pauseIfPlaying()
                                                videoUrl = r.mediaUrl
                                            } else {
                                                recordingsViewModel.playAudio(r)
                                            }
                                        },
                                        onFavorite = recordingsViewModel::toggleFavorite,
                                        onRename = recordingsViewModel::rename,
                                        onDelete = recordingsViewModel::delete,
                                        onQuery = recordingsViewModel::setQuery,
                                    ),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        is MusicPage.Songs -> {
                            val list = MusicFormat.sorted(LibraryQueries.forScope(page.scope, songs, role), state.sort)
                            SongListPage(
                                songs = list,
                                currentSongId = currentId,
                                emptyTitle = stringResource(R.string.music_list_empty_title),
                                emptyMessage = stringResource(R.string.music_list_empty_message),
                                onPlay = { song, l -> viewModel.play(l, song) },
                                onPlayAll = { shuffle -> viewModel.playAll(list, shuffle) },
                                onSongMenu = { dialog = MusicDialog.SongMenu(it.id) },
                                modifier = Modifier.fillMaxSize(),
                                headerExtra = { SortChips(state.sort, viewModel::setSort) },
                                ownerLabel = if (page.scope == LibraryScope.Liked || page.scope == LibraryScope.Recent) {
                                    { s -> if (s.isMine(role)) youLabel else partner }
                                } else {
                                    null
                                },
                            )
                        }
                        is MusicPage.PlaylistDetail -> {
                            val playlist = playlistById(page.id)
                            val list = playlist?.let { LibraryQueries.inPlaylist(it, songs) }.orEmpty()
                            SongListPage(
                                songs = list,
                                currentSongId = currentId,
                                emptyTitle = stringResource(R.string.music_playlist_empty_title),
                                emptyMessage = stringResource(R.string.music_playlist_empty_message),
                                onPlay = { song, l -> viewModel.play(l, song) },
                                onPlayAll = { shuffle -> viewModel.playAll(list, shuffle) },
                                onSongMenu = { dialog = MusicDialog.SongMenu(it.id, page.id) },
                                modifier = Modifier.fillMaxSize(),
                                headerExtra = {
                                    Row(
                                        modifier = Modifier
                                            .horizontalScroll(rememberScrollState())
                                            .padding(top = TwinTheme.spacing.s),
                                        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                                    ) {
                                        GlassButton(stringResource(R.string.music_playlist_add_songs), { dialog = MusicDialog.PickSongs(page.id) }, leadingIcon = Icons.Rounded.Add, style = GlassButtonStyle.Secondary)
                                        GlassButton(stringResource(R.string.music_rec_rename), { dialog = MusicDialog.RenamePlaylist(page.id) }, leadingIcon = Icons.Rounded.Edit, style = GlassButtonStyle.Ghost)
                                        GlassButton(stringResource(R.string.music_action_delete), { dialog = MusicDialog.DeletePlaylist(page.id) }, leadingIcon = Icons.Rounded.Delete, style = GlassButtonStyle.Ghost)
                                    }
                                },
                            )
                        }
                        MusicPage.Search -> SearchPage(
                            query = state.query,
                            onQuery = viewModel::setQuery,
                            songs = fullSongs,
                            recent = LibraryQueries.forScope(LibraryScope.Recent, songs, role).take(SEARCH_RECENT),
                            playlists = playlists,
                            currentSongId = currentId,
                            onPlay = { song, l -> viewModel.play(l, song) },
                            onSongMenu = { dialog = MusicDialog.SongMenu(it.id) },
                            onOpenPlaylist = { viewModel.open(MusicPage.PlaylistDetail(it.id)) },
                            modifier = Modifier.fillMaxSize(),
                        )
                        MusicPage.OnlineSearch -> OnlineSearchPage(
                            state = state.online,
                            onQuery = viewModel::setOnlineQuery,
                            onSearch = viewModel::searchOnline,
                            onPreview = viewModel::previewOnline,
                            onAdd = viewModel::addOnline,
                            modifier = Modifier.fillMaxSize(),
                        )
                        MusicPage.KaraokeList -> KaraokeListPage(
                            songs = LibraryQueries.singable(songs),
                            onSing = { viewModel.open(MusicPage.Sing(it.id)) },
                            onSongMenu = { dialog = MusicDialog.SongMenu(it.id) },
                            modifier = Modifier.fillMaxSize(),
                        )
                        MusicPage.Settings -> MusicSettingsPage(
                            settings = state.settings,
                            offline = state.offline,
                            milestones = Milestones.earned(songs, (rec.all as? UiState.Content)?.data.orEmpty(), role),
                            actions = MusicSettingsActions(
                                onCrossfade = viewModel::setCrossfade,
                                onAutoLyrics = viewModel::setAutoLyrics,
                                onNormalize = viewModel::setNormalize,
                                onVocalReduce = viewModel::setVocalReduce,
                                onPreset = viewModel::setPreset,
                                onFx = viewModel::setFx,
                                onResetSound = viewModel::resetSound,
                                onAddSongs = openPicker,
                                onSync = { viewModel.refresh(); recordingsViewModel.refresh() },
                                onClearDownloads = viewModel::clearDownloads,
                                onDeleteRecordings = recordingsViewModel::deleteAllMine,
                            ),
                            modifier = Modifier.fillMaxSize(),
                        )
                        is MusicPage.Mix -> {
                            val list = SmartMixes.build(songs, Instant.now()).firstOrNull { it.kind == page.kind }?.songs.orEmpty()
                            SongListPage(
                                songs = list,
                                currentSongId = currentId,
                                emptyTitle = stringResource(R.string.music_list_empty_title),
                                emptyMessage = stringResource(R.string.music_list_empty_message),
                                onPlay = { song, l -> viewModel.play(l, song) },
                                onPlayAll = { shuffle -> viewModel.playAll(list, shuffle) },
                                onSongMenu = { dialog = MusicDialog.SongMenu(it.id) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        is MusicPage.Lyrics -> {
                            val song = songById(page.songId)
                            LyricsPage(
                                song = song,
                                lyrics = state.lyrics,
                                saving = state.lyricsSaving,
                                player = player,
                                onRetry = { viewModel.loadLyrics(page.songId) },
                                onSave = { viewModel.saveLyrics(page.songId, it) },
                                onPlay = { song?.let { s -> viewModel.play(listFor(s), s) } },
                                onTogglePlay = viewModel::togglePlay,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        is MusicPage.Sing -> {
                            val song = songById(page.songId)
                            val singing = (take as? Take.Recording)?.karaokeSongId == page.songId
                            SingPage(
                                song = song,
                                lyrics = state.lyrics,
                                player = player,
                                singing = singing,
                                onRetry = { viewModel.loadLyrics(page.songId) },
                                onStart = {
                                    if (song != null) {
                                        mic.ask {
                                            if (recordingsViewModel.startTake()) {
                                                viewModel.play(listFor(song), song)
                                                take = Take.Recording(song.id)
                                            } else {
                                                Toast.makeText(context, R.string.music_mic_failed, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                onFinish = {
                                    pauseIfPlaying()
                                    val result = recordingsViewModel.stopTake()
                                    take = if (result != null && song != null) {
                                        Take.Naming(
                                            file = result.first,
                                            seconds = result.second,
                                            media = RecordingMedia.Audio,
                                            kind = RecordingKind.Karaoke,
                                            songId = song.id,
                                            defaultTitle = context.getString(R.string.music_sing_default_title, song.title),
                                        )
                                    } else {
                                        Toast.makeText(context, R.string.music_rec_too_short, Toast.LENGTH_LONG).show()
                                        Take.Idle
                                    }
                                },
                                onAbort = {
                                    recordingsViewModel.cancelTake()
                                    pauseIfPlaying()
                                    take = Take.Idle
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
                if (player.hasSong && !state.playerOpen) {
                    MiniPlayer(player, onOpen = viewModel::openPlayer, onTogglePlay = viewModel::togglePlay, onNext = viewModel::next)
                }
            }
            if (state.playerOpen && player.hasSong) {
                NowPlayingScreen(
                    state = player,
                    actions = PlayerActions(
                        onTogglePlay = viewModel::togglePlay,
                        onNext = viewModel::next,
                        onPrevious = viewModel::previous,
                        onSeek = viewModel::seekTo,
                        onShuffle = viewModel::toggleShuffle,
                        onRepeat = viewModel::cycleRepeat,
                        onLike = { s -> songById(s.id)?.let(viewModel::toggleLike) },
                        onLyrics = { s -> viewModel.closePlayer(); viewModel.open(MusicPage.Lyrics(s.id)) },
                        onQueue = { dialog = MusicDialog.Queue },
                        onSleep = { dialog = MusicDialog.Sleep },
                        onClose = viewModel::closePlayer,
                    ),
                )
            }
        }
    }

    // ── sheets and dialogs ──
    val close = { dialog = null }
    when (val d = dialog) {
        null -> Unit
        is MusicDialog.SongMenu -> songById(d.songId)?.let { s ->
            SongMenuSheet(s, s.isMine(role), menuActions(s, d.playlistId), close)
        }
        is MusicDialog.Details -> songById(d.songId)?.let { s ->
            SongDetailsDialog(s, if (s.isMine(role)) youLabel else partner, close)
        }
        is MusicDialog.Edit -> songById(d.songId)?.let { s ->
            EditSongDialog(s, onSave = { viewModel.editSong(s, it); dialog = null }, onDismiss = close)
        }
        is MusicDialog.DeleteSong -> songById(d.songId)?.let { s ->
            ConfirmDialog(
                title = stringResource(R.string.music_delete_title),
                message = stringResource(R.string.music_delete_message, s.title),
                confirmLabel = stringResource(R.string.music_action_delete),
                onConfirm = { viewModel.deleteSong(s); dialog = null },
                onDismiss = close,
            )
        }
        is MusicDialog.AddToPlaylist -> songById(d.songId)?.let { s ->
            AddToPlaylistSheet(
                song = s,
                playlists = playlists,
                available = library?.playlists?.available ?: false,
                onPick = { viewModel.addToPlaylist(it, listOf(s.id)); dialog = null },
                onNew = { dialog = MusicDialog.NewPlaylist(s.id) },
                onDismiss = close,
            )
        }
        is MusicDialog.NewPlaylist -> TextPromptDialog(
            title = stringResource(R.string.music_new_playlist),
            label = stringResource(R.string.music_playlist_name),
            initial = "",
            confirmLabel = stringResource(R.string.music_create),
            maxLength = 80,
            onConfirm = { name ->
                viewModel.createPlaylist(name, listOfNotNull(d.thenAddSongId))
                dialog = null
            },
            onDismiss = close,
        )
        is MusicDialog.RenamePlaylist -> playlistById(d.playlistId)?.let { p ->
            TextPromptDialog(
                title = stringResource(R.string.music_rec_rename),
                label = stringResource(R.string.music_playlist_name),
                initial = p.name,
                confirmLabel = stringResource(R.string.music_save),
                maxLength = 80,
                onConfirm = { viewModel.renamePlaylist(p, it); dialog = null },
                onDismiss = close,
            )
        }
        is MusicDialog.DeletePlaylist -> playlistById(d.playlistId)?.let { p ->
            ConfirmDialog(
                title = stringResource(R.string.music_playlist_delete_title),
                message = stringResource(R.string.music_playlist_delete_message, p.name),
                confirmLabel = stringResource(R.string.music_action_delete),
                onConfirm = { viewModel.deletePlaylist(p); dialog = null },
                onDismiss = close,
            )
        }
        is MusicDialog.PickSongs -> playlistById(d.playlistId)?.let { p ->
            PickSongsSheet(
                title = stringResource(R.string.music_playlist_add_songs),
                songs = fullSongs.filterNot { it.id in p.songIds },
                onDone = { viewModel.addToPlaylist(p, it); dialog = null },
                onDismiss = close,
            )
        }
        MusicDialog.Queue -> com.twinhearts.app.feature.music.ui.QueueSheet(player, onPick = viewModel::playQueueIndex, onDismiss = close)
        MusicDialog.Sleep -> SleepTimerSheet(player.sleepAtMs, onSet = { viewModel.setSleepTimer(it); dialog = null }, onDismiss = close)
    }

    TakeDialogs(
        take = take,
        onStop = {
            val result = recordingsViewModel.stopTake()
            take = if (result != null) {
                Take.Naming(result.first, result.second, RecordingMedia.Audio, RecordingKind.Voice, null, context.getString(R.string.music_rec_default_audio))
            } else {
                Toast.makeText(context, R.string.music_rec_too_short, Toast.LENGTH_LONG).show()
                Take.Idle
            }
        },
        onCancel = { recordingsViewModel.cancelTake(); take = Take.Idle },
        onSave = { n, name ->
            recordingsViewModel.save(n.file, n.media, name, n.seconds, n.kind, n.songId)
            take = Take.Idle
            viewModel.setTab(MusicTab.Recordings)
        },
        onDiscard = { n -> n.file.delete(); take = Take.Idle },
    )

    videoUrl?.let { VideoDialog(it) { videoUrl = null } }
}

@Composable
private fun HomeMenu(
    expanded: Boolean,
    sort: SongSort,
    onDismiss: () -> Unit,
    onAddSongs: () -> Unit,
    onSearchOnline: () -> Unit,
    onSort: (SongSort) -> Unit,
    onSleep: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    val colors = TwinTheme.colors
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.background(colors.card)) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.core_sync), color = colors.ink) },
            leadingIcon = { Icon(Icons.Rounded.Sync, null, tint = colors.ink) },
            onClick = onRefresh,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.music_add_songs), color = colors.ink) },
            leadingIcon = { Icon(Icons.Rounded.MusicNote, null, tint = colors.ink) },
            onClick = onAddSongs,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.music_quick_search_online), color = colors.ink) },
            leadingIcon = { Icon(Icons.Rounded.Language, null, tint = colors.ink) },
            onClick = onSearchOnline,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.music_sleep_title), color = colors.ink) },
            leadingIcon = { Icon(Icons.Rounded.Bedtime, null, tint = colors.ink) },
            onClick = onSleep,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.music_settings_title), color = colors.ink) },
            leadingIcon = { Icon(Icons.Rounded.Tune, null, tint = colors.ink) },
            onClick = onSettings,
        )
        Text(stringResource(R.string.music_sort_title), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, modifier = Modifier.padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s))
        SongSort.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(stringResource(sortLabel(option)), color = if (option == sort) MusicStyle.Accent else colors.ink) },
                trailingIcon = { if (option == sort) Icon(Icons.Rounded.Check, null, tint = MusicStyle.Accent) },
                onClick = { onSort(option) },
            )
        }
    }
}

@Composable
private fun SortChips(sort: SongSort, onSort: (SongSort) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(top = TwinTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        SongSort.entries.forEach { option ->
            TwinFilterChip(stringResource(sortLabel(option)), option == sort, { onSort(option) })
        }
    }
}

private fun sortLabel(sort: SongSort): Int = when (sort) {
    SongSort.Recent -> R.string.music_sort_recent
    SongSort.Title -> R.string.music_sort_title_az
    SongSort.Artist -> R.string.music_sort_artist
    SongSort.MostPlayed -> R.string.music_sort_most_played
}

/** Android 13+: explains why a notification is needed (lock-screen player), once, when music first starts. */
@Composable
private fun NotificationRationale(playing: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    var show by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(playing) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (playing && !asked && !granted) {
            asked = true
            show = true
        }
    }
    if (show) {
        val colors = TwinTheme.colors
        AlertDialog(
            onDismissRequest = { show = false },
            containerColor = colors.card,
            title = { Text(stringResource(R.string.music_notif_title), style = MaterialTheme.typography.titleLarge, color = colors.ink) },
            text = { Text(stringResource(R.string.music_notif_message), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted) },
            confirmButton = {
                TextButton(onClick = { show = false; launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text(stringResource(R.string.music_mic_allow), color = colors.accent)
                }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text(stringResource(R.string.music_not_now), color = colors.inkMuted) } },
        )
    }
}

private fun showEvent(context: Context, event: MusicEvent, onFailure: (AppError) -> Unit) {
    when (event) {
        is MusicEvent.Message -> Toast.makeText(context, context.getString(event.res, *event.args.toTypedArray()), Toast.LENGTH_SHORT).show()
        is MusicEvent.Plural -> Toast.makeText(context, context.resources.getQuantityString(event.res, event.count, event.count), Toast.LENGTH_SHORT).show()
        is MusicEvent.Failure -> onFailure(event.error)
    }
}

private const val SEARCH_RECENT = 8

/** Public entry: the page body wrapped in the shared near-black glass look. */
@Composable
fun MusicScreen(
    onBack: () -> Unit,
    viewModel: MusicViewModel = hiltViewModel(),
    recordingsViewModel: RecordingsViewModel = hiltViewModel(),
) {
    MusicStyled { MusicScreenContent(onBack = onBack, viewModel = viewModel, recordingsViewModel = recordingsViewModel) }
}
