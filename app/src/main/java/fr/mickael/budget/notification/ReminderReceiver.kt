package fr.mickael.budget.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import fr.mickael.budget.MainActivity
import fr.mickael.budget.R
import fr.mickael.budget.app
import fr.mickael.budget.domain.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = context.app.repository
                repo.materializeRecurrings()
                val settings = repo.settings.current()
                context.app.reminders.apply(settings)
                if (settings.reminderEnabled) {
                    val summary = repo.currentSummary()
                    show(context, "Reste ce mois : ${Money.format(summary.remainingCents)}")
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun show(context: Context, subtitle: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context,
            1,
            MainActivity.addExpenseIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Tes dépenses du jour ?")
            .setContentText(subtitle)
            .setContentIntent(open)
            .addAction(0, "Ajouter", open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ReminderScheduler.NOTIFICATION_ID, notification)
    }
}
