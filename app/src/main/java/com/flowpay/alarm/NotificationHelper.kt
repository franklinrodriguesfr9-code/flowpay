package com.flowpay.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.flowpay.MainActivity
import com.flowpay.R
import com.flowpay.data.CommitmentPurpose
import com.flowpay.data.FlowType
import com.flowpay.data.ScheduleItem
import com.flowpay.settings.AppPreferences
import com.flowpay.util.DateUtils
import com.flowpay.util.MoneyUtils
import kotlin.math.absoluteValue

object NotificationHelper {
    private const val DEFAULT_CHANNEL_ID = "flowpay_reminders"
    private const val CHANNEL_NAME = "Lembretes financeiros"
    private val vibrationPattern = longArrayOf(0, 450, 180, 450)

    fun ensureChannel(context: Context) {
        ensureChannel(context, AppPreferences(context).notificationSoundUriOrNull())
    }

    fun ensureChannel(context: Context, soundUri: Uri?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val resolvedSound = soundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            channelId(soundUri),
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alertas de compromissos e pagamentos do FlowPay"
            enableVibration(true)
            vibrationPattern = NotificationHelper.vibrationPattern
            setSound(resolvedSound, attributes)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showReminder(context: Context, item: ScheduleItem) {
        if (!canNotify(context)) return
        val soundUri = item.notificationSoundUri?.let(Uri::parse) ?: AppPreferences(context).notificationSoundUriOrNull()
        ensureChannel(context, soundUri)

        val openIntent = android.app.PendingIntent.getActivity(
            context,
            item.occurrenceId.toRequestCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val silenceIntent = android.app.PendingIntent.getBroadcast(
            context,
            (item.occurrenceId + SILENCE_REQUEST_OFFSET).toRequestCode(),
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_SILENCE_TODAY
                putExtra(AlarmReceiver.EXTRA_OCCURRENCE_ID, item.occurrenceId)
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissIntent = android.app.PendingIntent.getBroadcast(
            context,
            (item.occurrenceId + DISMISS_REQUEST_OFFSET).toRequestCode(),
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_DISMISS
                putExtra(AlarmReceiver.EXTRA_OCCURRENCE_ID, item.occurrenceId)
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val prefix = when {
            item.purpose == CommitmentPurpose.TASK -> "Lembrete"
            item.flowType == FlowType.ENTRADA -> "Entrada"
            else -> "Pagamento"
        }
        val category = item.categoryName?.let { " - $it" }.orEmpty()
        val text = "$prefix de ${MoneyUtils.format(item.expectedCents)}$category - ${DateUtils.formatDate(item.dueAt)}"
        val resolvedSound = soundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId(soundUri))
            .setSmallIcon(R.drawable.ic_stat_flowpay)
            .setContentTitle(item.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setSound(resolvedSound)
            .setVibrate(vibrationPattern)
            .addAction(R.drawable.ic_stat_flowpay, "Silenciar hoje", silenceIntent)
            .setDeleteIntent(dismissIntent)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .build()

        NotificationManagerCompat.from(context).notify(item.occurrenceId.toRequestCode(), notification)
    }

    fun cancel(context: Context, occurrenceId: Long) {
        NotificationManagerCompat.from(context).cancel(occurrenceId.toRequestCode())
    }

    fun channelSettingsIntent(context: Context): Intent {
        val channelId = channelId(AppPreferences(context).notificationSoundUriOrNull())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    private fun channelId(soundUri: Uri?): String {
        if (soundUri == null) return DEFAULT_CHANNEL_ID
        return "flowpay_reminders_${soundUri.toString().hashCode().absoluteValue}"
    }

    private const val SILENCE_REQUEST_OFFSET = 100_000L
    private const val DISMISS_REQUEST_OFFSET = 200_000L

    private fun canNotify(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}
