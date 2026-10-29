package com.twinhearts.app.feature.help.ui.components

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.UiState
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.help.domain.filterFaqs
import com.twinhearts.app.feature.help.domain.model.FaqItem
import com.twinhearts.app.feature.help.domain.model.HelpCategory
import com.twinhearts.app.feature.help.ui.HelpUiState

/** The id used to remember that a question is open. Falls back to the text if the id is empty. */
private val FaqItem.openKey: String get() = id.ifBlank { question }

@StringRes
private fun HelpCategory.labelRes(): Int = when (this) {
    HelpCategory.All -> R.string.help_cat_all
    HelpCategory.Pairing -> R.string.help_cat_pairing
    HelpCategory.Privacy -> R.string.help_cat_privacy
    HelpCategory.Globe -> R.string.help_cat_globe
    HelpCategory.Music -> R.string.help_cat_music
    HelpCategory.Notifications -> R.string.help_cat_notifications
    HelpCategory.Sync -> R.string.help_cat_sync
}

/**
 * "Questions & QA" tab (web `#viewFaq`): hero card, search, topic chips, the question list and
 * the "Start Chat with Care Team" banner. Shows loading, empty and error states (R-41).
 */
@Composable
internal fun HelpFaqView(
    state: HelpUiState,
    onQueryChange: (String) -> Unit,
    onCategory: (HelpCategory) -> Unit,
    onToggle: (String) -> Unit,
    onAskAi: (String) -> Unit,
    onMessageSupport: (String) -> Unit,
    onStartChat: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TwinTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        item { HeroCard() }
        item { SearchField(value = state.query, onValueChange = onQueryChange) }
        item { CategoryChips(selected = state.category, onSelect = onCategory) }

        when (val faqs = state.faqs) {
            UiState.Loading -> item { LoadingSkeleton(lines = 5) }
            is UiState.Error -> item { ErrorState(message = faqs.error.asText(), onRetry = onRetry) }
            UiState.Empty -> item { NoMatches(query = state.query, onAskAi = onAskAi) }
            is UiState.Content -> {
                val visible = filterFaqs(faqs.data, state.query, state.category)
                if (visible.isEmpty()) {
                    item { NoMatches(query = state.query, onAskAi = onAskAi) }
                } else {
                    items(visible) { faq ->
                        FaqCard(
                            faq = faq,
                            expanded = faq.openKey in state.expandedIds,
                            onToggle = { onToggle(faq.openKey) },
                            onAskAi = { onAskAi(faq.question) },
                            onMessageSupport = onMessageSupport,
                        )
                    }
                }
            }
        }

        item { CareBanner(onStartChat = onStartChat) }
    }
}

@Composable
private fun HeroCard() {
    val colors = TwinTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(TwinTheme.shapes.small)
                    .background(colors.primaryBrush),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "❤️", style = MaterialTheme.typography.headlineSmall)
            }
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                Text(
                    text = stringResource(R.string.help_hero_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                )
                Text(
                    text = stringResource(R.string.help_hero_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                )
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    val hint = stringResource(R.string.help_search_hint)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TwinTheme.shapes.fieldHeight)
                    .clip(shape)
                    .background(colors.glass)
                    .border(1.dp, colors.line, shape)
                    .padding(horizontal = TwinTheme.spacing.l),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.inkFaint, modifier = Modifier.size(20.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(text = hint, style = MaterialTheme.typography.bodyMedium, color = colors.inkFaint)
                    }
                    inner()
                }
            }
        },
    )
}

@Composable
private fun CategoryChips(selected: HelpCategory, onSelect: (HelpCategory) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        items(HelpCategory.entries) { category ->
            TwinFilterChip(
                label = stringResource(category.labelRes()),
                selected = category == selected,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun NoMatches(query: String, onAskAi: (String) -> Unit) {
    val text = query.trim()
    EmptyState(
        title = stringResource(R.string.help_faq_empty),
        icon = Icons.Rounded.Search,
        actionLabel = if (text.isNotEmpty()) stringResource(R.string.help_faq_empty_ask, text) else null,
        onAction = { onAskAi(text) },
    )
}

/** One question. Tap to open: the answer plus "Ask AI" and "Message Support" (web `.faq-item`). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FaqCard(
    faq: FaqItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAskAi: () -> Unit,
    onMessageSupport: (String) -> Unit,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    val about = stringResource(R.string.help_about_question, faq.question)
    val stateText = stringResource(if (expanded) R.string.help_faq_expanded else R.string.help_faq_collapsed)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, if (expanded) colors.accent else colors.line, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = stateText }
                .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = faq.question,
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = if (expanded) colors.accent else colors.inkMuted,
                modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 180f else 0f },
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .dashedTopBorder(colors.line)
                    .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                Text(text = faq.answer, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                    verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                ) {
                    ActionButton(text = stringResource(R.string.help_faq_ask_ai), onClick = onAskAi)
                    ActionButton(text = stringResource(R.string.help_faq_message_support), onClick = { onMessageSupport(about) })
                }
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.m),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = colors.ink)
    }
}

/** The pink banner at the bottom of the list (web `.live-support-banner`). */
@Composable
private fun CareBanner(onStartChat: () -> Unit) {
    val colors = TwinTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TwinTheme.shapes.card)
            .background(colors.primaryBrush)
            .padding(TwinTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Text(
            text = stringResource(R.string.help_banner_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
        )
        Text(
            text = stringResource(R.string.help_banner_text),
            style = MaterialTheme.typography.bodySmall,
            color = colors.ink,
        )
        Spacer(Modifier.height(TwinTheme.spacing.xs))
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(TwinTheme.shapes.small)
                .background(colors.ink)
                .clickable(role = Role.Button, onClick = onStartChat)
                .padding(horizontal = TwinTheme.spacing.l),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.help_banner_cta),
                style = MaterialTheme.typography.titleSmall,
                color = colors.brand,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = colors.brand,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
