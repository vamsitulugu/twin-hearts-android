package com.twinhearts.app.feature.movie.cinema.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.feature.movie.cinema.domain.CinemaInvite

private val ModalFill = Color(0xFF141414)
private val ModalEdge = Color.White.copy(alpha = 0.08f)
private val Muted = Color(0xFF9A9A9A)

/** The pink-purple card on the web's Watch Together page: "Watch in Twin Cinema 3D". Shown above the Home and Watch tabs. */
@Composable
fun CinemaCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(Color(0x47D6338A), Color(0x478E3FD4))))
            .border(1.dp, Color(0x73FF4FA0), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("🍿", fontSize = 30.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.movie_cinema_card_title), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.movie_cinema_card_sub), color = Color.White.copy(alpha = 0.8f), fontSize = 12.5.sp)
        }
        Text("›", color = Color.White.copy(alpha = 0.8f), fontSize = 26.sp)
    }
}

@Composable
private fun CinemaModal(dismissable: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
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

/** Same sheet as the web: "Movie night", optional movie name, then "Open cinema lobby". */
@Composable
fun CinemaOpenDialog(partnerName: String, busy: Boolean, onOpen: (title: String) -> Unit, onDismiss: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    CinemaModal(dismissable = !busy, onDismiss = onDismiss) {
        Text("🍿", fontSize = 40.sp)
        Text(
            stringResource(R.string.movie_cinema_sheet_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        Text(
            stringResource(R.string.movie_cinema_sheet_text, partnerName), color = Muted, fontSize = 13.sp,
            textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp),
        )
        TwinTextField(
            value = title, onValueChange = { title = it }, label = stringResource(R.string.movie_cinema_sheet_label),
            placeholder = stringResource(R.string.movie_cinema_sheet_hint), maxLength = 60,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(stringResource(R.string.movie_cinema_cancel), onDismiss, Modifier.weight(1f), style = GlassButtonStyle.Ghost, enabled = !busy)
            GlassButton(stringResource(R.string.movie_cinema_open), { onOpen(title) }, Modifier.weight(1f), loading = busy)
        }
    }
}

/** My partner is waiting in the lobby: "<name> invited you" with Accept / Not now (same words as the web card). */
@Composable
fun CinemaInviteDialog(invite: CinemaInvite, busy: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    CinemaModal(dismissable = false, onDismiss = {}) {
        Text("🎬", fontSize = 40.sp)
        Text(
            stringResource(R.string.movie_cinema_invite_title, invite.hostName), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        Text(
            stringResource(R.string.movie_cinema_invite_text), color = Muted, fontSize = 13.sp,
            textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 6.dp),
        )
        if (!invite.movie.isNullOrBlank()) {
            Text(invite.movie, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(stringResource(R.string.movie_cinema_not_now), onDecline, Modifier.weight(1f), style = GlassButtonStyle.Ghost, enabled = !busy)
            GlassButton(stringResource(R.string.movie_cinema_accept), onAccept, Modifier.weight(1f), loading = busy)
        }
    }
}
