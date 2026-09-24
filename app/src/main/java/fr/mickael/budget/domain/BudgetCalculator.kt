package fr.mickael.budget.domain

import fr.mickael.budget.data.Operation
import fr.mickael.budget.data.TxType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class MonthSummary(
    val month: YearMonth,
    /** Tous les revenus du mois (récurrents + ponctuels). */
    val incomeCents: Long,
    /** Dépenses issues des récurrents (prévues). */
    val plannedExpensesCents: Long,
    /** Dépenses ponctuelles saisies (quotidiennes). */
    val variableExpensesCents: Long,
    val savingsTargetCents: Long,
) {
    /** Somme qu'on peut dépenser dans le mois. */
    val spendableCents get() = incomeCents - plannedExpensesCents - savingsTargetCents

    /** Ce qu'il reste à dépenser. */
    val remainingCents get() = spendableCents - variableExpensesCents

    val totalExpensesCents get() = plannedExpensesCents + variableExpensesCents

    /** Solde réel du mois (+/−). */
    val balanceCents get() = incomeCents - totalExpensesCents

    /** Écart par rapport à l'objectif d'épargne (≥ 0 = objectif tenu). */
    val vsGoalCents get() = balanceCents - savingsTargetCents

    /** Part déjà consommée du budget dépensable, entre 0 et 1+. */
    val usedRatio: Float
        get() = if (spendableCents <= 0) (if (variableExpensesCents > 0) 1f else 0f)
        else variableExpensesCents.toFloat() / spendableCents

    /** Reste à dépenser par jour restant (aujourd'hui inclus). */
    fun remainingPerDayCents(today: LocalDate): Long? {
        if (YearMonth.from(today) != month) return null
        val days = ChronoUnit.DAYS.between(today, month.atEndOfMonth()) + 1
        return remainingCents / days
    }
}

object BudgetCalculator {
    fun summarize(month: YearMonth, operations: List<Operation>, savingsTargetCents: Long): MonthSummary {
        val inMonth = operations.filter { YearMonth.from(it.date) == month }
        return MonthSummary(
            month = month,
            incomeCents = inMonth.filter { it.type == TxType.INCOME }.sumOf { it.amountCents },
            plannedExpensesCents = inMonth.filter { it.type == TxType.EXPENSE && it.recurringId != null }.sumOf { it.amountCents },
            variableExpensesCents = inMonth.filter { it.type == TxType.EXPENSE && it.recurringId == null }.sumOf { it.amountCents },
            savingsTargetCents = savingsTargetCents,
        )
    }
}
