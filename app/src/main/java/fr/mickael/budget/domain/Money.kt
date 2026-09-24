package fr.mickael.budget.domain

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Money {
    private val locale: Locale = Locale.FRANCE

    fun format(cents: Long, withSign: Boolean = false): String {
        val nf = NumberFormat.getCurrencyInstance(locale).apply { currency = Currency.getInstance("EUR") }
        val text = nf.format(abs(cents) / 100.0)
        return when {
            cents < 0 -> "−$text"
            withSign && cents > 0 -> "+$text"
            else -> text
        }
    }

    /** Montant sans symbole, pour préremplir un champ ("12,50"). */
    fun toInput(cents: Long): String {
        if (cents == 0L) return ""
        val euros = cents / 100
        val rest = abs(cents % 100)
        return if (rest == 0L) euros.toString() else "%d,%02d".format(euros, rest)
    }

    /** "12", "12,5", "12.50", "1 234,56" → centimes ; null si invalide. */
    fun parse(input: String): Long? {
        val cleaned = input.trim().replace(" ", "").replace(" ", "").replace("€", "").replace(',', '.')
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return (value * 100).roundToLong()
    }
}
