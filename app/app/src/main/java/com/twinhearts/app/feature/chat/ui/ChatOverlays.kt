package com.twinhearts.app.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.chat.domain.MAX_SCHEDULE_MINUTES
import com.twinhearts.app.feature.chat.domain.formatClock
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.ScheduledMessage
import com.twinhearts.app.feature.chat.domain.model.SearchFilter
import com.twinhearts.app.feature.chat.domain.toLocalDate
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SEARCH_DEBOUNCE_MS = 350L
private const val MINUTES_15 = 15
private const val MINUTES_HOUR = 60
private const val MINUTES_3_HOURS = 180
private const val MINUTES_DAY = 1440
private val dateText: DateTimeFormatter get() = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

/** Full-screen sheet with a title and a close button. */
@Composable
private fun FullScreenSheet(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    val colors = TwinTheme.colors
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        TwinBackground {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = TwinTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.chat_close), tint = colors.ink)
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(TwinTheme.shapes.minTouch))
                }
                content()
            }
        }
    }
}

@Composable
private fun ResultRow(message: ChatMessage, partnerName: String, trailing: (@Composable () -> Unit)? = null) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TwinTheme.shapes.small)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
            .padding(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (message.mine) stringResource(R.string.chat_you) else partnerName,
                style = MaterialTheme.typography.labelMedium,
                color = colors.pinkSoft,
            )
            Text(
                text = message.previewText(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${message.createdAt.toLocalDate().format(dateText)} · ${formatClock(message.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.inkMuted,
            )
        }
        trailing?.invoke()
    }
}

/** Search in this chat (web: "Search messages..." with All / Media / Links / Docs). */
@Composable
internal fun ChatSearchDialog(
    state: SearchState,
    partnerName: String,
    onSearch: (String, SearchFilter) -> Unit,
    onClose: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    val filter = SearchFilter.entries[filterIndex]
    LaunchedEffect(query, filterIndex) {
        delay(SEARCH_DEBOUNCE_MS)
        onSearch(query, filter)
    }
    val labels = mapOf(
        SearchFilter.All to R.string.chat_filter_all,
        SearchFilter.Media to R.string.chat_filter_media,
        SearchFilter.Links to R.string.chat_filter_links,
        SearchFilter.Docs to R.string.chat_filter_docs,
    )
    FullScreenSheet(stringResource(R.string.chat_action_search), onClose) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = TwinTheme.spacing.screenEdge),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            TwinTextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.chat_search_hint),
                leadingIcon = Icons.Rounded.Search,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                SearchFilter.entries.forEachIndexed { index, f ->
                    TwinFilterChip(label = stringResource(labels.getValue(f)), selected = index == filterIndex, onClick = { filterIndex = index })
                }
            }
            when {
                state.failed != null -> ErrorState(message = state.failed.asText(), onRetry = { onSearch(query, filter) })
                state.searching -> Text(stringResource(R.string.chat_loading), color = TwinTheme.colors.inkMuted)
                query.isBlank() && filter == SearchFilter.All -> EmptyState(title = stringResource(R.string.chat_search_start), icon = Icons.Rounded.Search)
                state.results.isEmpty() -> EmptyState(title = stringResource(R.string.chat_search_empty), icon = Icons.Rounded.Search)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    items(state.results, key = { it.clientId }) { ResultRow(it, partnerName) }
                }
            }
        }
    }
}

/** Starred messages (web: the ★ button in the chat header). */
@Composable
internal fun StarredDialog(
    messages: List<ChatMessage>,
    partnerName: String,
    onUnstar: (ChatMessage) -> Unit,
    onClose: () -> Unit,
) {
    FullScreenSheet(stringResource(R.string.chat_starred_title), onClose) {
        if (messages.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.chat_starred_empty),
                message = stringResource(R.string.chat_starred_hint),
                icon = Icons.Rounded.StarBorder,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = TwinTheme.spacing.screenEdge),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                items(messages, key = { it.clientId }) { message ->
                    ResultRow(message, partnerName) {
                        IconButton(onClick = { onUnstar(message) }) {
                            Icon(Icons.Rounded.StarBorder, stringResource(R.string.chat_menu_unstar), tint = TwinTheme.colors.pinkSoft)
                        }
                    }
                }
            }
        }
    }
}

/** Send a message later; see and cancel what is waiting. */
@Composable
internal fun ScheduledDialog(
    scheduled: List<ScheduledMessage>,
    scheduling: Boolean,
    onSchedule: (String, Int) -> Unit,
    onCancel: (String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = TwinTheme.colors
    var text by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableIntStateOf(MINUTES_15) }
    val presets = listOf(
        MINUTES_15 to R.string.chat_scheduled_15m,
        MINUTES_HOUR to R.string.chat_scheduled_1h,
        MINUTES_3_HOURS to R.string.chat_scheduled_3h,
        MINUTES_DAY to R.string.chat_scheduled_1d,
    )
    FullScreenSheet(stringResource(R.string.chat_scheduled_title), onClose) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TwinTheme.spacing.screenEdge),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            TwinTextField(value = text, onValueChange = { text = it }, label = stringResource(R.string.chat_scheduled_message))
            Text(stringResource(R.string.chat_scheduled_when), style = MaterialTheme.typography.labelLarge, color = colors.inkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                presets.forEach { (value, label) ->
                    TwinFilterChip(label = stringResource(label), selected = minutes == value, onClick = { minutes = value })
                }
            }
            GlassButton(
                text = stringResource(R.string.chat_scheduled_button),
                onClick = { onSchedule(text, minutes.coerceAtMost(MAX_SCHEDULE_MINUTES)); text = "" },
                enabled = text.isNotBlank() && !scheduling,
                loading = scheduling,
                leadingIcon = Icons.Rounded.Schedule,
                modifier = Modifier.fillMaxWidth(),
            )
            if (scheduled.isEmpty()) {
                EmptyState(title = stringResource(R.string.chat_scheduled_empty), icon = Icons.Rounded.Schedule)
            } else {
                scheduled.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TwinTheme.shapes.small)
                            .background(colors.glass)
                            .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
                            .padding(TwinTheme.spacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.text, style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(
                                    R.string.chat_scheduled_sends,
                                    "${item.sendAt.toLocalDate().format(dateText)} · ${formatClock(item.sendAt)}",
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.inkMuted,
                            )
                        }
                        IconButton(onClick = { onCancel(item.id) }) {
                            Icon(Icons.Rounded.Delete, stringResource(R.string.chat_scheduled_cancel), tint = colors.error)
                        }
                    }
                }
            }
            Spacer(Modifier.padding(bottom = TwinTheme.spacing.xl))
        }
    }
}
