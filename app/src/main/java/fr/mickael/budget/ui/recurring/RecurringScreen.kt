package fr.mickael.budget.ui.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.mickael.budget.data.Category
import fr.mickael.budget.data.Frequency
import fr.mickael.budget.data.Recurring
import fr.mickael.budget.data.TxType
import fr.mickael.budget.domain.Money
import fr.mickael.budget.ui.MainViewModel
import fr.mickael.budget.ui.components.AmountField
import fr.mickael.budget.ui.components.CategoryBadge
import fr.mickael.budget.ui.components.CategoryChips
import fr.mickael.budget.ui.components.EmptyState
import fr.mickael.budget.ui.components.SectionCard
import fr.mickael.budget.ui.components.monthNames
import fr.mickael.budget.ui.components.weekDays
import fr.mickael.budget.ui.theme.MoneyColors
import java.time.LocalDate

@Composable
fun RecurringScreen(vm: MainViewModel, contentPadding: PaddingValues) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var editRequest by remember { mutableStateOf<RecurringEdit?>(null) }
    val onEditRequest: (RecurringEdit?) -> Unit = { editRequest = it }
    val type = if (tab == 0) TxType.EXPENSE else TxType.INCOME
    val all by vm.repository.recurrings.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by vm.categories.collectAsStateWithLifecycle()
    val byId by vm.categoriesById.collectAsStateWithLifecycle()
    val items = all.filter { it.type == type }

    Box(Modifier.fillMaxSize().padding(contentPadding)) {
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Dépenses fixes") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Revenus") })
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val monthlyEstimate = items.filter { it.active }.sumOf { r ->
                when (r.frequency) {
                    Frequency.WEEKLY -> r.amountCents * 52 / 12
                    Frequency.MONTHLY -> r.amountCents
                    Frequency.YEARLY -> r.amountCents / 12
                }
            }
            Text(
                "≈ ${Money.format(monthlyEstimate)} par mois en moyenne",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SectionCard {
                if (items.isEmpty()) {
                    EmptyState(
                        if (type == TxType.EXPENSE) "Ajoute tes dépenses fixes : loyer, abonnements, assurances…"
                        else "Ajoute tes revenus réguliers : salaire, allocations…",
                    )
                }
                items.forEach { r ->
                    RecurringRow(r, r.categoryId?.let(byId::get)) { onEditRequest(RecurringEdit(r.type, r)) }
                }
            }
            Spacer(Modifier.height(88.dp))
        }
    }
        ExtendedFloatingActionButton(
            onClick = { onEditRequest(RecurringEdit(type)) },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text(if (type == TxType.EXPENSE) "Dépense fixe" else "Revenu") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    editRequest?.let { req ->
        RecurringSheet(
            request = req,
            categories = categories,
            onSave = { vm.saveRecurring(it); onEditRequest(null) },
            onDelete = { vm.deleteRecurring(it); onEditRequest(null) },
            onDismiss = { onEditRequest(null) },
        )
    }
}

data class RecurringEdit(val type: TxType, val existing: Recurring? = null)

fun Recurring.scheduleLabel(): String = when (frequency) {
    Frequency.WEEKLY -> "Chaque ${weekDays[(dayOfWeek ?: 1) - 1].lowercase()}"
    Frequency.MONTHLY -> "Le ${dayOfMonth ?: 1} de chaque mois"
    Frequency.YEARLY -> "Chaque année le ${dayOfMonth ?: 1} ${monthNames[(monthOfYear ?: 1) - 1].lowercase()}"
}

