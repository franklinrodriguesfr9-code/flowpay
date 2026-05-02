package com.flowpay.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.flowpay.data.FlowPayDao
import com.flowpay.data.OccurrenceStatus
import com.flowpay.data.ScheduleItem
import com.flowpay.settings.AppPreferences
import com.flowpay.util.DateUtils

class AlarmScheduler(
    private val context: Context,
    private val dao: FlowPayDao,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun rescheduleAll() {
        dao.getPendingScheduleItems().forEach { schedule(it) }
    }

    suspend fun scheduleOccurrence(occurrenceId: Long) {
        dao.getScheduleItem(occurrenceId)?.let { schedule(it) }
    }

    fun schedule(item: ScheduleItem) {
        if (item.status != OccurrenceStatus.PENDENTE) {
            cancel(item.occurrenceId)
            return
        }
        if (item.reminderStoppedAt != null) {
            cancel(item.occurrenceId)
            return
        }
        val preferences = AppPreferences(context)
        val repeatEnabled = if (item.useGlobalReminderSettings) preferences.reminderRepeatEnabled else item.reminderRepeatEnabled
        val repeatUnit = if (item.useGlobalReminderSettings) preferences.reminderRepeatUnit else item.reminderRepeatUnit
        val repeatInterval = if (item.useGlobalReminderSettings) preferences.reminderRepeatInterval else item.reminderRepeatInterval
        val now = System.currentTimeMillis()
        val triggerAt = if (item.reminderSilencedUntil != null && item.reminderSilencedUntil > now) {
            DateUtils.nextDailyReminderMillis(
                hour = item.reminderHour,
                minute = item.reminderMinute,
                nowMillis = item.reminderSilencedUntil + 1_000L,
            )
        } else {
            DateUtils.nextReminderMillis(
                dueAt = item.dueAt,
                hour = item.reminderHour,
                minute = item.reminderMinute,
                repeatUnit = repeatUnit,
                repeatInterval = if (repeatEnabled) repeatInterval else 0,
            )
        }
        val pendingIntent = pendingIntent(item.occurrenceId)
        try {
            if (canUseExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent,
            )
        }
    }

    fun cancel(occurrenceId: Long) {
        alarmManager.cancel(pendingIntent(occurrenceId))
        NotificationHelper.cancel(context, occurrenceId)
    }

    fun canUseExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun pendingIntent(occurrenceId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REMIND
            putExtra(AlarmReceiver.EXTRA_OCCURRENCE_ID, occurrenceId)
        }
        return PendingIntent.getBroadcast(
            context,
            occurrenceId.toRequestCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

fun Long.toRequestCode(): Int = (this % Int.MAX_VALUE).toInt()
