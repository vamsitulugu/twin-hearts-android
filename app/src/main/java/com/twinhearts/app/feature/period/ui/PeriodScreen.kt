package com.twinhearts.app.feature.period.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.MoodBad
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.period.domain.cycleInsight
import com.twinhearts.app.feature.period.domain.dayMarks
import com.twinhearts.app.feature.period.domain.model.CycleBar
import com.twinhearts.app.feature.period.domain.model.CycleInsight
import com.twinhearts.app.feature.period.domain.model.CyclePhase
import com.twinhearts.app.feature.period.domain.model.DayMark
import com.twinhearts.app.feature.period.domain.model.Period
import com.twinhearts.app.feature.period.domain.model.PeriodAccess
import com.twinhearts.app.feature.period.domain.model.PeriodData
import com.twinhearts.app.feature.period.domain.model.Symptom
import com.twinhearts.app.feature.period.domain.phaseInfo
import com.twinhearts.app.feature.period.domain.recentCycleBars
import com.twinhearts.app.feature.period.domain.topSymptoms
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
private val BarMaxHeight = 60.dp
private const val BAR_MIN_SCALE_DAYS = 28

/**
 * Period Tracker page (web: `#page-period`). The girl logs periods and daily symptoms; the boy
 * sees everything read-only. Data lives in the couple's shared state, so web and app match.
 */
@Composable
fun PeriodScreen(
    onBack: () -> Unit,
    viewModel: PeriodViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showLog by rememberSaveable { mutableStateOf(false) }
    var showChooser by rememberSaveable { mutableStateOf(false) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Period?>(null) }

    val message = state.message
    val messageText = when (message) {
        null -> null
        PeriodMessage.PeriodLogged -> stringResource(R.string.period_logged)
        PeriodMessage.SymptomsSaved -> stringResource(R.string.period_symptoms_saved)
        PeriodMessage.GenderSaved -> stringResource(R.string.period_gender_saved)
        is PeriodMessage.NoAccess -> stringResource(
            if (message.access == PeriodAccess.ReadOnly) R.string.period_only_girl else R.string.period_choose_first,
        )
        is PeriodMessage.Failed -> message.error.asText()
    }
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        if (message == PeriodMessage.PeriodLogged) showLog = false
        if (message == PeriodMessage.GenderSaved) showChooser = false
        Toast.makeText(context, messageText.orEmpty(), Toast.LENGTH_SHORT).show()
        viewModel.messageShown()
    }

    // Like the web: ask once per visit when we do not know if this person is a girl or a boy.
    val access = state.data?.access
    LaunchedEffect(access) {
        if (access == PeriodAccess.Unknown && !askedOnce) {
            askedOnce = true
            showChooser = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TwinTopBar(
            title = stringResource(R.string.period_title),
            showSidebarButton = true,
            onSyncClick = viewModel::load,
        )
        val data = state.data
        val error = state.error
        when {
            state.loading && data == null -> {
                Column(modifier = Modifier.padding(TwinTheme.spacing.screenEdge)) { LoadingSkeleton(lines = 5) }
            }
            error != null && data == null -> {
                ErrorState(message = error.asText(), onRetry = viewModel::load)
            }
            data != null -> PeriodContent(
                data = data,
                today = remember { LocalDate.now() },
                ticked = state.todaySymptoms,
                saving = state.saving,
                onLog = { showLog = true },
                onChoose = { showChooser = true },
                onToggleSymptom = viewModel::toggleSymptom,
                onSaveSymptoms = viewModel::saveSymptoms,
                onDelete = { deleteTarget = it },
            )
        }
    }

    if (showLog) {
        LogPeriodSheet(saving = state.saving, onSave = viewModel::logPeriod, onDismiss = { showLog = false })
    }
    if (showChooser) {
        GenderDialog(onChoose = viewModel::chooseGender, onDismiss = { showChooser = false })
    }
    deleteTarget?.let { period ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = TwinTheme.colors.card,
            title = { Text(stringResource(R.string.period_delete_title), color = TwinTheme.colors.ink) },
            text = { Text(stringResource(R.string.period_delete_message), color = TwinTheme.colors.inkMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePeriod(period.id)
                        deleteTarget = null
                    },
                ) { Text(stringResource(R.string.period_delete_confirm), color = TwinTheme.colors.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.core_cancel)) } },
        )
    }
}

