package com.twinhearts.app.feature.surprises.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.surprises.domain.model.Capsule
import com.twinhearts.app.feature.surprises.domain.model.Surprise
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Surprises page (web: `#page-surprise`, reference screenshot "Surprises"): two tabs.
 * "Surprises" hides a message, photo, video or voice note until a date; "Love Capsule" seals a
 * written message until a date. Both live in the couple's shared state, so the web app and this
 * app show the same items.
 */
@Composable
fun SurprisesScreen(
    onBack: () -> Unit,
    viewModel: SurprisesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    var showNew by rememberSaveable { mutableStateOf(false) }
    var capsuleText by rememberSaveable { mutableStateOf("") }
    var capsuleDateText by rememberSaveable { mutableStateOf("") }
    var showCapsuleDate by rememberSaveable { mutableStateOf(false) }
    var surpriseToDelete by remember { mutableStateOf<Surprise?>(null) }
    var capsuleToDelete by remember { mutableStateOf<Capsule?>(null) }
    val capsuleDate = runCatching { LocalDate.parse(capsuleDateText) }.getOrNull()

    // One-off messages: toast once, then clear. A hidden surprise closes its sheet, a sealed capsule empties its form.
    val message = state.message
    val messageText = when (message) {
        null -> null
        SurprisesMessage.Hidden -> stringResource(R.string.surprises_toast_hidden)
        SurprisesMessage.Sealed -> stringResource(R.string.surprises_toast_sealed)
        SurprisesMessage.SurpriseDeleted -> stringResource(R.string.surprises_toast_surprise_deleted)
        SurprisesMessage.CapsuleDeleted -> stringResource(R.string.surprises_toast_capsule_deleted)
        SurprisesMessage.NeedTitleAndDate -> stringResource(R.string.surprises_toast_need_title_date)
        SurprisesMessage.NeedMessageAndDate -> stringResource(R.string.surprises_toast_need_message_date)
        is SurprisesMessage.StillLocked -> stringResource(R.string.surprises_toast_still_locked, formatDate(message.unlock))
        is SurprisesMessage.Failed -> message.error.asText()
    }
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        if (message == SurprisesMessage.Hidden) showNew = false
        if (message == SurprisesMessage.Sealed) {
            capsuleText = ""
            capsuleDateText = ""
        }
        Toast.makeText(context, messageText.orEmpty(), Toast.LENGTH_SHORT).show()
        viewModel.messageShown()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TwinTopBar(
            title = stringResource(R.string.surprises_title),
            showSidebarButton = true,
            onSyncClick = viewModel::refresh,
        )
        when {
            state.loading -> Column(modifier = Modifier.padding(TwinTheme.spacing.screenEdge)) { LoadingSkeleton(lines = 4) }
            state.loadError != null -> ErrorState(message = state.loadError!!.asText(), onRetry = viewModel::refresh)
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .navigationBarsPadding()
                    .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                SurprisesTabs(selected = state.tab, onSelect = viewModel::selectTab)
                when (state.tab) {
                    SurprisesTab.Surprises -> SurprisesTabContent(
                        surprises = state.data.surprises,
                        today = state.today,
                        onNew = { showNew = true },
                        onDelete = { surpriseToDelete = it },
                    )
                    SurprisesTab.Capsule -> CapsuleTabContent(
                        capsules = state.data.capsules,
                        today = state.today,
                        saving = state.saving,
                        text = capsuleText,
                        onText = { capsuleText = it },
                        date = capsuleDate,
                        onPickDate = { showCapsuleDate = true },
                        onSeal = { viewModel.addCapsule(capsuleText, capsuleDate) },
                        onOpen = { viewModel.openCapsule(it.id, it.unlock) },
                        onDelete = { capsuleToDelete = it },
                        onCreateFirst = { scope.launch { scroll.animateScrollTo(0) } },
                    )
                }
            }
        }
    }

    if (showNew) {
        NewSurpriseSheet(
            today = state.today,
            saving = state.saving,
            onSave = viewModel::addSurprise,
            onDismiss = { showNew = false },
        )
    }
    if (showCapsuleDate) {
        SurpriseDatePicker(
            initial = capsuleDate,
            minDate = state.today,
            onPick = {
                capsuleDateText = it.toString()
                showCapsuleDate = false
            },
            onDismiss = { showCapsuleDate = false },
        )
    }
    surpriseToDelete?.let { item ->
        ConfirmDelete(
            title = stringResource(R.string.surprises_delete_surprise_title),
            message = stringResource(R.string.surprises_delete_surprise_message),
            onConfirm = {
                viewModel.deleteSurprise(item.id)
                surpriseToDelete = null
            },
            onDismiss = { surpriseToDelete = null },
        )
    }
    capsuleToDelete?.let { item ->
        ConfirmDelete(
            title = stringResource(R.string.surprises_delete_capsule_title),
            message = stringResource(R.string.surprises_delete_capsule_message),
            onConfirm = {
                viewModel.deleteCapsule(item.id)
                capsuleToDelete = null
            },
            onDismiss = { capsuleToDelete = null },
        )
    }
}

