package com.twinhearts.app.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.PillSwitch
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.feature.chat.data.PhotoPick
import com.twinhearts.app.feature.chat.domain.DisappearPresets
import com.twinhearts.app.feature.chat.domain.NamedEmoji
import com.twinhearts.app.feature.chat.domain.QuickReactions
import com.twinhearts.app.feature.chat.domain.formatClock
import com.twinhearts.app.feature.chat.domain.model.ChatMessage
import com.twinhearts.app.feature.chat.domain.model.MessageKind
import com.twinhearts.app.feature.chat.domain.parseClock
import com.twinhearts.app.feature.chat.domain.toLocalDate
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Why this file exists: all the small pop-up pieces of Chat in one place: the long-press menu,
 * the attach sheet, sticker / gift / GIF / poll / contact pickers, the photo preview, message info,
 * and the little setting dialogs. Each one is stateless apart from what the person is typing.
 */

private const val GIF_DEBOUNCE_MS = 400L
private const val MAX_POLL_OPTIONS = 6
private const val MIN_POLL_OPTIONS = 2
private const val GIF_COLUMNS = 3
private const val EMOJI_COLUMNS = 3
private const val ROTATE_STEP = 90
private const val FULL_TURN = 360
private const val DAY_SECONDS = 86_400
private const val HOUR_SECONDS = 3_600
private val SheetCorner = 24.dp
private val SheetMaxHeight = 520.dp
private val GifCellHeight = 110.dp
private val PreviewHeight = 280.dp
private val EmojiBig = 40.dp
private val dateTime: DateTimeFormatter get() = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

/** A card that slides up from the bottom (Material bottom sheet: it respects the status bar, keyboard and nav bar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = TwinTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card,
        contentColor = colors.ink,
        scrimColor = colors.scrim,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = TwinTheme.spacing.l, end = TwinTheme.spacing.l, bottom = TwinTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
            content = content,
        )
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = TwinTheme.colors.ink)
}

@Composable
private fun SheetItem(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (danger) TwinTheme.colors.error else TwinTheme.colors.ink,
        modifier = Modifier
            .fillMaxWidth()
            .clip(TwinTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = TwinTheme.spacing.m, horizontal = TwinTheme.spacing.s),
    )
}

// ───────────── long-press menu ─────────────

/** What a long-press can do to one message. [mine] decides Edit and "Delete for everyone". */
internal class MessageMenuActions(
    val onSelect: () -> Unit,
    val onReact: (String) -> Unit,
    val onReply: () -> Unit,
    val onForward: () -> Unit,
    val onCopy: () -> Unit,
    val onPin: () -> Unit,
    val onStar: () -> Unit,
    val onEdit: () -> Unit,
    val onInfo: () -> Unit,
    val onDeleteEveryone: () -> Unit,
    val onDeleteMe: () -> Unit,
)

@Composable
internal fun MessageMenuSheet(message: ChatMessage, actions: MessageMenuActions, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    val real = message.id != ChatMessage.LOCAL_ID
    ChatSheet(onDismiss) {
        if (real) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                (QuickReactions + MORE_EMOJI).forEach { emoji ->
                    Text(
                        emoji,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .clip(TwinTheme.shapes.circle)
                            .background(ChatStyle.Glass)
                            .clickable { onDismiss(); actions.onReact(emoji) }
                            .padding(TwinTheme.spacing.s),
                    )
                }
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SheetItem(stringResource(R.string.chat_menu_select)) { onDismiss(); actions.onSelect() }
            SheetItem(stringResource(R.string.chat_menu_reply)) { onDismiss(); actions.onReply() }
            if (real) SheetItem(stringResource(R.string.chat_menu_forward)) { onDismiss(); actions.onForward() }
            if (!message.text.isNullOrBlank()) SheetItem(stringResource(R.string.chat_menu_copy)) { onDismiss(); actions.onCopy() }
            if (real) {
                SheetItem(stringResource(if (message.pinned) R.string.chat_menu_unpin else R.string.chat_menu_pin)) { onDismiss(); actions.onPin() }
                SheetItem(stringResource(if (message.starred) R.string.chat_menu_unstar else R.string.chat_menu_star)) { onDismiss(); actions.onStar() }
            }
            if (real && message.mine && message.kind == MessageKind.Text) {
                SheetItem(stringResource(R.string.chat_menu_edit)) { onDismiss(); actions.onEdit() }
            }
            if (real) SheetItem(stringResource(R.string.chat_menu_info)) { onDismiss(); actions.onInfo() }
            if (real && message.mine) SheetItem(stringResource(R.string.chat_menu_delete_everyone), danger = true) { onDismiss(); actions.onDeleteEveryone() }
            if (real) SheetItem(stringResource(R.string.chat_menu_delete_me), danger = true) { onDismiss(); actions.onDeleteMe() }
        }
    }
}

