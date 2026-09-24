package fr.mickael.budget.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.mickael.budget.app
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Replanifie le rappel après un redémarrage ou un changement d'heure. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.app.reminders.apply(context.app.repository.settings.current())
                context.app.repository.refreshWidget()
            } finally {
                pending.finish()
            }
        }
    }
}
