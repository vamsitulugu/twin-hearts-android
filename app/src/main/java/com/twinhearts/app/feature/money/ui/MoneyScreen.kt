package com.twinhearts.app.feature.money.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinhearts.app.R
import com.twinhearts.app.core.common.asText
import com.twinhearts.app.core.design.EmptyState
import com.twinhearts.app.core.design.ErrorState
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.LoadingSkeleton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.core.design.TwinTopBar
import com.twinhearts.app.feature.money.domain.formatRupees
import com.twinhearts.app.feature.money.domain.goalProgress
import com.twinhearts.app.feature.money.domain.model.Insight
import com.twinhearts.app.feature.money.domain.model.MoneyData
import com.twinhearts.app.feature.money.domain.model.Transaction
import com.twinhearts.app.feature.money.domain.model.TxType
import com.twinhearts.app.feature.money.domain.monthSummary
import com.twinhearts.app.feature.money.domain.spendingByCategory
import com.twinhearts.app.feature.money.domain.spendingInsight
import com.twinhearts.app.feature.money.domain.splitByOwner
import com.twinhearts.app.feature.money.domain.weeklySpending
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

/** Newest transactions shown in the main list (web shows 60) and in each person's list (web: 30). */
private const val MAIN_LIST_LIMIT = 60
private const val PERSON_LIST_LIMIT = 30

/**
 * Money page (web: `#page-money`): this month's net, a spending tip, income / expense / savings,
 * spending by category, an 8-week trend, the transaction list, and each partner's spending with
 * my monthly goal. Everything lives in the couple's shared state, so the web app shows the same.
 */
@Composable
fun MoneyScreen(
    onBack: () -> Unit,
    viewModel: MoneyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Transaction?>(null) }

    // One-off messages: toast once, then clear. A successful add also closes the form.
    val message = state.message
    val messageText = when (message) {
        null -> null
        MoneyMessage.Added -> stringResource(R.string.money_added)
        MoneyMessage.Deleted -> stringResource(R.string.money_deleted)
        MoneyMessage.GoalSaved -> stringResource(R.string.money_goal_saved)
        is MoneyMessage.Failed -> message.error.asText()
    }
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        if (message == MoneyMessage.Added) showAdd = false
        Toast.makeText(context, messageText.orEmpty(), Toast.LENGTH_SHORT).show()
        viewModel.messageShown()
    }

    val data = state.data
    Column(modifier = Modifier.fillMaxSize()) {
        TwinTopBar(
            title = stringResource(R.string.money_title),
            onBackClick = onBack,
            onSyncClick = viewModel::load,
        )
        when {
            data == null && state.loading -> {
                Column(modifier = Modifier.padding(TwinTheme.spacing.screenEdge)) { LoadingSkeleton(lines = 4) }
            }
            data == null -> {
                ErrorState(message = (state.error ?: com.twinhearts.app.core.common.AppError.Unknown).asText(), onRetry = viewModel::load)
            }
            else -> MoneyContent(
                data = data,
                saving = state.saving,
                onAdd = { showAdd = true },
                onDelete = { deleteTarget = it },
                onSaveGoal = viewModel::saveGoal,
            )
        }
    }

    if (showAdd) {
        AddTransactionSheet(
            saving = state.saving,
            onSave = viewModel::addTransaction,
            onDismiss = { showAdd = false },
        )
    }
    deleteTarget?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = TwinTheme.colors.card,
            title = { Text(stringResource(R.string.money_delete_title), color = TwinTheme.colors.ink) },
            text = {
                Text(
                    stringResource(R.string.money_delete_message, tx.description, formatRupees(tx.amount)),
                    color = TwinTheme.colors.inkMuted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx.id)
                        deleteTarget = null
                    },
                ) { Text(stringResource(R.string.money_delete_confirm), color = TwinTheme.colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.core_cancel)) }
            },
        )
    }
}

