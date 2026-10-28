package com.twinhearts.app.feature.calendar.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.calendar.domain.eventsOn
import com.twinhearts.app.feature.calendar.domain.formatEventTime
import com.twinhearts.app.feature.calendar.domain.model.CalendarEvent
import com.twinhearts.app.feature.calendar.domain.model.EventAlarm
import com.twinhearts.app.feature.calendar.domain.monthCells
import com.twinhearts.app.feature.calendar.domain.upcoming
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private val WeekOrder = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

/**
 * Calendar page (web: `#page-calendar`): a month grid with a dot on days that have events,
 * the events of the tapped day, and the next ten upcoming events. Events live in the couple's
 * shared state, so the web app and this app show the same list.
 */
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CalendarEvent?>(null) }

    // One-off messages: toast once, then clear. A successful add also closes the form.
    val message = state.message
    val messageText = when (message) {
        null -> null
        CalendarMessage.Added -> stringResource(R.string.calendar_added)
        CalendarMessage.Deleted -> stringResource(R.string.calendar_deleted)
        is CalendarMessage.Failed -> message.error.asText()
    }
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        if (message == CalendarMessage.Added) showAdd = false
        Toast.makeText(context, messageText.orEmpty(), Toast.LENGTH_SHORT).show()
        viewModel.messageShown()
    }

    val today = remember { LocalDate.now() }

    Column(modifier = Modifier.fillMaxSize()) {
        TwinTopBar(
            title = stringResource(R.string.calendar_title),
            onBackClick = onBack,
            onSyncClick = viewModel::load,
        )
        when {
            state.loading && state.events.isEmpty() -> {
                Column(modifier = Modifier.padding(TwinTheme.spacing.screenEdge)) { LoadingSkeleton(lines = 4) }
            }
            state.error != null && state.events.isEmpty() -> {
                ErrorState(message = state.error!!.asText(), onRetry = viewModel::load)
            }
            else -> {
                CalendarContent(
                    state = state,
                    today = today,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    onToday = viewModel::goToToday,
                    onSelectDay = viewModel::selectDay,
                    onAdd = { showAdd = true },
                    onDelete = { deleteTarget = it },
                )
            }
        }
    }

    if (showAdd) {
        AddEventSheet(
            initialDate = state.selected ?: today,
            saving = state.saving,
            onSave = viewModel::addEvent,
            onDismiss = { showAdd = false },
        )
    }
    deleteTarget?.let { event ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = TwinTheme.colors.card,
            title = { Text(stringResource(R.string.calendar_delete_title), color = TwinTheme.colors.ink) },
            text = {
                Text(stringResource(R.string.calendar_delete_message, event.title), color = TwinTheme.colors.inkMuted)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEvent(event.id)
                        deleteTarget = null
                    },
                ) { Text(stringResource(R.string.calendar_delete_confirm), color = TwinTheme.colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.core_cancel)) }
            },
        )
    }
}