// ───────────── attach sheet ─────────────

/** The "+" menu of the composer. Same choices as the web (without Memories, which lives in its own feature). */
internal class AttachActions(
    val onPhotos: () -> Unit,
    val onCamera: () -> Unit,
    val onDocument: () -> Unit,
    val onAudio: () -> Unit,
    val onLocation: () -> Unit,
    val onContact: () -> Unit,
    val onPoll: () -> Unit,
    val onSticker: () -> Unit,
    val onGift: () -> Unit,
    val onGif: () -> Unit,
    val onSchedule: () -> Unit,
    val onEmoji: () -> Unit,
)

@Composable
internal fun AttachSheet(actions: AttachActions, onDismiss: () -> Unit) {
    val options = listOf(
        Triple("🖼️", R.string.chat_attach_photos, actions.onPhotos),
        Triple("📷", R.string.chat_attach_camera, actions.onCamera),
        Triple("📄", R.string.chat_attach_document, actions.onDocument),
        Triple("🎵", R.string.chat_attach_audio, actions.onAudio),
        Triple("📍", R.string.chat_attach_location, actions.onLocation),
        Triple("👤", R.string.chat_attach_contact, actions.onContact),
        Triple("📊", R.string.chat_attach_poll, actions.onPoll),
        Triple("🎭", R.string.chat_attach_sticker, actions.onSticker),
        Triple("🎁", R.string.chat_attach_gift, actions.onGift),
        Triple("🎞️", R.string.chat_attach_gif, actions.onGif),
        Triple("⏰", R.string.chat_attach_schedule, actions.onSchedule),
        Triple("😀", R.string.chat_attach_emoji, actions.onEmoji),
    )
    ChatSheet(onDismiss) {
        options.chunked(EMOJI_COLUMNS + 1).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { (icon, label, action) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(TwinTheme.shapes.small)
                            .clickable { onDismiss(); action() }
                            .padding(vertical = TwinTheme.spacing.s),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(icon, style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = TwinTheme.colors.inkMuted, textAlign = TextAlign.Center)
                    }
                }
                repeat(EMOJI_COLUMNS + 1 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

// ───────────── sticker, gift, gif ─────────────

@Composable
internal fun EmojiCardSheet(title: String, items: List<NamedEmoji>, onPick: (NamedEmoji) -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    ChatSheet(onDismiss) {
        SheetTitle(title)
        LazyVerticalGrid(
            columns = GridCells.Fixed(EMOJI_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            items(items, key = { it.name }) { item ->
                Column(
                    modifier = Modifier
                        .clip(TwinTheme.shapes.small)
                        .background(ChatStyle.Glass)
                        .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
                        .clickable { onDismiss(); onPick(item) }
                        .padding(TwinTheme.spacing.s),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(item.emoji, style = MaterialTheme.typography.displaySmall)
                    Text(item.name, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                }
            }
        }
    }
}

@Composable
internal fun GifSheet(state: GifState, onSearch: (String) -> Unit, onPick: (com.twinhearts.app.feature.chat.domain.model.GifItem) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(GIF_DEBOUNCE_MS)
        onSearch(query)
    }
    ChatSheet(onDismiss) {
        SheetTitle(stringResource(R.string.chat_gif_title))
        TwinTextField(value = query, onValueChange = { query = it }, label = stringResource(R.string.chat_gif_search_hint), leadingIcon = Icons.Rounded.Search)
        when {
            state.failed != null -> if (state.failed == AppError.Unknown) {
                EmptyState(title = stringResource(R.string.chat_gif_unavailable), icon = Icons.Rounded.Search)
            } else {
                ErrorState(message = state.failed.asText(), onRetry = { onSearch(query) })
            }
            state.loading && state.items.isEmpty() -> Text(stringResource(R.string.chat_loading), color = TwinTheme.colors.inkMuted)
            state.items.isEmpty() -> EmptyState(title = stringResource(R.string.chat_search_empty), icon = Icons.Rounded.Search)
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(GIF_COLUMNS),
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
            ) {
                items(state.items, key = { it.id }) { gif ->
                    AsyncImage(
                        model = gif.url,
                        contentDescription = stringResource(R.string.chat_preview_gif),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .heightIn(min = GifCellHeight, max = GifCellHeight)
                            .clip(TwinTheme.shapes.small)
                            .clickable { onDismiss(); onPick(gif) },
                    )
                }
            }
        }
    }
}

// ───────────── poll, contact ─────────────

@Composable
internal fun PollSheet(onCreate: (String, List<String>) -> Unit, onDismiss: () -> Unit) {
    var question by rememberSaveable { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    val ready = question.isNotBlank() && options.count { it.isNotBlank() } >= MIN_POLL_OPTIONS
    ChatSheet(onDismiss) {
        SheetTitle(stringResource(R.string.chat_poll_title))
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            TwinTextField(value = question, onValueChange = { question = it }, label = stringResource(R.string.chat_poll_question))
            options.forEachIndexed { index, value ->
                TwinTextField(
                    value = value,
                    onValueChange = { options[index] = it },
                    label = stringResource(R.string.chat_poll_option, index + 1),
                )
            }
            if (options.size < MAX_POLL_OPTIONS) {
                GlassButton(
                    text = stringResource(R.string.chat_poll_add),
                    onClick = { options.add("") },
                    style = GlassButtonStyle.Ghost,
                    leadingIcon = Icons.Rounded.Add,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        GlassButton(
            text = stringResource(R.string.chat_poll_create),
            onClick = { onDismiss(); onCreate(question, options.toList()) },
            enabled = ready,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun ContactSheet(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    ChatSheet(onDismiss) {
        SheetTitle(stringResource(R.string.chat_contact_sheet_title))
        TwinTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.chat_contact_name))
        GlassButton(
            text = stringResource(R.string.chat_contact_send),
            onClick = { onDismiss(); onCreate(name) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ───────────── photo preview ─────────────

/** Look at the chosen photos before sending: turn them, add a caption, optionally "view once". */
@Composable
internal fun PhotoPreviewSheet(photos: List<PhotoPick>, onSend: (List<PhotoPick>, String, Boolean) -> Unit, onDismiss: () -> Unit) {
    val turns = remember(photos) { mutableStateListOf<Int>().apply { photos.forEach { add(it.rotation) } } }
    var caption by rememberSaveable { mutableStateOf("") }
    var viewOnce by rememberSaveable { mutableStateOf(false) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val current = index.coerceIn(0, photos.lastIndex.coerceAtLeast(0))
    ChatSheet(onDismiss) {
        if (photos.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().heightIn(max = PreviewHeight), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = photos[current].uri,
                    contentDescription = stringResource(R.string.chat_preview_photo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = PreviewHeight).rotate(turns[current].toFloat()),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
                photos.forEachIndexed { i, _ ->
                    TwinFilterChip(label = (i + 1).toString(), selected = i == current, onClick = { index = i })
                }
                Icon(
                    Icons.Rounded.RotateRight,
                    contentDescription = stringResource(R.string.chat_photo_rotate),
                    tint = TwinTheme.colors.ink,
                    modifier = Modifier
                        .size(TwinTheme.shapes.minTouch)
                        .clip(TwinTheme.shapes.circle)
                        .clickable { turns[current] = (turns[current] + ROTATE_STEP) % FULL_TURN }
                        .padding(TwinTheme.spacing.s),
                )
            }
        }
        TwinTextField(value = caption, onValueChange = { caption = it }, label = stringResource(R.string.chat_photo_caption_hint))
        if (photos.size == 1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔒 ${stringResource(R.string.chat_photo_view_once)}", style = MaterialTheme.typography.bodyLarge, color = TwinTheme.colors.ink, modifier = Modifier.weight(1f))
                PillSwitch(checked = viewOnce, onCheckedChange = { viewOnce = it })
            }
        }
        GlassButton(
            text = stringResource(R.string.chat_photo_send),
            onClick = {
                onDismiss()
                onSend(photos.mapIndexed { i, p -> p.copy(rotation = turns[i]) }, caption, viewOnce && photos.size == 1)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ───────────── message info, delete, settings dialogs ─────────────

@Composable
internal fun MessageInfoDialog(message: ChatMessage, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    fun stamp(t: java.time.Instant?): String =
        if (t == null) "" else "${t.toLocalDate().format(dateTime)} · ${formatClock(t)}"
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(stringResource(R.string.chat_info_title), color = colors.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                InfoLine(stringResource(R.string.chat_info_sent), stamp(message.createdAt))
                if (message.mine) {
                    InfoLine(stringResource(R.string.chat_info_delivered), stamp(message.deliveredAt).ifEmpty { stringResource(R.string.chat_info_not_yet) })
                    InfoLine(stringResource(R.string.chat_info_read), stamp(message.readAt).ifEmpty { stringResource(R.string.chat_info_not_yet) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_close), color = colors.pinkSoft) } },
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TwinTheme.colors.inkMuted)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = TwinTheme.colors.ink)
    }
}

/** Asks before deleting. "For everyone" is offered only when every picked message is mine. */
@Composable
internal fun ConfirmDeleteDialog(count: Int, canEveryone: Boolean, onConfirm: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(stringResource(R.string.chat_delete_title, count), color = colors.ink) },
        text = { Text(stringResource(R.string.chat_delete_body), color = colors.inkMuted) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (canEveryone) {
                    TextButton(onClick = { onDismiss(); onConfirm(true) }) { Text(stringResource(R.string.chat_menu_delete_everyone), color = colors.error) }
                }
                TextButton(onClick = { onDismiss(); onConfirm(false) }) { Text(stringResource(R.string.chat_menu_delete_me), color = colors.error) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel), color = colors.inkMuted) }
            }
        },
    )
}

@Composable
internal fun ConfirmDialog(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(title, color = colors.ink) },
        text = { Text(body, color = colors.inkMuted) },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirmLabel, color = colors.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel), color = colors.inkMuted) } },
    )
}

