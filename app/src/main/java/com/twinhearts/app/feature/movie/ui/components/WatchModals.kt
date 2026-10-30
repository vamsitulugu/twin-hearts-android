package com.twinhearts.app.feature.movie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.feature.movie.data.mapper.parseIsoMs
import com.twinhearts.app.feature.movie.domain.formatClock
import com.twinhearts.app.feature.movie.domain.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Why this file exists: the dark pop-ups of the web page's player (invite "MOVIE NIGHT", "End Watch
 * Together?" and the Movie Chat log), drawn like movie.css (.wt-modal): #141414 card, thin light edge,
 * centred text, ghost + filled buttons side by side.
 */

private val ModalFill = Color(0xFF141414)
private val ModalEdge = Color.White.copy(alpha = 0.08f)
private val Muted = Color(0xFF9A9A9A)
private val Soft = Color(0xFFDDDDDD)
private val Good = Color(0xFF34D399)
private val Wine = Color(0xFF6B093A)

@Composable
private fun WtModal(dismissable: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = dismissable, dismissOnClickOutside = dismissable),
    ) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().clip(shape).background(ModalFill).border(1.dp, ModalEdge, shape).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

/** The partner pressed START TOGETHER. Accepting begins the shared 3-2-1 countdown. */
@Composable
fun InviteDialog(movieTitle: String, matches: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    WtModal(dismissable = false, onDismiss = {}) {
        Text("🎬", fontSize = 40.sp)
        Text(
            stringResource(R.string.movie_invite_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        Text(stringResource(R.string.movie_invite_text), color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 10.dp))
        Text(
            stringResource(R.string.movie_invite_ask), color = Soft, fontSize = 14.sp, fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp),
        )
        Text(movieTitle, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 4.dp))
        if (matches) Text(stringResource(R.string.movie_match_ok), color = Good, fontSize = 12.sp, modifier = Modifier.padding(bottom = 18.dp))
        else Box(Modifier.padding(bottom = 18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(stringResource(R.string.movie_not_now), onDecline, Modifier.weight(1f), style = GlassButtonStyle.Ghost)
            GlassButton(stringResource(R.string.movie_accept), onAccept, Modifier.weight(1f))
        }
    }
}

@Composable
fun EndDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    WtModal(dismissable = true, onDismiss = onDismiss) {
        Text(
            stringResource(R.string.movie_end_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
        Text(stringResource(R.string.movie_end_text), color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(stringResource(R.string.movie_cancel_caps), onDismiss, Modifier.weight(1f), style = GlassButtonStyle.Ghost)
            GlassButton(stringResource(R.string.movie_end_confirm), onConfirm, Modifier.weight(1f))
        }
    }
}

/** The "Movie Chat" panel: this session's messages as left/right bubbles with movie time and sent time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatLogSheet(messages: List<ChatMessage>, partnerName: String, onDismiss: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ModalFill,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.movie_chat_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, stringResource(R.string.movie_close), tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
            if (messages.isEmpty()) {
                Text(
                    stringResource(R.string.movie_chat_session_empty), color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                )
            } else {
                LazyColumn(state = listState, modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(messages, key = { _, m -> m.id }) { i, m ->
                        val grouped = i > 0 && messages[i - 1].fromMe == m.fromMe
                        ChatBubble(m, partnerName, showName = !m.fromMe && !grouped)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(m: ChatMessage, partnerName: String, showName: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    val sent = remember(m.createdAt) {
        parseIsoMs(m.createdAt)?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it)) }.orEmpty()
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromMe) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 280.dp).clip(shape)
                .background(if (m.fromMe) Wine else Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (showName) Text(partnerName.ifBlank { stringResource(R.string.movie_partner) }, color = MovieChatYellow, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(m.text, color = Color.White, fontSize = 14.sp)
            Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                m.positionSec?.let { Text("🎬 ${formatClock(it)}", color = Muted, fontSize = 10.sp) }
                if (sent.isNotEmpty()) Text(sent, color = Muted, fontSize = 10.sp)
            }
        }
    }
}
