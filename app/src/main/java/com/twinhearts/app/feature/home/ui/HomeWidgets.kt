package com.twinhearts.app.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.home.domain.model.EventTarget
import com.twinhearts.app.feature.home.domain.model.EventTitle
import com.twinhearts.app.feature.home.domain.model.MoodBoard
import com.twinhearts.app.feature.home.domain.model.MoodKind
import com.twinhearts.app.feature.home.domain.model.UpcomingEvent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val EventCardMinWidth = 190.dp
private val EventCardMaxWidth = 260.dp
private val EventIconSize = 40.dp
private val GraphHeight = 72.dp
private val EmojiSize = 22.sp
private val QuickButtonMinHeight = 48.dp
private val DotRingMine = 6f
private val DotRingPartner = 4.5f
private val LineWidth = 2.5f
private val CalendarEmojiSize = 18.sp
private const val DAYS_IN_GRAPH = 7
private const val DAYS_IN_WEEK = 7
private const val SELECTED_FILL_ALPHA = 0.22f
private const val FUTURE_ALPHA = 0.35f
private const val GRAPH_VIEWBOX_HEIGHT = 72f // the web graph is drawn on a 280 x 72 box; we scale it

/**
 * Mood colours from the web (`MOODS[..].c`). They are the identity of each mood, not theme colours,
 * so they live here; see "CHANGE REQUEST: mood colour tokens" in SHARED_CHANGES.md.
 */
private fun MoodKind.color(): Color = when (this) {
    MoodKind.Excited -> Color(0xFFF5B301)
    MoodKind.Loved -> Color(0xFFC21A8B)
    MoodKind.Happy -> Color(0xFFFFD43B)
    MoodKind.Peaceful -> Color(0xFF8FC46A)
    MoodKind.Tired -> Color(0xFFB08968)
    MoodKind.Stressed -> Color(0xFFFF9248)
    MoodKind.Sad -> Color(0xFFC21A8B)
    MoodKind.Angry -> Color(0xFFEF5350)
}

private fun MoodKind.labelRes(): Int = when (this) {
    MoodKind.Excited -> R.string.home_mood_excited
    MoodKind.Loved -> R.string.home_mood_loved
    MoodKind.Happy -> R.string.home_mood_happy
    MoodKind.Peaceful -> R.string.home_mood_peaceful
    MoodKind.Tired -> R.string.home_mood_tired
    MoodKind.Stressed -> R.string.home_mood_stressed
    MoodKind.Sad -> R.string.home_mood_sad
    MoodKind.Angry -> R.string.home_mood_angry
}

private fun EventTarget.shortcut(): HomeShortcut = when (this) {
    EventTarget.Calendar -> HomeShortcut.Calendar
    EventTarget.LoveCounter -> HomeShortcut.LoveCounter
}

private fun ordinal(n: Int): String {
    val suffix = when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}

@Composable
private fun EventTitle.asText(partnerName: String): String = when (this) {
    is EventTitle.Custom -> text
    EventTitle.YourBirthday -> stringResource(R.string.home_event_your_birthday)
    EventTitle.PartnerBirthday ->
        stringResource(R.string.home_event_partner_birthday, partnerName.ifBlank { stringResource(R.string.home_partner_fallback) })
    is EventTitle.Anniversary -> stringResource(R.string.home_event_anniversary, ordinal(years))
    is EventTitle.MonthsTogether -> stringResource(R.string.home_event_months, months)
    is EventTitle.DayMark -> stringResource(R.string.home_event_day_mark, day)
}

private val shortDate: DateTimeFormatter get() = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

// ───────────────────────────── Coming up ─────────────────────────────

