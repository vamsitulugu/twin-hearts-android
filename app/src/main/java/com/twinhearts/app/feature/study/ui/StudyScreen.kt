package com.twinhearts.app.feature.study.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AutoRefresh
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.OfflineBanner
import com.twinhearts.app.core.design.PetalBurst
import com.twinhearts.app.core.reminders.rememberNotificationPermissionRequest
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.study.domain.formatSlotTime
import com.twinhearts.app.feature.study.domain.hoursByDay
import com.twinhearts.app.feature.study.domain.model.StudyData
import com.twinhearts.app.feature.study.domain.model.StudyInsight
import com.twinhearts.app.feature.study.domain.model.StudyPerson
import com.twinhearts.app.feature.study.domain.model.StudySlot
import com.twinhearts.app.feature.study.domain.model.Subject
import com.twinhearts.app.feature.study.domain.slotsOfDay
import com.twinhearts.app.feature.study.domain.slotsWithOtherDay
import com.twinhearts.app.feature.study.domain.model.SlotAlarm
import com.twinhearts.app.feature.study.domain.studyHero
import com.twinhearts.app.feature.study.domain.studyInsight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** What the delete confirmation is about. */
private sealed interface DeleteTarget {
    data class OfSubject(val subject: Subject) : DeleteTarget
    data class OfSlot(val slot: StudySlot) : DeleteTarget
}

/** Monday first, like the web timetable. */
private val timetableDays = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

/**
 * Study page (web: `#page-study`): chapters done and scheduled hours, a study-rhythm tip, weekly
 * hours, my subjects and timetable (add, +1 chapter, tick a slot, delete), and my partner's
 * subjects and timetable to look at. Everything lives in the couple's shared state.
 */
@Composable
fun StudyScreen(
    onBack: () -> Unit,
    viewModel: StudyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSubjectForm by rememberSaveable { mutableStateOf(false) }
    var showSlotForm by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }
    val askNotifications = rememberNotificationPermissionRequest()
    AutoRefresh(onRefresh = viewModel::refresh)

    // One-off messages: toast once, then clear. A successful add also closes its form.
    val message = state.message
    val messageText = when (message) {
        null -> null
        StudyMessage.SubjectAdded -> stringResource(R.string.study_subject_added)
        StudyMessage.SlotAdded -> stringResource(R.string.study_slot_added)
        StudyMessage.Deleted -> stringResource(R.string.study_deleted)
        is StudyMessage.Failed -> message.error.asText()
    }
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        if (message == StudyMessage.SubjectAdded) showSubjectForm = false
        if (message == StudyMessage.SlotAdded) showSlotForm = false
        Toast.makeText(context, messageText.orEmpty(), Toast.LENGTH_SHORT).show()
        viewModel.messageShown()
    }

    val data = state.data
    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        TwinTopBar(
            title = stringResource(R.string.study_title),
            onBackClick = onBack,
            onSyncClick = viewModel::load,
        )
        if (state.offline) OfflineBanner()
        when {
            data == null && state.loading -> {
                Column(modifier = Modifier.padding(TwinTheme.spacing.screenEdge)) { LoadingSkeleton(lines = 4) }
            }
            data == null -> ErrorState(
                message = (state.error ?: AppError.Unknown).asText(),
                onRetry = viewModel::load,
            )
            else -> StudyContent(
                data = data,
                saving = state.saving,
                onAddSubject = { showSubjectForm = true },
                onAddSlot = { showSlotForm = true },
                onProgress = viewModel::changeProgress,
                onToggleSlot = viewModel::toggleSlot,
                onDelete = { deleteTarget = it },
            )
        }
    }
    PetalBurst(trigger = state.petals)
    }

    if (showSubjectForm) {
        AddSubjectSheet(
            saving = state.saving,
            onSave = viewModel::addSubject,
            onDismiss = { showSubjectForm = false },
        )
    }
    if (showSlotForm) {
        AddSlotSheet(
            saving = state.saving,
            onSave = { slot ->
                if (slot.alarm != SlotAlarm.None) askNotifications()
                viewModel.addSlot(slot)
            },
            onDismiss = { showSlotForm = false },
        )
    }
    deleteTarget?.let { target ->
        val text = when (target) {
            is DeleteTarget.OfSubject -> stringResource(R.string.study_delete_subject_message, target.subject.name)
            is DeleteTarget.OfSlot -> stringResource(R.string.study_delete_slot_message, target.slot.subject.ifBlank { "—" })
        }
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = TwinTheme.colors.card,
            title = { Text(stringResource(R.string.study_delete_title), color = TwinTheme.colors.ink) },
            text = { Text(text, color = TwinTheme.colors.inkMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (target) {
                            is DeleteTarget.OfSubject -> viewModel.deleteSubject(target.subject.id)
                            is DeleteTarget.OfSlot -> viewModel.deleteSlot(target.slot.id)
                        }
                        deleteTarget = null
                    },
                ) { Text(stringResource(R.string.study_delete_confirm), color = TwinTheme.colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.core_cancel)) }
            },
        )
    }
}

