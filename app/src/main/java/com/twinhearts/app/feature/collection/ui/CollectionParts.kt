package com.twinhearts.app.feature.collection.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.collection.domain.CollectionLogic
import com.twinhearts.app.feature.collection.domain.model.CollectionTab
import com.twinhearts.app.feature.collection.domain.model.PhotoFilter
import com.twinhearts.app.feature.collection.domain.model.PhotoItem
import com.twinhearts.app.feature.collection.domain.model.VoiceFilter
import com.twinhearts.app.feature.collection.domain.model.VoiceItem
import com.twinhearts.app.feature.collection.domain.model.VoiceSort
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val Gold = Color(0xFFF5B82E)

/** Underlined tab row, as in the reference (not the pill tabs used elsewhere). */
@Composable
fun CollectionTabs(selected: CollectionTab, onSelect: (CollectionTab) -> Unit) {
    val colors = TwinTheme.colors
    val tabs = listOf(
        CollectionTab.Voice to R.string.collection_tab_voice,
        CollectionTab.Library to R.string.collection_tab_library,
        CollectionTab.Photos to R.string.collection_tab_photos,
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = TwinTheme.spacing.s),
        ) {
            tabs.forEach { (tab, label) ->
                val on = tab == selected
                Column(
                    modifier = Modifier
                        .clickable { onSelect(tab) }
                        .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (on) colors.ink else colors.inkMuted,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Box(
                        Modifier.padding(top = 10.dp).height(2.dp).fillMaxWidth()
                            .background(if (on) colors.accent else Color.Transparent),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.lineSoft))
    }
}

/** 2 x 2 stat cards. */
@Composable
fun StatGrid(stats: List<Triple<String, String, Color?>>, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        stats.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                pair.forEach { (value, label, tint) ->
                    GlassCard(modifier = Modifier.weight(1f), contentPadding = TwinTheme.spacing.m) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(value, style = MaterialTheme.typography.headlineSmall, color = tint ?: colors.ink)
                            Text(
                                label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.inkMuted,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Filter chips + (voice only) sort menu and Select toggle. */
@Composable
fun VoiceToolbar(
    filter: VoiceFilter,
    sort: VoiceSort,
    selectMode: Boolean,
    onFilter: (VoiceFilter) -> Unit,
    onSort: (VoiceSort) -> Unit,
    onToggleSelect: () -> Unit,
) {
    val items = listOf(
        VoiceFilter.All to R.string.collection_filter_all,
        VoiceFilter.Mine to R.string.collection_filter_my_voice,
        VoiceFilter.Partner to R.string.collection_filter_partner,
        VoiceFilter.Favorites to R.string.collection_filter_favorites,
        VoiceFilter.Recent to R.string.collection_filter_recent,
        VoiceFilter.Longest to R.string.collection_filter_longest,
    )
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = TwinTheme.spacing.l),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            items.forEach { (f, label) -> TwinFilterChip(stringResource(label), filter == f, { onFilter(f) }) }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.l),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SortMenu(sort, onSort)
            TwinFilterChip(
                label = stringResource(if (selectMode) R.string.collection_selecting else R.string.collection_select),
                selected = selectMode,
                onClick = onToggleSelect,
            )
        }
    }
}

@Composable
private fun SortMenu(sort: VoiceSort, onSort: (VoiceSort) -> Unit) {
    val colors = TwinTheme.colors
    var open by remember { mutableStateOf(false) }
    val options = listOf(
        VoiceSort.Newest to R.string.collection_sort_newest,
        VoiceSort.Oldest to R.string.collection_sort_oldest,
        VoiceSort.Longest to R.string.collection_sort_longest,
        VoiceSort.Shortest to R.string.collection_sort_shortest,
        VoiceSort.Favorites to R.string.collection_sort_favorites,
    )
    Box {
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(TwinTheme.shapes.pill)
                .background(colors.glass)
                .border(1.dp, colors.line, TwinTheme.shapes.pill)
                .clickable { open = true }
                .padding(start = TwinTheme.spacing.l, end = TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(options.first { it.first == sort }.second),
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
            )
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = colors.inkMuted)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (s, label) ->
                DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { onSort(s); open = false })
            }
        }
    }
}

