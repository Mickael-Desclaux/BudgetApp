package fr.mickael.budget.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.YearMonth

data class Settings(
    val savingsTargetCents: Long = 0,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val savings = longPreferencesKey("savings_target_cents")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val lastMaterialized = stringPreferencesKey("last_materialized_month")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): Settings = settings.first()

    private fun Preferences.toSettings() = Settings(
        savingsTargetCents = this[Keys.savings] ?: 0,
        reminderEnabled = this[Keys.reminderEnabled] ?: true,
        reminderHour = this[Keys.reminderHour] ?: 21,
        reminderMinute = this[Keys.reminderMinute] ?: 0,
    )

    suspend fun setSavingsTarget(cents: Long) {
        context.dataStore.edit { it[Keys.savings] = cents }
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.reminderEnabled] = enabled
            it[Keys.reminderHour] = hour
            it[Keys.reminderMinute] = minute
        }
    }

    suspend fun lastMaterializedMonth(): YearMonth? =
        context.dataStore.data.first()[Keys.lastMaterialized]?.let(YearMonth::parse)

    suspend fun setLastMaterializedMonth(month: YearMonth) {
        context.dataStore.edit { it[Keys.lastMaterialized] = month.toString() }
    }
}
