package com.twinhearts.app.feature.rewards.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.rewards.domain.Gift

private const val GIFT_COLUMNS = 3
private val GiftCellShape = RoundedCornerShape(18.dp)

/**
 * Twin Rewards Step 2: the Gift Shop, opened from the chat attach menu (web: openGiftPanel in chat.js).
 * Coins buy a gift; the server charges them and posts the gift in the chat.
 * [onSent] gets the gift that was just sent (the chat plays its animation); [onEarn] opens the Rewards page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftShopSheet(
    onDismiss: () -> Unit,
    onSent: (Gift) -> Unit,
    onEarn: () -> Unit,
    viewModel: GiftShopViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TwinTheme.colors

    LaunchedEffect(Unit) { viewModel.open() }
    LaunchedEffect(state.sent) {
        state.sent?.let { gift ->
            viewModel.consumeSent()
            onSent(gift)
            onDismiss()
        }
    }

    val partner = state.partnerName.ifBlank { stringResource(R.string.gift_shop_partner_fallback) }
    val shop = state.shop
    val selected = shop?.gifts?.firstOrNull { it.id == state.selectedId }

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
        ) {
            // header: title, partner line, balance pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.gift_shop_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.ink,
                    )
                    Text(
                        stringResource(R.string.gift_shop_subtitle, partner),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                    )
                }
                Spacer(Modifier.width(TwinTheme.spacing.s))
                BalancePill(balance = shop?.balance, onClick = onEarn)
            }

            when {
                shop == null && state.error != null -> Column(
                    Modifier.fillMaxWidth().padding(vertical = TwinTheme.spacing.l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
                ) {
                    Text(state.error?.asText().orEmpty(), color = colors.inkMuted, textAlign = TextAlign.Center)
                    GlassButton(text = stringResource(R.string.gift_shop_retry), onClick = viewModel::refresh)
                }
                shop == null -> Text(
                    stringResource(R.string.rewards_loading),
                    color = colors.inkMuted,
                    modifier = Modifier.padding(vertical = TwinTheme.spacing.l),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(GIFT_COLUMNS),
                    modifier = Modifier.heightIn(max = GRID_MAX_HEIGHT),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(shop.gifts, key = { it.id }) { gift ->
                        GiftCell(
                            gift = gift,
                            selected = gift.id == state.selectedId,
                            locked = gift.price > shop.balance,
                            onClick = { viewModel.select(gift.id) },
                        )
                    }
                }
            }

            state.notice?.let { notice ->
                Text(
                    text = when (notice) {
                        GiftNotice.NotEnough -> stringResource(R.string.gift_shop_not_enough)
                        GiftNotice.Offline -> stringResource(R.string.gift_shop_offline)
                        is GiftNotice.Server -> notice.text ?: stringResource(R.string.gift_shop_failed)
                    },
                    color = colors.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            if (shop != null && selected != null) {
                ConfirmBar(
                    gift = selected,
                    balance = shop.balance,
                    partner = partner,
                    sending = state.sending,
                    onSend = viewModel::send,
                    onEarn = onEarn,
                )
            }
        }
    }
}

private val GRID_MAX_HEIGHT = 330.dp

@Composable
private fun BalancePill(balance: Int?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(TwinTheme.shapes.pill)
            .background(RewardsStyle.Gold.copy(alpha = 0.16f))
            .border(1.dp, RewardsStyle.Gold.copy(alpha = 0.45f), TwinTheme.shapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 38.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CoinIcon(size = 18.dp)
        Text(balance?.toString() ?: "—", fontWeight = FontWeight.ExtraBold, color = TwinTheme.colors.ink, fontSize = 16.sp)
        Text(stringResource(R.string.gift_shop_earn), color = RewardsStyle.Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun GiftCell(gift: Gift, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val premium = gift.tier == "premium"
    val border = when {
        selected -> BorderStroke(2.dp, RewardsStyle.Gold)
        premium -> BorderStroke(1.5.dp, RewardsStyle.Gold.copy(alpha = 0.5f))
        else -> BorderStroke(1.5.dp, colors.lineSoft)
    }
    Column(
        modifier = Modifier
            .alpha(if (locked && !selected) 0.55f else 1f)
            .clip(GiftCellShape)
            .background(if (selected) RewardsStyle.Gold.copy(alpha = 0.2f) else colors.glass)
            .border(border, GiftCellShape)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 108.dp)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(gift.emoji, fontSize = 34.sp)
        Text(
            gift.name,
            color = colors.ink,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "🪙 ${gift.price}",
            color = RewardsStyle.Gold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(TwinTheme.shapes.pill)
                .background(RewardsStyle.Gold.copy(alpha = 0.14f))
                .padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun ConfirmBar(
    gift: Gift,
    balance: Int,
    partner: String,
    sending: Boolean,
    onSend: () -> Unit,
    onEarn: () -> Unit,
) {
    val colors = TwinTheme.colors
    val canAfford = gift.price <= balance
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TwinTheme.shapes.card)
            .background(colors.glass)
            .border(1.dp, colors.line, TwinTheme.shapes.card)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(gift.emoji, fontSize = 38.sp)
            Column(Modifier.weight(1f)) {
                Text(gift.name, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (gift.blurb.isNotBlank()) Text(gift.blurb, color = colors.inkMuted, fontSize = 12.sp)
            }
            Text("🪙 ${gift.price}", color = RewardsStyle.Gold, fontWeight = FontWeight.ExtraBold)
        }
        if (canAfford) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TwinTheme.shapes.minTouch)
                    .alpha(if (sending) 0.6f else 1f)
                    .clip(TwinTheme.shapes.pill)
                    .background(Brush.verticalGradient(listOf(RewardsStyle.Gold, RewardsStyle.GoldDeep)))
                    .clickable(enabled = !sending, role = Role.Button, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (sending) stringResource(R.string.gift_shop_sending) else stringResource(R.string.gift_shop_send, partner),
                    color = RewardsStyle.GoldInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                )
            }
        } else {
            Text(
                stringResource(R.string.gift_shop_need, gift.price - balance),
                color = colors.inkMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassButton(
                text = stringResource(R.string.gift_shop_earn_coins),
                onClick = onEarn,
                modifier = Modifier.fillMaxWidth(),
                style = GlassButtonStyle.Secondary,
            )
        }
    }
}
