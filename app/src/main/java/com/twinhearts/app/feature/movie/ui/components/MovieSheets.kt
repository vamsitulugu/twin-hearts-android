package com.twinhearts.app.feature.movie.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.movie.domain.formatClock
import com.twinhearts.app.feature.movie.domain.formatRuntime
import com.twinhearts.app.feature.movie.domain.model.ChatMessage
import com.twinhearts.app.feature.movie.domain.model.HistoryItem
import com.twinhearts.app.feature.movie.domain.model.MovieItem
import com.twinhearts.app.feature.movie.domain.model.WatchStatus

/** Why this file exists: the small pop-ups of Watch Together (movie details, history, chat log, invite, end). */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailSheet(
    movie: MovieItem,
    onDismiss: () -> Unit,
    onStatus: (WatchStatus) -> Unit,
    onRate: (Int) -> Unit,
    onRemove: () -> Unit,
    onAdd: () -> Unit,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
) {
    val colors = TwinTheme.colors
    val saved = movie.id.isNotEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.card) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(movie.title, color = colors.ink, style = MaterialTheme.typography.headlineSmall, fontFamily = SerifFamily)
            Text(
                listOfNotNull(movie.year, movie.genre?.takeIf { it.isNotBlank() }).joinToString(" · "),
                color = colors.inkMuted, style = MaterialTheme.typography.bodySmall,
            )
            movie.overview?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = colors.ink, style = MaterialTheme.typography.bodyMedium, maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
            if (saved) {
                Text(stringResource(R.string.movie_detail_status), color = colors.inkMuted, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    WatchStatus.entries.forEach { s ->
                        TwinFilterChip(stringResource(s.label()), movie.status == s, { onStatus(s) })
                    }
                }
                Text(stringResource(R.string.movie_detail_rating), color = colors.inkMuted, style = MaterialTheme.typography.labelMedium)
                Row {
                    val current = (movie.rating ?: 0.0).toInt()
                    (1..STARS).forEach { star ->
                        IconButton(onClick = { onRate(star) }) {
                            Icon(
                                if (star <= current) Icons.Rounded.Star else Icons.Rounded.StarBorder, null,
                                tint = if (star <= current) colors.accent else colors.inkFaint,
                            )
                        }
                    }
                }
                GlassButton(
                    stringResource(if (isFavorite) R.string.movie_fav_in else R.string.movie_fav_add), onFavorite,
                    Modifier.fillMaxWidth(), style = GlassButtonStyle.Secondary,
                )
                GlassButton(stringResource(R.string.movie_detail_remove), onRemove, Modifier.fillMaxWidth(), style = GlassButtonStyle.Ghost)
            } else {
                GlassButton(stringResource(R.string.movie_detail_add), onAdd, Modifier.fillMaxWidth())
            }
        }
    }
}

private fun WatchStatus.label(): Int = when (this) {
    WatchStatus.Watchlist -> R.string.movie_status_watchlist
    WatchStatus.Watching -> R.string.movie_status_watching
    WatchStatus.Watched -> R.string.movie_status_watched
}

private const val STARS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(
    history: UiState<List<HistoryItem>>,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    val colors = TwinTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.card) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = TwinTheme.spacing.screenEdge)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.movie_history_title), color = colors.ink, style = MaterialTheme.typography.headlineSmall, fontFamily = SerifFamily)
                if (history is UiState.Content) TextButton(onClearAll) { Text(stringResource(R.string.movie_history_clear_all), color = colors.accent) }
            }
            when (history) {
                UiState.Loading -> LoadingSkeleton()
                UiState.Empty -> EmptyState(
                    stringResource(R.string.movie_history_empty_title),
                    message = stringResource(R.string.movie_history_empty_message), icon = Icons.Rounded.History,
                )
                is UiState.Error -> ErrorState(message = history.error.asText(), onRetry = onRetry)
                is UiState.Content -> LazyColumn(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    items(history.data, key = { it.id }) { item ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, color = colors.ink, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        buildString {
                                            append(stringResource(R.string.movie_history_progress, item.completedPct))
                                            item.durationSec?.let { append(" · ").append(formatRuntime(it)) }
                                            if (item.chatCount > 0) append(" · ").append(stringResource(R.string.movie_history_messages, item.chatCount))
                                        },
                                        color = colors.inkMuted, style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                IconButton(onClick = { onDelete(item.id) }) {
                                    Icon(Icons.Rounded.Delete, stringResource(R.string.movie_history_delete), tint = colors.inkMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The partner pressed START TOGETHER. Accepting begins the shared 3-2-1 countdown. */
@Composable
fun InviteDialog(movieTitle: String, matches: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = {},
        containerColor = colors.card,
        title = { Text(stringResource(R.string.movie_invite_kicker), color = colors.accent, style = MaterialTheme.typography.labelLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                Text(stringResource(R.string.movie_invite_text), color = colors.ink, style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.movie_invite_ask), color = colors.inkMuted, style = MaterialTheme.typography.bodyMedium)
                Text(movieTitle, color = colors.ink, style = MaterialTheme.typography.titleMedium, fontFamily = SerifFamily)
                if (matches) Text(stringResource(R.string.movie_match_ok), color = colors.success, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onAccept) { Text(stringResource(R.string.movie_accept), color = colors.accent) } },
        dismissButton = { TextButton(onDecline) { Text(stringResource(R.string.movie_not_now), color = colors.inkMuted) } },
    )
}

@Composable
fun EndDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(stringResource(R.string.movie_end_title), color = colors.ink, fontFamily = SerifFamily) },
        text = { Text(stringResource(R.string.movie_end_text), color = colors.inkMuted, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onConfirm) { Text(stringResource(R.string.movie_end), color = colors.error) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.movie_cancel), color = colors.inkMuted) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatLogSheet(messages: List<ChatMessage>, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.card) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = TwinTheme.spacing.screenEdge)) {
            Text(stringResource(R.string.movie_chat_title), color = colors.ink, style = MaterialTheme.typography.headlineSmall, fontFamily = SerifFamily)
            if (messages.isEmpty()) {
                Text(
                    stringResource(R.string.movie_chat_empty), color = colors.inkMuted, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(TwinTheme.spacing.xl), textAlign = TextAlign.Center,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.padding(top = TwinTheme.spacing.m)) {
                    items(messages, key = { it.id }) { m ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                m.positionSec?.let { formatClock(it) }.orEmpty(), color = colors.inkFaint,
                                style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(52.dp),
                            )
                            Text(m.text, color = if (m.fromMe) colors.ink else colors.accent, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
