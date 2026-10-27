package com.twinhearts.app.feature.twinai.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.twinai.domain.model.BothResult
import com.twinhearts.app.feature.twinai.domain.model.BothRound
import com.twinhearts.app.feature.twinai.domain.model.BothSession
import com.twinhearts.app.feature.twinai.domain.model.RoundStatus
import com.twinhearts.app.feature.twinai.ui.BothHistory
import com.twinhearts.app.feature.twinai.ui.BothUiState
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Everything the BOTH tab can do, so the screen only has to pass these in. */
class BothActions(
    val onStart: () -> Unit,
    val onOpen: (String) -> Unit,
    val onClose: () -> Unit,
    val onReload: () -> Unit,
    val onDraft: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onBeginEdit: () -> Unit,
    val onCancelEdit: () -> Unit,
    val onSaveEdit: () -> Unit,
    val onNudge: () -> Unit,
    val onContinue: () -> Unit,
    val onRetry: () -> Unit,
    val onAskDelete: (BothSession) -> Unit,
    val onConfirmDelete: () -> Unit,
    val onCancelDelete: () -> Unit,
    val onMic: () -> Unit,
)

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@StringRes
private fun intentLabel(intent: String): Int = when (intent) {
    "CONFLICT" -> R.string.twinai_intent_conflict
    "DECISION" -> R.string.twinai_intent_decision
    "PLANNING" -> R.string.twinai_intent_planning
    "IDEAS" -> R.string.twinai_intent_ideas
    "QUESTION" -> R.string.twinai_intent_question
    "RELATIONSHIP_DISCUSSION" -> R.string.twinai_intent_discussion
    "FUN" -> R.string.twinai_intent_fun
    else -> R.string.twinai_intent_general
}

/** Why this file exists: the BOTH tab: the "Start Together" home with history, and an open discussion. */
@Composable
fun BothContent(state: BothUiState, actions: BothActions, modifier: Modifier = Modifier) {
    val open = state.open
    if (open == null) {
        BothHome(state, actions, modifier)
    } else {
        DiscussionView(state, actions, modifier)
    }
    state.deleteTarget?.let {
        AlertDialog(
            onDismissRequest = actions.onCancelDelete,
            title = { Text(stringResource(R.string.twinai_both_delete_title)) },
            text = { Text(stringResource(R.string.twinai_both_delete_body), color = TwinTheme.colors.inkMuted) },
            confirmButton = {
                TextButton(onClick = actions.onConfirmDelete, enabled = !state.deleting) {
                    Text(
                        stringResource(if (state.deleting) R.string.twinai_both_deleting else R.string.twinai_both_delete),
                        color = TwinTheme.colors.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onCancelDelete) { Text(stringResource(R.string.twinai_both_cancel)) }
            },
            containerColor = TwinTheme.colors.card,
            titleContentColor = TwinTheme.colors.ink,
            textContentColor = TwinTheme.colors.inkMuted,
        )
    }
}

@Composable
private fun BothHome(state: BothUiState, actions: BothActions, modifier: Modifier) {
    val colors = TwinTheme.colors
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TwinRobot(Modifier.width(96.dp))
                        Icon(Icons.Rounded.Favorite, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                        TwinRobot(Modifier.width(96.dp))
                    }
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.twinai_both_title_pre))
                            withStyle(SpanStyle(brush = colors.primaryBrush)) { append(stringResource(R.string.twinai_both_title_name)) }
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.twinai_both_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                    )
                    GlassButton(
                        text = stringResource(R.string.twinai_both_start),
                        onClick = actions.onStart,
                        modifier = Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s),
                        loading = state.starting,
                        leadingIcon = Icons.Rounded.PlayArrow,
                    )
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.twinai_both_how_title), style = MaterialTheme.typography.titleMedium, color = colors.ink, fontWeight = FontWeight.Bold)
                HowRow(1, Icons.Rounded.Edit, R.string.twinai_both_how_1)
                HowRow(2, Icons.Rounded.Psychology, R.string.twinai_both_how_2)
                HowRow(3, Icons.Rounded.Favorite, R.string.twinai_both_how_3)
            }
        }
        item {
            Text(
                stringResource(R.string.twinai_both_recent),
                style = MaterialTheme.typography.titleSmall,
                color = colors.inkMuted,
            )
        }
        when (val history = state.history) {
            BothHistory.Loading -> item { Text(stringResource(R.string.twinai_both_loading), color = colors.inkFaint, style = MaterialTheme.typography.bodyMedium) }
            BothHistory.Empty -> item { Text(stringResource(R.string.twinai_both_empty), color = colors.inkFaint, style = MaterialTheme.typography.bodyMedium) }
            is BothHistory.Error -> item {
                ErrorState(
                    message = stringResource(R.string.twinai_both_history_error) + " " + history.error.asText(),
                    onRetry = actions.onReload,
                )
            }
            is BothHistory.Content -> items(history.sessions, key = { it.id }) { session ->
                HistoryRow(session, opening = state.opening, onOpen = { actions.onOpen(session.id) }, onDelete = { actions.onAskDelete(session) })
            }
        }
    }
}