@Composable
private fun StudyContent(
    data: StudyData,
    saving: Boolean,
    onAddSubject: () -> Unit,
    onAddSlot: () -> Unit,
    onProgress: (id: String, delta: Int) -> Unit,
    onToggleSlot: (String) -> Unit,
    onDelete: (DeleteTarget) -> Unit,
) {
    val colors = TwinTheme.colors
    val me = data.me
    val hero = remember(me) { studyHero(me.subjects, me.slots) }
    val insight = remember(me.slots) { studyInsight(me.slots) }
    val weekHours = remember(me.slots) { hoursByDay(me.slots) }
    val today = remember { LocalDate.now().dayOfWeek }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TwinTheme.spacing.screenEdge)
            .padding(bottom = TwinTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        GlassCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
            ) {
                StudyRing(percent = hero.percent)
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                    Text(
                        stringResource(R.string.study_hero_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkMuted,
                    )
                    Text(
                        text = "%.1fh".format(hero.weeklyHours),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = SerifFamily,
                        color = colors.ink,
                    )
                    Text(
                        text = stringResource(
                            R.string.study_hero_sub,
                            hero.doneChapters,
                            hero.totalChapters,
                            hero.subjectCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                    )
                }
            }
        }

        InsightCard(insight)

        GlassCard {
            SectionTitle("📊 " + stringResource(R.string.study_weekly_hours))
            WeeklyHoursBars(weekHours, today, modifier = Modifier.padding(top = TwinTheme.spacing.m))
        }

        // ── Me ──
        val myName = data.myName.ifBlank { stringResource(R.string.study_me) }
        GlassCard {
            CardHeader(
                title = stringResource(R.string.study_person_study, myName),
                actionLabel = stringResource(R.string.study_add_subject),
                onAction = onAddSubject,
            )
            if (me.subjects.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.study_no_subjects_title),
                    message = stringResource(R.string.study_no_subjects_hint),
                    icon = Icons.Rounded.MenuBook,
                    actionLabel = stringResource(R.string.study_add_subject_long),
                    onAction = onAddSubject,
                )
            } else {
                me.subjects.forEach { subject ->
                    SubjectCard(
                        subject = subject,
                        editable = true,
                        saving = saving,
                        onProgress = { delta -> onProgress(subject.id, delta) },
                        onDelete = { onDelete(DeleteTarget.OfSubject(subject)) },
                    )
                }
            }
        }
        GlassCard {
            CardHeader(
                title = "🗓️ " + stringResource(R.string.study_person_timetable, myName),
                actionLabel = stringResource(R.string.study_add_slot),
                onAction = onAddSlot,
            )
            if (me.slots.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.study_no_slots_title),
                    message = stringResource(R.string.study_no_slots_hint),
                    actionLabel = stringResource(R.string.study_add_slot_long),
                    onAction = onAddSlot,
                )
            } else {
                Timetable(
                    slots = me.slots,
                    editable = true,
                    onToggle = onToggleSlot,
                    onDelete = { onDelete(DeleteTarget.OfSlot(it)) },
                )
            }
        }

        // ── Partner (read only) ──
        val partnerName = data.partnerName.ifBlank { stringResource(R.string.study_partner) }
        PartnerStudy(
            partnerName = partnerName,
            partner = data.partner,
            paired = data.paired,
        )
    }
}

@Composable
private fun PartnerStudy(partnerName: String, partner: StudyPerson, paired: Boolean) {
    GlassCard {
        SectionTitle(stringResource(R.string.study_person_study, partnerName))
        when {
            !paired -> MutedText(stringResource(R.string.study_partner_joins_study))
            partner.subjects.isEmpty() -> MutedText(stringResource(R.string.study_partner_no_subjects))
            else -> partner.subjects.forEach { subject ->
                SubjectCard(subject = subject, editable = false, saving = false, onProgress = {}, onDelete = {})
            }
        }
    }
    GlassCard {
        SectionTitle("🗓️ " + stringResource(R.string.study_person_timetable, partnerName))
        when {
            !paired -> MutedText(stringResource(R.string.study_partner_joins_timetable))
            partner.slots.isEmpty() -> MutedText(stringResource(R.string.study_partner_no_slots))
            else -> Timetable(slots = partner.slots, editable = false, onToggle = {}, onDelete = {})
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = TwinTheme.colors.ink)
}

@Composable
private fun MutedText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TwinTheme.colors.inkMuted,
        modifier = Modifier.padding(top = TwinTheme.spacing.s),
    )
}

