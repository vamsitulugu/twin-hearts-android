package com.twinhearts.app.feature.fights.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.fights.domain.model.FightApproach
import com.twinhearts.app.feature.fights.domain.model.FightCategory
import com.twinhearts.app.feature.fights.domain.model.FightFeeling
import com.twinhearts.app.feature.fights.domain.model.FightForm
import com.twinhearts.app.feature.fights.domain.model.FightStarter
import com.twinhearts.app.feature.fights.domain.model.FightStatus

/** Everything the Log Fight wizard can do, so the screen only passes these in. */
class LogActions(
    val onBack: () -> Unit,
    val onSaveDraft: () -> Unit,
    val onStep: (Int) -> Unit,
    val onNext: () -> Unit,
    val onForm: ((FightForm) -> FightForm) -> Unit,
    val onCategory: (String) -> Unit,
    val onIntensity: (Int) -> Unit,
    val onStarter: (FightStarter) -> Unit,
    val onStatus: (FightStatus) -> Unit,
    val onFeeling: (String) -> Unit,
    val onApproach: (String) -> Unit,
)

/** Why this file exists: the three-step Log Fight wizard (Details > Feelings > Resolution), same questions as the web. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogWizard(
    form: FightForm,
    step: Int,
    titleMissing: Boolean,
    saving: Boolean,
    actions: LogActions,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l)) {
        // Sub-header: back, title, Save Draft
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.core_back),
                tint = colors.ink,
                modifier = Modifier.size(TwinTheme.shapes.minTouch).clip(TwinTheme.shapes.circle).clickable(role = Role.Button, onClick = actions.onBack).padding(12.dp),
            )
            Text(stringResource(R.string.fights_log_title), style = MaterialTheme.typography.titleLarge, color = colors.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .heightIn(min = 44.dp)
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.glass)
                    .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
                    .clickable(role = Role.Button, onClick = actions.onSaveDraft)
                    .padding(horizontal = TwinTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
            ) {
                Icon(Icons.Rounded.Save, null, tint = colors.ink, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.fights_save_draft), style = MaterialTheme.typography.labelLarge, color = colors.ink)
            }
        }

        StepsBar(step, actions.onStep)

        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l)) {
                when (step) {
                    1 -> StepDetails(form, titleMissing, actions)
                    2 -> StepFeelings(form, actions)
                    else -> StepResolution(form, actions)
                }
            }
        }

        val nextRes = when (step) {
            1 -> R.string.fights_next_feelings
            2 -> R.string.fights_next_resolution
            else -> R.string.fights_save_fight
        }
        GlassButton(
            text = stringResource(nextRes),
            onClick = actions.onNext,
            modifier = Modifier.fillMaxWidth(),
            loading = saving,
            leadingIcon = if (step == 3) Icons.Rounded.Check else null,
        )
    }
}

@Composable
private fun StepsBar(step: Int, onStep: (Int) -> Unit) {
    val colors = TwinTheme.colors
    val labels = listOf(R.string.fights_step_details, R.string.fights_step_feelings, R.string.fights_step_resolution)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { index, label ->
            val n = index + 1
            val current = n == step
            val done = n < step
            Column(
                Modifier.clip(TwinTheme.shapes.small).clickable(role = Role.Tab) { onStep(n) }.padding(TwinTheme.spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(TwinTheme.shapes.circle)
                        .background(if (current || done) colors.brand else colors.glass)
                        .border(1.dp, if (current) colors.accent else colors.lineSoft, TwinTheme.shapes.circle),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) Icon(Icons.Rounded.Check, null, tint = colors.ink, modifier = Modifier.size(18.dp))
                    else Text(n.toString(), style = MaterialTheme.typography.labelLarge, color = colors.ink, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = if (current) colors.ink else colors.inkMuted)
            }
            if (index < labels.lastIndex) {
                Box(Modifier.weight(1f).height(1.dp).background(if (done) colors.accent else colors.lineSoft))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepDetails(form: FightForm, titleMissing: Boolean, a: LogActions) {
    FightQuestion(stringResource(R.string.fights_q_about))
    FightTextBox(form.title, { v -> a.onForm { it.copy(title = v) } }, stringResource(R.string.fights_ph_title), maxLength = 60, isError = titleMissing)

    FightQuestion(stringResource(R.string.fights_q_happened))
    FightTextBox(form.reason, { v -> a.onForm { it.copy(reason = v) } }, stringResource(R.string.fights_ph_reason), minHeight = 110, maxLines = 6, maxLength = 500)
    Counter(form.reason.length, 500)

    FightQuestion(stringResource(R.string.fights_q_category))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        FightCategory.entries.forEach { c ->
            FightChip(stringResource(categoryLabel(c)), form.category == c.label, { a.onCategory(c.label) }, icon = categoryIcon(c))
        }
    }

    FightQuestion(stringResource(R.string.fights_q_intensity), stringResource(R.string.fights_q_intensity_hint))
    IntensityScale(form.intensity, a.onIntensity)

    FightQuestion(stringResource(R.string.fights_q_started))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        FightStarter.entries.forEach { s ->
            FightChip(stringResource(starterLabel(s)), form.started == s, { a.onStarter(s) }, icon = starterIcon(s))
        }
    }

    FightQuestion(stringResource(R.string.fights_q_status))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        FightChip(stringResource(R.string.fights_state_open), form.status == FightStatus.Open, { a.onStatus(FightStatus.Open) }, Modifier.weight(1f), Icons.Rounded.Warning)
        FightChip(stringResource(R.string.fights_state_resolved), form.status == FightStatus.Resolved, { a.onStatus(FightStatus.Resolved) }, Modifier.weight(1f), Icons.Rounded.CheckCircle)
    }
}

@Composable
private fun IntensityScale(value: Int, onPick: (Int) -> Unit) {
    val colors = TwinTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..10).forEach { n ->
                val on = n == value
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(TwinTheme.shapes.small)
                        .background(if (on) colors.brand else colors.glass)
                        .border(1.dp, if (on) colors.accent else colors.lineSoft, TwinTheme.shapes.small)
                        .clickable(role = Role.RadioButton) { onPick(n) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(n.toString(), style = MaterialTheme.typography.labelLarge, color = if (on) colors.ink else colors.inkMuted, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.fights_scale_small), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
            Text(stringResource(R.string.fights_scale_moderate), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
            Text(stringResource(R.string.fights_scale_high), style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepFeelings(form: FightForm, a: LogActions) {
    FightQuestion(stringResource(R.string.fights_q_feelings), stringResource(R.string.fights_q_feelings_hint))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        FightFeeling.entries.forEach { f ->
            FightChip(stringResource(feelingLabel(f)), f.value in form.feelings, { a.onFeeling(f.value) }, icon = feelingIcon(f))
        }
    }
    FightQuestion(stringResource(R.string.fights_q_need))
    FightTextBox(form.need, { v -> a.onForm { it.copy(need = v) } }, stringResource(R.string.fights_ph_need), minHeight = 110, maxLines = 6, maxLength = 300)
    Counter(form.need.length, 300)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepResolution(form: FightForm, a: LogActions) {
    val resolved = form.status == FightStatus.Resolved
    FightQuestion(stringResource(if (resolved) R.string.fights_q_approach_done else R.string.fights_q_approach_next))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        FightApproach.entries.forEach { ap ->
            FightChip(stringResource(approachLabel(ap, resolved)), ap.key in form.approach, { a.onApproach(ap.key) }, icon = approachIcon(ap))
        }
    }
    FightQuestion(stringResource(R.string.fights_q_notes))
    FightTextBox(form.notes, { v -> a.onForm { it.copy(notes = v) } }, stringResource(R.string.fights_ph_notes), minHeight = 110, maxLines = 6, maxLength = 300)
    Counter(form.notes.length, 300)
}

@Composable
private fun Counter(length: Int, max: Int) {
    Text(
        stringResource(R.string.fights_count, length, max),
        style = MaterialTheme.typography.labelSmall,
        color = TwinTheme.colors.inkFaint,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth(),
    )
}