/** "Coming up" row: birthdays, anniversaries, day milestones and calendar events, soonest first. */
@Composable
internal fun HomeComingUp(
    events: List<UpcomingEvent>,
    partnerName: String,
    onShortcut: (HomeShortcut) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TwinTheme.spacing.screenEdge),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_coming_up),
                style = MaterialTheme.typography.titleLarge,
                color = TwinTheme.colors.ink,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(TwinTheme.shapes.small)
                    .clickable(role = Role.Button) { onShortcut(HomeShortcut.Calendar) }
                    .heightIn(min = TwinTheme.shapes.minTouch)
                    .padding(horizontal = TwinTheme.spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_calendar_link),
                    style = MaterialTheme.typography.labelLarge,
                    color = TwinTheme.colors.pinkSoft,
                )
            }
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = TwinTheme.spacing.screenEdge),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            if (events.isEmpty()) {
                EventCard(
                    icon = "📅",
                    title = stringResource(R.string.home_events_empty_title),
                    subtitle = stringResource(R.string.home_events_empty_hint),
                    badge = null,
                    highlight = false,
                    onClick = { onShortcut(HomeShortcut.Calendar) },
                )
            } else {
                events.forEach { event ->
                    EventCard(
                        icon = event.icon,
                        title = event.title.asText(partnerName),
                        subtitle = eventSubtitle(event),
                        badge = if (event.daysLeft == 0) "🎉" else stringResource(R.string.home_event_days_short, event.daysLeft),
                        highlight = event.daysLeft == 0,
                        onClick = { onShortcut(event.target.shortcut()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun eventSubtitle(event: UpcomingEvent): String {
    val whenText = when (event.daysLeft) {
        0 -> stringResource(R.string.home_event_today)
        1 -> stringResource(R.string.home_event_tomorrow)
        else -> stringResource(R.string.home_event_in_days, event.daysLeft)
    }
    val date = event.date.format(shortDate)
    return buildString {
        append(whenText).append(" · ").append(date)
        if (event.detail.isNotBlank()) append(" · ").append(event.detail)
    }
}

@Composable
private fun EventCard(
    icon: String,
    title: String,
    subtitle: String,
    badge: String?,
    highlight: Boolean,
    onClick: () -> Unit,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Row(
        modifier = Modifier
            .widthIn(min = EventCardMinWidth, max = EventCardMaxWidth)
            .clip(shape)
            .background(if (highlight) colors.accent.copy(alpha = SELECTED_FILL_ALPHA) else colors.glass)
            .border(1.dp, if (highlight) colors.accent else colors.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(EventIconSize)
                .clip(TwinTheme.shapes.circle)
                .background(colors.glass),
            contentAlignment = Alignment.Center,
        ) { Text(icon, fontSize = EmojiSize) }
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (badge != null) {
            Text(badge, style = MaterialTheme.typography.labelLarge, color = colors.pinkSoft)
        }
    }
}

// ───────────────────────────── Mood card ─────────────────────────────

/** Mood card: five one-tap moods for today, a 7-day graph for both of you, tap the card for the calendar. */
@Composable
internal fun HomeMoodCard(
    mood: MoodBoard,
    today: LocalDate,
    partnerName: String,
    paired: Boolean,
    onSetMood: (LocalDate, MoodKind?) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val todayKey = today.toString()
    val mine = mood.mine[todayKey]
    val theirs = if (paired) mood.partner[todayKey] else null
    val streak = mood.streak(today)
    val dash = stringResource(R.string.home_mood_dash)
    val partner = partnerName.ifBlank { stringResource(R.string.home_partner_fallback) }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpenCalendar,
        contentPadding = TwinTheme.spacing.m,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_mood_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (streak >= 2) stringResource(R.string.home_mood_streak, streak) else stringResource(R.string.home_calendar_link),
                style = MaterialTheme.typography.labelMedium,
                color = colors.pinkSoft,
            )
        }
        Spacer(Modifier.height(TwinTheme.spacing.s))
        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            MoodKind.quick.forEach { kind ->
                QuickMoodButton(
                    kind = kind,
                    selected = mine == kind,
                    modifier = Modifier.weight(1f),
                    onClick = { onSetMood(today, if (mine == kind) null else kind) },
                )
            }
        }
        Spacer(Modifier.height(TwinTheme.spacing.s))
        Text(
            text = when {
                !paired && mine != null -> stringResource(R.string.home_mood_today_single, mine.emoji, stringResource(mine.labelRes()))
                !paired -> stringResource(R.string.home_mood_ask)
                else -> stringResource(R.string.home_mood_pair_line, mine?.emoji ?: dash, partner, theirs?.emoji ?: dash)
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted,
        )
        Spacer(Modifier.height(TwinTheme.spacing.s))
        MoodGraph(mood = mood, today = today, paired = paired)
        Spacer(Modifier.height(TwinTheme.spacing.xs))
        MoodDayColumns(mood = mood, today = today, paired = paired)
        if (paired) {
            Spacer(Modifier.height(TwinTheme.spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l)) {
                Text(
                    stringResource(R.string.home_mood_legend_you, mine?.emoji ?: dash),
                    style = MaterialTheme.typography.labelMedium, color = colors.ink,
                )
                Text(
                    stringResource(R.string.home_mood_legend_partner, partner, theirs?.emoji ?: dash),
                    style = MaterialTheme.typography.labelMedium, color = colors.inkMuted,
                )
            }
        }
    }
}

@Composable
private fun QuickMoodButton(kind: MoodKind, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    val label = stringResource(kind.labelRes())
    Box(
        modifier = modifier
            .heightIn(min = QuickButtonMinHeight)
            .clip(shape)
            .background(if (selected) kind.color().copy(alpha = SELECTED_FILL_ALPHA) else colors.glass)
            .border(1.dp, if (selected) kind.color() else colors.lineSoft, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Text(kind.emoji, fontSize = EmojiSize) }
}

/** Two lines (me and my partner) over the last 7 days; the height of a dot is the mood score. */
@Composable
private fun MoodGraph(mood: MoodBoard, today: LocalDate, paired: Boolean) {
    val colors = TwinTheme.colors
    val days = (DAYS_IN_GRAPH - 1 downTo 0).map { today.minusDays(it.toLong()).toString() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(GraphHeight)
            .drawBehind {
                val colW = size.width / DAYS_IN_GRAPH
                fun yOf(score: Int) = size.height * (64f - (score - 1) / 4f * 56f) / GRAPH_VIEWBOX_HEIGHT
                listOf(8f, 36f, 64f).forEach { gy ->
                    val y = size.height * gy / GRAPH_VIEWBOX_HEIGHT
                    drawLine(colors.lineSoft, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }
                fun draw(moods: Map<String, MoodKind>, lineColor: Color, radius: Float, ring: Color) {
                    val points = days.mapIndexed { i, day ->
                        moods[day]?.let { Triple(colW * (i + 0.5f), yOf(it.score), it.color()) }
                    }
                    var run = mutableListOf<Offset>()
                    fun flush() {
                        if (run.size > 1) {
                            val path = Path().apply {
                                moveTo(run.first().x, run.first().y)
                                run.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(path, lineColor, style = Stroke(width = LineWidth.dp.toPx()))
                        }
                        run = mutableListOf()
                    }
                    points.forEach { p -> if (p != null) run += Offset(p.first, p.second) else flush() }
                    flush()
                    points.forEach { p ->
                        if (p != null) {
                            val center = Offset(p.first, p.second)
                            drawCircle(ring, radius.dp.toPx() + 1.5.dp.toPx(), center)
                            drawCircle(p.third, radius.dp.toPx(), center)
                        }
                    }
                }
                if (paired) draw(mood.partner, colors.inkMuted, DotRingPartner, Color.White)
                draw(mood.mine, colors.accent, DotRingMine, colors.card)
            },
    )
}

/** One column per day: my emoji, my partner's emoji, weekday and date number. */
@Composable
private fun MoodDayColumns(mood: MoodBoard, today: LocalDate, paired: Boolean) {
    val colors = TwinTheme.colors
    val todayLabel = stringResource(R.string.home_mood_today_label)
    Row(modifier = Modifier.fillMaxWidth()) {
        (DAYS_IN_GRAPH - 1 downTo 0).forEach { back ->
            val date = today.minusDays(back.toLong())
            val key = date.toString()
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(mood.mine[key]?.emoji ?: "·", fontSize = EmojiSize, color = colors.inkFaint)
                if (paired) Text(mood.partner[key]?.emoji ?: "·", fontSize = EmojiSize, color = colors.inkFaint)
                Text(
                    text = if (back == 0) todayLabel else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (back == 0) colors.pinkSoft else colors.inkMuted,
                    maxLines = 1,
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkMuted,
                )
            }
        }
    }
}

// ───────────────────────────── Mood Calendar ─────────────────────────────

/** Full-screen mood calendar (web: `#dwMoodOv`). Log or clear a mood for any past day, see my partner's. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoodCalendarDialog(
    mood: MoodBoard,
    today: LocalDate,
    partnerName: String,
    paired: Boolean,
    onSetMood: (LocalDate, MoodKind?) -> Unit,
    onClose: () -> Unit,
) {
    val colors = TwinTheme.colors
    val partner = partnerName.ifBlank { stringResource(R.string.home_partner_fallback) }
    var selected by rememberSaveable { mutableStateOf(today.toString()) }
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var who by rememberSaveable { mutableIntStateOf(0) }
    val ym = YearMonth.parse(month)
    val viewingMine = who == 0 || !paired
    val moods = if (viewingMine) mood.mine else mood.partner
    val selectedDate = LocalDate.parse(selected)
    val current = moods[selected]
    val dayText = selectedDate.format(shortDate)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        TwinBackground {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TwinTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.home_moodcal_close), tint = colors.ink)
                    }
                    Text(
                        stringResource(R.string.home_moodcal_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(TwinTheme.shapes.minTouch))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.m),
                    verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                ) {
                    if (paired) {
                        SegmentedTabs(
                            options = listOf(stringResource(R.string.home_moodcal_you), partner),
                            selectedIndex = who,
                            onSelect = { who = it },
                        )
                    }
                    Text(
                        text = when {
                            !viewingMine -> stringResource(R.string.home_moodcal_partner_on_day, partner, dayText)
                            selected == today.toString() -> stringResource(R.string.home_moodcal_ask_today)
                            else -> stringResource(R.string.home_moodcal_ask_day, dayText)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                    )
                    if (viewingMine) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
                        ) {
                            MoodKind.entries.forEach { kind ->
                                MoodChip(
                                    kind = kind,
                                    selected = current == kind,
                                    onClick = { onSetMood(selectedDate, if (current == kind) null else kind) },
                                )
                            }
                        }
                    }
                    if (paired) {
                        val theirs = mood.partner[selected]
                        Text(
                            text = if (viewingMine) {
                                if (theirs != null) {
                                    stringResource(R.string.home_moodcal_partner_felt, partner, theirs.emoji, stringResource(theirs.labelRes()))
                                } else {
                                    stringResource(R.string.home_moodcal_partner_none, partner)
                                }
                            } else if (current != null) {
                                "${current.emoji} ${stringResource(current.labelRes())}"
                            } else {
                                stringResource(R.string.home_moodcal_none_logged)
                            },
                            style = if (viewingMine) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleLarge,
                            color = colors.inkMuted,
                        )
                    }
                    Text(
                        text = if (viewingMine) stringResource(R.string.home_moodcal_hint_mine)
                        else stringResource(R.string.home_moodcal_hint_partner, partner),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint,
                    )
                    MonthBar(ym = ym, canGoNext = ym.isBefore(YearMonth.from(today)), onChange = { month = it.toString() })
                    CalendarGrid(
                        ym = ym, today = today, selected = selected, moods = moods,
                        onSelect = { selected = it.toString() },
                    )
                    MonthStats(ym = ym, moods = moods, viewingMine = viewingMine, partner = partner)
                    Spacer(Modifier.height(TwinTheme.spacing.xxl))
                }
            }
        }
    }
}

@Composable
private fun MoodChip(kind: MoodKind, selected: Boolean, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.pill
    Text(
        text = "${kind.emoji} ${stringResource(kind.labelRes())}",
        style = MaterialTheme.typography.labelLarge,
        color = colors.ink,
        modifier = Modifier
            .heightIn(min = TwinTheme.shapes.minTouch)
            .clip(shape)
            .background(if (selected) kind.color().copy(alpha = SELECTED_FILL_ALPHA) else colors.glass)
            .border(1.dp, if (selected) kind.color() else colors.lineSoft, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.m),
    )
}

@Composable
private fun MonthBar(ym: YearMonth, canGoNext: Boolean, onChange: (YearMonth) -> Unit) {
    val colors = TwinTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = { onChange(ym.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.home_moodcal_prev), tint = colors.ink)
        }
        Text(
            text = "${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}",
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onChange(ym.plusMonths(1)) }, enabled = canGoNext) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                stringResource(R.string.home_moodcal_next),
                tint = if (canGoNext) colors.ink else colors.inkFaint.copy(alpha = FUTURE_ALPHA),
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    ym: YearMonth,
    today: LocalDate,
    selected: String,
    moods: Map<String, MoodKind>,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = TwinTheme.colors
    val headers = stringArrayResource(R.array.home_moodcal_weekdays)
    val lead = ym.atDay(1).dayOfWeek.value - 1 // Monday first, like the web
    val cells: List<LocalDate?> = List(lead) { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
    Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        Row {
            headers.forEach {
                Text(
                    it, style = MaterialTheme.typography.labelMedium, color = colors.inkFaint,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(DAYS_IN_WEEK).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        CalendarCell(
                            date = date,
                            mood = moods[date.toString()],
                            isToday = date == today,
                            isSelected = date.toString() == selected,
                            isFuture = date.isAfter(today),
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(date) },
                        )
                    }
                }
                repeat(DAYS_IN_WEEK - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate,
    mood: MoodKind?,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.small
    val border = when {
        isSelected -> colors.accent
        isToday -> colors.pinkSoft
        else -> colors.lineSoft
    }
    Column(
        modifier = modifier
            .aspectRatio(0.8f)
            .clip(shape)
            .background(if (mood != null) mood.color().copy(alpha = SELECTED_FILL_ALPHA) else colors.glass)
            .border(if (isSelected || isToday) 2.dp else 1.dp, border, shape)
            .then(if (isFuture) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
            .padding(vertical = TwinTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (isFuture) colors.inkFaint.copy(alpha = FUTURE_ALPHA) else colors.ink,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        )
        Text(mood?.emoji.orEmpty(), fontSize = CalendarEmojiSize)
    }
}

@Composable
private fun MonthStats(ym: YearMonth, moods: Map<String, MoodKind>, viewingMine: Boolean, partner: String) {
    val colors = TwinTheme.colors
    val inMonth = moods.filterKeys { it.startsWith(ym.toString()) }
    val total = inMonth.size
    val monthName = ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(total.toString(), style = MaterialTheme.typography.displayMedium, color = colors.ink)
        Text(
            text = if (viewingMine) pluralStringResource(R.plurals.home_moodcal_stats_mine, total, monthName)
            else pluralStringResource(R.plurals.home_moodcal_stats_partner, total, monthName, partner),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
        )
        if (total > 0) {
            Spacer(Modifier.height(TwinTheme.spacing.s))
            FlowRowCenter(inMonth.values.groupingBy { it }.eachCount().entries.sortedByDescending { it.value })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCenter(counts: List<Map.Entry<MoodKind, Int>>) {
    val colors = TwinTheme.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        counts.forEach { (kind, n) ->
            Row(
                modifier = Modifier
                    .clip(TwinTheme.shapes.pill)
                    .background(colors.glass)
                    .border(1.dp, kind.color(), TwinTheme.shapes.pill)
                    .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
            ) {
                Text("$n", style = MaterialTheme.typography.labelLarge, color = colors.ink)
                Text(stringResource(kind.labelRes()), style = MaterialTheme.typography.labelLarge, color = colors.inkMuted)
            }
        }
    }
}

// ───────────────────────────── Previews ─────────────────────────────

private val previewToday = LocalDate.of(2026, 10, 26)
private val previewMood = MoodBoard(
    mine = mapOf("2026-10-26" to MoodKind.Happy, "2026-10-25" to MoodKind.Loved, "2026-10-23" to MoodKind.Sad),
    partner = mapOf("2026-10-26" to MoodKind.Peaceful, "2026-10-24" to MoodKind.Tired),
)

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360)
@Composable
private fun MoodCardPreview() {
    TwinTheme {
        TwinBackground {
            Column(Modifier.padding(TwinTheme.spacing.screenEdge)) {
                HomeMoodCard(previewMood, previewToday, "Asha", true, { _, _ -> }, {})
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, fontScale = 1.3f)
@Composable
private fun MoodCardEmptyLargeFontPreview() {
    TwinTheme {
        TwinBackground {
            Column(Modifier.padding(TwinTheme.spacing.screenEdge)) {
                HomeMoodCard(MoodBoard(), previewToday, "", false, { _, _ -> }, {})
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360)
@Composable
private fun ComingUpPreview() {
    val events = listOf(
        UpcomingEvent("🎂", EventTitle.PartnerBirthday, previewToday.plusDays(0), 0, "", EventTarget.LoveCounter),
        UpcomingEvent("🥂", EventTitle.MonthsTogether(14), previewToday.plusDays(3), 3, "", EventTarget.LoveCounter),
        UpcomingEvent("🌹", EventTitle.Custom("Dinner date"), previewToday.plusDays(9), 9, "8:00 PM", EventTarget.Calendar),
    )
    TwinTheme { TwinBackground { HomeComingUp(events, "Asha") {} } }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360)
@Composable
private fun ComingUpEmptyPreview() {
    TwinTheme { TwinBackground { HomeComingUp(emptyList(), "") {} } }
}
