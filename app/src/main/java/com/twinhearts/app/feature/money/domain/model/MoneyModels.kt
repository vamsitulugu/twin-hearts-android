package com.twinhearts.app.feature.money.domain.model

import java.time.LocalDate

/** `type` of a transaction in the shared state. [key] is what is stored, so never rename it. */
enum class TxType(val key: String) {
    Expense("expense"),
    Income("income");

    companion object {
        fun fromKey(key: String?): TxType = if (key == Income.key) Income else Expense
    }
}

/** One saved transaction (web: `S.transactions` item). [by] is "user1" / "user2" / blank (old items). */
data class Transaction(
    val id: String,
    val type: TxType,
    val description: String,
    val amount: Double,
    val category: String,
    /** Null when the saved date cannot be read; such items are listed but not counted in any month. */
    val date: LocalDate?,
    val by: String,
)

/** What the "Add Transaction" form hands to the repository. */
data class NewTransaction(
    val type: TxType,
    val description: String,
    val amount: Double,
    val category: String,
    val date: LocalDate,
)

/** Everything the Money page shows, read from the couple's shared state. */
data class MoneyData(
    val transactions: List<Transaction>,
    /** My monthly spending goal in rupees; 0 = not set. */
    val myGoal: Double,
    val myRole: String,
    val myName: String,
    val partnerName: String,
)

/** Totals for one month. */
data class MonthSummary(val income: Double, val expense: Double) {
    val net: Double get() = income - expense

    /** Share of income that was kept, 0..100 (web ring). */
    val savedPercent: Int
        get() = if (income > 0) (((income - expense) / income) * PERCENT).toInt().coerceIn(0, PERCENT.toInt()) else 0

    private companion object {
        const val PERCENT = 100.0
    }
}

data class CategoryTotal(val category: String, val amount: Double)

data class WeekTotal(val label: String, val amount: Double)

/** The one-line tip under the hero card (web `renderFinInsight`). */
sealed interface Insight {
    data object StartLogging : Insight
    data class SpentSoFar(val amount: Double) : Insight

    /** A past (or future) month with nothing to compare against. */
    data class SpentInMonth(val amount: Double) : Insight
    data class Up(val percent: Int) : Insight
    data class Down(val percent: Int) : Insight
    data class Steady(val lastMonth: Double) : Insight
}

/** My monthly goal progress (web `renderFinanceSplit`). */
data class GoalProgress(
    val goal: Double,
    val spent: Double,
    val daysLeft: Int,
) {
    val percent: Int
        get() = if (goal > 0) ((spent / goal) * PERCENT).toInt().coerceIn(0, PERCENT.toInt()) else 0
    val remaining: Double get() = (goal - spent).coerceAtLeast(0.0)

    private companion object {
        const val PERCENT = 100.0
    }
}
