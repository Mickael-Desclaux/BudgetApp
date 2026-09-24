package fr.mickael.budget.ui.components

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRANCE)
private val dayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE)
private val shortDayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE)

fun YearMonth.label(): String = format(monthFormatter).replaceFirstChar { it.uppercase() }

fun LocalDate.dayLabel(): String {
    val today = LocalDate.now()
    return when (this) {
        today -> "Aujourd'hui"
        today.minusDays(1) -> "Hier"
        today.plusDays(1) -> "Demain"
        else -> format(dayFormatter).replaceFirstChar { it.uppercase() }
    }
}

fun LocalDate.shortLabel(): String = format(shortDayFormatter)

val weekDays = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
val monthNames = listOf(
    "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre",
)
