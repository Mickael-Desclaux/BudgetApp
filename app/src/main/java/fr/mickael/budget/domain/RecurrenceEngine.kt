package fr.mickael.budget.domain

import fr.mickael.budget.data.Frequency
import fr.mickael.budget.data.Recurring
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

object RecurrenceEngine {

    /** Dates auxquelles [recurring] tombe dans [month], en respectant début/fin. */
    fun occurrencesIn(recurring: Recurring, month: YearMonth): List<LocalDate> {
        if (!recurring.active) return emptyList()
        val raw = when (recurring.frequency) {
            Frequency.WEEKLY -> {
                val dow = DayOfWeek.of((recurring.dayOfWeek ?: 1).coerceIn(1, 7))
                var d = month.atDay(1).with(TemporalAdjusters.nextOrSame(dow))
                buildList {
                    while (YearMonth.from(d) == month) {
                        add(d); d = d.plusWeeks(1)
                    }
                }
            }
            Frequency.MONTHLY -> listOf(month.clampedDay(recurring.dayOfMonth ?: 1))
            Frequency.YEARLY ->
                if ((recurring.monthOfYear ?: 1) == month.monthValue) listOf(month.clampedDay(recurring.dayOfMonth ?: 1))
                else emptyList()
        }
        return raw.filter { d ->
            !d.isBefore(recurring.startDate) && (recurring.endDate == null || !d.isAfter(recurring.endDate))
        }
    }

    private fun YearMonth.clampedDay(day: Int): LocalDate = atDay(day.coerceIn(1, lengthOfMonth()))
}
