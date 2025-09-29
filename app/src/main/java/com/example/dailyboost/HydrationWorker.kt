package com.example.dailyboost

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class HydrationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext

        // If user turned it off, quietly exit
        if (!HydrationPrefs.isEnabled(ctx)) return Result.success()

        // Show the reminder
        NotificationHelper.showHydration(ctx)

        // Update "next at" label for the UI
        val intervalMin = HydrationPrefs.intervalMinutes(ctx)
        val next = System.currentTimeMillis() + intervalMin * 60_000L
        HydrationPrefs.setNextEpochMs(ctx, next)

        return Result.success()
    }
}