@Composable
private fun PeriodContent(
    data: PeriodData,
    today: LocalDate,
    ticked: Set<String>,
    saving: Boolean,
    onLog: () -> Unit,
    onChoose: () -> Unit,
    onToggleSymptom: (Symptom) -> Unit,
    onSaveSymptoms: () -> Unit,
    onDelete: (Period) -> Unit,
) {
    val canEdit = data.access == PeriodAccess.Edit
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TwinTheme.spacing.screenEdge)
            .padding(bottom = TwinTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        if (!canEdit) AccessBanner(data, onChoose)
        InsightCard(cycleInsight(data.periods))
        HeroCard(data = data, today = today, canEdit = canEdit, onLog = onLog)
        CalendarCard(data.periods, today)

        val bars = recentCycleBars(data.periods)
        if (bars.isNotEmpty()) CycleBarsCard(bars)
        val common = topSymptoms(data.symptoms)
        if (common.isNotEmpty()) CommonSymptomsCard(common)

        SymptomsCard(ticked = ticked, canEdit = canEdit, saving = saving, onToggle = onToggleSymptom, onSave = onSaveSymptoms)
        HistoryCard(data.periods, canEdit, onDelete)
    }
}

@Composable
private fun AccessBanner(data: PeriodData, onChoose: () -> Unit) {
    val colors = TwinTheme.colors
    GlassCard {
        if (data.access == PeriodAccess.ReadOnly) {
            Text(
                text = stringResource(R.string.period_view_only, data.partnerName.ifBlank { stringResource(R.string.period_partner_fallback) }),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
            )
        } else {
            Text(
                text = stringResource(R.string.period_choose_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
            )
            GlassButton(
                text = stringResource(R.string.period_choose),
                onClick = onChoose,
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.padding(top = TwinTheme.spacing.s),
            )
        }
    }
}

@Composable
private fun InsightCard(insight: CycleInsight) {
    val colors = TwinTheme.colors
    val text = when (insight) {
        CycleInsight.NeedMore -> stringResource(R.string.period_insight_need_more)
        is CycleInsight.Regular -> stringResource(R.string.period_insight_regular, insight.averageDays)
        is CycleInsight.Varies -> stringResource(R.string.period_insight_varies, insight.averageDays, insight.varianceDays)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TwinTheme.shapes.card)
            .background(colors.brand.copy(alpha = 0.18f))
            .border(1.dp, colors.brand.copy(alpha = 0.4f), TwinTheme.shapes.card)
            .padding(TwinTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
    }
}

@Composable
private fun HeroCard(data: PeriodData, today: LocalDate, canEdit: Boolean, onLog: () -> Unit) {
    val colors = TwinTheme.colors
    val info = phaseInfo(data.periods, today)
    GlassCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                Text(
                    text = stringResource(R.string.period_hero_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = SerifFamily,
                    color = colors.ink,
                )
                PhaseBadge(info.phase)
            }
            if (canEdit) {
                GlassButton(
                    text = stringResource(R.string.period_log_button),
                    onClick = onLog,
                    style = GlassButtonStyle.Secondary,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TwinTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            StatBox(info.daysToNext?.toString() ?: stringResource(R.string.period_dash), stringResource(R.string.period_stat_days_to_next), Modifier.weight(1f))
            StatBox(info.cycleLength.toString(), stringResource(R.string.period_stat_cycle_length), Modifier.weight(1f))
            StatBox(info.flowDays.toString(), stringResource(R.string.period_stat_flow_days), Modifier.weight(1f))
        }
        Text(
            text = "💡 " + stringResource(info.phase.noteRes),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            modifier = Modifier
                .fillMaxWidth()
                .clip(TwinTheme.shapes.small)
                .background(colors.glass)
                .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
                .padding(TwinTheme.spacing.m),
        )
    }
}

@Composable
private fun PhaseBadge(phase: CyclePhase) {
    val colors = TwinTheme.colors
    val tone = when (phase) {
        CyclePhase.Menstrual -> colors.accent
        CyclePhase.Follicular, CyclePhase.Unknown -> PeriodAmber
        CyclePhase.Ovulation -> colors.success
        CyclePhase.Luteal -> colors.orchid
    }
    Text(
        text = stringResource(phase.labelRes).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = tone,
        modifier = Modifier
            .clip(TwinTheme.shapes.pill)
            .background(tone.copy(alpha = 0.14f))
            .border(1.dp, tone.copy(alpha = 0.4f), TwinTheme.shapes.pill)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
    )
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Column(
        modifier = modifier
            .clip(TwinTheme.shapes.small)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
            .padding(vertical = TwinTheme.spacing.m, horizontal = TwinTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontFamily = SerifFamily, color = colors.ink)
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.inkFaint,
            textAlign = TextAlign.Center,
        )
    }
}

