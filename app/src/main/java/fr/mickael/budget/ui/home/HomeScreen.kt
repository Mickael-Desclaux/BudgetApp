package fr.mickael.budget.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.mickael.budget.data.Operation
import fr.mickael.budget.data.TxType
import fr.mickael.budget.domain.Money
import fr.mickael.budget.domain.MonthSummary
import fr.mickael.budget.ui.MainViewModel
import fr.mickael.budget.ui.components.EmptyState
import fr.mickael.budget.ui.components.OperationRow
import fr.mickael.budget.ui.components.SectionCard
import fr.mickael.budget.ui.components.StatTile
import fr.mickael.budget.ui.components.label
import fr.mickael.budget.ui.components.remainingColor
import fr.mickael.budget.ui.theme.MoneyColors
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HomeScreen(
    vm: MainViewModel,
    contentPadding: PaddingValues,
    onAdd: (TxType) -> Unit,
    onEdit: (Operation) -> Unit,
    onSeeAll: () -> Unit,
) {
    val month = remember { YearMonth.now() }
    val summary by remember(month) { vm.repository.summaryOf(month) }
        .collectAsStateWithLifecycle(initialValue = null)
    val operations by remember(month) { vm.repository.operationsIn(month) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by vm.categoriesById.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(month.label(), style = MaterialTheme.typography.titleLarge)
        summary?.let { s ->
            RemainingCard(s, today)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Revenus", Money.format(s.incomeCents), Modifier.weight(1f), MoneyColors.positive)
                StatTile("Dépenses fixes", Money.format(s.plannedExpensesCents), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Épargne visée", Money.format(s.savingsTargetCents), Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                StatTile("Dépensé", Money.format(s.variableExpensesCents), Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = { onAdd(TxType.EXPENSE) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.padding(4.dp)); Text("Dépense")
            }
            FilledTonalButton(onClick = { onAdd(TxType.INCOME) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.padding(4.dp)); Text("Revenu")
            }
        }
        SectionCard {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Dernières opérations", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onSeeAll) { Text("Tout voir") }
            }
            val recent = operations.filter { !it.date.isAfter(today) }.take(6)
            if (recent.isEmpty()) EmptyState("Aucune opération ce mois-ci")
            recent.forEach { op ->
                OperationRow(op, op.categoryId?.let(categories::get), onClick = { onEdit(op) }, showDate = true)
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun RemainingCard(s: MonthSummary, today: LocalDate) {
    val color = remainingColor(s.remainingCents, s.usedRatio)
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Reste à dépenser", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Money.format(s.remainingCents), style = MaterialTheme.typography.displaySmall, color = color)
            LinearProgressIndicator(
                progress = { s.usedRatio.coerceIn(0f, 1f) },
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
            Row {
                Text(
                    "${Money.format(s.variableExpensesCents)} dépensés",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "sur ${Money.format(s.spendableCents)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            s.remainingPerDayCents(today)?.let { perDay ->
                val daysLeft = s.month.lengthOfMonth() - today.dayOfMonth + 1
                Text(
                    "≈ ${Money.format(perDay)} par jour sur les $daysLeft jours restants",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
