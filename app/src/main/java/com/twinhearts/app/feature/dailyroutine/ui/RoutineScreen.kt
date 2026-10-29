package com.twinhearts.app.feature.dailyroutine.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.dailyroutine.data.RoutineIdentity
import com.twinhearts.app.feature.dailyroutine.domain.model.DayRow
import com.twinhearts.app.feature.dailyroutine.domain.model.Occurrence
import com.twinhearts.app.feature.dailyroutine.domain.model.Reminder
import com.twinhearts.app.feature.dailyroutine.domain.model.Repeat
import com.twinhearts.app.feature.dailyroutine.domain.model.Routine
import com.twinhearts.app.feature.dailyroutine.domain.model.RoutineType
import com.twinhearts.app.feature.dailyroutine.domain.model.Visibility
import com.twinhearts.app.feature.dailyroutine.ui.components.DateBar
import com.twinhearts.app.feature.dailyroutine.ui.components.DayRowItem
import com.twinhearts.app.feature.dailyroutine.ui.components.RoutineDetailSheet
import com.twinhearts.app.feature.dailyroutine.ui.components.RoutineFormSheet
import com.twinhearts.app.feature.dailyroutine.ui.components.SummaryCard
import com.twinhearts.app.feature.dailyroutine.ui.components.WeekStrip
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime

/*
 * Why this file exists: the Daily Routine screen. It connects the ViewModel to the stateless
 * day view and sheets, ticks the clock once a minute and shows one-off messages.
 */

private const val TICK_MS = 60_000L

@Composable
fun RoutineScreen(onBack: () -> Unit, viewModel: RoutineViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var message by remember { mutableStateOf<RoutineMessage?>(null) }

    LaunchedEffect(viewModel) { viewModel.events.collect { message = it } }
    message?.let { current ->
        val text = current.asText()
        LaunchedEffect(current) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            message = null
        }
    }
    // Keeps NOW and "In Progress" in step with the clock (the web re-renders every minute too).
    LaunchedEffect(viewModel) {
        while (true) {
            delay(TICK_MS)
            viewModel.tick()
        }
    }

    RoutineContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onTab = viewModel::selectTab,
        onShift = viewModel::shiftDate,
        onPickDate = viewModel::pickDate,
        onAdd = { viewModel.openAdd() },
        onAddInGap = { from, to -> viewModel.openAdd(from, to) },
        onOpen = { viewModel.openDetail(it.routine.id, it.date) },
    )

    state.form?.let { target ->
        RoutineFormSheet(
            target = target,
            existing = target.editId?.let(state::routine),
            error = state.formError,
            isSaving = state.isSaving,
            onDismiss = viewModel::closeForm,
            onSave = viewModel::save,
        )
    }
    state.detail?.let { target ->
        val routine = state.routine(target.id)
        val me = state.identity
        if (routine != null && me != null) {
            RoutineDetailSheet(
                routine = routine,
                isMine = routine.role == me.role,
                partnerName = me.partnerName,
                isSaving = state.isSaving,
                onDismiss = viewModel::closeDetail,
                onComplete = viewModel::markCompleted,
                onSkip = viewModel::skip,
                onEdit = { viewModel.openEdit(routine.id) },
                onDuplicate = viewModel::duplicate,
                onDelete = viewModel::askDelete,
            )
        }
    }
    if (state.confirmDelete) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            text = { Text(stringResource(R.string.dr_delete_confirm)) },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.dr_delete)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.core_cancel)) } },
        )
    }
}