@Composable
fun PhotoToolbar(filter: PhotoFilter, onFilter: (PhotoFilter) -> Unit) {
    val items = listOf(
        PhotoFilter.All to R.string.collection_filter_all,
        PhotoFilter.Photos to R.string.collection_filter_photos,
        PhotoFilter.Videos to R.string.collection_filter_videos,
        PhotoFilter.Favorites to R.string.collection_filter_favorites,
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = TwinTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        items.forEach { (f, label) -> TwinFilterChip(stringResource(label), filter == f, { onFilter(f) }) }
    }
}

/** Bar shown while rows are ticked: count + Save to Library (voice) + Delete + Cancel. */
@Composable
fun BulkBar(count: Int, canSave: Boolean, onSave: () -> Unit, onDelete: () -> Unit, onCancel: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TwinTheme.spacing.l)
            .clip(TwinTheme.shapes.card)
            .background(colors.card)
            .border(1.dp, colors.line, TwinTheme.shapes.card)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.collection_selected_count, count),
            style = MaterialTheme.typography.titleSmall,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        if (canSave) {
            IconButton(onClick = onSave) {
                Icon(Icons.Rounded.BookmarkAdd, stringResource(R.string.collection_save_to_library), tint = colors.pinkSoft)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.collection_delete), tint = colors.inkMuted)
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Rounded.Close, stringResource(R.string.collection_cancel), tint = colors.inkMuted)
        }
    }
}