@Composable
internal fun NicknameDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    var text by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(stringResource(R.string.chat_nickname_title), color = colors.ink) },
        text = { TwinTextField(value = text, onValueChange = { text = it }, label = stringResource(R.string.chat_nickname_hint), maxLength = NICKNAME_MAX) },
        confirmButton = { TextButton(onClick = { onDismiss(); onSave(text) }) { Text(stringResource(R.string.chat_save), color = colors.pinkSoft) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel), color = colors.inkMuted) } },
    )
}

private const val NICKNAME_MAX = 24

/** Do-not-disturb window as two "HH:mm" times. Saving with both empty turns it off. */
@Composable
internal fun DndDialog(start: String?, end: String?, onSave: (String?, String?) -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    var from by rememberSaveable { mutableStateOf(start.orEmpty()) }
    var to by rememberSaveable { mutableStateOf(end.orEmpty()) }
    val off = from.isBlank() && to.isBlank()
    val valid = off || (parseClock(from) != null && parseClock(to) != null)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(stringResource(R.string.chat_dnd_title), color = colors.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                Text(stringResource(R.string.chat_dnd_hint), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                TwinTextField(value = from, onValueChange = { from = it }, label = stringResource(R.string.chat_dnd_from), placeholder = "22:00", maxLength = CLOCK_MAX)
                TwinTextField(value = to, onValueChange = { to = it }, label = stringResource(R.string.chat_dnd_to), placeholder = "07:00", maxLength = CLOCK_MAX)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onDismiss(); if (off) onSave(null, null) else onSave(from.trim(), to.trim()) }) {
                Text(stringResource(R.string.chat_save), color = colors.pinkSoft)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel), color = colors.inkMuted) } },
    )
}

private const val CLOCK_MAX = 5

@Composable
internal fun ChoiceDialog(title: String, labels: List<String>, selectedIndex: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = TwinTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        title = { Text(title, color = colors.ink) },
        text = {
            Column {
                labels.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TwinTheme.shapes.small)
                            .clickable { onDismiss(); onPick(index) }
                            .padding(vertical = TwinTheme.spacing.m, horizontal = TwinTheme.spacing.s),
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (index == selectedIndex) colors.pinkSoft else colors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        if (index == selectedIndex) Text("✓", color = colors.pinkSoft)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel), color = colors.inkMuted) } },
    )
}

@Composable
internal fun disappearLabels(): List<String> = DisappearPresets.map { seconds ->
    when (seconds) {
        0 -> stringResource(R.string.chat_disappear_off)
        HOUR_SECONDS -> stringResource(R.string.chat_disappear_1h)
        DAY_SECONDS -> stringResource(R.string.chat_disappear_24h)
        else -> stringResource(R.string.chat_disappear_7d)
    }
}
