package com.twinhearts.app.feature.rewards.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.core.sound.TwinSound
import com.twinhearts.app.feature.rewards.domain.CardPrize
import com.twinhearts.app.feature.rewards.domain.GameCard
import kotlinx.coroutines.launch

private enum class CardPhase { Scratching, Claiming, Won, Failed }

/** Emoji and name for the game a card was won in (same keys the server uses). */
private fun gameInfo(key: String): Pair<String, String> = when (key) {
    "ttt" -> "❌⭕" to "Tic Tac Toe"
    "ludo" -> "🎲" to "Real Ludo"
    "tod" -> "🔥" to "Truth or Dare"
    "draw" -> "🎨" to "Draw & Guess"
    "memory" -> "🧠" to "Memory Match"
    "bomb" -> "💣" to "Bomb Game"
    "puzzle" -> "🧩" to "Picture Puzzle"
    "mindreader" -> "🔮" to "Mind Reader"
    "space" -> "🚀" to "Space Date"
    "atw" -> "🌍" to "Around The World"
    else -> "🎮" to "Couple Game"
}

/**
 * My Scratch Cards (web: Rewards → My Scratch Cards). Finishing a couple game gives BOTH partners a card here.
 * Scratch it with a finger, or tap Skip to reveal the prize at once. The server picks the prize and pays the coins.
 */
@Composable
fun ScratchCardsScreen(
    onPlayGames: () -> Unit = {},
    viewModel: ScratchCardsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TwinTheme.colors
    // Back from an open card goes to the list of cards, not out of the page.
    BackHandler(enabled = state.openCard != null) { viewModel.closeCard() }

    Column(Modifier.fillMaxSize()) {
        TwinTopBar(
            title = stringResource(R.string.rewards_cards_title),
            showSidebarButton = true,
            onSyncClick = viewModel::load,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TwinTheme.spacing.screenEdge),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val shown = rememberCountedBalance(state.balance)
            val bump = rememberCoinBump(state.balance)
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(40.dp, Modifier.graphicsLayer { scaleX = bump; scaleY = bump })
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("$shown", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = colors.ink, lineHeight = 32.sp)
                        Text(stringResource(R.string.rewards_gold_coins), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                    }
                }
            }

            val open = state.openCard
            when {
                open != null -> OpenCardView(
                    card = open,
                    prize = state.prize,
                    others = state.cards.count { it.id != open.id },
                    viewModel = viewModel,
                )
                state.loading -> LoadingSkeleton(lines = 3)
                state.failed -> ErrorState(message = stringResource(R.string.rewards_cards_load_failed), onRetry = viewModel::load)
                state.cards.isEmpty() -> GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎫", fontSize = 40.sp)
                        Text(stringResource(R.string.rewards_cards_empty_title), color = colors.ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(
                            stringResource(R.string.rewards_cards_empty_body),
                            color = colors.inkMuted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        GoldPill(stringResource(R.string.rewards_cards_play), filled = true, enabled = true, onClick = onPlayGames)
                    }
                }
                else -> GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.rewards_cards_unscratched) + " · " + state.cards.size,
                        color = colors.ink,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.cards.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { card -> CardTile(card, Modifier.weight(1f)) { viewModel.openTile(card) } }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (open == null && state.recent.isNotEmpty()) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.rewards_cards_recent), color = colors.ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    state.recent.forEach { r ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(gameInfo(r.game).first, fontSize = 22.sp)
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.title, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(gameInfo(r.game).second, color = colors.inkMuted, fontSize = 11.5.sp)
                            }
                            Text("+${r.coins} 🪙", color = RewardsStyle.Gold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }
            // Room for the floating bottom bar.
            Spacer(Modifier.height(112.dp))
        }
    }
}

@Composable
private fun CardTile(card: GameCard, modifier: Modifier, onClick: () -> Unit) {
    val info = gameInfo(card.game)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(RewardsStyle.GoldDeep, RewardsStyle.Gold, RewardsStyle.GoldDeep)))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(info.first, fontSize = 30.sp)
        Text(info.second, color = RewardsStyle.GoldInk, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, textAlign = TextAlign.Center)
        Text(stringResource(R.string.rewards_cards_tap), color = RewardsStyle.GoldInk.copy(alpha = 0.8f), fontSize = 11.sp)
    }
}

