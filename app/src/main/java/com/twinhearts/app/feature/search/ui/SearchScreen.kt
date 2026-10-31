package com.twinhearts.app.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.search.domain.SearchCatalog
import com.twinhearts.app.feature.search.domain.SearchMatcher
import com.twinhearts.app.feature.search.domain.model.ContentStatus
import com.twinhearts.app.feature.search.domain.model.SearchDestination
import com.twinhearts.app.feature.search.domain.model.SearchResult
import com.twinhearts.app.feature.search.domain.model.SearchSection

/** The Search page opened from the Home search bar. Tapping a result opens the page it lives in. */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpen: (SearchDestination) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    SearchContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onBack = onBack,
        onOpen = { result ->
            viewModel.onResultOpened()
            onOpen(result.destination)
        },
        onUseQuery = viewModel::useQuery,
        onForgetRecent = viewModel::forgetRecent,
        onClearRecents = viewModel::clearRecents,
    )
}

@Composable
internal fun SearchContent(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpen: (SearchResult) -> Unit,
    onUseQuery: (String) -> Unit,
    onForgetRecent: (String) -> Unit,
    onClearRecents: () -> Unit,
) {
    TwinBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            SearchBar(query = state.query, onQueryChange = onQueryChange, onBack = onBack)
            val blank = state.query.isBlank()
            when {
                blank -> StartPanel(state.recents, onUseQuery, onForgetRecent, onClearRecents)
                else -> Results(state, onOpen)
            }
        }
    }
}

// ── Top bar with the text field ─────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onBack: () -> Unit) {
    val colors = TwinTheme.colors
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.core_back), tint = colors.ink)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = TwinTheme.shapes.minTouch)
                .clip(TwinTheme.shapes.pill)
                .background(colors.glass)
                .border(1.dp, colors.line, TwinTheme.shapes.pill)
                .padding(horizontal = TwinTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.search_clear), tint = colors.inkMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Before typing: recent searches and ideas ────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StartPanel(
    recents: List<String>,
    onUseQuery: (String) -> Unit,
    onForgetRecent: (String) -> Unit,
    onClearRecents: () -> Unit,
) {
    val colors = TwinTheme.colors
    val suggestions = SearchCatalog.suggestionTitles.map { stringResource(it) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = TwinTheme.spacing.screenEdge,
            vertical = TwinTheme.spacing.m,
        ),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        if (recents.isNotEmpty()) {
            item(key = "recent-head") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.search_recent),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.inkMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.search_clear_recent),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.accent,
                        modifier = Modifier
                            .clip(TwinTheme.shapes.small)
                            .clickable(role = Role.Button, onClick = onClearRecents)
                            .padding(TwinTheme.spacing.s),
                    )
                }
            }
            items(recents, key = { "recent-$it" }) { query ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TwinTheme.shapes.minTouch)
                        .clip(TwinTheme.shapes.small)
                        .clickable(role = Role.Button) { onUseQuery(query) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                    Text(
                        text = query,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onForgetRecent(query) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.search_clear), tint = colors.inkMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        item(key = "ideas") {
            Column(
                modifier = Modifier.padding(top = TwinTheme.spacing.l),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            ) {
                EmptyState(
                    title = stringResource(R.string.search_start_title),
                    message = stringResource(R.string.search_start_message),
                    icon = Icons.Rounded.Search,
                )
                Text(
                    text = stringResource(R.string.search_suggest_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.inkMuted,
                )
                // Wrap the chips onto several lines on narrow phones.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                    verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                ) {
                    suggestions.forEach { word ->
                        TwinFilterChip(label = word, selected = false, onClick = { onUseQuery(word) })
                    }
                }
            }
        }
    }
}

// ── While typing: grouped results ───────────────────────────────────────────────────────────

@Composable
private fun Results(state: SearchUiState, onOpen: (SearchResult) -> Unit) {
    val colors = TwinTheme.colors
    val all = state.local + state.content
    val grouped = SearchSection.entries.mapNotNull { section ->
        all.filter { it.section == section }.takeIf { it.isNotEmpty() }?.let { section to it }
    }
    val words = remember(state.query) { SearchMatcher.tokens(state.query) }
    val loading = state.status == ContentStatus.Loading
    if (grouped.isEmpty() && !loading) {
        EmptyState(
            title = stringResource(R.string.search_empty_title, state.query.trim()),
            message = if (state.status == ContentStatus.Failed) {
                stringResource(R.string.search_content_error)
            } else {
                stringResource(R.string.search_empty_message)
            },
            icon = Icons.Rounded.Search,
            modifier = Modifier.padding(top = TwinTheme.spacing.xxl),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = TwinTheme.spacing.screenEdge,
            end = TwinTheme.spacing.screenEdge,
            top = TwinTheme.spacing.s,
            bottom = TwinTheme.spacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        if (loading) {
            item(key = "loading") {
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.accent,
                        trackColor = colors.glass,
                    )
                    Text(
                        text = stringResource(R.string.search_content_loading),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkMuted,
                    )
                }
            }
        }
        if (state.status == ContentStatus.Failed) {
            item(key = "failed") {
                Text(
                    text = stringResource(R.string.search_content_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                )
            }
        }
        grouped.forEach { (section, rows) ->
            item(key = "head-${section.name}") {
                Text(
                    text = stringResource(section.titleRes()),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = TwinTheme.spacing.m),
                )
            }
            items(rows, key = { it.key }) { result -> ResultRow(result, words, onClick = { onOpen(result) }) }
        }
        item(key = "bottom-space") { Spacer(Modifier.navigationBarsPadding()) }
    }
}

