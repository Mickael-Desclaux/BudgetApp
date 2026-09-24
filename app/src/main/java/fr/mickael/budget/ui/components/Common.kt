package fr.mickael.budget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.mickael.budget.data.Category
import fr.mickael.budget.data.Operation
import fr.mickael.budget.data.TxType
import fr.mickael.budget.domain.Money
import fr.mickael.budget.ui.theme.MoneyColors
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthSelector(month: YearMonth, onChange: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(month.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mois précédent")
        }
        Text(
            month.label(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = { onChange(month.plusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mois suivant")
        }
    }
}

@Composable
fun CategoryBadge(category: Category?, size: Int = 40) {
    val color = category?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .size(size.dp)
            .background(color.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(category?.emoji ?: "💶", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun OperationRow(
    operation: Operation,
    category: Category?,
    onClick: () -> Unit,
    showDate: Boolean = false,
) {
    val isIncome = operation.type == TxType.INCOME
    val planned = operation.date.isAfter(LocalDate.now())
    val title = operation.label.ifBlank {
        when {
            operation.isQuickTotal -> "Dépenses du jour"
            category != null -> category.name
            else -> "Non catégorisé"
        }
    }
    val subtitle = buildList {
        if (showDate) add(operation.date.shortLabel())
        if (operation.label.isNotBlank()) add(category?.name ?: "Non catégorisé")
        if (operation.recurringId != null) add(if (planned) "Prévu" else "Récurrent")
    }.joinToString(" · ")
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            (if (isIncome) "+" else "−") + Money.format(operation.amountCents),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = when {
                planned -> MaterialTheme.colorScheme.onSurfaceVariant
                isIncome -> MoneyColors.positive
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

/** Liste d'opérations groupées par jour, avec total des dépenses du jour. */
@Composable
fun GroupedOperations(
    operations: List<Operation>,
    categories: Map<Long, Category>,
    onClick: (Operation) -> Unit,
) {
    val byDay = operations.groupBy { it.date }.toSortedMap(compareByDescending { it })
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        byDay.forEach { (day, ops) ->
            val spent = ops.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
            SectionCard {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        day.dayLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (spent > 0) {
                        Text(
                            "−" + Money.format(spent),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ops.forEach { op -> OperationRow(op, op.categoryId?.let(categories::get), onClick = { onClick(op) }) }
                Spacer(Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) { Column { content() } }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 1)
        }
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun remainingColor(remainingCents: Long, usedRatio: Float): Color = when {
    remainingCents < 0 -> MoneyColors.negative
    usedRatio > 0.8f -> MoneyColors.warning
    else -> MoneyColors.positive
}
