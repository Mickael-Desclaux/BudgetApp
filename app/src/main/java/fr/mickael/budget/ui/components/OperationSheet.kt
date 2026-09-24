package fr.mickael.budget.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import fr.mickael.budget.data.Category
import fr.mickael.budget.data.Operation
import fr.mickael.budget.data.TxType
import fr.mickael.budget.domain.Money
import java.time.LocalDate

/** Demande d'ouverture de la feuille d'ajout/édition. */
data class OperationRequest(val type: TxType = TxType.EXPENSE, val existing: Operation? = null)

@Composable
fun OperationSheet(
    request: OperationRequest,
    categories: List<Category>,
    onSave: (Operation) -> Unit,
    onDelete: (Operation) -> Unit,
    onDismiss: () -> Unit,
) {
    val existing = request.existing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by rememberSaveable { mutableStateOf(existing?.type ?: request.type) }
    var quick by rememberSaveable { mutableStateOf(existing?.isQuickTotal ?: false) }
    var amount by rememberSaveable { mutableStateOf(existing?.let { Money.toInput(it.amountCents) } ?: "") }
    var label by rememberSaveable { mutableStateOf(existing?.label ?: "") }
    var categoryId by rememberSaveable { mutableStateOf(existing?.categoryId) }
    var dateEpoch by rememberSaveable { mutableStateOf((existing?.date ?: LocalDate.now()).toEpochDay()) }
    val focus = remember { FocusRequester() }
    val parsed = Money.parse(amount)
    val isQuick = quick && type == TxType.EXPENSE

    val save = {
        if (parsed != null && parsed > 0) {
            onSave(
                (existing ?: Operation(date = LocalDate.now(), amountCents = 0, type = type)).copy(
                    date = LocalDate.ofEpochDay(dateEpoch),
                    amountCents = parsed,
                    type = type,
                    label = if (isQuick) "" else label.trim(),
                    categoryId = if (isQuick) null else categoryId,
                    isQuickTotal = isQuick,
                ),
            )
        }
    }

    LaunchedEffect(Unit) {
        if (existing == null) {
            // Attend que la feuille soit affichée avant de donner le focus (ouvre le clavier).
            kotlinx.coroutines.delay(350)
            runCatching { focus.requestFocus() }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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
                if (existing == null) "Nouvelle opération" else "Modifier l'opération",
                style = MaterialTheme.typography.titleLarge,
            )
            if (existing?.recurringId == null) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf(
                        Triple("Dépense", TxType.EXPENSE, false),
                        Triple("Total du jour", TxType.EXPENSE, true),
                        Triple("Revenu", TxType.INCOME, false),
                    )
                    options.forEachIndexed { i, (text, t, q) ->
                        SegmentedButton(
                            selected = type == t && isQuick == q,
                            onClick = {
                                if (type != t) categoryId = null
                                type = t; quick = q
                            },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) { Text(text, maxLines = 1) }
                    }
                }
            }
            if (isQuick) {
                Text(
                    "Pas le temps de tout détailler ? Saisis juste le total dépensé aujourd'hui.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AmountField(amount, { amount = it }, Modifier.fillMaxWidth(), focusRequester = focus, onDone = save)
            if (!isQuick) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Libellé (optionnel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Catégorie", style = MaterialTheme.typography.labelLarge)
                CategoryChips(categories.filter { it.type == type }, categoryId, { categoryId = it })
            }
            DateChip(LocalDate.ofEpochDay(dateEpoch)) { dateEpoch = it.toEpochDay() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing) }) {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    }
                }
                Row(Modifier.weight(1f)) {}
                Button(
                    enabled = parsed != null && parsed > 0,
                    onClick = save,
                ) { Text("Enregistrer") }
            }
        }
    }
}
