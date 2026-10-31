package com.twinhearts.app.feature.myspace.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.StickyNote2
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AutoRefresh
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.PetalBurst
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.core.reminders.rememberNotificationPermissionRequest

/**
 * My Space page (web: `#page-myspace`): Journal, Habits, Notes, Wellness and Reminders in one
 * place. Everything lives in the couple's shared state, so the web app and this app show the
 * same entries. Long-press an item to select several and delete them.
 */
@Composable
fun MySpaceScreen(
    onBack: () -> Unit,
    onSearch: () -> Unit,
    viewModel: MySpaceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var resetKey by rememberSaveable { mutableIntStateOf(0) }
    var deleteIds by remember { mutableStateOf<Set<String>?>(null) }
    val askNotifications = rememberNotificationPermissionRequest()

    AutoRefresh(onRefresh = viewModel::refresh)

    // Back leaves select mode first, then the page.
    BackHandler(enabled = state.selectMode) { viewModel.exitSelecting() }

    // One-off messages: toast once, then clear. A successful add also empties its form.
    val message = state.message
    val messageText = when (message) {
        null -> null
        is MySpaceMessage.Done -> stringResource(message.textRes)
        is MySpaceMessage.Failed -> message.error.asText()
    }
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        if (message is MySpaceMessage.Done && message.clearsForm) resetKey++
        Toast.makeText(context, messageText.orEmpty(), Toast.LENGTH_SHORT).show()
        viewModel.messageShown()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        TwinTopBar(
            title = stringResource(R.string.myspace_title),
            showSidebarButton = true,
            onSearchClick = onSearch,
            onSyncClick = viewModel::load,
        )
        val data = state.data
        when {
            state.loading && data == null -> {
                Column(modifier = Modifier.padding(TwinTheme.spacing.screenEdge)) { LoadingSkeleton(lines = 5) }
            }
            state.error != null && data == null -> {
                ErrorState(message = state.error!!.asText(), onRetry = viewModel::load)
            }
            data != null -> {
                Banner()
                TabBar(selected = state.tab, onSelect = viewModel::selectTab)
                if (state.selectMode) {
                    SelectBar(
                        count = state.selected.size,
                        onExit = viewModel::exitSelecting,
                        onDelete = { deleteIds = state.selected },
                    )
                }
                val selection = Selection(
                    mode = state.selectMode,
                    ids = state.selected,
                    onLongPress = viewModel::startSelecting,
                    onToggle = viewModel::toggleSelected,
                    onDeleteOne = { deleteIds = setOf(it) },
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = TwinTheme.spacing.screenEdge)
                        .padding(bottom = TwinTheme.spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                ) {
                    when (state.tab) {
                        MySpaceTab.Journal -> JournalTab(data, state.saving, resetKey, selection, viewModel::addJournal)
                        MySpaceTab.Habits -> HabitsTab(data, state.saving, selection, viewModel::addHabit, viewModel::toggleHabit)
                        MySpaceTab.Notes -> NotesTab(data, state.saving, resetKey, selection, viewModel::addNote)
                        MySpaceTab.Wellness -> WellnessTab(data, state.saving, viewModel::saveWellness)
                        MySpaceTab.Reminders -> RemindersTab(data, state.saving, resetKey, selection) {
                            // The phone rings for it, so ask once (Android 13+) why we want notifications.
                            askNotifications()
                            viewModel.addReminder(it)
                        }
                    }
                }
            }
        }
    }

    PetalBurst(trigger = state.petals)
    }

    deleteIds?.let { ids ->
        AlertDialog(
            onDismissRequest = { deleteIds = null },
            containerColor = TwinTheme.colors.card,
            title = { Text(stringResource(R.string.myspace_delete_title, ids.size), color = TwinTheme.colors.ink) },
            text = { Text(stringResource(R.string.myspace_delete_message), color = TwinTheme.colors.inkMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(ids)
                        deleteIds = null
                    },
                ) { Text(stringResource(R.string.myspace_delete_confirm), color = TwinTheme.colors.error) }
            },
            dismissButton = { TextButton(onClick = { deleteIds = null }) { Text(stringResource(R.string.core_cancel)) } },
        )
    }
}

@Composable
private fun Banner() {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .padding(horizontal = TwinTheme.spacing.screenEdge)
            .fillMaxWidth()
            .clip(TwinTheme.shapes.small)
            .background(colors.glass)
            .border(1.dp, colors.line, TwinTheme.shapes.small)
            .padding(TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.myspace_banner),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted,
        )
    }
}

private val MySpaceTab.icon: ImageVector
    get() = when (this) {
        MySpaceTab.Journal -> Icons.Rounded.MenuBook
        MySpaceTab.Habits -> Icons.Outlined.CheckCircle
        MySpaceTab.Notes -> Icons.Rounded.StickyNote2
        MySpaceTab.Wellness -> Icons.Rounded.SelfImprovement
        MySpaceTab.Reminders -> Icons.Rounded.NotificationsNone
    }

/** All five tabs in one rounded bar with an icon each; the active one is grey glass (web `stabs`). */
@Composable
private fun TabBar(selected: MySpaceTab, onSelect: (MySpaceTab) -> Unit) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    Row(
        modifier = Modifier
            .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s)
            .fillMaxWidth()
            .clip(pill)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, pill)
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        MySpaceTab.entries.forEach { tab ->
            val on = tab == selected
            Row(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(pill)
                    .background(if (on) colors.neutralGlass else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(tab.icon, contentDescription = null, tint = if (on) colors.ink else colors.inkMuted, modifier = Modifier.size(16.dp))
                Text(
                    text = stringResource(tab.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) colors.ink else colors.inkMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Bar shown while several items are being picked: close, how many, and the trash button. */
@Composable
private fun SelectBar(count: Int, onExit: () -> Unit, onDelete: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onExit) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.myspace_select_exit), tint = colors.ink)
        }
        Text(
            stringResource(R.string.myspace_select_count, count),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete, enabled = count > 0) {
            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.myspace_delete), tint = colors.error)
        }
    }
}
