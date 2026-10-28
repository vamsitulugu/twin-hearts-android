package com.twinhearts.app.feature.movie.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SectionHeader
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.StatTile
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.movie.data.upload.UploadState
import com.twinhearts.app.feature.movie.domain.LibraryFilter
import com.twinhearts.app.feature.movie.domain.OwnerFilter
import com.twinhearts.app.feature.movie.domain.applyFilter
import com.twinhearts.app.feature.movie.domain.countByStatus
import com.twinhearts.app.feature.movie.domain.model.CloudLibrary
import com.twinhearts.app.feature.movie.domain.model.MovieItem
import com.twinhearts.app.feature.movie.domain.model.SharedMovie
import com.twinhearts.app.feature.movie.domain.model.WatchStatus
import com.twinhearts.app.feature.movie.domain.movieGenres
import com.twinhearts.app.feature.movie.ui.LocalVideo
import com.twinhearts.app.feature.movie.ui.BrowseViewModel
import com.twinhearts.app.feature.movie.ui.OnlineSearch
import com.twinhearts.app.feature.movie.ui.readLocalVideo
import kotlinx.coroutines.launch

/**
 * Why this file exists: the four tabs of the web page (Home, Library, Genres, Watch) in one place.
 * They are stateless; the list comes from [BrowseViewModel] and the pick actions from the room engine.
 */
