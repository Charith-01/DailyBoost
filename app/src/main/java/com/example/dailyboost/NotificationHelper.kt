package com.example.dailyboost

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    const val CHANNEL_ID = "hydration_reminders"
    private const val NOTIF_ID_HYDRATION = 1001 // stable ID so new reminders replace old ones

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hydration reminders"
            val desc = "Periodic reminders to drink water"
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = desc
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
            }
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showHydration(ctx: Context) {
        // Android 13+ requires POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return // quietly skip if not granted
        }

        // When tapped, open HomeScreen
        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            // ensure single task feel from outside app context (e.g., from Worker)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPI = PendingIntent.getActivity(
            ctx,
            1001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use an icon that definitely exists in your project
        val smallIcon = try {
            R.mipmap.ic_launcher_round
        } catch (_: Throwable) {
            R.mipmap.ic_launcher // fallback, in case round variant is missing
        }

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle("Time to drink water 💧")
            .setContentText("Small sips add up—grab a glass now.")
            .setContentIntent(contentPI)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build() // IMPORTANT: build to get android.app.Notification

        // Notify with a stable ID to prevent stacking
        val nm = NotificationManagerCompat.from(ctx)
        nm.notify(NOTIF_ID_HYDRATION, notification)
    }
}
