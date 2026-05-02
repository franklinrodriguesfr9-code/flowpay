package com.flowpay.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flowpay.data.AppDatabase
import com.flowpay.data.FlowPayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).dao()
                FlowPayRepository(dao).ensureOccurrencesAroundNow()
                AlarmScheduler(context, dao).rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

