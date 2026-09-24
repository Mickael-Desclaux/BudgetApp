package fr.mickael.budget.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.mickael.budget.data.Category
import fr.mickael.budget.data.TxType
import fr.mickael.budget.domain.Money
import fr.mickael.budget.ui.MainViewModel
import fr.mickael.budget.ui.components.AmountField
import fr.mickael.budget.ui.components.CategoryBadge
import fr.mickael.budget.ui.components.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: MainViewModel, contentPadding: PaddingValues) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var savings by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(settings.savingsTargetCents) { savings = Money.toInput(settings.savingsTargetCents) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val text = vm.repository.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) }
                }
            }.isSuccess
            Toast.makeText(context, if (ok) "Sauvegarde exportée" else "Échec de l'export", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                }
                vm.repository.importJson(text)
            }
            Toast.makeText(
                context,
                if (result.isSuccess) "Données restaurées" else "Fichier invalide",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Réglages", style = MaterialTheme.typography.titleLarge)

        SectionCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Épargne mensuelle", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Somme mise de côté chaque mois, déduite du budget disponible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AmountField(savings, { savings = it }, Modifier.weight(1f), label = "Objectif")
                    val parsed = if (savings.isBlank()) 0L else Money.parse(savings)
                    Button(
                        enabled = parsed != null && parsed != settings.savingsTargetCents,
                        onClick = { parsed?.let(vm::setSavingsTarget); focusManager.clearFocus() },
                    ) { Text("OK") }
                }
            }
        }

        SectionCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Rappel quotidien", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Notification pour saisir les dépenses du jour",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(settings.reminderEnabled, { vm.setReminder(it, settings.reminderHour, settings.reminderMinute) })
                }
                OutlinedButton(onClick = { showTimePicker = true }, enabled = settings.reminderEnabled) {
                    Text("Heure : %02d:%02d".format(settings.reminderHour, settings.reminderMinute))
                }
            }
        }

        SectionCard {
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Catégories", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        editingCategory = Category(name = "", emoji = "🏷️", color = palette.first(), type = TxType.EXPENSE)
                    }) { Icon(Icons.Default.Add, null); Text("Ajouter") }
                }
                listOf(TxType.EXPENSE to "Dépenses", TxType.INCOME to "Revenus").forEach { (type, title) ->
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    categories.filter { it.type == type }.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().clickable { editingCategory = c }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CategoryBadge(c, size = 34)
                            Spacer(Modifier.width(12.dp))
                            Text(c.name)
                        }
                    }
                }
            }
        }

        SectionCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Sauvegarde", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Exporte toutes tes données dans un fichier JSON (à garder en lieu sûr), ou restaure-les. " +
                        "L'import remplace toutes les données actuelles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("budget-${LocalDate.now()}.json") }) { Text("Exporter") }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) { Text("Importer") }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(settings.reminderHour, settings.reminderMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setReminder(true, state.hour, state.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Annuler") } },
            text = { TimePicker(state = state) },
        )
    }

    editingCategory?.let { c ->
        CategoryDialog(
            initial = c,
            onSave = { vm.saveCategory(it); editingCategory = null },
            onDelete = if (c.id != 0L) ({ vm.deleteCategory(c); editingCategory = null }) else null,
            onDismiss = { editingCategory = null },
        )
    }
}

private val palette = listOf(
    0xFF4CAF50, 0xFFFF9800, 0xFF2196F3, 0xFF795548, 0xFFFFC107, 0xFFE91E63,
    0xFF00BCD4, 0xFF9C27B0, 0xFFFF5722, 0xFF607D8B, 0xFF2E7D32, 0xFF1565C0,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDialog(initial: Category, onSave: (Category) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var emoji by remember { mutableStateOf(initial.emoji) }
    var color by remember { mutableStateOf(initial.color) }
    var type by remember { mutableStateOf(initial.type) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Nouvelle catégorie" else "Modifier la catégorie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (initial.id == 0L) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(TxType.EXPENSE to "Dépense", TxType.INCOME to "Revenu").forEachIndexed { i, (t, l) ->
                            SegmentedButton(type == t, { type = t }, SegmentedButtonDefaults.itemShape(i, 2)) { Text(l) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(emoji, { emoji = it.take(4) }, label = { Text("Emoji") }, singleLine = true, modifier = Modifier.width(84.dp))
                    OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.forEach { p ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(Color(p), CircleShape)
                                .then(if (p == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { color = p },
                        )
                    }
                }
                if (onDelete != null) {
                    HorizontalDivider()
                    TextButton(onClick = onDelete) {
                        Text("Supprimer (les opérations deviennent non catégorisées)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && emoji.isNotBlank(),
                onClick = { onSave(initial.copy(name = name.trim(), emoji = emoji.trim(), color = color, type = type)) },
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
