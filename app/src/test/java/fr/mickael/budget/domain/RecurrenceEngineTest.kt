package fr.mickael.budget.domain

import fr.mickael.budget.data.Frequency
import fr.mickael.budget.data.Recurring
import fr.mickael.budget.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RecurrenceEngineTest {
    private fun rec(frequency: Frequency, dow: Int? = null, dom: Int? = null, moy: Int? = null, start: LocalDate = LocalDate.of(2020, 1, 1), end: LocalDate? = null) =
        Recurring(label = "t", amountCents = 100, type = TxType.EXPENSE, frequency = frequency, dayOfWeek = dow, dayOfMonth = dom, monthOfYear = moy, startDate = start, endDate = end)

    @Test
    fun weeklyHasFourOrFiveOccurrences() {
        // Septembre 2026 : commence un mardi, 30 jours → 5 mardis, 4 lundis.
        assertEquals(5, RecurrenceEngine.occurrencesIn(rec(Frequency.WEEKLY, dow = 2), YearMonth.of(2026, 9)).size)
        assertEquals(4, RecurrenceEngine.occurrencesIn(rec(Frequency.WEEKLY, dow = 1), YearMonth.of(2026, 9)).size)
        assertEquals(LocalDate.of(2026, 9, 1), RecurrenceEngine.occurrencesIn(rec(Frequency.WEEKLY, dow = 2), YearMonth.of(2026, 9)).first())
    }

    @Test
    fun monthlyDayIsClampedToEndOfMonth() {
        assertEquals(listOf(LocalDate.of(2026, 2, 28)), RecurrenceEngine.occurrencesIn(rec(Frequency.MONTHLY, dom = 31), YearMonth.of(2026, 2)))
        assertEquals(listOf(LocalDate.of(2026, 9, 30)), RecurrenceEngine.occurrencesIn(rec(Frequency.MONTHLY, dom = 31), YearMonth.of(2026, 9)))
        assertEquals(listOf(LocalDate.of(2026, 9, 5)), RecurrenceEngine.occurrencesIn(rec(Frequency.MONTHLY, dom = 5), YearMonth.of(2026, 9)))
    }

    @Test
    fun yearlyOnlyInItsMonth() {
        val r = rec(Frequency.YEARLY, dom = 15, moy = 3)
        assertEquals(listOf(LocalDate.of(2027, 3, 15)), RecurrenceEngine.occurrencesIn(r, YearMonth.of(2027, 3)))
        assertEquals(emptyList<LocalDate>(), RecurrenceEngine.occurrencesIn(r, YearMonth.of(2027, 4)))
    }

    @Test
    fun respectsStartEndAndActive() {
        val month = YearMonth.of(2026, 9)
        assertEquals(emptyList<LocalDate>(), RecurrenceEngine.occurrencesIn(rec(Frequency.MONTHLY, dom = 5, start = LocalDate.of(2026, 9, 10)), month))
        assertEquals(emptyList<LocalDate>(), RecurrenceEngine.occurrencesIn(rec(Frequency.MONTHLY, dom = 5, end = LocalDate.of(2026, 9, 4)), month))
        assertEquals(emptyList<LocalDate>(), RecurrenceEngine.occurrencesIn(rec(Frequency.MONTHLY, dom = 5).copy(active = false), month))
        assertEquals(2, RecurrenceEngine.occurrencesIn(rec(Frequency.WEEKLY, dow = 2, start = LocalDate.of(2026, 9, 10), end = LocalDate.of(2026, 9, 23)), month).size)
    }
}
