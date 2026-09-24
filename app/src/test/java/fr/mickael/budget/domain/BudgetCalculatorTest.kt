package fr.mickael.budget.domain

import fr.mickael.budget.data.Operation
import fr.mickael.budget.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetCalculatorTest {
    private val month = YearMonth.of(2026, 9)
    private fun op(day: Int, cents: Long, type: TxType, recurring: Boolean = false) =
        Operation(date = month.atDay(day), amountCents = cents, type = type, recurringId = if (recurring) day.toLong() else null)

    private val ops = listOf(
        op(1, 250_000, TxType.INCOME, recurring = true), // salaire
        op(5, 10_000, TxType.INCOME), // virement ponctuel
        op(3, 80_000, TxType.EXPENSE, recurring = true), // loyer
        op(10, 1_500, TxType.EXPENSE, recurring = true), // abonnement
        op(12, 4_000, TxType.EXPENSE), // courses
        op(13, 6_000, TxType.EXPENSE), // total du jour
        Operation(date = LocalDate.of(2026, 8, 31), amountCents = 99_999, type = TxType.EXPENSE), // autre mois
    )

    @Test
    fun computesSpendableAndRemaining() {
        val s = BudgetCalculator.summarize(month, ops, savingsTargetCents = 30_000)
        assertEquals(260_000, s.incomeCents)
        assertEquals(81_500, s.plannedExpensesCents)
        assertEquals(10_000, s.variableExpensesCents)
        assertEquals(260_000 - 81_500 - 30_000, s.spendableCents)
        assertEquals(148_500 - 10_000, s.remainingCents)
    }

    @Test
    fun balanceAndGoalGap() {
        val s = BudgetCalculator.summarize(month, ops, savingsTargetCents = 30_000)
        assertEquals(260_000 - 91_500, s.balanceCents)
        assertEquals(168_500 - 30_000, s.vsGoalCents)
    }

    @Test
    fun remainingPerDay() {
        val s = BudgetCalculator.summarize(month, ops, savingsTargetCents = 30_000)
        // Du 21 au 30 inclus = 10 jours.
        assertEquals(138_500L / 10, s.remainingPerDayCents(LocalDate.of(2026, 9, 21)))
        assertNull(s.remainingPerDayCents(LocalDate.of(2026, 10, 1)))
    }

    @Test
    fun moneyParsingAndFormatting() {
        assertEquals(1250L, Money.parse("12,5"))
        assertEquals(123456L, Money.parse("1 234,56"))
        assertNull(Money.parse("abc"))
        assertEquals("12,50", Money.toInput(1250))
        assertEquals("12", Money.toInput(1200))
    }
}
