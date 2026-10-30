package com.twinhearts.app.feature.movie.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.movie.domain.genreStyle
import com.twinhearts.app.feature.movie.domain.model.MovieItem
import com.twinhearts.app.feature.movie.domain.model.WatchStatus

/**
 * Why this file exists: the look of the web Watch Together page that the shared design kit does not
 * have: tabs with icons, the Home hero card, colourful genre cards and the two-button mode switch.
 */

private val tabIcons = listOf(Icons.Rounded.Home, Icons.Rounded.VideoLibrary, Icons.Rounded.GridView, Icons.Rounded.PlayCircleOutline)

/** Pill tabs with an icon above the label. The active one is grey glass, like the web page. */
@Composable
fun MovieTabBar(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    Row(
        modifier.fillMaxWidth().movieGlass(pill).padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Column(
                Modifier.weight(1f).heightIn(min = 52.dp)
                    .then(if (selected) Modifier.movieWine(pill) else Modifier.clip(pill))
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(tabIcons[index], null, Modifier.size(18.dp), tint = if (selected) Color.White else colors.inkMuted)
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (selected) Color.White else colors.inkMuted)
            }
        }
    }
}

/** Direct Device / Cloud Storage switch: two outlined buttons with an icon, the chosen one pink-outlined. */
@Composable
fun ModeToggle(cloudMode: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        ModeButton(Icons.Rounded.PhoneAndroid, R.string.movie_mode_direct, !cloudMode, { onChange(false) }, Modifier.weight(1f))
        ModeButton(Icons.Rounded.Cloud, R.string.movie_mode_cloud, cloudMode, { onChange(true) }, Modifier.weight(1f))
    }
}

@Composable
private fun ModeButton(icon: ImageVector, label: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.pill
    Row(
        modifier.heightIn(min = 48.dp)
            .then(if (selected) Modifier.movieWine(shape) else Modifier.movieGlass(shape))
            .clickable(onClick = onClick).padding(horizontal = TwinTheme.spacing.m),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = if (selected) Color.White else colors.ink)
        Text(stringResource(label), color = if (selected) Color.White else colors.ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp), maxLines = 1)
    }
}

/**
 * The top of Home: the hero cards of all your movies slide by themselves every few seconds (swipe works too),
 * with small dots under them. One movie = just the card.
 */
@Composable
fun HeroCarousel(movies: List<MovieItem>, favorites: Set<String>, onWatch: () -> Unit, onOpen: (MovieItem) -> Unit) {
    val slides = movies.take(HERO_MAX_SLIDES)
    if (slides.size <= 1) {
        slides.firstOrNull()?.let { top -> HeroBanner(top, top.id in favorites, onWatch, { onOpen(top) }) }
        return
    }
    val pager = rememberPagerState(pageCount = { slides.size })
    // Slides on its own, but waits while a finger is on it.
    LaunchedEffect(pager, slides.size) {
        while (true) {
            delay(HERO_SLIDE_MS)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % slides.size)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        HorizontalPager(state = pager, key = { slides[it].id }) { page ->
            val movie = slides[page]
            HeroBanner(movie, movie.id in favorites, onWatch, { onOpen(movie) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            repeat(slides.size) { i ->
                val on = i == pager.currentPage
                Box(
                    Modifier.size(if (on) 8.dp else 6.dp).clip(CircleShape)
                        .background(if (on) Color(0xFFFF7AB8) else Color.White.copy(alpha = 0.3f)),
                )
            }
        }
    }
}

/** The big card at the top of Home (web ".mc-hero-slide"). */
@Composable
fun HeroBanner(movie: MovieItem, isFavorite: Boolean, onWatch: () -> Unit, onDetails: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    val art = movie.posterUrl ?: movie.backdropUrl
    Box(
        Modifier.padding(horizontal = TwinTheme.spacing.screenEdge).fillMaxWidth().aspectRatio(HERO_RATIO)
            .movieGlass(shape).background(colors.card),
    ) {
        if (art != null) {
            AsyncImage(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            val g = genreStyle(movie.genre)
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(g.startArgb), Color(g.endArgb)))))
            Text(g.emoji, fontSize = 72.sp, modifier = Modifier.align(Alignment.Center))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
        Box(
            Modifier.align(Alignment.TopEnd).padding(TwinTheme.spacing.m).size(36.dp).clip(TwinTheme.shapes.pill)
                .background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onDetails),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.movie_more_title), tint = Color.White) }
        Column(Modifier.align(Alignment.BottomStart).padding(TwinTheme.spacing.m), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Text(
                stringResource(eyebrow(movie, isFavorite)).uppercase(), color = Color(0xFFFF7AB8),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold,
            )
            Text(movie.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(movie.genre ?: stringResource(R.string.movie_hero_default_genre))
                movie.year?.let { Chip(it) }
                movie.rating?.takeIf { it > 0 }?.let { Chip("⭐ ${it.toInt()}/5") }
                Chip(stringResource(statusLabel(movie.status)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                GlassButton(stringResource(R.string.movie_watch_together), onWatch, Modifier.weight(1f), style = GlassButtonStyle.Primary, leadingIcon = Icons.Rounded.Favorite)
                GlassButton(stringResource(R.string.movie_view_details), onDetails, Modifier.weight(1f), style = GlassButtonStyle.Secondary)
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text, color = Color.White, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.clip(TwinTheme.shapes.pill).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private fun eyebrow(movie: MovieItem, fav: Boolean): Int = when {
    fav -> R.string.movie_hero_loved
    movie.status == WatchStatus.Watching -> R.string.movie_hero_watching
    movie.status == WatchStatus.Watched -> R.string.movie_hero_watched
    else -> R.string.movie_hero_default
}

private fun statusLabel(s: WatchStatus): Int = when (s) {
    WatchStatus.Watchlist -> R.string.movie_status_watchlist
    WatchStatus.Watching -> R.string.movie_status_watching
    WatchStatus.Watched -> R.string.movie_status_watched
}

/** A genre card: the first poster of that genre darkened, or the genre's own two-colour gradient (like the web page). */
@Composable
fun GenreCard(name: String, count: Int, posterUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val g = genreStyle(name)
    val shape = TwinTheme.shapes.card
    // Every genre card is the same tall 9:16 card, with or without a poster, so the grid always looks even.
    Box(
        modifier.aspectRatio(GENRE_RATIO).movieGlass(shape).clickable(onClick = onClick),
    ) {
        if (posterUrl != null) {
            AsyncImage(posterUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.7f)))))
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(g.startArgb), Color(g.endArgb)))))
        }
        Text(g.emoji, fontSize = 30.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 8.dp, vertical = 10.dp)) {
            Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(pluralStringResource(R.plurals.movie_genre_count, count, count), color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, maxLines = 1)
        }
    }
}

private const val HERO_RATIO = 1.08f
private const val GENRE_RATIO = 9f / 16f
private const val HERO_MAX_SLIDES = 12
private const val HERO_SLIDE_MS = 4000L
