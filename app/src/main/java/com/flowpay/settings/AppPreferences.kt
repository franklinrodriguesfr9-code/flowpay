package com.flowpay.settings

import android.content.Context
import android.net.Uri
import com.flowpay.data.NotificationDismissPolicy
import com.flowpay.data.ReminderRepeatUnit

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("flowpay_settings", Context.MODE_PRIVATE)

    var notificationSoundUri: String?
        get() = preferences.getString(KEY_NOTIFICATION_SOUND_URI, null)
        set(value) {
            preferences.edit().putString(KEY_NOTIFICATION_SOUND_URI, value).apply()
        }

    var reminderRepeatEnabled: Boolean
        get() = preferences.getBoolean(KEY_REMINDER_REPEAT_ENABLED, true)
        set(value) {
            preferences.edit().putBoolean(KEY_REMINDER_REPEAT_ENABLED, value).apply()
        }

    var reminderRepeatUnit: ReminderRepeatUnit
        get() = runCatching {
            ReminderRepeatUnit.valueOf(preferences.getString(KEY_REMINDER_REPEAT_UNIT, ReminderRepeatUnit.MINUTE.name)!!)
        }.getOrDefault(ReminderRepeatUnit.MINUTE)
        set(value) {
            preferences.edit().putString(KEY_REMINDER_REPEAT_UNIT, value.name).apply()
        }

    var reminderRepeatInterval: Int
        get() = preferences.getInt(KEY_REMINDER_REPEAT_INTERVAL, 5)
        set(value) {
            preferences.edit().putInt(KEY_REMINDER_REPEAT_INTERVAL, value.coerceIn(1, maxIntervalFor(reminderRepeatUnit))).apply()
        }

    var notificationDismissPolicy: NotificationDismissPolicy
        get() = runCatching {
            NotificationDismissPolicy.valueOf(
                preferences.getString(KEY_NOTIFICATION_DISMISS_POLICY, NotificationDismissPolicy.KEEP_UNTIL_DONE.name)!!,
            )
        }.getOrDefault(NotificationDismissPolicy.KEEP_UNTIL_DONE)
        set(value) {
            preferences.edit().putString(KEY_NOTIFICATION_DISMISS_POLICY, value.name).apply()
        }

    fun notificationSoundUriOrNull(): Uri? = notificationSoundUri?.let(Uri::parse)

    companion object {
        private const val KEY_NOTIFICATION_SOUND_URI = "notification_sound_uri"
        private const val KEY_REMINDER_REPEAT_ENABLED = "reminder_repeat_enabled"
        private const val KEY_REMINDER_REPEAT_UNIT = "reminder_repeat_unit"
        private const val KEY_REMINDER_REPEAT_INTERVAL = "reminder_repeat_interval"
        private const val KEY_NOTIFICATION_DISMISS_POLICY = "notification_dismiss_policy"

        fun maxIntervalFor(unit: ReminderRepeatUnit): Int = when (unit) {
            ReminderRepeatUnit.MINUTE -> 59
            ReminderRepeatUnit.HOUR -> 23
            ReminderRepeatUnit.DAY -> 30
        }
    }
}