private fun SearchSection.titleRes(): Int = when (this) {
    SearchSection.Pages -> R.string.search_sec_pages
    SearchSection.Features -> R.string.search_sec_features
    SearchSection.Messages -> R.string.search_sec_messages
    SearchSection.Inside -> R.string.search_sec_inside
}

@Composable
private fun ResultRow(result: SearchResult, words: List<String>, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TwinTheme.shapes.minTouch)
            .clip(TwinTheme.shapes.small)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.card),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = result.emoji, style = MaterialTheme.typography.titleMedium)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = highlight(result.title, words, colors.accent),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = subtitle(result)
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.search_open), tint = colors.inkMuted)
    }
}

@Composable
private fun subtitle(result: SearchResult): String = when {
    result.detail.isNotBlank() && result.where.isNotBlank() ->
        result.detail + " · " + stringResource(R.string.search_in, result.where)
    result.detail.isNotBlank() -> result.detail
    result.where.isNotBlank() && result.section != SearchSection.Pages ->
        stringResource(R.string.search_in, result.where)
    else -> ""
}

/** Bold + accent colour on the parts of [text] that the person typed. */
private fun highlight(text: String, words: List<String>, color: Color) = buildAnnotatedString {
    val lower = text.lowercase()
    // Lower-casing can change the length of rare letters; then skip highlighting rather than risk a crash.
    if (lower.length != text.length || words.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    val marked = BooleanArray(text.length)
    for (word in words) {
        var from = 0
        while (true) {
            val at = lower.indexOf(word, from)
            if (at < 0) break
            for (i in at until at + word.length) marked[i] = true
            from = at + word.length
        }
    }
    var i = 0
    while (i < text.length) {
        var j = i
        while (j < text.length && marked[j] == marked[i]) j++
        if (marked[i]) {
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append(text.substring(i, j)) }
        } else {
            append(text.substring(i, j))
        }
        i = j
    }
}

// ── Previews (R-57) ─────────────────────────────────────────────────────────────────────────

private val previewResults = listOf(
    SearchResult("catalog:calendar", SearchSection.Pages, "Calendar", "", "", "📅", SearchDestination.Calendar, 100),
    SearchResult("catalog:set_notifications", SearchSection.Features, "Notifications", "", "Settings", "🔔", SearchDestination.Settings, 90),
    SearchResult("msg:1", SearchSection.Messages, "…dinner at 8 tonight?", "You · 29 Oct 2026", "Chat", "💬", SearchDestination.Chat, 0),
    SearchResult("state:events:2", SearchSection.Inside, "Dinner date", "Calendar · 9 Nov 2026", "Calendar", "📅", SearchDestination.Calendar, 60),
)

@Preview(showBackground = true, backgroundColor = 0xFF14030E)
@Composable
private fun SearchResultsPreview() {
    TwinTheme {
        SearchContent(
            state = SearchUiState(query = "dinner", local = previewResults.take(2), content = previewResults.drop(2), status = ContentStatus.Done),
            onQueryChange = {}, onBack = {}, onOpen = {}, onUseQuery = {}, onForgetRecent = {}, onClearRecents = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E)
@Composable
private fun SearchStartPreview() {
    TwinTheme {
        SearchContent(
            state = SearchUiState(recents = listOf("anniversary", "money")),
            onQueryChange = {}, onBack = {}, onOpen = {}, onUseQuery = {}, onForgetRecent = {}, onClearRecents = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E)
@Composable
private fun SearchEmptyPreview() {
    TwinTheme {
        SearchContent(
            state = SearchUiState(query = "zzzz", status = ContentStatus.Done),
            onQueryChange = {}, onBack = {}, onOpen = {}, onUseQuery = {}, onForgetRecent = {}, onClearRecents = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, fontScale = 1.3f)
@Composable
private fun SearchErrorLargeFontPreview() {
    TwinTheme {
        SearchContent(
            state = SearchUiState(query = "dinner", local = previewResults.take(1), status = ContentStatus.Failed),
            onQueryChange = {}, onBack = {}, onOpen = {}, onUseQuery = {}, onForgetRecent = {}, onClearRecents = {},
        )
    }
}