/** Stateless: top bar, tabs, date bar, week strip, summary, the day's list and the add button. */
@Composable
internal fun RoutineContent(
    state: RoutineUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onTab: (RoutineTab) -> Unit,
    onShift: (Long) -> Unit,
    onPickDate: (LocalDate) -> Unit,
    onAdd: () -> Unit,
    onAddInGap: (String, String) -> Unit,
    onOpen: (Occurrence) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TwinTopBar(title = stringResource(R.string.dr_title), showLogo = true, onBackClick = onBack, onSyncClick = onRefresh)
            val identity = state.identity
            when {
                state.error != null && identity == null -> ErrorState(
                    message = state.error.asText(), onRetry = onRefresh,
                    modifier = Modifier.padding(TwinTheme.spacing.screenEdge),
                )
                state.isLoading && identity == null -> LoadingSkeleton(modifier = Modifier.padding(TwinTheme.spacing.screenEdge), lines = 5)
                else -> {
                    val partnerName = identity?.partnerName.orEmpty()
                    val rows = state.rows
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = TwinTheme.spacing.screenEdge, end = TwinTheme.spacing.screenEdge,
                            bottom = TwinTheme.spacing.xl * 3,
                        ),
                        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                    ) {
                        item {
                            SegmentedTabs(
                                options = listOf(stringResource(R.string.dr_tab_mine), stringResource(R.string.dr_tab_partner)),
                                selectedIndex = state.tab.ordinal,
                                onSelect = { onTab(RoutineTab.entries[it]) },
                            )
                        }
                        item { DateBar(state.date, state.now.toLocalDate(), onShift, onPickDate) }
                        item { WeekStrip(state.date, state.now.toLocalDate(), onPickDate) }
                        if (state.occurrences.isNotEmpty()) {
                            item { SummaryCard(state.summary, state.date, state.isToday) }
                        }
                        if (rows.none { it is DayRow.Activity }) {
                            item {
                                val mine = state.tab == RoutineTab.Mine
                                EmptyState(
                                    title = if (mine) stringResource(R.string.dr_empty_mine_title) else stringResource(R.string.dr_empty_partner_title, partnerName),
                                    message = stringResource(if (mine) R.string.dr_empty_mine_message else R.string.dr_empty_partner_message),
                                    actionLabel = if (mine) stringResource(R.string.dr_add_routine_button) else null,
                                    onAction = if (mine) onAdd else null,
                                    icon = Icons.Rounded.Add,
                                )
                            }
                        } else {
                            items(rows) { row -> DayRowItem(row, onOpen, onAddInGap) }
                        }
                    }
                }
            }
        }
        GlassIconButton(
            icon = Icons.Rounded.Add,
            contentDescription = stringResource(R.string.dr_add_cd),
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(TwinTheme.spacing.screenEdge),
        )
    }
}

// ---- Previews (R-57): content, empty, loading, error, large font ------------------------------

private val previewNow: LocalDateTime = LocalDateTime.of(2026, 10, 25, 10, 30)

private fun routine(id: String, title: String, start: String, end: String, role: String = "user1") = Routine(
    id = id, role = role, title = title, description = "", type = RoutineType.Workout, location = "", notes = "",
    startTime = start, endTime = end, date = previewNow.toLocalDate(), repeat = Repeat.None, repeatDays = emptyList(),
    visibility = Visibility.Shared, reminder = Reminder.None, statusByDate = emptyMap(), timeByDate = emptyMap(),
)

private val previewState = RoutineUiState(
    isLoading = false, date = previewNow.toLocalDate(), now = previewNow,
    identity = RoutineIdentity("user1", "Vamsi", "Likitha"),
    routines = listOf(routine("1", "Gym", "07:00", "08:00"), routine("2", "Study", "11:00", "12:30"), routine("3", "Dinner", "20:00", "21:00")),
)

@Composable
private fun PreviewHost(state: RoutineUiState) {
    TwinTheme { TwinBackground { RoutineContent(state, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}) } }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360)
@Composable
private fun RoutineContentPreview() = PreviewHost(previewState)

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360)
@Composable
private fun RoutineEmptyPreview() = PreviewHost(previewState.copy(routines = emptyList()))

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360)
@Composable
private fun RoutineLoadingPreview() = PreviewHost(RoutineUiState())

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360)
@Composable
private fun RoutineErrorPreview() = PreviewHost(RoutineUiState(isLoading = false, error = AppError.Network))

@Preview(showBackground = true, backgroundColor = 0xFF14030E, heightDp = 780, widthDp = 360, fontScale = 1.3f)
@Composable
private fun RoutineLargeFontPreview() = PreviewHost(previewState)
