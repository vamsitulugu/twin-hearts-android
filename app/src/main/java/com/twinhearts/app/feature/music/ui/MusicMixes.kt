package com.twinhearts.app.feature.music.ui

import com.twinhearts.app.feature.music.ui.components.musicWine
import com.twinhearts.app.feature.music.ui.components.musicGlass
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.music.domain.MixKind
import com.twinhearts.app.feature.music.domain.SmartMix
import com.twinhearts.app.feature.music.domain.model.Song
import kotlinx.coroutines.delay

/*
 * Why this file exists: the two Music home pieces the first version left out: the "Made for Us" smart
 * mixes row, and the swipeable "Suggested for you" hero. Both follow public/music/library.js.
 */

/** Name of a mix, e.g. "Loved by Both". [partner] is used by "Their Favourites". */
@Composable
internal fun mixName(kind: MixKind, partner: String): String = when (kind) {
    MixKind.Ours -> stringResource(R.string.music_mix_ours)
    MixKind.Both -> stringResource(R.string.music_mix_both)
    MixKind.Theirs -> stringResource(R.string.music_mix_theirs, partner)
    MixKind.Repeat -> stringResource(R.string.music_mix_repeat)
    MixKind.New -> stringResource(R.string.music_mix_new)
    MixKind.Sing -> stringResource(R.string.music_mix_sing)
}

@Composable
private fun mixSubtitle(mix: SmartMix): String = when (mix.kind) {
    MixKind.Ours -> stringResource(R.string.music_mix_ours_sub)
    MixKind.Both -> stringResource(R.string.music_mix_both_sub, mix.songs.size)
    MixKind.Theirs -> stringResource(R.string.music_mix_theirs_sub, mix.songs.size)
    MixKind.Repeat -> stringResource(R.string.music_mix_repeat_sub)
    MixKind.New -> stringResource(R.string.music_mix_new_sub)
    MixKind.Sing -> stringResource(R.string.music_mix_sing_sub)
}

private fun mixIcon(kind: MixKind): ImageVector = when (kind) {
    MixKind.Ours -> Icons.Rounded.AutoAwesome
    MixKind.Both -> Icons.Rounded.Favorite
    MixKind.Theirs -> Icons.Rounded.Person
    MixKind.Repeat -> Icons.Rounded.Refresh
    MixKind.New -> Icons.Rounded.Add
    MixKind.Sing -> Icons.Rounded.Mic
}

/** The "Made for Us" row: wide cover cards with the name and a one-line subtitle on the glass. */
@Composable
internal fun MadeForUsRow(mixes: List<SmartMix>, partner: String, onOpen: (MixKind) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        items(mixes, key = { it.kind.name }) { mix ->
            MixCard(mix, mixName(mix.kind, partner), mixSubtitle(mix)) { onOpen(mix.kind) }
        }
    }
}

@Composable
private fun MixCard(mix: SmartMix, name: String, subtitle: String, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    val cover = mix.songs.firstNotNullOfOrNull { it.coverUrl?.takeIf { url -> url.isNotBlank() } }
    Box(
        modifier = Modifier
            .width(MIX_WIDTH)
            .height(MIX_HEIGHT)
            .musicGlass(shape)
            .clickable(onClick = onClick),
    ) {
        if (cover != null) {
            AsyncImage(model = cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        // Wine tint over the whole card, darker towards the text.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(colors.primaryStart.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.78f)),
                ),
            ),
        )
        Box(
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp).size(30.dp).musicGlass(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(mixIcon(mix.kind), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 10.dp, end = 56.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(36.dp).musicWine(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.music_action_play), tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Swipeable "Suggested for you" hero: covers of up to 12 songs, hearted ones first. It moves on by
 * itself every 4.5 s, and waits 7 s after the person touches it (same timing as the web).
 */
@Composable
internal fun HeroSlider(picks: List<Song>, onPlay: (Song) -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val pager = rememberPagerState { picks.size }
    var lastTouch by remember { mutableLongStateOf(0L) }
    var autoMoving by remember { androidx.compose.runtime.mutableStateOf(false) }
    val scrolling = pager.isScrollInProgress
    LaunchedEffect(scrolling) {
        if (scrolling && !autoMoving) lastTouch = System.currentTimeMillis()
    }
    val count by rememberUpdatedState(picks.size)
    LaunchedEffect(Unit) {
        while (true) {
            delay(HERO_ADVANCE_MS)
            if (count < 2 || System.currentTimeMillis() - lastTouch < HERO_PAUSE_MS) continue
            autoMoving = true
            pager.animateScrollToPage((pager.currentPage + 1) % count)
            autoMoving = false
        }
    }
    Column(modifier) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(HERO_HEIGHT), key = { picks[it].id }) { page ->
            val song = picks[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(TwinTheme.shapes.card)
                    .background(colors.card)
                    .clickable { onPlay(song) },
            ) {
                AsyncImage(model = song.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.78f), Color.Black.copy(alpha = 0.15f)))))
                Column(Modifier.align(Alignment.CenterStart).padding(start = TwinTheme.spacing.l, end = 88.dp)) {
                    Text(
                        stringResource(if (song.liked) R.string.music_hero_loved else R.string.music_hero_suggested),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.pinkSoft,
                    )
                    Text(song.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = TwinTheme.spacing.l)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colors.primaryBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.music_action_play), tint = colors.ink, modifier = Modifier.size(30.dp))
                }
            }
        }
        if (picks.size > 1) {
            Row(Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s), horizontalArrangement = Arrangement.Center) {
                repeat(picks.size) { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (i == pager.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (i == pager.currentPage) colors.pinkSoft else colors.inkFaint),
                    )
                }
            }
            Spacer(Modifier.height(0.dp))
        }
    }
}

private val MIX_WIDTH = 220.dp
private val MIX_HEIGHT = 132.dp
private val HERO_HEIGHT = 168.dp
private const val HERO_ADVANCE_MS = 4_500L
private const val HERO_PAUSE_MS = 7_000L