@Composable
fun MovieTabsContent(
    tab: Int,
    onTab: (Int) -> Unit,
    browseVm: BrowseViewModel,
    cloud: UiState<CloudLibrary>,
    upload: UploadState,
    onLoadCloud: () -> Unit,
    onPickLocal: (uri: String, title: String, durationSec: Int) -> Unit,
    onPickShared: (SharedMovie) -> Unit,
    onUpload: (LocalVideo) -> Unit,
    onCancelUpload: () -> Unit,
    onDismissUpload: () -> Unit,
    onDeleteShared: (SharedMovie) -> Unit,
) {
    val movies by browseVm.movies.collectAsState()
    val online by browseVm.online.collectAsState()
    val favorites by browseVm.favorites.collectAsState()
    val role by browseVm.role.collectAsState()
    var selected by remember { mutableStateOf<MovieItem?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var genre by rememberSaveable { mutableStateOf<String?>(null) }
    var libraryFilter by rememberSaveable { mutableStateOf(LibraryFilter.All) }
    val edge = TwinTheme.spacing.screenEdge

    Column(Modifier.fillMaxSize()) {
        MovieTabBar(
            options = listOf(
                stringResource(R.string.movie_tab_home), stringResource(R.string.movie_tab_library),
                stringResource(R.string.movie_tab_genres), stringResource(R.string.movie_tab_watch),
            ),
            selectedIndex = tab,
            onSelect = onTab,
            modifier = Modifier.padding(horizontal = edge, vertical = TwinTheme.spacing.s),
        )
        if (tab == 3) {
            WatchTab(cloud, upload, onLoadCloud, onPickLocal, onPickShared, onUpload, onCancelUpload, onDismissUpload, onDeleteShared)
        } else when (val m = movies) {
            UiState.Loading -> LoadingSkeleton(Modifier.padding(edge))
            is UiState.Error -> ErrorState(message = m.error.asText(), onRetry = browseVm::load)
            else -> {
                val list = if (m is UiState.Content) m.data else emptyList()
                when (tab) {
                    0 -> HomeTab(list, favorites, browseVm::toggleFavorite, onWatch = { onTab(3) }, onSeeAll = { libraryFilter = it; onTab(1) }) { selected = it }
                    1 -> LibraryTab(
                        list = list, online = online, genre = genre, onClearGenre = { genre = null },
                        filter = libraryFilter, onFilter = { libraryFilter = it },
                        onOpen = { selected = it }, onSearchOnline = browseVm::searchOnline,
                        onClearOnline = browseVm::clearSearch,
                        favorites = favorites, role = role, onToggleFavorite = browseVm::toggleFavorite,
                        onAddMovie = { adding = true },
                    )
                    else -> GenresTab(list) { genre = it; onTab(1) }
                }
            }
        }
    }
    selected?.let { movie ->
        MovieDetailSheet(
            movie = movie,
            onDismiss = { selected = null },
            onStatus = { browseVm.setStatus(movie, it); selected = movie.copy(status = it) },
            onRate = { browseVm.setRating(movie, it); selected = movie.copy(rating = it.toDouble()) },
            onRemove = { browseVm.remove(movie); selected = null },
            onAdd = { browseVm.add(movie); selected = null },
            isFavorite = movie.id in favorites,
            onFavorite = { browseVm.toggleFavorite(movie) },
        )
    }
    if (adding) {
        AddMovieSheet(
            onSave = { title, year, genre, status, stars, owner ->
                browseVm.addManual(title, year, genre, status, stars, owner) { adding = false }
            },
            onDismiss = { adding = false },
        )
    }
}

// ── Home ──

@Composable
private fun HomeTab(
    list: List<MovieItem>,
    favorites: Set<String>,
    onToggleFavorite: (MovieItem) -> Unit,
    onWatch: () -> Unit,
    onSeeAll: (LibraryFilter) -> Unit,
    onOpen: (MovieItem) -> Unit,
) {
    if (list.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.movie_home_empty_title),
            message = stringResource(R.string.movie_home_empty_message),
            icon = Icons.Rounded.Movie,
        )
        return
    }
    val rows = listOf(
        Triple(R.string.movie_home_favorites, list.filter { it.id in favorites }, LibraryFilter.Favorites),
        Triple(R.string.movie_home_watched_together, list.filter { it.status == WatchStatus.Watched }, LibraryFilter.Watched),
        Triple(R.string.movie_home_watching, list.filter { it.status == WatchStatus.Watching }, LibraryFilter.Watching),
        Triple(R.string.movie_home_watchlist, list.filter { it.status == WatchStatus.Watchlist }, LibraryFilter.Watchlist),
    ).filter { it.second.isNotEmpty() }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l), modifier = Modifier.fillMaxSize()) {
        item { list.first().let { top -> HeroBanner(top, top.id in favorites, onWatch, { onOpen(top) }) } }
        items(rows, key = { it.first }) { (title, movies, seeAll) ->
            Column {
                SectionHeader(
                    stringResource(title), Modifier.padding(horizontal = TwinTheme.spacing.screenEdge),
                    actionLabel = stringResource(R.string.movie_see_all), onAction = { onSeeAll(seeAll) },
                )
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = TwinTheme.spacing.screenEdge),
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                ) {
                    items(movies, key = { it.id }) { PosterCard(it, { onOpen(it) }, Modifier.width(112.dp), favorite = it.id in favorites, onToggleFavorite = { onToggleFavorite(it) }) }
                }
            }
        }
    }
}

// ── Library ──

