package fr.mickael.budget

import android.app.Application
import fr.mickael.budget.data.BudgetDatabase
import fr.mickael.budget.data.BudgetRepository
import fr.mickael.budget.data.SettingsStore
import fr.mickael.budget.notification.ReminderScheduler

class BudgetApp : Application() {
    lateinit var repository: BudgetRepository
        private set
    lateinit var reminders: ReminderScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        repository = BudgetRepository(this, BudgetDatabase.build(this), SettingsStore(this))
        reminders = ReminderScheduler(this)
        reminders.createChannel()
    }
}

val android.content.Context.app: BudgetApp get() = applicationContext as BudgetApp