@Composable
private fun SurprisesTabContent(
    surprises: List<Surprise>,
    today: LocalDate,
    onNew: () -> Unit,
    onDelete: (Surprise) -> Unit,
) {
    val colors = TwinTheme.colors
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
            CardTitle(stringResource(R.string.surprises_create_title), Modifier.weight(1f), TextAlign.Center)
            GlassButton(
                text = stringResource(R.string.surprises_new),
                onClick = onNew,
                style = GlassButtonStyle.Secondary,
            )
        }
        Text(
            text = stringResource(R.string.surprises_create_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkFaint,
            modifier = Modifier.padding(top = TwinTheme.spacing.s),
        )
    }
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(Icons.Rounded.Celebration, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
            CardTitle(stringResource(R.string.surprises_list_title), Modifier, TextAlign.End)
        }
        if (surprises.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.surprises_empty_title),
                message = stringResource(R.string.surprises_empty_message),
                icon = Icons.Rounded.CardGiftcard,
                actionLabel = stringResource(R.string.surprises_empty_action),
                onAction = onNew,
            )
        } else {
            Column(
                modifier = Modifier.padding(top = TwinTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                surprises.forEach { surprise ->
                    SurpriseCard(surprise = surprise, today = today, onDelete = { onDelete(surprise) })
                }
            }
        }
    }
}

@Composable
private fun CapsuleTabContent(
    capsules: List<Capsule>,
    today: LocalDate,
    saving: Boolean,
    text: String,
    onText: (String) -> Unit,
    date: LocalDate?,
    onPickDate: () -> Unit,
    onSeal: () -> Unit,
    onOpen: (Capsule) -> Unit,
    onDelete: (Capsule) -> Unit,
    onCreateFirst: () -> Unit,
) {
    val colors = TwinTheme.colors
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
            CardTitle(stringResource(R.string.surprises_capsule_create_title), Modifier.weight(1f), TextAlign.Start)
        }
        Column(
            modifier = Modifier.padding(top = TwinTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            SurpriseTextArea(
                value = text,
                onValueChange = onText,
                label = stringResource(R.string.surprises_capsule_message),
                placeholder = stringResource(R.string.surprises_capsule_message_hint),
            )
            SurpriseDateField(
                label = stringResource(R.string.surprises_capsule_date),
                date = date,
                onClick = onPickDate,
            )
            GlassButton(
                text = stringResource(R.string.surprises_capsule_seal),
                onClick = onSeal,
                loading = saving,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    GlassCard {
        CardTitle("💕 " + stringResource(R.string.surprises_capsule_list_title), Modifier.fillMaxWidth(), TextAlign.Start)
        if (capsules.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.surprises_capsule_empty_title),
                message = stringResource(R.string.surprises_capsule_empty_message),
                icon = Icons.Rounded.Inventory2,
                actionLabel = stringResource(R.string.surprises_capsule_empty_action),
                onAction = onCreateFirst,
            )
        } else {
            Column(
                modifier = Modifier.padding(top = TwinTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            ) {
                capsules.forEach { capsule ->
                    CapsuleCard(
                        capsule = capsule,
                        today = today,
                        busy = saving,
                        onOpen = { onOpen(capsule) },
                        onDelete = { onDelete(capsule) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CardTitle(text: String, modifier: Modifier, align: TextAlign) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontFamily = SerifFamily,
        color = TwinTheme.colors.ink,
        textAlign = align,
        modifier = modifier,
    )
}

/** The two-tab switcher. Like `SegmentedTabs`, but with the small icons the reference shows. */
@Composable
private fun SurprisesTabs(selected: SurprisesTab, onSelect: (SurprisesTab) -> Unit) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(pill)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, pill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TabItem(
            icon = Icons.Rounded.CardGiftcard,
            label = stringResource(R.string.surprises_tab_surprises),
            selected = selected == SurprisesTab.Surprises,
            onClick = { onSelect(SurprisesTab.Surprises) },
            modifier = Modifier.weight(1f),
        )
        TabItem(
            icon = Icons.Rounded.Inventory2,
            label = stringResource(R.string.surprises_tab_capsule),
            selected = selected == SurprisesTab.Capsule,
            onClick = { onSelect(SurprisesTab.Capsule) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TabItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    Box(
        modifier = modifier
            .heightIn48()
            .clip(pill)
            .background(if (selected) colors.neutralGlass else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            Icon(icon, contentDescription = null, tint = if (selected) colors.ink else colors.inkMuted, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) colors.ink else colors.inkMuted,
            )
        }
    }
}

/** Minimum touch height of 48 dp (R-51). */
private fun Modifier.heightIn48(): Modifier = this.defaultMinSize(minHeight = 48.dp)

@Composable
private fun ConfirmDelete(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TwinTheme.colors.card,
        title = { Text(title, color = TwinTheme.colors.ink) },
        text = { Text(message, color = TwinTheme.colors.inkMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.surprises_delete), color = TwinTheme.colors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_cancel)) }
        },
    )
}