/** One card on screen: scratch it, or tap Skip to see the prize straight away. */
@Composable
private fun OpenCardView(
    card: GameCard,
    prize: CardPrize?,
    others: Int,
    viewModel: ScratchCardsViewModel,
) {
    val colors = TwinTheme.colors
    val info = gameInfo(card.game)
    val scope = rememberCoroutineScope()
    var phase by remember(card.id) { mutableStateOf(if (prize != null) CardPhase.Won else CardPhase.Scratching) }
    var shower by remember(card.id) { mutableIntStateOf(0) }

    fun reveal(skip: Boolean) {
        if (phase != CardPhase.Scratching) return
        phase = CardPhase.Claiming
        scope.launch {
            val paid = viewModel.open(skip)
            if (paid == null) {
                phase = CardPhase.Failed
            } else {
                phase = CardPhase.Won
                if (paid.coins > 0 && !paid.duplicate) { shower++; TwinSound.ad() }
            }
        }
    }

    Box {
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                info.first + " " + info.second,
                color = colors.ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, RewardsStyle.Gold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .background(RewardsStyle.Gold.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (prize != null) {
                        Text(prize.emoji, fontSize = 34.sp)
                        Text(prize.title, color = colors.ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.rewards_cards_won, prize.coins), color = RewardsStyle.Gold, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(prize.message, color = colors.inkMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    } else {
                        Text(info.first, fontSize = 34.sp)
                        Text(stringResource(R.string.rewards_cards_prize_hint), color = RewardsStyle.Gold, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
                if (phase != CardPhase.Won || prize == null) {
                    ScratchFoil(
                        hint = stringResource(R.string.rewards_cards_hint),
                        modifier = Modifier.fillMaxSize(),
                        enabled = phase == CardPhase.Scratching,
                        revealNow = phase == CardPhase.Claiming || phase == CardPhase.Won,
                        onThreshold = { reveal(skip = false) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            when (phase) {
                CardPhase.Scratching, CardPhase.Claiming -> {
                    GoldPill(
                        text = stringResource(R.string.rewards_cards_skip),
                        filled = true,
                        enabled = phase == CardPhase.Scratching,
                        onClick = { reveal(skip = true) },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.rewards_cards_scratch_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    GoldPill(stringResource(R.string.rewards_cards_back), filled = false, enabled = phase == CardPhase.Scratching, onClick = viewModel::closeCard)
                }
                CardPhase.Failed -> {
                    Text(
                        stringResource(R.string.rewards_cards_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    GoldPill(
                        text = stringResource(R.string.rewards_cards_retry),
                        filled = true,
                        enabled = true,
                        onClick = { phase = CardPhase.Scratching; reveal(skip = true) },
                    )
                    Spacer(Modifier.height(8.dp))
                    GoldPill(stringResource(R.string.rewards_cards_back), filled = false, enabled = true, onClick = viewModel::closeCard)
                }
                CardPhase.Won -> {
                    if (others > 0) {
                        GoldPill(stringResource(R.string.rewards_cards_next, others), filled = true, enabled = true, onClick = viewModel::next)
                        Spacer(Modifier.height(8.dp))
                    }
                    GoldPill(stringResource(R.string.rewards_cards_all), filled = others == 0, enabled = true, onClick = viewModel::closeCard)
                }
            }
        }
        // Coins burst over the card when the prize is paid.
        CoinShower(trigger = shower, modifier = Modifier.matchParentSize())
    }
}

@Composable
private fun GoldPill(text: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = TwinTheme.shapes.pill
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TwinTheme.shapes.minTouch)
            .clip(shape)
            .then(
                if (filled) Modifier.background(Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep)))
                else Modifier.border(1.5.dp, RewardsStyle.Gold, shape),
            )
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (filled) RewardsStyle.GoldInk else RewardsStyle.Gold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.5.sp,
            textAlign = TextAlign.Center,
        )
    }
}
