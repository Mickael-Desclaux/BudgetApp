package fr.mickael.budget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import fr.mickael.budget.MainActivity
import fr.mickael.budget.app
import fr.mickael.budget.domain.Money
import fr.mickael.budget.domain.MonthSummary
import java.time.LocalDate
import java.time.YearMonth

class BudgetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = context.app.repository
        repo.materializeRecurrings()
        val initial = repo.currentSummary()
        provideContent {
            // Observe la base : le widget se met à jour tant que sa session est active.
            val summary by remember { repo.summaryOf(YearMonth.now()) }.collectAsState(initial)
            GlanceTheme { Content(context, summary) }
        }
    }

    @Composable
    private fun Content(context: Context, summary: MonthSummary) {
        val remaining = summary.remainingCents
        val amountColor = when {
            remaining < 0 -> Color(0xFFE57373)
            summary.usedRatio > 0.8f -> Color(0xFFFFB74D)
            else -> Color(0xFF81C784)
        }
        val white = ColorProvider(day = Color.White, night = Color.White)
        val muted = ColorProvider(day = Color(0xFFB0BEC5), night = Color(0xFFB0BEC5))
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = Color(0xFF1E2A33), night = Color(0xFF1E2A33)))
                .cornerRadius(20.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clickable(actionStartActivity(MainActivity.openIntent(context))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Reste ce mois", style = TextStyle(color = muted, fontSize = 12.sp))
                Text(
                    Money.format(remaining),
                    style = TextStyle(
                        color = ColorProvider(day = amountColor, night = amountColor),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                summary.remainingPerDayCents(LocalDate.now())?.let {
                    Text("${Money.format(it)} / jour", style = TextStyle(color = muted, fontSize = 12.sp))
                }
            }
            Spacer(GlanceModifier.width(8.dp))
            Box(
                modifier = GlanceModifier
                    .size(48.dp)
                    .background(ColorProvider(day = Color(0xFF26A69A), night = Color(0xFF26A69A)))
                    .cornerRadius(24.dp)
                    .clickable(actionStartActivity(MainActivity.addExpenseIntent(context))),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", style = TextStyle(color = white, fontSize = 26.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}
