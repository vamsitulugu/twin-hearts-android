package com.twinhearts.app.feature.money.domain

import com.twinhearts.app.feature.money.domain.model.CategoryTotal
import com.twinhearts.app.feature.money.domain.model.GoalProgress
import com.twinhearts.app.feature.money.domain.model.Insight
import com.twinhearts.app.feature.money.domain.model.MonthSummary
import com.twinhearts.app.feature.money.domain.model.Transaction
import com.twinhearts.app.feature.money.domain.model.TxType
import com.twinhearts.app.feature.money.domain.model.WeekTotal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Categories offered as quick picks in the form (same names as the web colour map). */
val moneyCategories = listOf("Food", "Transport", "Entertainment", "Shopping", "Travel", "Health", "Date", "Other")

private const val STEADY_BAND = 5
private const val TREND_WEEKS = 8
private const val DAYS_IN_WEEK = 7

/** "₹1,23,456" with Indian digit grouping, no decimals unless the amount has paise. */
fun formatRupees(amount: Double): String {
    val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    return "₹" + format.format(amount)
}

private fun List<Transaction>.inMonth(month: YearMonth) = filter { YearMonth.from(it.date) == month }

private fun List<Transaction>.expenseSum() = filter { it.type == TxType.Expense }.sumOf { it.amount }

/** Income and expense of one month. */
fun monthSummary(all: List<Transaction>, month: YearMonth): MonthSummary {
    val inMonth = all.inMonth(month)
    return MonthSummary(
        income = inMonth.filter { it.type == TxType.Income }.sumOf { it.amount },
        expense = inMonth.expenseSum(),
    )
}

/** Expenses of one month grouped by category, biggest first. A blank category counts as "Other". */
fun spendingByCategory(all: List<Transaction>, month: YearMonth): List<CategoryTotal> =
    all.inMonth(month)
        .filter { it.type == TxType.Expense }
        .groupBy { it.category.ifBlank { "Other" } }
        .map { (category, items) -> CategoryTotal(category, items.sumOf { it.amount }) }
        .sortedByDescending { it.amount }

/** Spending in the last 8 weeks, oldest first, labelled W1..W8 (web `renderFinTrend`). */
fun weeklySpending(all: List<Transaction>, today: LocalDate): List<WeekTotal> =
    (TREND_WEEKS - 1 downTo 0).map { weeksAgo ->
        val end = today.minusDays((weeksAgo * DAYS_IN_WEEK).toLong())
        val start = end.minusDays((DAYS_IN_WEEK - 1).toLong())
        val amount = all
            .filter { it.type == TxType.Expense && !it.date.isBefore(start) && !it.date.isAfter(end) }
            .sumOf { it.amount }
        WeekTotal("W${TREND_WEEKS - weeksAgo}", amount)
    }

/** This month's spending compared with last month's (web `renderFinInsight`). */
fun spendingInsight(all: List<Transaction>, today: LocalDate): Insight {
    val thisMonth = YearMonth.from(today)
    val thisExp = all.inMonth(thisMonth).expenseSum()
    val lastExp = all.inMonth(thisMonth.minusMonths(1)).expenseSum()
    return when {
        lastExp == 0.0 && thisExp == 0.0 -> Insight.StartLogging
        lastExp == 0.0 -> Insight.SpentSoFar(thisExp)
        else -> {
            val diff = (((thisExp - lastExp) / lastExp) * 100).roundToInt()
            when {
                diff > STEADY_BAND -> Insight.Up(diff)
                diff < -STEADY_BAND -> Insight.Down(abs(diff))
                else -> Insight.Steady(lastExp)
            }
        }
    }
}

/** Items I added (or old ones with no owner) and items my partner added. */
fun splitByOwner(all: List<Transaction>, myRole: String): Pair<List<Transaction>, List<Transaction>> =
    all.partition { it.by.isBlank() || it.by == myRole }

fun goalProgress(mine: List<Transaction>, goal: Double, today: LocalDate): GoalProgress {
    val month = YearMonth.from(today)
    return GoalProgress(
        goal = goal,
        spent = mine.inMonth(month).expenseSum(),
        daysLeft = month.lengthOfMonth() - today.dayOfMonth,
    )
}
