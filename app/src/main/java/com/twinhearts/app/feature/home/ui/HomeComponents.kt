package com.twinhearts.app.feature.home.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.Avatar
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.home.domain.model.LoveInteraction
import com.twinhearts.app.feature.home.domain.model.LoveKind
import java.util.Calendar

private val LogoSize = 36.dp
private val LogoCorner = 10.dp
private val ProfileSize = 40.dp
private val SearchHeight = 48.dp
private val CardShadeAlpha = 0.78f
private val PageIconCircle = 52.dp
private val PageIconSize = 24.dp
private const val MS_PER_DAY = 86_400_000L

/** Logo + name on the left, my avatar (opens the profile) on the right. */
@Composable
internal fun HomeHeader(myName: String, onProfileClick: () -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.core_logo),
            contentDescription = null,
            modifier = Modifier.size(LogoSize).clip(RoundedCornerShape(LogoCorner)),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
            modifier = Modifier
                .weight(1f)
                .padding(start = TwinTheme.spacing.m),
        )
        Box(
            modifier = Modifier
                .clip(TwinTheme.shapes.circle)
                .clickable(role = Role.Button, onClick = onProfileClick),
        ) {
            Avatar(name = myName, size = ProfileSize)
        }
    }
}

/** "Good evening, Name 👋" and the quote of the day, with its last word in italic pink. */
@Composable
internal fun HomeHero(myName: String, nowMillis: Long) {
    val colors = TwinTheme.colors
    val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
    val part = stringResource(
        when {
            hour < 5 -> R.string.home_greeting_night
            hour < 12 -> R.string.home_greeting_morning
            hour < 17 -> R.string.home_greeting_afternoon
            hour < 21 -> R.string.home_greeting_evening
            else -> R.string.home_greeting_night
        },
    )
    val greeting = if (myName.isBlank()) {
        stringResource(R.string.home_greeting_no_name, part)
    } else {
        stringResource(R.string.home_greeting_with_name, part, myName)
    }
    val before = stringArrayResource(R.array.home_quote_before)
    val words = stringArrayResource(R.array.home_quote_word)
    val after = stringArrayResource(R.array.home_quote_after)
    val index = ((nowMillis / MS_PER_DAY) % before.size).toInt()
    val quote = buildAnnotatedString {
        append(before[index])
        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = colors.pinkSoft)) { append(words[index]) }
        append(after[index])
    }
    Column(
        modifier = Modifier.padding(horizontal = TwinTheme.spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Text(text = greeting, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        Text(text = quote, style = MaterialTheme.typography.headlineLarge, color = colors.ink)
    }
}

/** "Search chats, memories…" bar and the grid/menu button. */
@Composable
internal fun HomeSearchRow(onShortcut: (HomeShortcut) -> Unit) {
    val colors = TwinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TwinTheme.spacing.screenEdge),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = SearchHeight)
                .clip(TwinTheme.shapes.pill)
                .background(colors.glass)
                .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
                .clickable(role = Role.Button) { onShortcut(HomeShortcut.Search) }
                .padding(horizontal = TwinTheme.spacing.l),
            horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.inkFaint, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.home_search_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GlassIconButton(
            icon = HomeShortcut.Menu.icon,
            contentDescription = stringResource(R.string.core_menu),
            onClick = { onShortcut(HomeShortcut.Menu) },
        )
    }
}

/** Camera (highlighted), Music, Movie, Meet Planner, Daily Routine. Scrolls sideways. */
@Composable
internal fun HomeChips(onShortcut: (HomeShortcut) -> Unit) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = TwinTheme.spacing.screenEdge),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        items(homeChips) { chip ->
            ShortcutChip(chip, primary = chip == HomeShortcut.Camera, onClick = { onShortcut(chip) })
        }
    }
}

