package com.twinhearts.app.feature.pro

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border as borderModifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.core.pro.ProPlan
import com.twinhearts.app.core.pro.findActivity
import com.twinhearts.app.core.pro.openRazorpayCheckout
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The Pro page (web: public/pro.html). Plans, what Pro unlocks, status and receipts.
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
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            Column(Modifier.fillMaxWidth().padding(top = spacing.s), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = colors.pinkSoft, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(spacing.s))
                Text(
                    text = if (feature != null) stringResource(R.string.pro_unlock_feature, feature) else stringResource(R.string.pro_headline),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.pro_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                )
            }

            StatusCard(state)

            Text(stringResource(R.string.pro_choose), style = MaterialTheme.typography.titleSmall, color = colors.inkMuted)
            ProPlan.entries.forEach { plan ->
                PlanRow(
                    plan = plan,
                    selected = state.selected == plan,
                    enabled = !(state.hasLifetime),
                    onClick = { viewModel.select(plan) },
                )
            }

            val selected = state.selected
            GlassButton(
                text = when {
                    state.hasLifetime -> stringResource(R.string.pro_have_lifetime)
                    state.status.isActive() && selected != ProPlan.Lifetime ->
                        stringResource(R.string.pro_btn_extend, selected.rupees)
                    else -> stringResource(R.string.pro_btn_pay, selected.rupees)
                },
                onClick = viewModel::pay,
                enabled = !state.hasLifetime,
                loading = state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
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
            Text(
                stringResource(R.string.pro_secure),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.pro_unlocks), style = MaterialTheme.typography.titleSmall, color = colors.inkMuted)
            FeatureRow(Icons.Outlined.AutoAwesome, R.string.pro_f_both, R.string.pro_f_both_d)
            FeatureRow(Icons.Outlined.Public, R.string.pro_f_globe, R.string.pro_f_globe_d)
            FeatureRow(Icons.Outlined.LibraryMusic, R.string.pro_f_music, R.string.pro_f_music_d)
            FeatureRow(Icons.Outlined.CloudUpload, R.string.pro_f_movie, R.string.pro_f_movie_d)
            Text(
                stringResource(R.string.pro_free_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.history.isNotEmpty()) {
                Text(stringResource(R.string.pro_history), style = MaterialTheme.typography.titleSmall, color = colors.inkMuted)
                GlassCard(Modifier.fillMaxWidth()) {
                    state.history.forEach { p ->
                        Row(Modifier.fillMaxWidth().padding(vertical = spacing.xs), horizontalArrangement = Arrangement.SpaceBetween) {
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
private fun StatusCard(state: ProUiState) {
    val colors = TwinTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            val active = state.status.isActive()
            Icon(
                if (active) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                null,
                tint = if (active) colors.success else colors.inkMuted,
                modifier = Modifier.size(28.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (active) R.string.pro_status_on else R.string.pro_status_off),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                )
                Text(
                    text = when {
                        !active -> stringResource(R.string.pro_status_off_d)
                        state.status.plan == ProPlan.Lifetime -> stringResource(R.string.pro_status_lifetime)
                        else -> stringResource(R.string.pro_status_until, formatDate(state.status.expiresAt))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                )
            }
        }
    }
}

@Composable
private fun PlanRow(plan: ProPlan, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val ringColor = if (selected && enabled) colors.pinkSoft else colors.line
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .borderModifier(if (selected) 2.dp else 1.dp, ringColor, TwinTheme.shapes.card),
        onClick = if (enabled) onClick else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    Text(stringResource(planName(plan)), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    planBadge(plan)?.let {
                        Text(
                            stringResource(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.pinkSoft,
                            modifier = Modifier
                                .clip(CircleShape)
                                .borderModifier(1.dp, colors.pinkSoft, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(stringResource(planNote(plan)), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
            Text("₹${plan.rupees}", style = MaterialTheme.typography.titleLarge, color = colors.ink)
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: Int, desc: Int) {
    val colors = TwinTheme.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Icon(icon, null, tint = colors.pinkSoft, modifier = Modifier.size(26.dp))
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
        }
    }
}

private fun planName(p: ProPlan) = when (p) {
    ProPlan.Monthly -> R.string.pro_plan_monthly
    ProPlan.Yearly -> R.string.pro_plan_yearly
    ProPlan.Lifetime -> R.string.pro_plan_lifetime
}

private fun planNote(p: ProPlan) = when (p) {
    ProPlan.Monthly -> R.string.pro_plan_monthly_d
    ProPlan.Yearly -> R.string.pro_plan_yearly_d
    ProPlan.Lifetime -> R.string.pro_plan_lifetime_d
}

private fun planBadge(p: ProPlan): Int? = when (p) {
    ProPlan.Monthly -> null
    ProPlan.Yearly -> R.string.pro_badge_popular
    ProPlan.Lifetime -> R.string.pro_badge_best
}

private fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(Instant.parse(iso))
    }.getOrDefault("")
}
