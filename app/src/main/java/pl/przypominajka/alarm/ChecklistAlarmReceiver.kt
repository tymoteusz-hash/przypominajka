package pl.przypominajka.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import pl.przypominajka.appContainer
import pl.przypominajka.widget.ChecklistWidget

/** Alarm punktu checklisty (pokazuje powiadomienie) i przycisk „Zrobione” w tym powiadomieniu. */
class ChecklistAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (itemId <= 0L) return

        val container = context.appContainer
        val repository = container.checklistRepository
        when (intent.action) {
            ACTION_FIRE -> launchAsync {
                val item = repository.getItem(itemId)
                if (item != null && !item.done && item.remindAt != null) {
                    container.notificationHelper.showChecklistItem(item)
                }
            }
            ACTION_DONE -> {
                container.notificationHelper.cancelChecklistItem(itemId)
                launchAsync {
                    repository.setDone(itemId, true)
                    // Widżet Checklisty od razu pokazuje odhaczony punkt.
                    runCatching { ChecklistWidget().updateAll(context) }
                }
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "pl.przypominajka.action.FIRE_CHECKLIST"
        const val ACTION_DONE = "pl.przypominajka.action.DONE_CHECKLIST"
        const val EXTRA_ITEM_ID = "checklist_item_id"
    }
}
