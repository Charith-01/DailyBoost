package com.example.dailyboost

import android.content.Context
import android.content.Context.MODE_PRIVATE

object HydrationPrefs {
    private const val PREFS = "prefs_hydration"
    private const val KEY_ENABLED = "hydration_enabled"
    private const val KEY_INTERVAL_MIN = "hydration_interval_minutes"
    private const val KEY_NEXT_EPOCH = "hydration_next_epoch_ms"

    private const val DEFAULT_INTERVAL_MIN = 120

    private fun sp(ctx: Context) = ctx.getSharedPreferences(PREFS, MODE_PRIVATE)

    fun isEnabled(ctx: Context) = sp(ctx).getBoolean(KEY_ENABLED, false)
    fun setEnabled(ctx: Context, enabled: Boolean) {
        sp(ctx).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun intervalMinutes(ctx: Context) = sp(ctx).getInt(KEY_INTERVAL_MIN, DEFAULT_INTERVAL_MIN)
    fun setIntervalMinutes(ctx: Context, min: Int) {
        sp(ctx).edit().putInt(KEY_INTERVAL_MIN, min).apply()
    }

    fun nextEpochMs(ctx: Context) = sp(ctx).getLong(KEY_NEXT_EPOCH, 0L)
    fun setNextEpochMs(ctx: Context, epochMs: Long) {
        sp(ctx).edit().putLong(KEY_NEXT_EPOCH, epochMs).apply()
    }

    fun clearNext(ctx: Context) = sp(ctx).edit().remove(KEY_NEXT_EPOCH).apply()
}
