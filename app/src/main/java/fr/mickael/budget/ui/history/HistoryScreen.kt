package fr.mickael.budget.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.mickael.budget.data.Operation
import fr.mickael.budget.data.TxType
import fr.mickael.budget.domain.Money
import fr.mickael.budget.domain.MonthSummary
import fr.mickael.budget.ui.MainViewModel
import fr.mickael.budget.ui.components.EmptyState
import fr.mickael.budget.ui.components.GroupedOperations
import fr.mickael.budget.ui.components.SectionCard
import fr.mickael.budget.ui.components.StatTile
import fr.mickael.budget.ui.components.label
import fr.mickael.budget.ui.theme.MoneyColors
import java.time.YearMonth

@Composable
fun HistoryScreen(vm: MainViewModel, contentPadding: PaddingValues, onOpen: (YearMonth) -> Unit) {
    val history by vm.repository.history.collectAsStateWithLifecycle(initialValue = emptyList())
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Historique", style = MaterialTheme.typography.titleLarge) }
        if (history.isEmpty()) item { EmptyState("Pas encore d'historique") }
        items(history, key = { it.month.toString() }) { s -> MonthCard(s) { onOpen(s.month) } }
    }
}

@Composable
private fun MonthCard(s: MonthSummary, onClick: () -> Unit) {
    val current = s.month == YearMonth.now()
    SectionCard(Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.month.label() + if (current) " (en cours)" else "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    Money.format(s.balanceCents, withSign = true),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (s.balanceCents >= 0) MoneyColors.positive else MoneyColors.negative,
                )
            }
            Text(
                "Revenus ${Money.format(s.incomeCents)} · Dépenses ${Money.format(s.totalExpensesCents)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (s.savingsTargetCents > 0) {
                val ok = s.vsGoalCents >= 0
                Text(
                    when {
                        current && ok -> "⏳ En bonne voie"
                        current -> "⚠️ En dessous de l'objectif"
                        ok -> "✅ Épargne atteinte"
                        else -> "⚠️ Épargne manquée"
                    } +
                        " (objectif ${Money.format(s.savingsTargetCents)}, écart ${Money.format(s.vsGoalCents, withSign = true)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ok) MoneyColors.positive else MoneyColors.warning,
                )
            }
        }
    }
}

@Composable
fun MonthDetailScreen(
    vm: MainViewModel,
    month: YearMonth,
    contentPadding: PaddingValues,
    onEdit: (Operation) -> Unit,
) {
    val summary by remember(month) { vm.repository.summaryOf(month) }.collectAsStateWithLifecycle(initialValue = null)
    val operations by remember(month) { vm.repository.operationsIn(month) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val byId by vm.categoriesById.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        summary?.let { s ->
            Text(
                Money.format(s.balanceCents, withSign = true),
                style = MaterialTheme.typography.displaySmall,
                color = if (s.balanceCents >= 0) MoneyColors.positive else MoneyColors.negative,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Revenus", Money.format(s.incomeCents), Modifier.weight(1f), MoneyColors.positive)
                StatTile("Dépenses", Money.format(s.totalExpensesCents), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("dont fixes", Money.format(s.plannedExpensesCents), Modifier.weight(1f))
                StatTile("dont variables", Money.format(s.variableExpensesCents), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Épargne visée", Money.format(s.savingsTargetCents), Modifier.weight(1f))
                StatTile(
                    "Écart objectif",
                    Money.format(s.vsGoalCents, withSign = true),
                    Modifier.weight(1f),
                    if (s.vsGoalCents >= 0) MoneyColors.positive else MoneyColors.negative,
                )
            }
        }

        val expenses = operations.filter { it.type == TxType.EXPENSE }
        val total = expenses.sumOf { it.amountCents }
        if (total > 0) {
            SectionCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Par catégorie", style = MaterialTheme.typography.titleMedium)
                    expenses.groupBy { it.categoryId }
                        .mapValues { (_, list) -> list.sumOf { it.amountCents } }
                        .entries.sortedByDescending { it.value }
                        .forEach { (catId, amount) ->
                            val cat = catId?.let(byId::get)
                            CategoryBar(
                                label = cat?.let { "${it.emoji} ${it.name}" } ?: "💶 Non catégorisé",
                                amountCents = amount,
                                ratio = amount.toFloat() / total,
                                color = cat?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline,
                            )
                        }
                }
            }
        }

        Text("Opérations", style = MaterialTheme.typography.titleMedium)
        if (operations.isEmpty()) EmptyState("Aucune opération")
        else GroupedOperations(operations, byId, onEdit)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CategoryBar(label: String, amountCents: Long, ratio: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "${Money.format(amountCents)} · ${(ratio * 100).toInt()} %",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(ratio.coerceIn(0.01f, 1f)).background(color))
        }
    }
    Spacer(Modifier.width(0.dp))
}
