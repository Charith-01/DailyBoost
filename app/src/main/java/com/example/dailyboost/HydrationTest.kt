// app/src/main/java/com/example/dailyboost/HydrationTest.kt
package com.example.dailyboost

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object HydrationTest {
    fun scheduleTestReminder(ctx: Context, delaySec: Long = 30) {
        val data = Data.Builder().putBoolean("force", true).build()
        val req = OneTimeWorkRequestBuilder<HydrationWorker>()
            .setInitialDelay(delaySec, TimeUnit.SECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(ctx).enqueue(req)
    }
}
