package com.twinhearts.app.feature.myspace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.myspace.domain.MySpaceLogic
import com.twinhearts.app.feature.myspace.domain.model.DefaultMood
import com.twinhearts.app.feature.myspace.domain.model.JournalEntry
import com.twinhearts.app.feature.myspace.domain.model.JournalMoods
import com.twinhearts.app.feature.myspace.domain.model.MySpaceData
import com.twinhearts.app.feature.myspace.domain.model.NewJournal
import com.twinhearts.app.feature.myspace.domain.model.NewNote
import com.twinhearts.app.feature.myspace.domain.model.Note
import com.twinhearts.app.feature.myspace.domain.model.Visibility
import java.time.LocalDate
import java.time.LocalTime

/** The ticked items of the tab being shown, and the taps that change them. */
internal class Selection(
    val mode: Boolean,
    val ids: Set<String>,
    val onLongPress: (String) -> Unit,
    val onToggle: (String) -> Unit,
    /** Opens the delete question for just this one item (its trash button). */
    val onDeleteOne: (String) -> Unit,
)

/** Tiny pill that says who can see an item (web `privBadgeHtml`). */
@Composable
internal fun PrivacyBadge(visibility: Visibility) {
    val colors = TwinTheme.colors
    Text(
        text = "${visibility.emoji} ${stringResource(visibility.labelRes)}",
        style = MaterialTheme.typography.labelSmall,
        color = colors.inkMuted,
        modifier = Modifier
            .clip(TwinTheme.shapes.pill)
            .background(colors.glass)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
internal fun TrashButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            Icons.Rounded.Delete,
            contentDescription = stringResource(R.string.myspace_delete),
            tint = TwinTheme.colors.inkFaint,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ───────────────────────────── Journal ─────────────────────────────

/** Web "Journal": mood, a free write box, who sees it, the partner's shared entries, then my entries. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun JournalTab(
    data: MySpaceData,
    saving: Boolean,
    resetKey: Int,
    selection: Selection,
    onSave: (NewJournal) -> Unit,
) {
    var mood by rememberSaveable(resetKey) { mutableStateOf(DefaultMood) }
    var body by rememberSaveable(resetKey) { mutableStateOf("") }
    var visibility by rememberSaveable(resetKey) { mutableStateOf(Visibility.Self) }
    val focus = remember { FocusRequester() }
    val mine = remember(data.journal, data.role) { MySpaceLogic.myJournal(data.journal, data.role) }
    val partners = remember(data.journal, data.role) { MySpaceLogic.partnerJournal(data.journal, data.role) }

    UserBlockHead(name = data.myName, title = stringResource(R.string.myspace_journal_mine, data.myName))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        JournalMoods.forEach { option ->
            TwinFilterChip(label = option, selected = option == mood, onClick = { mood = option })
        }
    }
    TwinTextArea(
        value = body,
        onValueChange = { body = it },
        placeholder = stringResource(R.string.myspace_journal_hint),
        focusRequester = focus,
    )
    GlassButton(
        text = stringResource(R.string.myspace_journal_save),
        onClick = { onSave(NewJournal(body.trim(), mood, visibility)) },
        style = GlassButtonStyle.Secondary,
        enabled = body.isNotBlank(),
        loading = saving,
        leadingIcon = Icons.Rounded.Edit,
    )
    VisibilitySelect(selected = visibility, onSelect = { visibility = it })

    UserBlockHead(name = data.partnerName, title = stringResource(R.string.myspace_journal_mine, data.partnerName))
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = TwinTheme.colors.inkMuted, modifier = Modifier.size(16.dp))
            Text(
                stringResource(R.string.myspace_journal_partner_card).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = TwinTheme.colors.ink,
            )
        }
        if (partners.isEmpty()) {
            Text(
                stringResource(R.string.myspace_journal_partner_empty),
                style = MaterialTheme.typography.bodySmall,
                color = TwinTheme.colors.inkFaint,
                modifier = Modifier.padding(top = TwinTheme.spacing.s),
            )
        } else {
            // The partner's entries are read-only here: only they can change or delete them.
            partners.forEach { entry ->
                Column(modifier = Modifier.padding(top = TwinTheme.spacing.m)) { JournalBody(entry, onDelete = null) }
            }
        }
    }

    SectionTitle(stringResource(R.string.myspace_journal_entries).uppercase())
    if (mine.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.myspace_journal_empty_title),
            message = stringResource(R.string.myspace_journal_empty_hint),
            icon = Icons.Rounded.MenuBook,
            actionLabel = stringResource(R.string.myspace_journal_empty_action),
            onAction = { focus.requestFocus() },
        )
    } else {
        mine.forEach { JournalCard(it, selection) }
    }
}

@Composable
private fun JournalCard(entry: JournalEntry, selection: Selection) {
    SelectableCard(
        selectMode = selection.mode,
        selected = entry.id in selection.ids,
        onLongPress = { selection.onLongPress(entry.id) },
        onToggle = { selection.onToggle(entry.id) },
    ) {
        JournalBody(entry, onDelete = if (selection.mode) null else ({ selection.onDeleteOne(entry.id) }))
    }
}

@Composable
private fun JournalBody(entry: JournalEntry, onDelete: (() -> Unit)?) {
    val colors = TwinTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = formatDay(entry.date).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.inkFaint,
        )
        if (onDelete != null) TrashButton(onDelete)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), modifier = Modifier.padding(vertical = TwinTheme.spacing.xs)) {
        if (entry.mood.isNotBlank()) {
            Text(
                text = entry.mood,
                style = MaterialTheme.typography.labelMedium,
                color = colors.ink,
                modifier = Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.glass)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
        PrivacyBadge(entry.visibility)
    }
    Text(entry.body, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
}

// ───────────────────────────── Notes ─────────────────────────────

/** Web "📝 Notes": a quick love note with an optional alarm, shown as soft tinted cards. */
@Composable
internal fun NotesTab(
    data: MySpaceData,
    saving: Boolean,
    resetKey: Int,
    selection: Selection,
    onSave: (NewNote) -> Unit,
) {
    var text by rememberSaveable(resetKey) { mutableStateOf("") }
    var visibility by rememberSaveable(resetKey) { mutableStateOf(Visibility.Both) }
    var date by rememberSaveable(resetKey) { mutableStateOf("") } // yyyy-MM-dd or empty
    var time by rememberSaveable(resetKey) { mutableStateOf("") } // HH:mm or empty
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    val notes = remember(data.notes, data.role) { MySpaceLogic.visibleNotes(data.notes, data.role) }

    TwinTextField(
        value = text,
        onValueChange = { text = it },
        label = stringResource(R.string.myspace_note_label),
        placeholder = stringResource(R.string.myspace_note_hint),
        maxLength = 300,
    )
    VisibilitySelect(selected = visibility, onSelect = { visibility = it })
    PickerRow(
        label = stringResource(R.string.myspace_note_reminder),
        value = if (date.isBlank()) stringResource(R.string.myspace_no_date) else formatDay(date),
        onClick = { showDate = true },
        trailing = if (date.isBlank()) null else stringResource(R.string.myspace_clear),
        onTrailingClick = {
            date = ""
            time = ""
        },
    )
    if (date.isNotBlank()) {
        PickerRow(
            label = stringResource(R.string.myspace_time),
            value = if (time.isBlank()) stringResource(R.string.myspace_default_time) else formatClock(time),
            onClick = { showTime = true },
        )
    }
    GlassButton(
        text = stringResource(R.string.myspace_note_add),
        onClick = { onSave(NewNote(text.trim(), visibility, date, time)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = text.isNotBlank(),
        loading = saving,
    )

    if (notes.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.myspace_notes_empty_title),
            message = stringResource(R.string.myspace_notes_empty_hint),
        )
    } else {
        notes.forEach { NoteCard(it, data, selection) }
    }

    if (showDate) {
        DatePickDialog(
            initial = MySpaceLogic.parseDateOrNull(date) ?: LocalDate.now(),
            onPick = {
                date = it.toString()
                showDate = false
            },
            onDismiss = { showDate = false },
        )
    }
    if (showTime) {
        TimePickDialog(
            initial = time.takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
            onPick = {
                time = it.toStoredTime()
                showTime = false
            },
            onDismiss = { showTime = false },
        )
    }
}

@Composable
private fun NoteCard(note: Note, data: MySpaceData, selection: Selection) {
    val colors = TwinTheme.colors
    val mine = MySpaceLogic.isMine(note.by, data.role)
    SelectableCard(
        selectMode = selection.mode,
        selected = note.id in selection.ids,
        onLongPress = { selection.onLongPress(note.id) },
        onToggle = { selection.onToggle(note.id) },
        fill = noteTint(note.color) ?: colors.glass,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
                PrivacyBadge(note.visibility)
                Text(
                    text = "${if (mine) data.myName else data.partnerName} · ${formatDay(note.date)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!selection.mode && mine) TrashButton { selection.onDeleteOne(note.id) }
        }
        Text(note.text, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
        if (note.reminderDate.isNotBlank()) {
            Text(
                text = "🔔 ${formatDay(note.reminderDate)} ${formatClock(note.reminderTime)}".trim(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.pinkSoft,
                modifier = Modifier.padding(top = TwinTheme.spacing.s),
            )
        }
    }
}

private val RgbaPattern = Regex("""rgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})\s*(?:,\s*([0-9.]+)\s*)?\)""")

/** The web saves note colours as CSS `rgba(r,g,b,a)`; this turns one into a colour, or null if unreadable. */
internal fun noteTint(css: String): Color? {
    val m = RgbaPattern.matchEntire(css.trim()) ?: return null
    val (r, g, b) = listOf(1, 2, 3).map { (m.groupValues[it].toIntOrNull() ?: return null).coerceIn(0, 255) }
    val a = m.groupValues[4].toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f
    // Slightly stronger than the web so the tint still shows on the dark background.
    return Color(r / 255f, g / 255f, b / 255f, (a * 1.6f).coerceAtMost(1f))
}
