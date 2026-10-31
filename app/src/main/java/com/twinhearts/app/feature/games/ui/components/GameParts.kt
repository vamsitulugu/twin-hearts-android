package com.twinhearts.app.feature.games.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.games.domain.engine.ChatLine
import com.twinhearts.app.feature.games.domain.model.Players
import kotlinx.coroutines.delay

/** Centered game title with the "Everything Together" line under it. */
@Composable
fun HeroTitle(title: String, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.ink, textAlign = TextAlign.Center)
        Text(stringResource(R.string.games_hero_tag), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
    }
}

/** A big number over a small label (win streak, fastest time, rounds...). */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, shape)
            .padding(vertical = TwinTheme.spacing.m, horizontal = TwinTheme.spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
    }
}

/** A tappable option card that glows burgundy when selected (difficulty, mode, grid size). */
@Composable
fun SelectCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.card
    val fill = if (selected) Modifier.background(colors.primaryBrush) else Modifier.background(colors.glass)
    Column(
        modifier = modifier
            .clip(shape)
            .then(fill)
            .border(1.dp, if (selected) colors.accent else colors.lineSoft, shape)
            .clickable(onClick = onClick)
            .padding(TwinTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** A small rounded label (round number, streak, timer, hud chips). */
@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val colors = TwinTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = colors.ink,
        modifier = modifier
            .clip(TwinTheme.shapes.pill)
            .background(if (highlight) colors.primaryStart.copy(alpha = 0.55f) else colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
            .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
    )
}

/** Thin bar that empties as time runs out. [low] turns it red. */
@Composable
fun GameTimerBar(fraction: Float, low: Boolean, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Box(modifier = modifier.fillMaxWidth().height(6.dp).clip(TwinTheme.shapes.pill).background(colors.lineSoft)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(TwinTheme.shapes.pill)
                .background(if (low) colors.error else colors.accent),
        )
    }
}

/** The big 3 - 2 - 1 - GO! number. */
@Composable
fun CountdownNumber(secondsLeft: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        Text(
            text = if (secondsLeft > 0) "$secondsLeft" else stringResource(R.string.games_go),
            style = MaterialTheme.typography.displayLarge,
            color = TwinTheme.colors.ink,
        )
    }
}

/** The current time in milliseconds, refreshed every [stepMs] so timers redraw. */
@Composable
fun rememberNow(stepMs: Long = 250L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(stepMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(stepMs)
        }
    }
    return now
}

/** Glass text field with a round send button. Sends on the keyboard's Send key too. */
@Composable
fun GameInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    sendLabel: String = stringResource(R.string.games_send_desc),
    enabled: Boolean = true,
) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TwinTheme.shapes.fieldHeight)
                        .clip(shape)
                        .background(colors.glass)
                        .border(1.dp, colors.line, shape)
                        .padding(horizontal = TwinTheme.spacing.l),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.inkFaint)
                    inner()
                }
            },
        )
        Box(
            modifier = Modifier
                .size(TwinTheme.shapes.minTouch)
                .clip(TwinTheme.shapes.circle)
                .background(colors.primaryBrush)
                .clickable(enabled = enabled, onClick = onSend)
                .semantics { contentDescription = sendLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text("➤", fontSize = 18.sp, color = colors.ink)
        }
    }
}

/** A tappable emoji (guidance arrows, reactions). */
@Composable
fun EmojiButton(emoji: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwinTheme.colors
    Box(
        modifier = modifier
            .size(TwinTheme.shapes.minTouch)
            .clip(TwinTheme.shapes.small)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 22.sp)
    }
}

/**
 * The emoji strip, text field and last few lines shared by Bomb and Picture Puzzle
 * (the web app calls it the "guide bar" + chat row).
 */
@Composable
fun CoopChat(
    emojis: List<String>,
    log: List<ChatLine>,
    players: Players,
    placeholder: String,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    var text by rememberSaveable { mutableStateOf("") }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            emojis.forEach { EmojiButton(it, onClick = { onSend(it) }) }
        }
        GameInput(
            value = text,
            onValueChange = { text = it },
            placeholder = placeholder,
            onSend = {
                if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                }
            },
        )
        log.takeLast(6).forEach { line ->
            val name = if (line.who == players.side) players.myName else players.partnerName
            Text(
                stringResource(R.string.games_bm_chat_line, name, line.text),
                style = MaterialTheme.typography.bodySmall,
                color = if (line.who == players.side) colors.inkMuted else colors.ink,
            )
        }
    }
}