/** Card whose icon sits on the left and serif title on the right, as in the reference. */
@Composable
private fun TitledCard(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    val colors = TwinTheme.colors
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TwinTheme.spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontFamily = SerifFamily, color = colors.ink)
        }
        content()
    }
}

@Composable
private fun CalendarCard(periods: List<Period>, today: LocalDate) {
    val colors = TwinTheme.colors
    val month = YearMonth.from(today)
    val marks = remember(periods, month) { dayMarks(periods, month) }
    val offset = month.atDay(1).dayOfWeek.value % 7 // Sunday = 0
    val rows = (offset + month.lengthOfMonth() + 6) / 7
    TitledCard(Icons.Rounded.CalendarMonth, stringResource(R.string.period_calendar_title)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            WeekOrder.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkFaint,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = TwinTheme.spacing.xs),
                )
            }
        }
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val day = row * 7 + col - offset + 1
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..month.lengthOfMonth()) DayCell(day, marks[day], isToday = day == today.dayOfMonth)
                    }
                }
            }
        }
        Legend()
    }
}

@Composable
private fun DayCell(day: Int, mark: DayMark?, isToday: Boolean) {
    val colors = TwinTheme.colors
    val fill = when (mark) {
        DayMark.Period -> colors.brand
        DayMark.Ovulation -> colors.success
        DayMark.Fertile -> PeriodAmber
        null -> if (isToday) colors.brand else Color.Transparent
    }
    val textColor = if (mark == DayMark.Ovulation || mark == DayMark.Fertile) colors.background else colors.ink
    val description = when (mark) {
        DayMark.Period -> stringResource(R.string.period_mark_period)
        DayMark.Ovulation -> stringResource(R.string.period_mark_ovulation)
        DayMark.Fertile -> stringResource(R.string.period_mark_fertile)
        null -> ""
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .then(if (isToday && mark != null) Modifier.border(2.dp, colors.ink, RoundedCornerShape(10.dp)) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (mark == null && !isToday) colors.inkMuted else textColor,
            modifier = if (description.isNotEmpty()) Modifier.semantics { contentDescription = description } else Modifier,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend() {
    val colors = TwinTheme.colors
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TwinTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
    ) {
        LegendItem(colors.brand, stringResource(R.string.period_mark_period))
        LegendItem(colors.success, stringResource(R.string.period_mark_ovulation))
        LegendItem(PeriodAmber, stringResource(R.string.period_mark_fertile))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
        Box(modifier = Modifier.size(10.dp).clip(TwinTheme.shapes.circle).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TwinTheme.colors.inkFaint)
    }
}

@Composable
private fun CycleBarsCard(bars: List<CycleBar>) {
    val colors = TwinTheme.colors
    val max = maxOf(bars.maxOf { it.days }, BAR_MIN_SCALE_DAYS)
    val labelFormat = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }
    TitledCard(Icons.Rounded.AutoAwesome, stringResource(R.string.period_cycles_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { bar ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(bar.days.toString(), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxOf(3f, bar.days.toFloat() / max * BarMaxHeight.value).dp)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(colors.brand),
                    )
                    Text(
                        text = bar.label.format(labelFormat),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkFaint,
                        maxLines = 1,
                        modifier = Modifier.padding(top = TwinTheme.spacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommonSymptomsCard(common: List<Pair<String, Int>>) {
    val colors = TwinTheme.colors
    val max = common.maxOf { it.second }
    TitledCard(Icons.Rounded.Checklist, stringResource(R.string.period_common_title)) {
        common.forEach { (name, count) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TwinTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, modifier = Modifier.weight(0.9f))
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(6.dp)
                        .clip(TwinTheme.shapes.pill)
                        .background(colors.glass),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(count.toFloat() / max)
                            .background(colors.brand),
                    )
                }
                Text(
                    text = stringResource(R.string.period_times, count),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.inkFaint,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.4f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomsCard(
    ticked: Set<String>,
    canEdit: Boolean,
    saving: Boolean,
    onToggle: (Symptom) -> Unit,
    onSave: () -> Unit,
) {
    val colors = TwinTheme.colors
    TitledCard(Icons.Rounded.Checklist, stringResource(R.string.period_symptoms_title)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            Symptom.entries.forEach { symptom ->
                val selected = symptom.key in ticked
                Row(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(TwinTheme.shapes.pill)
                        .background(if (selected) colors.brand.copy(alpha = 0.5f) else colors.glass)
                        .border(1.dp, if (selected) colors.accent else colors.lineSoft, TwinTheme.shapes.pill)
                        .clickable(role = Role.Checkbox) { onToggle(symptom) }
                        .padding(horizontal = TwinTheme.spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
                ) {
                    Icon(symptomIcon(symptom), contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(16.dp))
                    Text(stringResource(symptom.labelRes), style = MaterialTheme.typography.labelMedium, color = colors.ink)
                }
            }
        }
        if (canEdit) {
            GlassButton(
                text = stringResource(R.string.period_symptoms_save),
                onClick = onSave,
                loading = saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwinTheme.spacing.m),
            )
        }
    }
}

private fun symptomIcon(symptom: Symptom): ImageVector = when (symptom) {
    Symptom.Cramps -> Icons.Rounded.Waves
    Symptom.MoodSwings -> Icons.Rounded.Mood
    Symptom.Bloating -> Icons.Rounded.WaterDrop
    Symptom.Fatigue -> Icons.Rounded.Bedtime
    Symptom.Headache -> Icons.Rounded.Healing
    Symptom.BackPain -> Icons.Rounded.Accessibility
    Symptom.Tender -> Icons.Rounded.FavoriteBorder
    Symptom.Nausea -> Icons.Rounded.MoodBad
    Symptom.HighEnergy -> Icons.Rounded.Bolt
    Symptom.Acne -> Icons.Rounded.AutoAwesome
}

@Composable
private fun HistoryCard(periods: List<Period>, canEdit: Boolean, onDelete: (Period) -> Unit) {
    val colors = TwinTheme.colors
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    TitledCard(Icons.Rounded.Visibility, stringResource(R.string.period_history_title)) {
        if (periods.isEmpty()) {
            Text(
                text = stringResource(R.string.period_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TwinTheme.spacing.l),
            )
        } else {
            periods.forEach { period ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = TwinTheme.spacing.s)
                        .clip(TwinTheme.shapes.small)
                        .background(colors.brand.copy(alpha = 0.15f))
                        .border(1.dp, colors.brand.copy(alpha = 0.4f), TwinTheme.shapes.small)
                        .padding(TwinTheme.spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                        Text(period.start.format(dateFormat), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                        Text(
                            text = stringResource(R.string.period_history_detail, period.flow, period.cycle),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkFaint,
                        )
                        if (period.note.isNotBlank()) {
                            Text(period.note, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                        }
                    }
                    if (canEdit) {
                        IconButton(onClick = { onDelete(period) }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.period_delete_entry),
                                tint = colors.inkFaint,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "One quick thing" chooser for accounts that never picked Girl or Boy (web `Gender.choose`). */
@Composable
private fun GenderDialog(onChoose: (female: Boolean) -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(stringResource(R.string.period_gender_title), color = colors.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                Text(stringResource(R.string.period_gender_message), color = colors.inkMuted)
                GlassButton(
                    text = stringResource(R.string.period_gender_girl),
                    onClick = { onChoose(true) },
                    modifier = Modifier.fillMaxWidth(),
                )
                GlassButton(
                    text = stringResource(R.string.period_gender_boy),
                    onClick = { onChoose(false) },
                    style = GlassButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.period_gender_later)) } },
    )
}