@Composable
private fun HowRow(number: Int, icon: ImageVector, @StringRes text: Int) {
    val colors = TwinTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Text(number.toString(), style = MaterialTheme.typography.titleMedium, color = colors.accent, fontWeight = FontWeight.Bold)
        Icon(icon, null, tint = colors.pinkSoft, modifier = Modifier.size(24.dp))
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
    }
}

@Composable
private fun HistoryRow(session: BothSession, opening: Boolean, onOpen: () -> Unit, onDelete: () -> Unit) {
    val colors = TwinTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f).clickable(enabled = !opening, onClick = onOpen).padding(vertical = TwinTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.intent?.let { stringResource(intentLabel(it)) } ?: "…",
                        style = MaterialTheme.typography.labelMedium, color = colors.pinkSoft, fontWeight = FontWeight.Bold,
                    )
                    session.lastActivity?.let {
                        Text(dateFormat.format(it.atZone(ZoneId.systemDefault())), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
                    }
                }
                Text(
                    session.title ?: stringResource(R.string.twinai_both_untitled),
                    style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1,
                )
                Text(
                    if (session.roundCount == 1) stringResource(R.string.twinai_both_round_one)
                    else stringResource(R.string.twinai_both_rounds, session.roundCount),
                    style = MaterialTheme.typography.bodySmall, color = colors.inkFaint,
                )
            }
            Box {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.twinai_both_menu),
                    tint = colors.inkFaint,
                    modifier = Modifier.size(TwinTheme.shapes.minTouch).clip(TwinTheme.shapes.circle).clickable { menuOpen = true }.padding(12.dp),
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.background(colors.card)) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.twinai_both_delete_menu), color = colors.error) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscussionView(state: BothUiState, actions: BothActions, modifier: Modifier) {
    val colors = TwinTheme.colors
    val discussion = state.open ?: return
    val current = discussion.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = actions.onClose) {
                    Text(stringResource(R.string.twinai_both_back), color = colors.pinkSoft)
                }
                Text(
                    discussion.session.title ?: stringResource(R.string.twinai_both_untitled),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                current?.let {
                    Text(stringResource(R.string.twinai_both_round_label, it.number), style = MaterialTheme.typography.labelMedium, color = colors.inkFaint)
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s, Alignment.CenterHorizontally),
            ) {
                Avatar(state.myName, size = 32.dp)
                Text(state.myName, style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 1)
                Text(stringResource(R.string.twinai_both_vs), style = MaterialTheme.typography.labelMedium, color = colors.accent)
                Avatar(state.partnerName, size = 32.dp)
                Text(state.partnerName, style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 1)
            }
        }
        // Earlier rounds that are finished: folded away, tap to open (like the web "details" rows).
        items(
            discussion.rounds.filter { it.id != current?.id && (it.status == RoundStatus.Done || it.status == RoundStatus.Safety) }.sortedBy { it.number },
            key = { it.id },
        ) { round -> PriorRound(round) }
        if (current != null) {
            item(key = "current") {
                RoundBody(current, state, actions)
            }
        }
    }
}

