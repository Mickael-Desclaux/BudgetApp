package fr.mickael.budget.ui.operations

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.mickael.budget.data.Operation
import fr.mickael.budget.data.TxType
import fr.mickael.budget.domain.Money
import fr.mickael.budget.ui.MainViewModel
import fr.mickael.budget.ui.components.EmptyState
import fr.mickael.budget.ui.components.GroupedOperations
import fr.mickael.budget.ui.components.MonthSelector
import fr.mickael.budget.ui.components.StatTile
import fr.mickael.budget.ui.theme.MoneyColors
import java.time.YearMonth

/** Filtre : null = tout, -1 = non catégorisé, -2 = revenus, sinon id de catégorie. */
private const val UNCATEGORIZED = -1L
private const val INCOMES = -2L

@Composable
fun OperationsScreen(vm: MainViewModel, contentPadding: PaddingValues, onEdit: (Operation) -> Unit) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)
    var filter by rememberSaveable { mutableStateOf<Long?>(null) }
    val operations by remember(month) { vm.repository.operationsIn(month) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by vm.categories.collectAsStateWithLifecycle()
    val byId by vm.categoriesById.collectAsStateWithLifecycle()

    val filtered = operations.filter { op ->
        when (filter) {
            null -> true
            UNCATEGORIZED -> op.type == TxType.EXPENSE && op.categoryId == null
            INCOMES -> op.type == TxType.INCOME
            else -> op.categoryId == filter
        }
    }
    val usedExpenseCategories = categories.filter { c -> c.type == TxType.EXPENSE && operations.any { it.categoryId == c.id } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
    ) {
        MonthSelector(month, { monthText = it.toString(); filter = null })
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(filter == null, { filter = null }, { Text("Tout") })
            FilterChip(filter == INCOMES, { filter = INCOMES }, { Text("Revenus") })
            FilterChip(filter == UNCATEGORIZED, { filter = UNCATEGORIZED }, { Text("Non catégorisé") })
            usedExpenseCategories.forEach { c ->
                FilterChip(filter == c.id, { filter = c.id }, { Text("${c.emoji} ${c.name}") })
            }
        }
        Spacer(Modifier.height(8.dp))
        val income = filtered.filter { it.type == TxType.INCOME }.sumOf { it.amountCents }
        val expense = filtered.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Entrées", Money.format(income), Modifier.weight(1f), MoneyColors.positive)
            StatTile("Sorties", Money.format(expense), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            EmptyState("Aucune opération")
        } else {
            GroupedOperations(filtered, byId, onEdit)
        }
        Spacer(Modifier.height(88.dp))
    }
}
