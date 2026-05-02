package com.flowpay.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flowpay.data.NotificationDismissPolicy
import com.flowpay.data.AppDatabase
import com.flowpay.data.OccurrenceStatus
import com.flowpay.settings.AppPreferences
import com.flowpay.util.DateUtils
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val occurrenceId = intent.getLongExtra(EXTRA_OCCURRENCE_ID, -1L)
        if (occurrenceId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).dao()
                when (intent.action) {
                    ACTION_REMIND -> {
                        val item = dao.getScheduleItem(occurrenceId)
                        if (item != null && item.status == OccurrenceStatus.PENDENTE) {
                            NotificationHelper.showReminder(context, item)
                            AlarmScheduler(context, dao).schedule(item)
                        }
                    }
                    ACTION_SILENCE_TODAY -> {
                        dao.silenceOccurrenceUntil(occurrenceId, DateUtils.endOfDayMillis(LocalDate.now()))
                        NotificationHelper.cancel(context, occurrenceId)
                        dao.getScheduleItem(occurrenceId)?.let { AlarmScheduler(context, dao).schedule(it) }
                    }
                    ACTION_DISMISS -> {
                        val item = dao.getScheduleItem(occurrenceId)
                        if (item != null && item.status == OccurrenceStatus.PENDENTE) {
                            val preferences = AppPreferences(context)
                            val policy = if (item.useGlobalReminderSettings) {
                                preferences.notificationDismissPolicy
                            } else {
                                item.notificationDismissPolicy
                            }
                            if (policy == NotificationDismissPolicy.STOP_ON_DISMISS) {
                                dao.stopOccurrenceReminder(occurrenceId, System.currentTimeMillis())
                                AlarmScheduler(context, dao).cancel(occurrenceId)
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.flowpay.action.REMIND"
        const val ACTION_SILENCE_TODAY = "com.flowpay.action.SILENCE_TODAY"
        const val ACTION_DISMISS = "com.flowpay.action.DISMISS"
        const val EXTRA_OCCURRENCE_ID = "occurrence_id"
    }
}
