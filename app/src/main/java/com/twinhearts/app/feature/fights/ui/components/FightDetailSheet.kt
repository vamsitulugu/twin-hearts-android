package com.twinhearts.app.feature.fights.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.fights.domain.FightMath
import com.twinhearts.app.feature.fights.domain.model.Fight
import com.twinhearts.app.feature.fights.domain.model.FightApproach
import com.twinhearts.app.feature.fights.domain.model.FightCategory

/**
 * Why this file exists: the sheet that opens when a fight is tapped: facts, what happened, feelings,
 * what was needed, how it was sorted out, then Mark resolved / AI Help / Delete.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FightDetailSheet(
    fight: Fight,
    myRole: String,
    partnerName: String,
    confirmDelete: Boolean,
    onDismiss: () -> Unit,
    onResolve: () -> Unit,
    onAskAi: () -> Unit,
    onDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    val colors = TwinTheme.colors
    val category = FightCategory.from(fight.category)
    val open = fight.isOpen
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.card, contentColor = colors.ink) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                ToneTile(categoryIcon(category), toneColor(category.tone))
                Column(Modifier.weight(1f)) {
                    Text(fight.title, style = MaterialTheme.typography.titleMedium, color = colors.ink, fontWeight = FontWeight.Bold)
                    Text(formatFightDate(fight.date), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                }
                StatePill(open, stringResource(if (open) R.string.fights_state_open else R.string.fights_state_resolved))
            }

            val starter = FightMath.starterOf(fight.startedBy, myRole, partnerName)
            val starterText = when (starter) {
                FightMath.Starter.Both -> stringResource(R.string.fights_starter_both)
                FightMath.Starter.Unsure -> stringResource(R.string.fights_starter_unsure)
                FightMath.Starter.Me -> stringResource(R.string.fights_starter_me)
                is FightMath.Starter.Partner -> starter.name
                FightMath.Starter.Unknown -> null
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                Fact(stringResource(R.string.fights_out_of_ten, fight.intensity.toString()), stringResource(R.string.fights_detail_intensity), Modifier.weight(1f))
                Fact(stringResource(categoryLabel(category)).takeIf { fight.category == category.label } ?: fight.category, stringResource(R.string.fights_detail_category), Modifier.weight(1f))
                if (starterText != null) Fact(starterText, stringResource(R.string.fights_detail_started), Modifier.weight(1f))
            }

            if (fight.reason.isNotBlank()) Section(stringResource(R.string.fights_detail_happened)) { BodyText(fight.reason) }
            if (fight.feelings.isNotEmpty()) {
                Section(stringResource(R.string.fights_detail_feelings)) { Marks(fight.feelings) }
            }
            if (fight.need.isNotBlank()) Section(stringResource(R.string.fights_detail_need)) { BodyText(fight.need) }
            val approaches = fight.approach.mapNotNull { FightApproach.from(it) }
            if (approaches.isNotEmpty()) {
                Section(stringResource(if (open) R.string.fights_detail_help else R.string.fights_detail_sorted)) {
                    Marks(approaches.map { stringResource(approachLabel(it, resolved = !open)) })
                }
            }
            if (fight.notes.isNotBlank()) Section(stringResource(R.string.fights_detail_next_time)) { BodyText(fight.notes) }
            fight.resolvedDate?.let {
                Text(
                    stringResource(R.string.fights_detail_resolved_on, formatFightDate(it)),
                    style = MaterialTheme.typography.titleSmall, color = colors.success, fontWeight = FontWeight.Bold,
                )
            }

            if (open) {
                GlassButton(stringResource(R.string.fights_mark_resolved), onResolve, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Check)
                GlassButton(stringResource(R.string.fights_ai_help), onAskAi, Modifier.fillMaxWidth(), GlassButtonStyle.Secondary, leadingIcon = Icons.Rounded.SmartToy)
            }
            GlassButton(stringResource(R.string.fights_delete), onDelete, Modifier.fillMaxWidth(), GlassButtonStyle.Ghost, leadingIcon = Icons.Rounded.Delete)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(R.string.fights_delete_title)) },
            text = { Text(stringResource(R.string.fights_delete_msg)) },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text(stringResource(R.string.fights_delete), color = colors.error) } },
            dismissButton = { TextButton(onClick = onCancelDelete) { Text(stringResource(R.string.fights_cancel)) } },
            containerColor = colors.card,
            titleContentColor = colors.ink,
            textContentColor = colors.inkMuted,
        )
    }
}

@Composable
private fun Fact(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Column(
        modifier
            .clip(TwinTheme.shapes.small)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
            .padding(TwinTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall, color = colors.ink, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
    }
}

/** A titled block that is left out entirely when its [content] would be empty (the caller decides). */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = TwinTheme.colors.pinkSoft, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun BodyText(text: String) {
    if (text.isBlank()) return
    Text(text, style = MaterialTheme.typography.bodyMedium, color = TwinTheme.colors.ink)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Marks(items: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        items.forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelLarge,
                color = TwinTheme.colors.ink,
                modifier = Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(TwinTheme.colors.glass)
                    .border(1.dp, TwinTheme.colors.lineSoft, TwinTheme.shapes.pill)
                    .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
            )
        }
    }
}
