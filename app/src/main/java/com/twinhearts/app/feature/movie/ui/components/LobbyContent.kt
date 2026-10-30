package com.twinhearts.app.feature.movie.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.feature.movie.domain.formatRuntime
import com.twinhearts.app.feature.movie.domain.model.RoomStatus
import com.twinhearts.app.feature.movie.domain.model.WatchRoom

/**
 * Why this file exists: the "lobby" after a movie is picked, drawn like the web page. Both people
 * confirm they chose the same movie, press I'M READY, and one presses START TOGETHER. The other
 * must accept (see InviteDialog).
 */

private val CardFill = Color.White.copy(alpha = 0.045f)
private val CardEdge = Color.White.copy(alpha = 0.09f)
private val Wine = Color(0xFF6B093A)
private val WineLight = Color(0xFFE5668F)
private val Muted = Color(0xFFA89AA0)
private val Green = Color(0xFF34D399)
private val Amber = Color(0xFFFFB43C)

/** The web page's dark card: very light fill, thin light edge. */
private fun Modifier.wtCard(radius: Int = 18, fill: Color = CardFill, edge: Color = CardEdge): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return this.clip(shape).background(fill).border(1.dp, edge, shape)
}

@Composable
fun LobbyContent(
    room: WatchRoom,
    busy: Boolean,
    onReady: () -> Unit,
    onStart: () -> Unit,
    onCancelStart: () -> Unit,
    onCancelMovie: () -> Unit,
    onChange: () -> Unit,
    subtitle: SubtitleChoice?,
    onSubtitle: (SubtitleChoice?) -> Unit,
) {
    val context = LocalContext.current
    val waitingForPartner = room.status == RoomStatus.StartRequested && room.startRequestedByMe
    val bothReady = room.myReady && room.partnerReady && room.moviesMatch
    val bothPicked = room.myTitle != null && room.partnerTitle != null
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val choice = readSubtitle(context, uri)
        if (choice == null) {
            Toast.makeText(context, R.string.movie_toast_subs_failed, Toast.LENGTH_SHORT).show()
        } else {
            onSubtitle(choice)
            Toast.makeText(context, R.string.movie_toast_subs_loaded, Toast.LENGTH_SHORT).show()
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── The movie I picked ──
        Row(
            Modifier.fillMaxWidth().wtCard().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF5A143A), Color(0xFF200815))))
                    .border(1.dp, CardEdge, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Movie, null, tint = WineLight, modifier = Modifier.size(22.dp)) }
            Column(Modifier.weight(1f)) {
                Text(room.myTitle.orEmpty(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        room.myDurationSec?.let { formatRuntime(it) },
                        stringResource(if (room.sharedMovieId != null) R.string.movie_source_cloud else R.string.movie_source_device),
                    ).joinToString(" · "),
                    color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.08f))
                    .clickable(onClick = onChange).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.movie_change), color = Color.White, fontSize = 12.sp) }
        }

        // ── Movie match banner ──
        if (bothPicked) {
            val ok = room.moviesMatch
            val tone = if (ok) Green else Amber
            Text(
                stringResource(if (ok) R.string.movie_banner_match else R.string.movie_banner_mismatch),
                color = tone, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
                    .wtCard(radius = 12, fill = tone.copy(alpha = 0.12f), edge = tone.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }

        // ── Playback settings: two tiles ──
        Column(Modifier.fillMaxWidth().wtCard().padding(12.dp)) {
            Text(
                stringResource(R.string.movie_playback_settings).uppercase(), color = Muted, fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 10.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingTile(
                    Icons.Rounded.Sync, stringResource(R.string.movie_auto_sync), stringResource(R.string.movie_always_on),
                    on = true, modifier = Modifier.weight(1f),
                )
                SettingTile(
                    Icons.Rounded.ClosedCaption, stringResource(R.string.movie_subtitles),
                    subtitle?.name ?: stringResource(R.string.movie_subs_none), on = subtitle != null, modifier = Modifier.weight(1f),
                    onClick = { if (subtitle != null) onSubtitle(null) else picker.launch(arrayOf("*/*")) },
                )
            }
        }

        // ── You ❤️ Partner ──
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PersonChip(stringResource(R.string.movie_you), mine = true, room = room, modifier = Modifier.weight(1f))
            Text("❤️", fontSize = 18.sp)
            PersonChip(stringResource(R.string.movie_partner), mine = false, room = room, modifier = Modifier.weight(1f))
        }

        when {
            waitingForPartner -> WaitingBlock(onCancelStart)
            bothReady -> Column(
                Modifier.fillMaxWidth().wtCard().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.movie_both_ready), color = WineLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                GlassButton(stringResource(R.string.movie_start), onStart, Modifier.fillMaxWidth(), loading = busy)
            }
            // The ready button shows when the partner has not picked yet, or both picked the same movie.
            room.myTitle != null && (room.partnerTitle == null || room.moviesMatch) -> ReadyButton(room.myReady, busy, onReady)
        }
        if (!waitingForPartner && room.myReady && !bothReady) {
            Text(
                stringResource(R.string.movie_waiting_join), color = Muted, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
        }
        GlassButton(
            stringResource(R.string.movie_cancel_together), onCancelMovie, Modifier.fillMaxWidth().padding(top = 4.dp),
            style = GlassButtonStyle.Ghost, enabled = !busy,
        )
    }
}