/** Voice list grouped by day (Voice and Library tabs share it). */
@Composable
fun VoiceList(
    voices: List<VoiceItem>,
    isLibrary: Boolean,
    favorites: Set<String>,
    libraryIds: Set<String>,
    selectMode: Boolean,
    selected: Set<String>,
    playingId: String?,
    myRole: String,
    myName: String,
    partnerName: String,
    onPlay: (VoiceItem) -> Unit,
    onFavorite: (String) -> Unit,
    onSave: (VoiceItem) -> Unit,
    onDelete: (String) -> Unit,
    onTapRow: (String) -> Unit,
    onLongPressRow: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = remember(voices) { CollectionLogic.voicesByDay(voices) }
    val today = remember { LocalDate.now().toString() }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = TwinTheme.spacing.l, end = TwinTheme.spacing.l, bottom = 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        grouped.forEach { (day, list) ->
            item(key = "day_$day") {
                Text(
                    if (day == today) stringResource(R.string.collection_today) else prettyDate(day),
                    style = MaterialTheme.typography.labelLarge,
                    color = TwinTheme.colors.inkMuted,
                    modifier = Modifier.padding(top = TwinTheme.spacing.s),
                )
            }
            items(list, key = { (if (isLibrary) "l_" else "v_") + it.id }) { v ->
                VoiceRow(
                    item = v,
                    mine = v.by == myRole,
                    name = if (v.by == myRole) myName else partnerName,
                    favorite = v.id in favorites,
                    inLibrary = !isLibrary && v.id in libraryIds,
                    isLibrary = isLibrary,
                    selectMode = selectMode,
                    checked = v.id in selected,
                    playing = v.id == playingId,
                    onPlay = { onPlay(v) },
                    onFavorite = { onFavorite(v.id) },
                    onSave = { onSave(v) },
                    onDelete = { onDelete(v.id) },
                    onTap = { onTapRow(v.id) },
                    onLongPress = { onLongPressRow(v.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VoiceRow(
    item: VoiceItem,
    mine: Boolean,
    name: String,
    favorite: Boolean,
    inLibrary: Boolean,
    isLibrary: Boolean,
    selectMode: Boolean,
    checked: Boolean,
    playing: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    val time = remember(item.createdAt) { prettyTime(item.createdAt) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (playing) colors.card else colors.glass)
            .border(BorderStroke(1.dp, if (playing || checked) colors.accent else colors.line), shape)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectMode) {
            Box(
                Modifier.padding(end = TwinTheme.spacing.s).size(22.dp).clip(CircleShape)
                    .background(if (checked) colors.accent else Color.Transparent)
                    .border(1.5.dp, if (checked) colors.accent else colors.inkFaint, CircleShape),
                contentAlignment = Alignment.Center,
            ) { if (checked) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        }
        Avatar(name = name, size = 36.dp)
        IconButton(onClick = onPlay, modifier = Modifier.size(44.dp)) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(colors.primaryBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    stringResource(if (playing) R.string.collection_pause else R.string.collection_play),
                    tint = Color.White,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 1)
                if (inLibrary) {
                    Text(
                        stringResource(R.string.collection_saved_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.pinkSoft,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Text(
                "$time · ${CollectionLogic.durationLabel(item.durationSec)}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.inkMuted,
            )
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
            Icon(
                if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                stringResource(R.string.collection_favorite),
                tint = if (favorite) Gold else colors.inkMuted,
            )
        }
        if (!isLibrary && !inLibrary) {
            IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.BookmarkAdd, stringResource(R.string.collection_save_to_library), tint = colors.inkMuted)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.collection_delete), tint = colors.inkMuted)
        }
    }
}

/** Photo grid grouped by month, 3 columns. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGrid(
    photos: List<PhotoItem>,
    favorites: Set<String>,
    onOpen: (Int) -> Unit,
    onFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = remember(photos) { CollectionLogic.photosByMonth(photos) }
    val indexById = remember(photos) { photos.withIndex().associate { it.value.id to it.index } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = TwinTheme.spacing.l, end = TwinTheme.spacing.l, bottom = 120.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grouped.forEach { (month, list) ->
            item(key = "m_$month", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.m, bottom = TwinTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        prettyMonth(month),
                        style = MaterialTheme.typography.titleSmall,
                        color = TwinTheme.colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (list.size == 1) stringResource(R.string.collection_item_count_one)
                        else stringResource(R.string.collection_items_count, list.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = TwinTheme.colors.inkMuted,
                    )
                }
            }
            items(list, key = { it.id }) { p ->
                PhotoTile(p, p.id in favorites, onClick = { onOpen(indexById[p.id] ?: 0) }, onFavorite = { onFavorite(p.id) })
            }
        }
    }
}

@Composable
private fun PhotoTile(photo: PhotoItem, favorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit) {
    val colors = TwinTheme.colors
    Box(
        Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(colors.glass).clickable(onClick = onClick),
    ) {
        AsyncImage(photo.url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (photo.isVideo) {
            Text(
                stringResource(R.string.collection_video_badge),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
            Icon(
                if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                stringResource(R.string.collection_favorite),
                tint = if (favorite) Gold else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Full-screen swipeable viewer over the visible photos. */
@Composable
fun PhotoLightbox(
    photos: List<PhotoItem>,
    startIndex: Int,
    favorites: Set<String>,
    onFavorite: (String) -> Unit,
    onDownload: (PhotoItem) -> Unit,
    onOpenVideo: (PhotoItem) -> Unit,
    onClose: () -> Unit,
) {
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))) { photos.size }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, modifier = Modifier.fillMaxSize()) { page ->
                val p = photos[page]
                Box(Modifier.fillMaxSize().clickable(enabled = p.isVideo) { onOpenVideo(p) }, contentAlignment = Alignment.Center) {
                    AsyncImage(p.url, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    if (p.isVideo) {
                        Icon(Icons.Rounded.PlayArrow, stringResource(R.string.collection_play), tint = Color.White, modifier = Modifier.size(72.dp))
                    }
                }
            }
            val current = photos.getOrNull(pager.currentPage)
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.collection_close), tint = Color.White) }
                Text(
                    if (photos.size > 1) "${pager.currentPage + 1} / ${photos.size}" else "",
                    color = Color.White, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                )
                if (current != null) {
                    IconButton(onClick = { onFavorite(current.id) }) {
                        val fav = current.id in favorites
                        Icon(if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder, stringResource(R.string.collection_favorite), tint = if (fav) Gold else Color.White)
                    }
                    IconButton(onClick = { onDownload(current) }) {
                        Icon(Icons.Rounded.Download, stringResource(R.string.collection_download), tint = Color.White)
                    }
                }
            }
            val date = current?.date.orEmpty()
            if (date.isNotEmpty()) {
                Text(
                    prettyDate(date), color = Color.White, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(TwinTheme.spacing.xl),
                )
            }
        }
    }
}

private fun prettyDate(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
} catch (e: Exception) { iso }

private fun prettyMonth(ym: String): String = try {
    LocalDate.parse("$ym-01").format(DateTimeFormatter.ofPattern("MMMM yyyy"))
} catch (e: Exception) { ym }

/** Local clock time from an ISO instant, e.g. "10:54 AM". */
private fun prettyTime(iso: String): String = try {
    java.time.OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
} catch (e: Exception) {
    try {
        java.time.LocalDateTime.parse(iso.removeSuffix("Z")).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    } catch (e2: Exception) { "" }
}