@Composable
private fun RoundBody(round: BothRound, state: BothUiState, actions: BothActions) {
    val colors = TwinTheme.colors
    val partner = state.partnerName
    when {
        round.status == RoundStatus.Failed -> StatusCard {
            Text(stringResource(R.string.twinai_both_failed), style = MaterialTheme.typography.bodyLarge, color = colors.ink, textAlign = TextAlign.Center)
            GlassButton(stringResource(R.string.twinai_both_retry), actions.onRetry, style = GlassButtonStyle.Secondary)
        }
        round.status == RoundStatus.Done || round.status == RoundStatus.Safety -> {
            val result = round.result
            if (result == null) {
                Text(stringResource(R.string.twinai_both_no_result), color = colors.inkFaint, style = MaterialTheme.typography.bodyMedium)
            } else {
                ResultCard(result, onContinue = actions.onContinue)
            }
        }
        round.youSubmitted && round.partnerSubmitted -> StatusCard {
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp, color = colors.accent)
            Text(stringResource(R.string.twinai_both_analyzing), style = MaterialTheme.typography.bodyLarge, color = colors.ink, textAlign = TextAlign.Center)
        }
        round.youSubmitted && state.editing -> GlassCard(Modifier.fillMaxWidth()) {
            GlassTextArea(value = state.draft, onValueChange = actions.onDraft, hint = stringResource(R.string.twinai_both_edit), minHeight = 120.dp, maxLines = 8, trailing = { MicButton(actions.onMic) })
            Row(Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.m), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                GlassButton(stringResource(R.string.twinai_both_cancel), actions.onCancelEdit, Modifier.weight(1f), GlassButtonStyle.Ghost)
                GlassButton(
                    stringResource(R.string.twinai_both_save), actions.onSaveEdit, Modifier.weight(1f),
                    enabled = state.draft.isNotBlank(), loading = state.saving,
                )
            }
        }
        round.youSubmitted -> StatusCard {
            Text("✓", style = MaterialTheme.typography.headlineSmall, color = colors.success)
            Text(stringResource(R.string.twinai_both_submitted), style = MaterialTheme.typography.bodyLarge, color = colors.ink, textAlign = TextAlign.Center)
            Text(stringResource(R.string.twinai_both_waiting, partner), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = actions.onBeginEdit) { Text(stringResource(R.string.twinai_both_edit), color = colors.pinkSoft) }
                Text("·", color = colors.inkFaint)
                TextButton(onClick = actions.onNudge, enabled = !state.reminded) {
                    Text(
                        stringResource(if (state.reminded) R.string.twinai_both_reminded else R.string.twinai_both_remind, partner),
                        color = colors.pinkSoft,
                    )
                }
            }
        }
        else -> GlassCard(Modifier.fillMaxWidth()) {
            GlassTextArea(
                value = state.draft,
                onValueChange = actions.onDraft,
                hint = stringResource(R.string.twinai_both_placeholder, partner),
                minHeight = 120.dp,
                maxLines = 8,
                trailing = { MicButton(actions.onMic) },
            )
            GlassButton(
                text = stringResource(R.string.twinai_both_submit),
                onClick = actions.onSubmit,
                modifier = Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.m),
                enabled = state.draft.isNotBlank(),
                loading = state.submitting,
            )
            if (round.partnerSubmitted) {
                Text(
                    stringResource(R.string.twinai_both_partner_done, partner),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = TwinTheme.spacing.s),
                )
            }
        }
    }
}

@Composable
private fun StatusCard(content: @Composable () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            content()
        }
    }
}

@Composable
private fun ResultCard(result: BothResult, onContinue: (() -> Unit)?) {
    val colors = TwinTheme.colors
    if (result.safety) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.twinai_both_safety_title), style = MaterialTheme.typography.titleMedium, color = colors.ink, fontWeight = FontWeight.Bold)
            result.sections.forEach {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it.title + ": ") }
                        append(it.content)
                    },
                    style = MaterialTheme.typography.bodyMedium, color = colors.ink,
                    modifier = Modifier.padding(top = TwinTheme.spacing.s),
                )
            }
            Text(
                stringResource(R.string.twinai_both_safety_note),
                style = MaterialTheme.typography.bodySmall, color = colors.inkMuted,
                modifier = Modifier.padding(top = TwinTheme.spacing.m),
            )
        }
        return
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Text(stringResource(intentLabel(result.intent)), style = MaterialTheme.typography.titleSmall, color = colors.pinkSoft, fontWeight = FontWeight.Bold)
        result.sections.forEach { section ->
            Text(section.title, style = MaterialTheme.typography.titleSmall, color = colors.ink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = TwinTheme.spacing.m))
            Text(section.content, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, modifier = Modifier.padding(top = TwinTheme.spacing.xs))
        }
        if (onContinue != null) {
            GlassButton(
                stringResource(R.string.twinai_both_continue), onContinue,
                Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.l),
            )
        }
    }
}

@Composable
private fun PriorRound(round: BothRound) {
    val colors = TwinTheme.colors
    var expanded by rememberSaveable(round.id) { mutableStateOf(false) }
    val result = round.result
    GlassCard(Modifier.fillMaxWidth(), contentPadding = TwinTheme.spacing.m) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (result == null) stringResource(R.string.twinai_both_prior_round_plain, round.number)
                else stringResource(R.string.twinai_both_prior_round, round.number, stringResource(intentLabel(result.intent))),
                style = MaterialTheme.typography.titleSmall, color = colors.ink, modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null, tint = colors.inkFaint,
            )
        }
        if (expanded) {
            if (result == null) {
                Text(stringResource(R.string.twinai_both_no_result_short), color = colors.inkFaint, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = TwinTheme.spacing.s))
            } else {
                result.sections.forEach { section ->
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(section.title) }
                        },
                        style = MaterialTheme.typography.titleSmall, color = colors.ink, modifier = Modifier.padding(top = TwinTheme.spacing.m),
                    )
                    Text(section.content, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, modifier = Modifier.padding(top = TwinTheme.spacing.xs))
                }
            }
        }
    }
}

@Composable
private fun MicButton(onClick: () -> Unit) {
    Icon(
        Icons.Rounded.Mic,
        contentDescription = stringResource(R.string.twinai_both_mic),
        tint = TwinTheme.colors.inkMuted,
        modifier = Modifier.size(TwinTheme.shapes.minTouch).clip(TwinTheme.shapes.circle).clickable(onClick = onClick).padding(12.dp),
    )
}
