package com.example.dailyboost

import android.content.Context
import android.text.format.DateFormat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object HydrationScheduler {
    private const val UNIQUE = "hydration_periodic"

    fun enable(ctx: Context, intervalMinutes: Int) {
        NotificationHelper.createChannel(ctx)
        HydrationPrefs.setEnabled(ctx, true)
        HydrationPrefs.setIntervalMinutes(ctx, intervalMinutes)

        val req = PeriodicWorkRequestBuilder<HydrationWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES
        )
            // start counting from now
            .setInitialDelay(intervalMinutes.toLong(), TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.REPLACE, req)

        // set "next at" for UI
        val next = System.currentTimeMillis() + intervalMinutes * 60_000L
        HydrationPrefs.setNextEpochMs(ctx, next)
    }

    fun updateInterval(ctx: Context, newIntervalMinutes: Int) {
        HydrationPrefs.setIntervalMinutes(ctx, newIntervalMinutes)
        if (HydrationPrefs.isEnabled(ctx)) {
            enable(ctx, newIntervalMinutes) // REPLACE existing
        }
    }

    fun disable(ctx: Context) {
        HydrationPrefs.setEnabled(ctx, false)
        WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE)
        HydrationPrefs.clearNext(ctx)
    }

    fun ensureScheduled(ctx: Context) {
        if (!HydrationPrefs.isEnabled(ctx)) return
        val mins = HydrationPrefs.intervalMinutes(ctx)
        // KEEP avoids duplicating if already scheduled
        val req = PeriodicWorkRequestBuilder<HydrationWorker>(
            mins.toLong(), TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, req)

        if (HydrationPrefs.nextEpochMs(ctx) == 0L) {
            HydrationPrefs.setNextEpochMs(ctx, System.currentTimeMillis() + mins * 60_000L)
        }
    }

    fun nextLabel(ctx: Context): String {
        val mins = HydrationPrefs.intervalMinutes(ctx)
        val next = HydrationPrefs.nextEpochMs(ctx)
        val every = when {
            mins % 60 == 0 -> "Every ${mins / 60} hour${if (mins >= 120) "s" else ""}"
            else -> "Every ${mins} minutes"
        }
        if (next <= 0L) return every
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        val timeText = DateFormat.getTimeFormat(ctx).format(cal.time)
        return "Next at $timeText • $every"
    }
}
