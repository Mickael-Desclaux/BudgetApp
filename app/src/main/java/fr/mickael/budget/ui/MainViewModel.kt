package fr.mickael.budget.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mickael.budget.app
import fr.mickael.budget.data.Category
import fr.mickael.budget.data.Operation
import fr.mickael.budget.data.Recurring
import fr.mickael.budget.data.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = application.app.repository
    private val reminders = application.app.reminders

    val categories: StateFlow<List<Category>> =
        repository.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categoriesById: StateFlow<Map<Long, Category>> =
        repository.categories.map { list -> list.associateBy { it.id } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val settings: StateFlow<Settings> =
        repository.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    fun saveOperation(op: Operation) = viewModelScope.launch { repository.saveOperation(op) }
    fun deleteOperation(op: Operation) = viewModelScope.launch { repository.deleteOperation(op) }
    fun saveRecurring(r: Recurring) = viewModelScope.launch { repository.saveRecurring(r) }
    fun deleteRecurring(r: Recurring) = viewModelScope.launch { repository.deleteRecurring(r) }
    fun saveCategory(c: Category) = viewModelScope.launch { repository.saveCategory(c) }
    fun deleteCategory(c: Category) = viewModelScope.launch { repository.deleteCategory(c) }
    fun setSavingsTarget(cents: Long) = viewModelScope.launch { repository.setSavingsTarget(cents) }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) = viewModelScope.launch {
        repository.settings.setReminder(enabled, hour, minute)
        reminders.apply(repository.settings.current())
    }

    fun onAppStart() = viewModelScope.launch {
        repository.materializeRecurrings()
        reminders.apply(repository.settings.current())
    }
}