@Composable
private fun SettingTile(
    icon: ImageVector,
    label: String,
    value: String,
    on: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.clip(shape)
            .background(if (on) Wine.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, if (on) Wine else CardEdge, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = Color(0xFFF3E9EC), modifier = Modifier.size(22.dp))
        Text(label, color = Color(0xFFF3E9EC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(value, color = if (on) WineLight else Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReadyButton(ready: Boolean, busy: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .wtCard(radius = 16, fill = if (ready) Green.copy(alpha = 0.14f) else CardFill, edge = if (ready) Green.copy(alpha = 0.55f) else CardEdge)
            .clickable(enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(if (ready) R.string.movie_ready_on else R.string.movie_ready),
            color = if (ready) Green else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
        )
    }
}

/** A pulsing heart while the partner has not accepted yet, with a Cancel under it. */
@Composable
private fun WaitingBlock(onCancel: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse), label = "heart",
    )
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp).wtCard(radius = 22).padding(horizontal = 18.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(84.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape).background(Wine.copy(alpha = 0.14f))
                .border(5.dp, Wine.copy(alpha = 0.07f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("❤️", fontSize = 38.sp) }
        Text(stringResource(R.string.movie_waiting_partner), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        Text(stringResource(R.string.movie_waiting_join), color = Muted, fontSize = 13.sp)
        GlassButton(stringResource(R.string.movie_cancel), onCancel, Modifier.padding(top = 8.dp), style = GlassButtonStyle.Ghost)
    }
}

@Composable
private fun PersonChip(name: String, mine: Boolean, room: WatchRoom, modifier: Modifier) {
    val ready = if (mine) room.myReady else room.partnerReady
    val hasMovie = if (mine) room.myTitle != null else room.partnerTitle != null
    val status = when {
        ready -> R.string.movie_state_ready
        hasMovie -> R.string.movie_state_selected
        mine -> R.string.movie_selecting
        else -> R.string.movie_waiting
    }
    Column(
        modifier.wtCard(
            fill = if (ready) Green.copy(alpha = 0.08f) else CardFill,
            edge = if (ready) Green.copy(alpha = 0.5f) else CardEdge,
        ).padding(horizontal = 10.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Wine, Color(0xFF2A0A1C))))
                .border(2.dp, Wine.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Person, null, tint = WineLight, modifier = Modifier.size(26.dp)) }
        Text(name, color = Color(0xFFF1E6E9), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(stringResource(status), color = if (ready) Green else Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Big 3 · 2 · 1 shown on both phones until the shared start time (wine glow on black, like the web page). */
@Composable
fun CountdownContent(seconds: Int) {
    val n = seconds.coerceAtLeast(1)
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Wine.copy(alpha = 0.35f), Color.Black), radius = 900f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.movie_starting_together).uppercase(), color = Color(0xFF9A9A9A), fontSize = 13.sp,
                letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp),
            )
            // Each new number pops in from small, like wtCountPop on the web page.
            key(n) {
                val pop = remember { Animatable(0.6f) }
                LaunchedEffect(Unit) { pop.animateTo(1f, tween(900)) }
                Text(
                    n.toString(),
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(Color(0xFFC8102E), Color.White)),
                        fontSize = 96.sp, fontWeight = FontWeight.ExtraBold,
                    ),
                    modifier = Modifier.scale(pop.value),
                )
            }
        }
    }
}
