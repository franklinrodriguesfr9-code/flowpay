package com.flowpay

import android.app.Application
import com.flowpay.alarm.NotificationHelper
import com.flowpay.data.AppDatabase
import com.flowpay.data.FlowPayRepository

class FlowPayApplication : Application() {
    lateinit var database: AppDatabase
        private set

    lateinit var repository: FlowPayRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = FlowPayRepository(database.dao())
        NotificationHelper.ensureChannel(this)
    }
}

