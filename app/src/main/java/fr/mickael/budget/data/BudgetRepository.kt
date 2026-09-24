package fr.mickael.budget.data

import android.content.Context
import fr.mickael.budget.domain.BudgetCalculator
import fr.mickael.budget.domain.MonthSummary
import fr.mickael.budget.domain.RecurrenceEngine
import fr.mickael.budget.widget.BudgetWidget
import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.YearMonth

@Serializable
data class Backup(
    val version: Int = 1,
    val savingsTargetCents: Long,
    val categories: List<Category>,
    val recurrings: List<Recurring>,
    val operations: List<Operation>,
    val monthBudgets: List<MonthBudget>,
)

class BudgetRepository(
    private val context: Context,
    private val db: BudgetDatabase,
    val settings: SettingsStore,
) {
    private val categoryDao = db.categoryDao()
    private val recurringDao = db.recurringDao()
    private val operationDao = db.operationDao()
    private val monthBudgetDao = db.monthBudgetDao()

    val categories: Flow<List<Category>> = categoryDao.observeAll()
    val recurrings: Flow<List<Recurring>> = recurringDao.observeAll()
    val allOperations: Flow<List<Operation>> = operationDao.observeAll()

    fun operationsIn(month: YearMonth): Flow<List<Operation>> =
        operationDao.observeBetween(month.atDay(1), month.atEndOfMonth())

    fun summaryOf(month: YearMonth): Flow<MonthSummary> =
        combine(operationsIn(month), monthBudgetDao.observeAll(), settings.settings) { ops, budgets, s ->
            val savings = budgets.firstOrNull { it.yearMonth == month.toString() }?.savingsTargetCents
                ?: s.savingsTargetCents
            BudgetCalculator.summarize(month, ops, savings)
        }

    /** Tous les mois ayant des opérations, du plus récent au plus ancien. */
    val history: Flow<List<MonthSummary>> =
        combine(allOperations, monthBudgetDao.observeAll(), settings.settings) { ops, budgets, s ->
            val savingsByMonth = budgets.associate { it.yearMonth to it.savingsTargetCents }
            ops.groupBy { YearMonth.from(it.date) }
                .toSortedMap(compareByDescending { it })
                .map { (m, list) ->
                    BudgetCalculator.summarize(m, list, savingsByMonth[m.toString()] ?: s.savingsTargetCents)
                }
        }

    suspend fun currentSummary(): MonthSummary {
        val month = YearMonth.now()
        val ops = operationDao.getBetween(month.atDay(1), month.atEndOfMonth())
        val savings = monthBudgetDao.get(month.toString())?.savingsTargetCents
            ?: settings.current().savingsTargetCents
        return BudgetCalculator.summarize(month, ops, savings)
    }

    // --- Opérations ---

    suspend fun saveOperation(op: Operation) {
        operationDao.upsert(op)
        refreshWidget()
    }

    suspend fun deleteOperation(op: Operation) {
        operationDao.delete(op)
        refreshWidget()
    }

    // --- Catégories ---

    suspend fun saveCategory(c: Category) = categoryDao.upsert(c)

    suspend fun deleteCategory(c: Category) = db.withTransaction {
        categoryDao.detachOperations(c.id)
        categoryDao.detachRecurrings(c.id)
        categoryDao.delete(c)
    }

    // --- Récurrents ---

    /**
     * Enregistre le récurrent puis régénère ses occurrences du mois courant
     * (les mois passés ne sont pas touchés).
     */
    suspend fun saveRecurring(r: Recurring) {
        db.withTransaction {
            val id = recurringDao.upsert(r).let { if (it == -1L) r.id else it }
            val saved = r.copy(id = id)
            val month = YearMonth.now()
            operationDao.deleteForRecurring(id, month.atDay(1), month.atEndOfMonth())
            operationDao.insertIgnore(RecurrenceEngine.occurrencesIn(saved, month).map { saved.toOperation(it) })
        }
        refreshWidget()
    }

    /** Supprime le récurrent et ses occurrences futures ; ce qui est déjà passé reste dans l'historique. */
    suspend fun deleteRecurring(r: Recurring) {
        db.withTransaction {
            operationDao.deleteForRecurring(r.id, LocalDate.now().plusDays(1), LocalDate.MAX)
            recurringDao.delete(r)
        }
        refreshWidget()
    }

    /**
     * Génère les opérations des récurrents pour chaque mois pas encore traité jusqu'au mois courant,
     * et fige l'objectif d'épargne de ces mois. Chaque mois n'est généré qu'une fois, pour que
     * les suppressions/modifications manuelles d'une occurrence soient conservées.
     */
    suspend fun materializeRecurrings() {
        val current = YearMonth.now()
        val last = settings.lastMaterializedMonth()
        if (last != null && !last.isBefore(current)) return
        val savings = settings.current().savingsTargetCents
        val recurrings = recurringDao.getActive()
        var month = last?.plusMonths(1) ?: current
        db.withTransaction {
            while (!month.isAfter(current)) {
                val m = month
                operationDao.insertIgnore(recurrings.flatMap { r ->
                    RecurrenceEngine.occurrencesIn(r, m).map { r.toOperation(it) }
                })
                monthBudgetDao.insertIgnore(MonthBudget(m.toString(), savings))
                month = month.plusMonths(1)
            }
        }
        settings.setLastMaterializedMonth(current)
        refreshWidget()
    }

    // --- Épargne ---

    /** Change l'objectif par défaut et celui du mois courant. */
    suspend fun setSavingsTarget(cents: Long) {
        settings.setSavingsTarget(cents)
        monthBudgetDao.upsert(MonthBudget(YearMonth.now().toString(), cents))
        refreshWidget()
    }

    // --- Sauvegarde ---

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun exportJson(): String = json.encodeToString(
        Backup.serializer(),
        Backup(
            savingsTargetCents = settings.current().savingsTargetCents,
            categories = categoryDao.getAll(),
            recurrings = recurringDao.getAll(),
            operations = operationDao.getAll(),
            monthBudgets = monthBudgetDao.getAll(),
        ),
    )

    suspend fun importJson(text: String) {
        val backup = json.decodeFromString(Backup.serializer(), text)
        db.withTransaction {
            operationDao.deleteAll()
            recurringDao.deleteAll()
            categoryDao.deleteAll()
            monthBudgetDao.deleteAll()
            categoryDao.insertAll(backup.categories)
            recurringDao.insertAll(backup.recurrings)
            operationDao.insertAll(backup.operations)
            monthBudgetDao.insertAll(backup.monthBudgets)
        }
        settings.setSavingsTarget(backup.savingsTargetCents)
        settings.setLastMaterializedMonth(YearMonth.now())
        refreshWidget()
    }

    suspend fun refreshWidget() {
        runCatching { BudgetWidget().updateAll(context) }
    }
}

fun Recurring.toOperation(date: LocalDate) = Operation(
    date = date,
    amountCents = amountCents,
    type = type,
    label = label,
    categoryId = categoryId,
    recurringId = id,
)
