package com.twinhearts.app.feature.pro

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.core.pro.ProPlan
import com.twinhearts.app.core.pro.findActivity
import com.twinhearts.app.core.pro.openRazorpayCheckout
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val Wine = Color(0xFFB0235F)
private val WineDeep = Color(0xFF7A0B44)
private val Gold = Color(0xFFFBBF24)
private val CardDark = Color(0xFF120A12)
private val Hair = Color.White.copy(alpha = 0.09f)
private val CardShape = RoundedCornerShape(22.dp)

/**
 * The Pro page (web: public/pro.html): plan tabs, one card per plan, tap "Choose ... Plan" to pay.
 * [feature] is set when a locked feature sent the person here; after paying they go straight back to it.
 */
@Composable
fun ProScreen(
    feature: String?,
    onBack: () -> Unit,
    viewModel: ProViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = TwinTheme.colors
    val spacing = TwinTheme.spacing

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { fx ->
            when (fx) {
                is ProEffect.LaunchCheckout -> {
                    val activity = context.findActivity()
                    if (activity == null || !openRazorpayCheckout(activity, fx.order)) viewModel.checkoutFailedToOpen()
                }
                ProEffect.Purchased -> {
                    Toast.makeText(context, R.string.pro_toast_welcome, Toast.LENGTH_LONG).show()
                    if (feature != null) onBack()
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TwinTopBar(title = stringResource(R.string.pro_title), onBackClick = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenEdge)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Header(feature)
            StatusPill(state)

            Text(
                stringResource(R.string.pro_choose),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
            PlanTabs(selected = state.selected, onSelect = viewModel::select)

            ProPlan.entries.forEach { plan ->
                PlanCard(
                    plan = plan,
                    selected = state.selected == plan,
                    status = state.status.takeIf { state.status.isActive() },
                    busy = state.busy,
                    onSelect = { viewModel.select(plan) },
                    onChoose = { viewModel.select(plan); viewModel.pay() },
                )
            }

            state.notice?.let { n ->
                Text(
                    text = when (n) {
                        is ProNotice.Res -> stringResource(n.id)
                        is ProNotice.Text -> n.text
                        is ProNotice.Error -> n.error.asText()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FreeNote()
            Text(
                stringResource(R.string.pro_secure),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.history.isNotEmpty()) {
                Text(stringResource(R.string.pro_history), style = MaterialTheme.typography.titleSmall, color = colors.inkMuted)
                Column(
                    Modifier.fillMaxWidth().background(CardDark, RoundedCornerShape(18.dp)).border(1.dp, Hair, RoundedCornerShape(18.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.history.forEach { p ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(p.plan?.name ?: "Pro", style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                                Text(formatDate(p.paidAt), style = MaterialTheme.typography.bodySmall, color = colors.inkFaint)
                            }
                            Text("₹${p.rupees}", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                        }
                    }
                }
            }
            Spacer(Modifier.height(spacing.xl))
        }
    }
}

@Composable
private fun Header(feature: String?) {
    val colors = TwinTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.WorkspacePremium, null, tint = colors.pinkSoft, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (feature != null) stringResource(R.string.pro_unlock_feature, feature) else stringResource(R.string.pro_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(stringResource(R.string.pro_everything), style = MaterialTheme.typography.titleSmall, color = colors.ink)
        Text(stringResource(R.string.pro_sub), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
        Text(stringResource(R.string.pro_sub2), style = MaterialTheme.typography.bodySmall, color = colors.inkFaint, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StatusPill(state: ProUiState) {
    if (!state.status.isActive()) return
    val green = Color(0xFF34D399)
    val text = if (state.status.plan == ProPlan.Lifetime) {
        stringResource(R.string.pro_status_lifetime_pill)
    } else {
        stringResource(R.string.pro_status_until_pill, formatDate(state.status.expiresAt))
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = green,
            modifier = Modifier
                .clip(CircleShape)
                .background(green.copy(alpha = 0.12f))
                .border(1.dp, green.copy(alpha = 0.35f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PlanTabs(selected: ProPlan, onSelect: (ProPlan) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(4.dp),
    ) {
        ProPlan.entries.forEach { plan ->
            val on = plan == selected
            // Outer box is not clipped, so the "Save 58%" badge can float above the Yearly tab.
            Box(Modifier.weight(1f).height(42.dp)) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (on) Modifier.background(Brush.horizontalGradient(listOf(Wine, WineDeep))) else Modifier)
                        .clickable { onSelect(plan) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(planName(plan)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (on) Color.White else Color.White.copy(alpha = 0.6f),
                    )
                }
                if (plan == ProPlan.Yearly) {
                    Text(
                        stringResource(R.string.pro_save_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-14).dp)
                            .background(Brush.horizontalGradient(listOf(TwinTheme.colors.pinkSoft, Wine)), CircleShape)
                            .padding(horizontal = 9.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: ProPlan,
    selected: Boolean,
    status: com.twinhearts.app.core.pro.ProStatus?,
    busy: Boolean,
    onSelect: () -> Unit,
    onChoose: () -> Unit,
) {
    val colors = TwinTheme.colors
    val lifetimeOwned = status?.plan == ProPlan.Lifetime
    val bg = if (selected) Modifier.background(Brush.linearGradient(listOf(Wine.copy(alpha = 0.35f), Color(0xFF3C0824).copy(alpha = 0.55f))), CardShape)
    else Modifier.background(CardDark, CardShape)
    Column(
        Modifier
            .fillMaxWidth()
            .then(bg)
            .border(if (selected) BorderStroke(1.5.dp, colors.pinkSoft) else BorderStroke(1.dp, Hair), CardShape)
            .clip(CardShape)
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(54.dp).background(if (plan == ProPlan.Yearly) Gold.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(planIcon(plan), null, tint = if (plan == ProPlan.Yearly) Gold else Color.White, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(planName(plan)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.ink)
                Text(stringResource(planLine1(plan)), style = MaterialTheme.typography.bodySmall, color = if (selected) colors.pinkSoft else colors.inkMuted)
                planLine2(plan)?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = colors.inkFaint) }
            }
            Column(horizontalAlignment = Alignment.End) {
                planBadge(plan)?.let {
                    val gold = plan == ProPlan.Yearly
                    val tone = if (gold) Gold else colors.pinkSoft
                    Text(
                        stringResource(it),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tone,
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .background(tone.copy(alpha = 0.14f), CircleShape)
                            .border(1.dp, tone.copy(alpha = 0.35f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                Text("₹${plan.rupees}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = colors.ink)
                Text(stringResource(planPer(plan)), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
        }

        Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureLine(Icons.Outlined.AutoAwesome, R.string.pro_f_both, R.string.pro_f_both_d)
            FeatureLine(Icons.Outlined.Public, R.string.pro_f_globe, R.string.pro_f_globe_d)
            FeatureLine(Icons.Outlined.LibraryMusic, R.string.pro_f_music, R.string.pro_f_music_d)
            FeatureLine(Icons.Outlined.CloudUpload, R.string.pro_f_movie, R.string.pro_f_movie_d)
            FeatureLine(Icons.Outlined.VerifiedUser, R.string.pro_f_noads, R.string.pro_f_noads_d)
        }

        val enabled = !lifetimeOwned && !busy
        Row(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    if (selected) Brush.horizontalGradient(listOf(Wine, WineDeep))
                    else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))),
                )
                .clickable(enabled = enabled, onClick = onChoose),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (busy && selected) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(
                    text = when {
                        lifetimeOwned -> stringResource(R.string.pro_have_lifetime)
                        status != null && plan != ProPlan.Lifetime -> stringResource(R.string.pro_btn_extend_plan, stringResource(planName(plan)))
                        else -> stringResource(R.string.pro_btn_choose, stringResource(planName(plan)))
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = if (enabled) 1f else 0.55f),
                )
                if (!lifetimeOwned) {
                    Spacer(Modifier.size(10.dp))
                    Icon(Icons.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun FeatureLine(icon: ImageVector, title: Int, desc: Int) {
    val colors = TwinTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Color(0xFFF3D3E4), modifier = Modifier.padding(top = 2.dp).size(20.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.ink)
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
    }
}

@Composable
private fun FreeNote() {
    val colors = TwinTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(18.dp))
            .border(1.dp, Hair, RoundedCornerShape(18.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).background(Gold.copy(alpha = 0.14f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.CardGiftcard, null, tint = Gold, modifier = Modifier.size(20.dp))
        }
        Text(stringResource(R.string.pro_free_note), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
    }
}

private fun planIcon(p: ProPlan) = when (p) {
    ProPlan.Monthly -> Icons.Outlined.CalendarMonth
    ProPlan.Yearly -> Icons.Outlined.WorkspacePremium
    ProPlan.Lifetime -> Icons.Outlined.AllInclusive
}

private fun planName(p: ProPlan) = when (p) {
    ProPlan.Monthly -> R.string.pro_plan_monthly
    ProPlan.Yearly -> R.string.pro_plan_yearly
    ProPlan.Lifetime -> R.string.pro_plan_lifetime
}

private fun planLine1(p: ProPlan) = when (p) {
    ProPlan.Monthly -> R.string.pro_plan_monthly_l1
    ProPlan.Yearly -> R.string.pro_plan_yearly_l1
    ProPlan.Lifetime -> R.string.pro_plan_lifetime_d
}

private fun planLine2(p: ProPlan): Int? = when (p) {
    ProPlan.Monthly -> R.string.pro_plan_monthly_l2
    ProPlan.Yearly -> R.string.pro_plan_yearly_l2
    ProPlan.Lifetime -> null
}

private fun planPer(p: ProPlan) = when (p) {
    ProPlan.Monthly -> R.string.pro_per_30
    ProPlan.Yearly -> R.string.pro_per_year
    ProPlan.Lifetime -> R.string.pro_per_once
}

private fun planBadge(p: ProPlan): Int? = when (p) {
    ProPlan.Monthly -> R.string.pro_badge_recommended
    ProPlan.Yearly -> R.string.pro_badge_popular
    ProPlan.Lifetime -> null
}

private fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(Instant.parse(iso))
    }.getOrDefault("")
}
