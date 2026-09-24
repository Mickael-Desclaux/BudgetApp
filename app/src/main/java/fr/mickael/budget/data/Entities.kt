package fr.mickael.budget.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate

enum class TxType { EXPENSE, INCOME }

enum class Frequency(val label: String) {
    WEEKLY("Hebdomadaire"),
    MONTHLY("Mensuel"),
    YEARLY("Annuel"),
}

@Serializable
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: Long,
    val type: TxType,
)

@Serializable
@Entity(tableName = "recurrings")
data class Recurring(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val amountCents: Long,
    val type: TxType,
    val frequency: Frequency,
    val categoryId: Long? = null,
    /** 1 = lundi … 7 = dimanche (hebdomadaire). */
    val dayOfWeek: Int? = null,
    /** 1..31, ramené au dernier jour du mois si besoin (mensuel/annuel). */
    val dayOfMonth: Int? = null,
    /** 1..12 (annuel). */
    val monthOfYear: Int? = null,
    @Serializable(with = LocalDateSerializer::class) val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val endDate: LocalDate? = null,
    val active: Boolean = true,
)

@Serializable
@Entity(
    tableName = "operations",
    indices = [Index(value = ["recurringId", "date"], unique = true), Index("date")],
)
data class Operation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val amountCents: Long,
    val type: TxType,
    val label: String = "",
    /** null = non catégorisé (saisie rapide). */
    val categoryId: Long? = null,
    /** Renseigné si l'opération a été générée par un récurrent. */
    val recurringId: Long? = null,
    val isQuickTotal: Boolean = false,
)

/** Objectif d'épargne figé pour un mois donné ("2026-09"). */
@Serializable
@Entity(tableName = "month_budgets")
data class MonthBudget(
    @PrimaryKey val yearMonth: String,
    val savingsTargetCents: Long,
)