@Composable
private fun MoneyContent(
    data: MoneyData,
    saving: Boolean,
    onAdd: () -> Unit,
    onDelete: (Transaction) -> Unit,
    onSaveGoal: (Double) -> Unit,
) {
    val colors = TwinTheme.colors
    val today = remember { LocalDate.now() }
    val month = YearMonth.from(today)
    val all = data.transactions
    val summary = remember(all) { monthSummary(all, month) }
    val categories = remember(all) { spendingByCategory(all, month) }
    val weeks = remember(all) { weeklySpending(all, today) }
    val insight = remember(all) { spendingInsight(all, today) }
    val (mine, theirs) = remember(all, data.myRole) { splitByOwner(all, data.myRole) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TwinTheme.spacing.screenEdge)
            .padding(bottom = TwinTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        // Hero: saved ring + this month's net
        GlassCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.l),
            ) {
                SavedRing(percent = summary.savedPercent)
                Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs)) {
                    Text(
                        stringResource(R.string.money_net_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkMuted,
                    )
                    Text(
                        text = (if (summary.net >= 0) "+" else "-") + formatRupees(abs(summary.net)),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = SerifFamily,
                        color = if (summary.net >= 0) colors.success else colors.error,
                    )
                    Text(
                        stringResource(R.string.money_in_out, formatRupees(summary.income), formatRupees(summary.expense)),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                    )
                }
            }
        }

        InsightCard(insight)

        Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
            AmountTile(formatRupees(summary.income), stringResource(R.string.money_income), colors.success, Modifier.weight(1f))
            AmountTile(formatRupees(summary.expense), stringResource(R.string.money_expense), colors.error, Modifier.weight(1f))
            AmountTile(
                formatRupees(abs(summary.net)),
                stringResource(R.string.money_savings),
                if (summary.net >= 0) colors.success else colors.error,
                Modifier.weight(1f),
            )
        }

        GlassCard {
            SectionTitle("🥧 " + stringResource(R.string.money_by_category))
            if (categories.isEmpty()) {
                MutedText(stringResource(R.string.money_no_expenses))
            } else {
                CategoryDonut(categories, modifier = Modifier.padding(top = TwinTheme.spacing.m))
            }
        }

        GlassCard {
            SectionTitle("📊 " + stringResource(R.string.money_trend))
            WeeklyTrend(weeks, modifier = Modifier.padding(top = TwinTheme.spacing.m))
        }

        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionTitle("💳 " + stringResource(R.string.money_transactions))
                GlassButton(
                    text = stringResource(R.string.money_add),
                    onClick = onAdd,
                    leadingIcon = Icons.Rounded.Add,
                )
            }
            if (all.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.money_empty_title),
                    message = stringResource(R.string.money_empty_hint),
                    icon = Icons.Rounded.AccountBalanceWallet,
                    actionLabel = stringResource(R.string.money_empty_action),
                    onAction = onAdd,
                )
            } else {
                all.sortedByDescending { it.date }.take(MAIN_LIST_LIMIT).forEach { tx ->
                    TransactionRow(tx, onDelete = { onDelete(tx) })
                }
            }
        }

        PersonCard(
            title = stringResource(R.string.money_person_spending, data.myName.ifBlank { stringResource(R.string.money_me) }),
            items = mine,
        ) {
            GoalSection(
                goal = data.myGoal,
                mine = mine,
                today = today,
                saving = saving,
                onSaveGoal = onSaveGoal,
            )
        }
        PersonCard(
            title = stringResource(R.string.money_person_spending, data.partnerName.ifBlank { stringResource(R.string.money_partner) }),
            items = theirs,
        ) {}
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = TwinTheme.colors.ink)
}

@Composable
private fun MutedText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TwinTheme.colors.inkMuted,
        modifier = Modifier.padding(top = TwinTheme.spacing.s),
    )
}

@Composable
private fun InsightCard(insight: Insight) {
    val colors = TwinTheme.colors
    val (icon, text) = when (insight) {
        Insight.StartLogging -> "💡" to stringResource(R.string.money_insight_start)
        is Insight.SpentSoFar -> "💡" to stringResource(R.string.money_insight_so_far, formatRupees(insight.amount))
        is Insight.Up -> "📈" to stringResource(R.string.money_insight_up, insight.percent)
        is Insight.Down -> "📉" to stringResource(R.string.money_insight_down, insight.percent)
        is Insight.Steady -> "⚖️" to stringResource(R.string.money_insight_steady, formatRupees(insight.lastMonth))
    }
    GlassCard(contentPadding = TwinTheme.spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
        }
    }
}