@Composable
private fun LibraryTab(
    list: List<MovieItem>,
    online: OnlineSearch,
    genre: String?,
    onClearGenre: () -> Unit,
    filter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    onOpen: (MovieItem) -> Unit,
    onSearchOnline: (String) -> Unit,
    onClearOnline: () -> Unit,
    favorites: Set<String>,
    role: String?,
    onToggleFavorite: (MovieItem) -> Unit,
    onAddMovie: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var owner by rememberSaveable { mutableStateOf(OwnerFilter.Everyone) }
    var onlineMode by rememberSaveable { mutableStateOf(false) }
    val shown = list.applyFilter(filter, genre, if (onlineMode) "" else query, owner, role, favorites)
    val edge = TwinTheme.spacing.screenEdge

    LazyVerticalGrid(
        columns = GridCells.Adaptive(108.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = edge, vertical = TwinTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
                TwinTextField(
                    value = query, onValueChange = { query = it },
                    label = stringResource(if (onlineMode) R.string.movie_online_hint else R.string.movie_search_hint),
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                    onImeAction = { if (onlineMode) onSearchOnline(query) },
                    modifier = Modifier.weight(1f),
                )
                GlassButton(stringResource(R.string.movie_add_short), onAddMovie, leadingIcon = Icons.Rounded.Add)
                GlassButton(
                    stringResource(R.string.movie_search_online_btn),
                    { if (onlineMode) onSearchOnline(query) else onlineMode = true },
                    leadingIcon = Icons.Rounded.Public,
                )
            }
        }
        if (onlineMode) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                TwinFilterChip(stringResource(R.string.movie_tab_library) + " ✕", true, { onlineMode = false; onClearOnline() })
            }
            onlineItems(online, onOpen)
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) { Stats(list, favorites) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    LibraryFilter.entries.forEach { f ->
                        TwinFilterChip(stringResource(f.label()), filter == f, { onFilter(f) })
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    OwnerFilter.entries.forEach { o -> TwinFilterChip(stringResource(o.label()), owner == o, { owner = o }) }
                }
            }
            if (genre != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    TwinFilterChip("$genre ✕", true, onClearGenre)
                }
            }
            if (shown.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(stringResource(R.string.movie_library_empty_title), message = stringResource(R.string.movie_library_empty_message), icon = Icons.Rounded.Movie)
                }
            }
            items(shown, key = { it.id }) { PosterCard(it, { onOpen(it) }, favorite = it.id in favorites, onToggleFavorite = { onToggleFavorite(it) }) }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.onlineItems(online: OnlineSearch, onOpen: (MovieItem) -> Unit) {
    when (online) {
        OnlineSearch.Idle -> Unit
        OnlineSearch.Loading -> item(span = { GridItemSpan(maxLineSpan) }) { LoadingSkeleton(lines = 2) }
        OnlineSearch.Failed -> item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.movie_online_failed), color = TwinTheme.colors.error, style = MaterialTheme.typography.bodyMedium)
        }
        is OnlineSearch.Results -> {
            if (online.items.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.movie_online_none), color = TwinTheme.colors.inkMuted, style = MaterialTheme.typography.bodyMedium)
            }
            items(online.items, key = { it.tmdbId ?: it.title }) { PosterCard(it, { onOpen(it) }) }
        }
    }
}

private fun OwnerFilter.label(): Int = when (this) {
    OwnerFilter.Everyone -> R.string.movie_owner_all
    OwnerFilter.Mine -> R.string.movie_owner_chip_mine
    OwnerFilter.Partner -> R.string.movie_owner_chip_partner
    OwnerFilter.Together -> R.string.movie_owner_chip_together
}

private fun LibraryFilter.label(): Int = when (this) {
    LibraryFilter.All -> R.string.movie_filter_all
    LibraryFilter.Recent -> R.string.movie_filter_recent
    LibraryFilter.Favorites -> R.string.movie_filter_favorites
    LibraryFilter.Watchlist -> R.string.movie_filter_watchlist
    LibraryFilter.Watching -> R.string.movie_filter_watching
    LibraryFilter.Watched -> R.string.movie_filter_watched
}

@Composable
private fun Stats(list: List<MovieItem>, favorites: Set<String>) {
    // Same as the web page: the average counts every movie (unrated ones as 0); "—" when the list is empty.
    val avg = if (list.isEmpty()) "—" else "%.1f".format(java.util.Locale.US, list.sumOf { it.rating ?: 0.0 } / list.size)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatTile(list.size.toString(), stringResource(R.string.movie_stat_total), Modifier.weight(1f))
        StatTile(list.countByStatus(WatchStatus.Watched).toString(), stringResource(R.string.movie_stat_watched), Modifier.weight(1f))
        StatTile(list.countByStatus(WatchStatus.Watchlist).toString(), stringResource(R.string.movie_stat_watchlist), Modifier.weight(1f))
        StatTile(list.count { it.id in favorites }.toString(), stringResource(R.string.movie_stat_favorites), Modifier.weight(1f))
        StatTile("⭐$avg", stringResource(R.string.movie_stat_avg), Modifier.weight(1f))
    }
}

