package com.twinhearts.app.feature.games.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.games.domain.model.GameId
import com.twinhearts.app.feature.games.domain.model.PlayerProfile
import com.twinhearts.app.feature.games.domain.model.Players
import com.twinhearts.app.feature.games.ui.components.GameBadge
import com.twinhearts.app.feature.games.ui.components.GameCatalog
import com.twinhearts.app.feature.games.ui.components.GameInfo
import com.twinhearts.app.feature.games.ui.components.gameBrush
import com.twinhearts.app.feature.games.ui.play.LudoScreen
import com.twinhearts.app.feature.games.ui.play.MemoryScreen
import com.twinhearts.app.feature.games.ui.play.TicTacToeScreen
import com.twinhearts.app.feature.games.ui.play.TruthOrDareScreen

/** The Games page: the hub, and whichever game is open on top of it. */
@Composable
fun GamesScreen(onBack: () -> Unit, viewModel: GamesViewModel = hiltViewModel()) {
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    val open = openKey?.let { GameId.fromKey(it) }
    val exit = { openKey = null }

    when (open) {
        GameId.TruthOrDare -> TruthOrDareScreen(onExit = exit)
        GameId.Ludo -> LudoScreen(onExit = exit)
        GameId.TicTacToe -> TicTacToeScreen(onExit = exit)
        GameId.Memory -> MemoryScreen(onExit = exit)
        else -> HubScreen(
            viewModel = viewModel,
            onBack = onBack,
            onOpen = { game ->
                viewModel.markOpened(game)
                openKey = game.key
            },
        )
    }
}

@Composable
private fun HubScreen(viewModel: GamesViewModel, onBack: () -> Unit, onOpen: (GameId) -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val soon = stringResource(R.string.games_soon)

    LaunchedEffect(viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.run() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TwinTopBar(title = stringResource(R.string.games_title), showSidebarButton = true)
        when (val current = ui) {
            UiState.Loading, UiState.Empty -> LoadingSkeleton(modifier = Modifier.padding(TwinTheme.spacing.screenEdge))
            is UiState.Error -> ErrorState(message = current.error.asText(), onRetry = viewModel::retry)
            is UiState.Content -> HubContent(
                players = current.data,
                onFavorite = viewModel::toggleFavorite,
                onOpen = { info ->
                    if (info.playable) onOpen(info.id) else Toast.makeText(context, soon, Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}

@Composable
private fun HubContent(players: Players, onFavorite: (GameId) -> Unit, onOpen: (GameInfo) -> Unit) {
    val colors = TwinTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = TwinTheme.spacing.screenEdge,
            vertical = TwinTheme.spacing.m,
        ),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.games_hero_title), style = MaterialTheme.typography.headlineMedium, color = colors.ink, textAlign = TextAlign.Center)
                Text(stringResource(R.string.games_hero_sub), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RankCard(
                    name = stringResource(R.string.games_you_suffix, players.myName),
                    avatar = players.myAvatar,
                    profile = players.me,
                    online = true,
                    playingTitle = null,
                    modifier = Modifier.weight(1f),
                )
                Text(stringResource(R.string.games_vs), style = MaterialTheme.typography.titleSmall, color = colors.inkFaint)
                val playing = GameId.fromKey(players.partnerPresence.game.orEmpty())
                RankCard(
                    name = players.partnerName,
                    avatar = players.partnerAvatar,
                    profile = players.partner,
                    online = players.partnerOnline,
                    playingTitle = if (players.partnerOnline && playing != null) stringResource(GameCatalog.of(playing).title) else null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                Text(stringResource(R.string.games_section_games), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.lineSoft))
            }
        }
        items(GameCatalog.all.chunked(2)) { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                pair.forEach { info ->
                    GameCard(
                        info = info,
                        favorite = info.id.key in players.me.favorites,
                        continuing = info.id.key in players.me.history,
                        partnerHere = players.partnerOnline && players.partnerPresence.game == info.id.key,
                        partnerName = players.partnerName,
                        onFavorite = { onFavorite(info.id) },
                        onOpen = { onOpen(info) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RankCard(
    name: String,
    avatar: String?,
    profile: PlayerProfile,
    online: Boolean,
    playingTitle: String?,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.line, shape)
            .padding(TwinTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Box {
                Avatar(name = name, size = 36.dp, imageUrl = avatar)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (online) colors.success else colors.inkFaint)
                        .border(1.dp, colors.background, CircleShape),
                )
            }
            Column {
                Text(name, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.games_level_xp, profile.level, profile.xp), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(TwinTheme.shapes.pill).background(colors.lineSoft)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(profile.levelProgress.coerceAtLeast(0.02f))
                    .height(6.dp)
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.primaryBrush),
            )
        }
        Text(stringResource(R.string.games_stat_coins, profile.coins), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        Text(stringResource(R.string.games_stat_wins, profile.wins), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        Text(stringResource(R.string.games_stat_streak, profile.streakDays), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        if (playingTitle != null) {
            Text(stringResource(R.string.games_playing_now, playingTitle), style = MaterialTheme.typography.labelMedium, color = colors.success)
        }
    }
}

@Composable
private fun GameCard(
    info: GameInfo,
    favorite: Boolean,
    continuing: Boolean,
    partnerHere: Boolean,
    partnerName: String,
    onFavorite: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    val favLabel = stringResource(if (favorite) R.string.games_fav_remove else R.string.games_fav_add)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onOpen),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(104.dp).background(gameBrush(info.id)),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(info.emoji), fontSize = 44.sp)
            val badge = when {
                !info.playable -> R.string.games_soon_badge
                info.badge == GameBadge.New -> R.string.games_badge_new
                info.badge == GameBadge.Trending -> R.string.games_badge_trending
                else -> null
            }
            if (badge != null) {
                Text(
                    stringResource(badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.ink,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(TwinTheme.spacing.s)
                        .clip(TwinTheme.shapes.pill)
                        .background(colors.scrim)
                        .padding(horizontal = TwinTheme.spacing.s, vertical = 2.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(TwinTheme.shapes.minTouch)
                    .clip(CircleShape)
                    .clickable(onClick = onFavorite)
                    .semantics { contentDescription = favLabel },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (favorite) "❤️" else "🤍", fontSize = 18.sp)
            }
            if (partnerHere) {
                Text(
                    stringResource(R.string.games_partner_is_playing, partnerName),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.success,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(TwinTheme.spacing.s)
                        .clip(TwinTheme.shapes.pill)
                        .background(colors.scrim)
                        .padding(horizontal = TwinTheme.spacing.s, vertical = 2.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(TwinTheme.spacing.m), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Text(stringResource(info.title), style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(info.description), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Text(
                info.tags.map { stringResource(it) }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = colors.inkFaint,
            )
            Text(
                stringResource(if (continuing) R.string.games_continue else R.string.games_play_now),
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TwinTheme.shapes.pill)
                    .background(gameBrush(info.id))
                    .padding(vertical = TwinTheme.spacing.s),
            )
        }
    }
}