@Composable
private fun AmountTile(amount: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, contentPadding = TwinTheme.spacing.m) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = TwinTheme.colors.inkMuted)
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction, onDelete: () -> Unit) {
    val colors = TwinTheme.colors
    val income = tx.type == TxType.Income
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
    ) {
        Box(
            modifier = Modifier
                .clip(TwinTheme.shapes.circle)
                .background(colors.glass)
                .padding(TwinTheme.spacing.s),
            contentAlignment = Alignment.Center,
        ) {
            Text(categoryEmoji(tx), style = MaterialTheme.typography.titleMedium)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description,
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(
                    tx.category.ifBlank { "Other" },
                    tx.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
            )
        }
        Text(
            text = (if (income) "+" else "-") + formatRupees(tx.amount),
            style = MaterialTheme.typography.titleSmall,
            color = if (income) colors.success else colors.error,
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.money_delete_cd, tx.description),
                tint = colors.inkMuted,
            )
        }
    }
}

/** Same emoji per category as the web list. */
private fun categoryEmoji(tx: Transaction): String = when (tx.category) {
    "Food" -> "🍽️"
    "Transport" -> "🚗"
    "Entertainment" -> "🎬"
    "Shopping" -> "🛒"
    "Travel" -> "✈️"
    "Health" -> "💊"
    "Date" -> "🌹"
    else -> if (tx.type == TxType.Income) "💰" else "💳"
}

@Composable
private fun PersonCard(title: String, items: List<Transaction>, extra: @Composable () -> Unit) {
    val colors = TwinTheme.colors
    GlassCard {
        SectionTitle(title)
        if (items.isEmpty()) {
            MutedText(stringResource(R.string.money_empty_title))
        } else {
            items.sortedByDescending { it.date }.take(PERSON_LIST_LIMIT).forEach { tx ->
                val income = tx.type == TxType.Income
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TwinTheme.spacing.s),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = tx.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = TwinTheme.spacing.s),
                    )
                    Text(
                        text = (if (income) "+" else "-") + formatRupees(tx.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (income) colors.success else colors.error,
                    )
                }
            }
        }
        extra()
    }
}

/** My monthly spending goal: a field to set it, a bar and "₹x left · n days". */
@Composable
private fun GoalSection(
    goal: Double,
    mine: List<Transaction>,
    today: LocalDate,
    saving: Boolean,
    onSaveGoal: (Double) -> Unit,
) {
    val colors = TwinTheme.colors
    var goalText by rememberSaveable(goal) { mutableStateOf(if (goal > 0) goal.toLong().toString() else "") }
    val newGoal = goalText.toDoubleOrNull() ?: 0.0
    val progress = goalProgress(mine, goal, today)
    val barColor = when {
        progress.percent >= FULL_PERCENT -> colors.error
        progress.percent >= WARN_PERCENT -> Color(0xFFFBBF24)
        else -> colors.success
    }

    Column(
        modifier = Modifier.padding(top = TwinTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        TwinTextField(
            value = goalText,
            onValueChange = { goalText = it.filter(Char::isDigit) },
            label = stringResource(R.string.money_goal_label),
            placeholder = "0",
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
            maxLength = GOAL_DIGITS,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${progress.percent}%", style = MaterialTheme.typography.labelLarge, color = barColor)
            Text(
                text = if (goal > 0) {
                    stringResource(R.string.money_goal_left, formatRupees(progress.remaining), progress.daysLeft)
                } else {
                    stringResource(R.string.money_goal_set)
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(TwinTheme.shapes.pill)
                .background(colors.lineSoft),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.percent / 100f)
                    .height(8.dp)
                    .clip(TwinTheme.shapes.pill)
                    .background(barColor),
            )
        }
        GlassButton(
            text = stringResource(R.string.money_goal_save),
            onClick = { onSaveGoal(newGoal) },
            style = GlassButtonStyle.Secondary,
            enabled = !saving && newGoal != goal,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val FULL_PERCENT = 100
private const val WARN_PERCENT = 80
private const val GOAL_DIGITS = 9
