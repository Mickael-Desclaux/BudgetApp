package fr.mickael.budget.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Category::class, Recurring::class, Operation::class, MonthBudget::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringDao(): RecurringDao
    abstract fun operationDao(): OperationDao
    abstract fun monthBudgetDao(): MonthBudgetDao

    companion object {
        fun build(context: Context): BudgetDatabase =
            Room.databaseBuilder(context, BudgetDatabase::class.java, "budget.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        DefaultCategories.all.forEach { c ->
                            db.execSQL(
                                "INSERT INTO categories (name, emoji, color, type) VALUES (?, ?, ?, ?)",
                                arrayOf<Any>(c.name, c.emoji, c.color, c.type.name),
                            )
                        }
                    }
                })
                .build()
    }
}

object DefaultCategories {
    val all = listOf(
        Category(name = "Courses", emoji = "🛒", color = 0xFF4CAF50, type = TxType.EXPENSE),
        Category(name = "Restaurants", emoji = "🍽️", color = 0xFFFF9800, type = TxType.EXPENSE),
        Category(name = "Transport", emoji = "🚗", color = 0xFF2196F3, type = TxType.EXPENSE),
        Category(name = "Logement", emoji = "🏠", color = 0xFF795548, type = TxType.EXPENSE),
        Category(name = "Factures", emoji = "💡", color = 0xFFFFC107, type = TxType.EXPENSE),
        Category(name = "Loisirs", emoji = "🎉", color = 0xFFE91E63, type = TxType.EXPENSE),
        Category(name = "Santé", emoji = "💊", color = 0xFF00BCD4, type = TxType.EXPENSE),
        Category(name = "Abonnements", emoji = "📺", color = 0xFF9C27B0, type = TxType.EXPENSE),
        Category(name = "Shopping", emoji = "🛍️", color = 0xFFFF5722, type = TxType.EXPENSE),
        Category(name = "Divers", emoji = "📦", color = 0xFF607D8B, type = TxType.EXPENSE),
        Category(name = "Salaire", emoji = "💼", color = 0xFF2E7D32, type = TxType.INCOME),
        Category(name = "Allocations", emoji = "🏛️", color = 0xFF1565C0, type = TxType.INCOME),
        Category(name = "Virement", emoji = "🔁", color = 0xFF6A1B9A, type = TxType.INCOME),
        Category(name = "Autre revenu", emoji = "💰", color = 0xFFF9A825, type = TxType.INCOME),
    )
}