@Composable
private fun ShortcutChip(shortcut: HomeShortcut, primary: Boolean, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    val fill: Brush = if (primary) colors.primaryBrush else Brush.linearGradient(listOf(colors.glass, colors.glass))
    Row(
        modifier = Modifier
            .heightIn(min = TwinTheme.shapes.minTouch)
            .clip(pill)
            .background(fill)
            .border(1.dp, if (primary) Color.White.copy(alpha = 0.18f) else colors.line, pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(shortcut.icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
        Text(stringResource(shortcut.labelRes), style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 1)
    }
}

/**
 * The 16:9 picture card with the newest signal and the Hug / Touch / Miss You buttons.
 * The picture changes with the newest signal, exactly like the web card.
 */
@Composable
internal fun StayConnectedCard(
    last: LoveInteraction?,
    nowMillis: Long,
    actionsEnabled: Boolean,
    onSend: (LoveKind) -> Unit,
) {
    val colors = TwinTheme.colors
    val image = when (last?.kind) {
        null -> R.drawable.home_card_idle
        LoveKind.Hug -> R.drawable.home_card_hug
        LoveKind.Touch -> R.drawable.home_card_touch
        LoveKind.MissYou -> R.drawable.home_card_miss
    }
    val caption = if (last == null) {
        stringResource(R.string.home_caption_idle)
    } else {
        stringResource(
            R.string.home_caption_format,
            stringResource(kindLabel(last.kind)),
            stringResource(if (last.fromMe) R.string.home_sent else R.string.home_received),
            agoText(last.timestampMillis, nowMillis),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(TwinTheme.shapes.card)
            .border(1.dp, colors.line, TwinTheme.shapes.card),
    ) {
        Crossfade(targetState = image, label = "loveCardImage", modifier = Modifier.fillMaxSize()) { res ->
            Image(
                painter = painterResource(res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background.copy(alpha = CardShadeAlpha)))),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(TwinTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
        ) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                LoveKind.entries.forEach { kind ->
                    LoveActionButton(
                        label = stringResource(kindLabel(kind)),
                        active = last?.kind == kind,
                        enabled = actionsEnabled,
                        onClick = { onSend(kind) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun kindLabel(kind: LoveKind): Int = when (kind) {
    LoveKind.Hug -> R.string.home_kind_hug
    LoveKind.Touch -> R.string.home_kind_touch
    LoveKind.MissYou -> R.string.home_kind_miss_you
}

@Composable
private fun LoveActionButton(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    val fill: Brush = if (active) colors.primaryBrush else Brush.linearGradient(listOf(colors.glass, colors.glass))
    Box(
        modifier = modifier
            .heightIn(min = TwinTheme.shapes.minTouch)
            .clip(pill)
            .background(fill)
            .border(1.dp, colors.lineSoft, pill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (enabled) colors.ink else colors.inkFaint,
            maxLines = 1,
        )
    }
}

/** "5m ago" style text from the web (`fmtAgo`). */
@Composable
private fun agoText(timestampMillis: Long, nowMillis: Long): String {
    val seconds = ((nowMillis - timestampMillis) / 1000L).coerceAtLeast(0L).toInt()
    return when {
        seconds < 10 -> stringResource(R.string.home_ago_now)
        seconds < 60 -> stringResource(R.string.home_ago_seconds, seconds)
        seconds < 3600 -> stringResource(R.string.home_ago_minutes, seconds / 60)
        seconds < 86_400 -> stringResource(R.string.home_ago_hours, seconds / 3600)
        else -> stringResource(R.string.home_ago_days, seconds / 86_400)
    }
}

/** A hug my partner sent that waits for me: "Name is sending you a hug 🤗" + Accept / Later. */
@Composable
internal fun IncomingHugCard(
    partnerName: String,
    enabled: Boolean,
    onAccept: () -> Unit,
    onLater: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(
                text = stringResource(R.string.home_hug_incoming, partnerName),
                style = MaterialTheme.typography.titleMedium,
                color = TwinTheme.colors.ink,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                GlassButton(
                    text = stringResource(R.string.home_hug_accept),
                    onClick = onAccept,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                GlassButton(
                    text = stringResource(R.string.home_hug_later),
                    onClick = onLater,
                    style = GlassButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Shown while there is no partner yet: opens the Pairing screen. */
@Composable
internal fun ConnectPartnerCard(onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text(
            text = stringResource(R.string.home_connect_cta),
            style = MaterialTheme.typography.titleMedium,
            color = TwinTheme.colors.pinkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Short green/red message under the card (replaces the web's toast). */
@Composable
internal fun HomeBanner(message: HomeMessage?) {
    if (message == null) return
    val colors = TwinTheme.colors
    val tint = if (message is HomeMessage.Info) colors.success else colors.error
    Text(
        text = message.asText(),
        style = MaterialTheme.typography.bodySmall,
        color = tint,
        modifier = Modifier
            .fillMaxWidth()
            .clip(TwinTheme.shapes.small)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.3f), TwinTheme.shapes.small)
            .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.m)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Groups of page shortcuts: Us, Explore Together, My Space, Plans. */
@Composable
internal fun HomeAllPages(onShortcut: (HomeShortcut) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = TwinTheme.spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xl),
    ) {
        homeGroups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                Text(
                    text = stringResource(group.titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    color = TwinTheme.colors.ink,
                )
                val perRow = if (group.items.size == 5) 5 else 4
                group.items.chunked(perRow).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                        rowItems.forEach { item ->
                            PageTile(item, onShortcut, Modifier.weight(1f))
                        }
                        repeat(perRow - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageTile(shortcut: HomeShortcut, onShortcut: (HomeShortcut) -> Unit, modifier: Modifier) {
    val colors = TwinTheme.colors
    Column(
        modifier = modifier
            .clip(TwinTheme.shapes.small)
            .clickable(role = Role.Button) { onShortcut(shortcut) }
            .padding(vertical = TwinTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(PageIconCircle)
                .clip(TwinTheme.shapes.circle)
                .background(colors.glass)
                .border(1.dp, colors.line, TwinTheme.shapes.circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(shortcut.icon, contentDescription = null, tint = colors.pinkSoft, modifier = Modifier.size(PageIconSize))
        }
        Text(
            text = stringResource(shortcut.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** Quote at the very bottom (offset from the top quote, like the web). */
@Composable
internal fun HomeFooterQuote(nowMillis: Long) {
    val quotes = stringArrayResource(R.array.home_footer_quotes)
    val day = nowMillis / MS_PER_DAY
    Text(
        text = quotes[((day * 3 + 1) % quotes.size).toInt()],
        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SerifFamily, fontStyle = FontStyle.Italic),
        color = TwinTheme.colors.inkMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TwinTheme.spacing.xxl),
    )
}