@Composable
private fun CalendarContent(
    state: CalendarUiState,
    today: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onAdd: () -> Unit,
    onDelete: (CalendarEvent) -> Unit,
) {
    val datesWithEvents = remember(state.events) { state.events.map { it.date }.toSet() }
    val selected = state.selected
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TwinTheme.spacing.screenEdge)
            .padding(bottom = TwinTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        GlassCard {
            MonthHeader(state.month, onPrevious, onNext)
            WeekdayHeader()
            MonthGrid(
                month = state.month,
                today = today,
                selected = selected,
                datesWithEvents = datesWithEvents,
                onSelectDay = onSelectDay,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            GlassButton(
                text = stringResource(R.string.calendar_add_event),
                onClick = onAdd,
                leadingIcon = Icons.Rounded.Add,
                modifier = Modifier.weight(1f),
            )
            if (state.month != YearMonth.from(today) || selected != today) {
                GlassButton(
                    text = stringResource(R.string.calendar_today),
                    onClick = onToday,
                    style = GlassButtonStyle.Secondary,
                )
            }
        }

        if (selected != null) {
            val dayEvents = eventsOn(state.events, selected)
            GlassCard {
                Text(
                    text = stringResource(
                        R.string.calendar_day_events,
                        selected.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = TwinTheme.colors.ink,
                )
                if (dayEvents.isEmpty()) {
                    Text(
                        text = stringResource(R.string.calendar_day_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TwinTheme.colors.inkMuted,
                        modifier = Modifier.padding(top = TwinTheme.spacing.s),
                    )
                } else {
                    dayEvents.forEach { event ->
                        EventRow(event, showDate = false, onDelete = { onDelete(event) })
                    }
                }
            }
        }

        val next = remember(state.events, today) { upcoming(state.events, today) }
        GlassCard {
            Text(
                text = stringResource(R.string.calendar_upcoming),
                style = MaterialTheme.typography.titleMedium,
                color = TwinTheme.colors.ink,
            )
            if (next.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.calendar_upcoming_empty_title),
                    message = stringResource(R.string.calendar_upcoming_empty_hint),
                    icon = Icons.Rounded.CalendarMonth,
                    actionLabel = stringResource(R.string.calendar_add_event),
                    onAction = onAdd,
                )
            } else {
                next.forEach { event ->
                    EventRow(event, showDate = true, onDelete = { onDelete(event) })
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    val colors = TwinTheme.colors
    val title = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthArrow(rotation = 180f, description = stringResource(R.string.calendar_prev_month), onClick = onPrevious)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = SerifFamily,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        MonthArrow(rotation = 0f, description = stringResource(R.string.calendar_next_month), onClick = onNext)
    }
}

@Composable
private fun MonthArrow(rotation: Float, description: String, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Box(
        modifier = Modifier
            .size(TwinTheme.shapes.minTouch)
            .clip(TwinTheme.shapes.circle)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.circle)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = description,
            tint = colors.ink,
            modifier = Modifier.rotate(rotation),
        )
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        WeekOrder.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = TwinTheme.colors.inkMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = TwinTheme.spacing.xs),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate?,
    datesWithEvents: Set<LocalDate>,
    onSelectDay: (LocalDate) -> Unit,
) {
    val cells = remember(month) { monthCells(month) }
    cells.chunked(WeekOrder.size).forEach { week ->
        Row(modifier = Modifier.fillMaxWidth()) {
            week.forEach { date ->
                Box(modifier = Modifier.weight(1f)) {
                    if (date != null) {
                        DayCell(
                            date = date,
                            isToday = date == today,
                            isSelected = date == selected,
                            hasEvents = date in datesWithEvents,
                            onClick = { onSelectDay(date) },
                        )
                    }
                }
            }
            // The last week is shorter: fill it so the cells keep the same width.
            repeat(WeekOrder.size - week.size) { Box(modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit,
) {
    val colors = TwinTheme.colors
    val eventsHint = stringResource(R.string.calendar_has_events)
    val description = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) +
        if (hasEvents) ", $eventsHint" else ""
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(TwinTheme.shapes.circle)
            .background(if (isSelected) colors.neutralGlass else androidx.compose.ui.graphics.Color.Transparent)
            .then(if (isToday) Modifier.border(1.5.dp, colors.accent, TwinTheme.shapes.circle) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isToday || isSelected) colors.ink else colors.inkSoft,
        )
        if (hasEvents) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(5.dp)
                    .clip(TwinTheme.shapes.circle)
                    .background(colors.pinkSoft),
            )
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, showDate: Boolean, onDelete: () -> Unit) {
    val colors = TwinTheme.colors
    val parts = buildList {
        if (showDate) add(event.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
        if (event.time.isNotBlank()) add(formatEventTime(event.time))
        if (event.alarm != EventAlarm.None) add("🔔")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Text(event.category.emoji, style = MaterialTheme.typography.titleLarge)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (parts.isNotEmpty()) {
                Text(
                    text = parts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                )
            }
            if (event.note.isNotBlank()) {
                Text(
                    text = event.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.calendar_delete_cd, event.title),
                tint = colors.inkMuted,
            )
        }
    }
}