@Composable
private fun CardHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TwinTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = TwinTheme.spacing.s),
        )
        GlassButton(
            text = actionLabel,
            onClick = onAction,
            style = GlassButtonStyle.Secondary,
            leadingIcon = Icons.Rounded.Add,
        )
    }
}

@Composable
private fun InsightCard(insight: StudyInsight) {
    val (icon, text) = when (insight) {
        StudyInsight.NoSlots -> "💡" to stringResource(R.string.study_insight_none)
        is StudyInsight.Great -> "🔥" to stringResource(R.string.study_insight_great, insight.percent)
        is StudyInsight.Some -> "📚" to stringResource(R.string.study_insight_some, insight.done, insight.total)
        is StudyInsight.Behind -> "⏰" to stringResource(R.string.study_insight_behind, insight.done, insight.total)
    }
    GlassCard(contentPadding = TwinTheme.spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TwinTheme.colors.ink)
        }
    }
}

@Composable
private fun SubjectCard(
    subject: Subject,
    editable: Boolean,
    saving: Boolean,
    onProgress: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = TwinTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TwinTheme.spacing.m)
            .clip(TwinTheme.shapes.small)
            .background(colors.glass)
            .padding(TwinTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${subject.emoji} ${subject.name}",
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.study_chapters_count, subject.done, subject.total),
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkMuted,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(TwinTheme.shapes.pill)
                .background(colors.lineSoft),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(subject.percent / 100f)
                    .height(8.dp)
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.accent),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (editable) "${subject.percent}%" else stringResource(R.string.study_percent_complete, subject.percent),
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkMuted,
            )
            if (editable) {
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(
                        icon = Icons.Rounded.Remove,
                        contentDescription = stringResource(R.string.study_chapter_minus_cd, subject.name),
                        onClick = { onProgress(-1) },
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.study_chapter_plus_cd, subject.name),
                        onClick = { onProgress(1) },
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.study_delete_cd, subject.name),
                            tint = colors.inkMuted,
                        )
                    }
                }
            }
        }
    }
}

/** Slots grouped by day, Monday first. Only my own timetable can be ticked or deleted. */
@Composable
private fun Timetable(
    slots: List<StudySlot>,
    editable: Boolean,
    onToggle: (String) -> Unit,
    onDelete: (StudySlot) -> Unit,
) {
    val colors = TwinTheme.colors
    timetableDays.forEach { day ->
        val daySlots = slotsOfDay(slots, day)
        if (daySlots.isEmpty()) return@forEach
        Text(
            text = day.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = TwinTheme.spacing.m, bottom = TwinTheme.spacing.xs),
        )
        daySlots.forEach { slot -> SlotRow(slot, editable, onToggle, onDelete) }
    }
    // Slots saved with a day name this app does not know are still shown, under that name.
    slotsWithOtherDay(slots).forEach { (label, daySlots) ->
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = TwinTheme.spacing.m, bottom = TwinTheme.spacing.xs),
        )
        daySlots.forEach { slot -> SlotRow(slot, editable, onToggle, onDelete) }
    }
}

@Composable
private fun SlotRow(
    slot: StudySlot,
    editable: Boolean,
    onToggle: (String) -> Unit,
    onDelete: (StudySlot) -> Unit,
) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TwinTheme.spacing.xs)
            .clip(TwinTheme.shapes.small)
            .background(if (slot.done) colors.neutralGlass.copy(alpha = 0.35f) else colors.glass)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Column {
            Text(
                text = formatSlotTime(slot.start),
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent,
            )
            Text(
                text = "– " + formatSlotTime(slot.end),
                style = MaterialTheme.typography.labelSmall,
                color = colors.inkMuted,
            )
        }
        Text(
            text = slot.subject,
            style = MaterialTheme.typography.bodyMedium,
            color = if (slot.done) colors.inkMuted else colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (editable) {
            SlotCheck(done = slot.done, subject = slot.subject, onToggle = { onToggle(slot.id) })
            IconButton(onClick = { onDelete(slot) }) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.study_delete_cd, slot.subject),
                    tint = colors.inkMuted,
                )
            }
        } else if (slot.done) {
            Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.study_slot_done_cd), tint = colors.success)
        }
    }
}

@Composable
private fun SlotCheck(done: Boolean, subject: String, onToggle: () -> Unit) {
    val colors = TwinTheme.colors
    val description = stringResource(R.string.study_slot_toggle_cd, subject)
    Box(
        modifier = Modifier
            .size(TwinTheme.shapes.minTouch)
            .toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(TwinTheme.shapes.circle)
                .background(if (done) colors.success else colors.lineSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Rounded.Check, contentDescription = description, tint = colors.ink, modifier = Modifier.size(16.dp))
        }
    }
}