@Composable
private fun RecurringRow(r: Recurring, category: Category?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                r.scheduleLabel() + if (!r.active) " · en pause" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            Money.format(r.amountCents),
            fontWeight = FontWeight.SemiBold,
            color = when {
                !r.active -> MaterialTheme.colorScheme.onSurfaceVariant
                r.type == TxType.INCOME -> MoneyColors.positive
                else -> Color.Unspecified
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurringSheet(
    request: RecurringEdit,
    categories: List<Category>,
    onSave: (Recurring) -> Unit,
    onDelete: (Recurring) -> Unit,
    onDismiss: () -> Unit,
) {
    val existing = request.existing
    val today = LocalDate.now()
    var label by rememberSaveable { mutableStateOf(existing?.label ?: "") }
    var amount by rememberSaveable { mutableStateOf(existing?.let { Money.toInput(it.amountCents) } ?: "") }
    var categoryId by rememberSaveable { mutableStateOf(existing?.categoryId) }
    var frequency by rememberSaveable { mutableStateOf(existing?.frequency ?: Frequency.MONTHLY) }
    var dayOfWeek by rememberSaveable { mutableStateOf(existing?.dayOfWeek ?: today.dayOfWeek.value) }
    var dayOfMonth by rememberSaveable { mutableStateOf((existing?.dayOfMonth ?: today.dayOfMonth).toString()) }
    var monthOfYear by rememberSaveable { mutableStateOf(existing?.monthOfYear ?: today.monthValue) }
    var active by rememberSaveable { mutableStateOf(existing?.active ?: true) }
    val parsedAmount = Money.parse(amount)
    val parsedDay = dayOfMonth.toIntOrNull()?.takeIf { it in 1..31 }
    val valid = label.isNotBlank() && parsedAmount != null && parsedAmount > 0 &&
        (frequency == Frequency.WEEKLY || parsedDay != null)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                when {
                    existing != null -> "Modifier"
                    request.type == TxType.EXPENSE -> "Nouvelle dépense fixe"
                    else -> "Nouveau revenu régulier"
                },
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Libellé") },
                placeholder = { Text(if (request.type == TxType.EXPENSE) "Loyer, Netflix…" else "Salaire, CAF…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            AmountField(amount, { amount = it }, Modifier.fillMaxWidth())
            Text("Fréquence", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Frequency.entries.forEachIndexed { i, f ->
                    SegmentedButton(
                        selected = frequency == f,
                        onClick = { frequency = f },
                        shape = SegmentedButtonDefaults.itemShape(i, Frequency.entries.size),
                    ) { Text(f.label, maxLines = 1) }
                }
            }
            when (frequency) {
                Frequency.WEEKLY -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    weekDays.forEachIndexed { i, d ->
                        FilterChip(dayOfWeek == i + 1, { dayOfWeek = i + 1 }, { Text(d.take(3)) })
                    }
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = dayOfMonth,
                        onValueChange = { v -> dayOfMonth = v.filter(Char::isDigit).take(2) },
                        label = { Text("Jour") },
                        singleLine = true,
                        isError = parsedDay == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(90.dp),
                    )
                    if (frequency == Frequency.YEARLY) MonthDropdown(monthOfYear, { monthOfYear = it }, Modifier.weight(1f))
                }
            }
            Text("Catégorie", style = MaterialTheme.typography.labelLarge)
            CategoryChips(categories.filter { it.type == request.type }, categoryId, { categoryId = it })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Actif", modifier = Modifier.weight(1f))
                Switch(active, { active = it })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing) }) {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(enabled = valid, onClick = {
                    onSave(
                        Recurring(
                            id = existing?.id ?: 0,
                            label = label.trim(),
                            amountCents = parsedAmount ?: 0,
                            type = request.type,
                            frequency = frequency,
                            categoryId = categoryId,
                            dayOfWeek = if (frequency == Frequency.WEEKLY) dayOfWeek else null,
                            dayOfMonth = if (frequency != Frequency.WEEKLY) parsedDay else null,
                            monthOfYear = if (frequency == Frequency.YEARLY) monthOfYear else null,
                            startDate = existing?.startDate ?: today.withDayOfMonth(1),
                            endDate = existing?.endDate,
                            active = active,
                        ),
                    )
                }) { Text("Enregistrer") }
            }
            Text(
                "Les opérations du mois en cours sont recalculées ; les mois passés ne changent pas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthDropdown(month: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }, modifier) {
        OutlinedTextField(
            value = monthNames[month - 1],
            onValueChange = {},
            readOnly = true,
            label = { Text("Mois") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            monthNames.forEachIndexed { i, name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onChange(i + 1); expanded = false })
            }
        }
    }
}