// ── Genres ──

@Composable
private fun GenresTab(list: List<MovieItem>, onPick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(TwinTheme.spacing.screenEdge),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(stringResource(R.string.movie_genres_title)) }
        items(movieGenres, key = { it }) { name ->
            val inGenre = list.applyFilter(LibraryFilter.All, name, "")
            GenreCard(name, inGenre.size, inGenre.firstOrNull { it.posterUrl != null }?.posterUrl, { onPick(name) }, Modifier.fillMaxWidth())
        }
    }
}

// ── Watch (pick a movie) ──

@Composable
private fun WatchTab(
    cloud: UiState<CloudLibrary>,
    upload: UploadState,
    onLoadCloud: () -> Unit,
    onPickLocal: (String, String, Int) -> Unit,
    onPickShared: (SharedMovie) -> Unit,
    onUpload: (LocalVideo) -> Unit,
    onCancelUpload: () -> Unit,
    onDismissUpload: () -> Unit,
    onDeleteShared: (SharedMovie) -> Unit,
) {
    var cloudMode by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(cloudMode) { if (cloudMode) onLoadCloud() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(TwinTheme.spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
    ) {
        ModeToggle(cloudMode) { cloudMode = it }
        if (cloudMode) {
            CloudPane(cloud, upload, onLoadCloud, onPickShared, onUpload, onCancelUpload, onDismissUpload, onDeleteShared)
        } else {
            DirectPane(onPickLocal)
        }
    }
}

@Composable
private fun DirectPane(onPick: (String, String, Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val video = readLocalVideo(context, uri)
            if (video == null) Toast.makeText(context, R.string.movie_toast_bad_video, Toast.LENGTH_SHORT).show()
            else onPick(video.uri, video.title, video.durationSec)
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(64.dp).clip(TwinTheme.shapes.pill).background(TwinTheme.colors.glass)
                .border(1.dp, TwinTheme.colors.lineSoft, TwinTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Movie, null, tint = TwinTheme.colors.accent) }
        Text(
            stringResource(R.string.movie_direct_title), color = TwinTheme.colors.ink, style = MaterialTheme.typography.titleLarge,
            fontFamily = SerifFamily, modifier = Modifier.padding(top = TwinTheme.spacing.m),
        )
        Text(
            stringResource(R.string.movie_direct_hint), color = TwinTheme.colors.inkMuted, style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(vertical = TwinTheme.spacing.s),
        )
        GlassButton(stringResource(R.string.movie_choose), { launcher.launch(arrayOf("video/*")) }, Modifier.padding(top = TwinTheme.spacing.s))
    }
}

// ── Shared poster tile ──

@Composable
fun PosterCard(
    movie: MovieItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
) {
    Column(modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(TwinTheme.shapes.small).background(TwinTheme.colors.card),
            contentAlignment = Alignment.Center,
        ) {
            if (movie.posterUrl == null) {
                Icon(Icons.Rounded.Movie, null, Modifier.size(32.dp), tint = TwinTheme.colors.inkFaint)
            } else {
                AsyncImage(movie.posterUrl, movie.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            // The ⭐ is kept on this phone only (like the web page), so it shows only for saved movies.
            if (onToggleFavorite != null && movie.id.isNotEmpty()) {
                Text(
                    if (favorite) "★" else "☆", color = TwinTheme.colors.accent, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.TopEnd).clickable(onClick = onToggleFavorite).padding(TwinTheme.spacing.s),
                )
            }
        }
        Text(movie.title, color = TwinTheme.colors.ink, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
