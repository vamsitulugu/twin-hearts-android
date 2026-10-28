package com.twinhearts.app.feature.money

import com.twinhearts.app.feature.money.domain.formatRupees
import com.twinhearts.app.feature.money.domain.goalProgress
import com.twinhearts.app.feature.money.domain.model.Insight
import com.twinhearts.app.feature.money.domain.model.Transaction
import com.twinhearts.app.feature.money.domain.model.TxType
import com.twinhearts.app.feature.money.domain.monthSummary
import com.twinhearts.app.feature.money.domain.spendingByCategory
import com.twinhearts.app.feature.money.domain.spendingInsight
import com.twinhearts.app.feature.money.domain.splitByOwner
import com.twinhearts.app.feature.money.domain.weeklySpending
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MoneyMathTest {

    private val today = LocalDate.of(2026, 10, 28)
    private val oct = YearMonth.of(2026, 10)

    private fun tx(
        amount: Double,
        date: String?,
        type: TxType = TxType.Expense,
        category: String = "Food",
        by: String = "user1",
    ) = Transaction("id$amount$date", type, "d", amount, category, date?.let(LocalDate::parse), by)

    @Test fun `month summary adds income and expense of that month only`() {
        val all = listOf(
            tx(1000.0, "2026-10-02", TxType.Income),
            tx(300.0, "2026-10-10"),
            tx(50.0, "2026-09-30"),
        )
        val s = monthSummary(all, oct)
        assertEquals(1000.0, s.income, 0.0)
        assertEquals(300.0, s.expense, 0.0)
        assertEquals(70, s.savedPercent)
    }

    @Test fun `items with no readable date are left out of every month`() {
        val all = listOf(tx(500.0, null), tx(100.0, "2026-10-01"))
        assertEquals(100.0, monthSummary(all, oct).expense, 0.0)
        assertEquals(100.0, weeklySpending(all, today).sumOf { it.amount }, 0.0)
    }

    @Test fun `saved percent never goes below zero`() {
        val s = monthSummary(listOf(tx(10.0, "2026-10-01", TxType.Income), tx(90.0, "2026-10-02")), oct)
        assertEquals(0, s.savedPercent)
    }

    @Test fun `categories are grouped biggest first and blank becomes Other`() {
        val all = listOf(tx(10.0, "2026-10-01", category = "Food"), tx(70.0, "2026-10-02", category = ""), tx(5.0, "2026-10-03", category = "Food"))
        val result = spendingByCategory(all, oct)
        assertEquals(listOf("Other", "Food"), result.map { it.category })
        assertEquals(15.0, result[1].amount, 0.0)
    }

    @Test fun `weekly trend has eight weeks oldest first`() {
        val weeks = weeklySpending(listOf(tx(40.0, "2026-10-28")), today)
        assertEquals(8, weeks.size)
        assertEquals("W1", weeks.first().label)
        assertEquals(40.0, weeks.last().amount, 0.0)
    }

    @Test fun `insight start logging when nothing in either month`() {
        assertEquals(Insight.StartLogging, spendingInsight(emptyList(), today))
    }

    @Test fun `insight compares with last month`() {
        val up = listOf(tx(100.0, "2026-09-10"), tx(150.0, "2026-10-10"))
        assertEquals(Insight.Up(50), spendingInsight(up, today))
        val down = listOf(tx(100.0, "2026-09-10"), tx(40.0, "2026-10-10"))
        assertEquals(Insight.Down(60), spendingInsight(down, today))
        val steady = listOf(tx(100.0, "2026-09-10"), tx(103.0, "2026-10-10"))
        assertEquals(Insight.Steady(100.0), spendingInsight(steady, today))
    }

    @Test fun `insight for a past month with nothing before it says spent in month`() {
        val all = listOf(tx(80.0, "2026-08-10"))
        assertEquals(Insight.SpentInMonth(80.0), spendingInsight(all, today, YearMonth.of(2026, 8)))
        assertEquals(Insight.SpentSoFar(80.0), spendingInsight(all, LocalDate.of(2026, 8, 20)))
    }

    @Test fun `owner split treats blank owner as mine`() {
        val (mine, theirs) = splitByOwner(listOf(tx(1.0, "2026-10-01", by = ""), tx(2.0, "2026-10-01", by = "user2"), tx(3.0, "2026-10-01", by = "user1")), "user1")
        assertEquals(2, mine.size)
        assertEquals(1, theirs.size)
    }

    @Test fun `goal progress counts only my spending this month`() {
        val p = goalProgress(listOf(tx(250.0, "2026-10-05"), tx(999.0, "2026-09-05")), 1000.0, today)
        assertEquals(25, p.percent)
        assertEquals(750.0, p.remaining, 0.0)
        assertEquals(3, p.daysLeft)
    }

    @Test fun `rupees use Indian grouping`() {
        assertEquals("₹1,23,456", formatRupees(123456.0))
        assertEquals("₹12,34,56,789", formatRupees(123456789.0))
        assertEquals("₹999", formatRupees(999.0))
        assertEquals("₹0", formatRupees(0.0))
    }

    @Test fun `rupees show paise only when there are some`() {
        assertEquals("₹1,250.5", formatRupees(1250.5))
        assertEquals("₹12.75", formatRupees(12.75))
        assertEquals("-₹500", formatRupees(-500.0))
    }
}
