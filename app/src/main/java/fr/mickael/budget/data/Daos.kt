package fr.mickael.budget.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type, name")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<Category>

    @Upsert
    suspend fun upsert(category: Category): Long

    @Insert
    suspend fun insertAll(categories: List<Category>)

    @Delete
    suspend fun delete(category: Category)

    @Query("UPDATE operations SET categoryId = NULL WHERE categoryId = :id")
    suspend fun detachOperations(id: Long)

    @Query("UPDATE recurrings SET categoryId = NULL WHERE categoryId = :id")
    suspend fun detachRecurrings(id: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurrings ORDER BY type, label")
    fun observeAll(): Flow<List<Recurring>>

    @Query("SELECT * FROM recurrings WHERE active = 1")
    suspend fun getActive(): List<Recurring>

    @Query("SELECT * FROM recurrings")
    suspend fun getAll(): List<Recurring>

    @Upsert
    suspend fun upsert(recurring: Recurring): Long

    @Delete
    suspend fun delete(recurring: Recurring)

    @Insert
    suspend fun insertAll(items: List<Recurring>)

    @Query("DELETE FROM recurrings")
    suspend fun deleteAll()
}

@Dao
interface OperationDao {
    @Query("SELECT * FROM operations WHERE date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Operation>>

    @Query("SELECT * FROM operations WHERE date BETWEEN :start AND :end")
    suspend fun getBetween(start: LocalDate, end: LocalDate): List<Operation>

    @Query("SELECT * FROM operations ORDER BY date DESC")
    fun observeAll(): Flow<List<Operation>>

    @Query("SELECT * FROM operations")
    suspend fun getAll(): List<Operation>

    @Upsert
    suspend fun upsert(operation: Operation): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(operations: List<Operation>)

    @Insert
    suspend fun insertAll(operations: List<Operation>)

    @Delete
    suspend fun delete(operation: Operation)

    @Query("DELETE FROM operations WHERE recurringId = :recurringId AND date BETWEEN :start AND :end")
    suspend fun deleteForRecurring(recurringId: Long, start: LocalDate, end: LocalDate)

    @Query("DELETE FROM operations")
    suspend fun deleteAll()
}

@Dao
interface MonthBudgetDao {
    @Query("SELECT * FROM month_budgets")
    fun observeAll(): Flow<List<MonthBudget>>

    @Query("SELECT * FROM month_budgets")
    suspend fun getAll(): List<MonthBudget>

    @Query("SELECT * FROM month_budgets WHERE yearMonth = :yearMonth")
    suspend fun get(yearMonth: String): MonthBudget?

    @Upsert
    suspend fun upsert(budget: MonthBudget)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(budget: MonthBudget)

    @Insert
    suspend fun insertAll(items: List<MonthBudget>)

    @Query("DELETE FROM month_budgets")
    suspend fun deleteAll()
}
