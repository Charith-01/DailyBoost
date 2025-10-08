package com.example.dailyboost

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

class HydrationData(context: Context) {

    private val sp = context.getSharedPreferences("hydration_data", Context.MODE_PRIVATE)
    private val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Keys
    private val KEY_TOTAL_TODAY = "total_today"
    private val KEY_GOAL = "goal"
    private val KEY_DRINK_AMOUNT = "drink_amount"
    private val KEY_LAST_DATE = "last_date"
    private val KEY_INTERVAL_MIN = "interval_minutes"

    /**
     * Automatically resets daily total when the app detects a new date.
     */
    fun ensureNewDayReset() {
        val today = df.format(Date())
        val last = sp.getString(KEY_LAST_DATE, "")
        if (last != today) {
            sp.edit()
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_TOTAL_TODAY, 0)
                .apply()
        }
    }

    /**
     * Total consumed water (mL) for today.
     */
    var totalToday: Int
        get() = sp.getInt(KEY_TOTAL_TODAY, 0)
        set(v) = sp.edit().putInt(KEY_TOTAL_TODAY, v).apply()

    /**
     * Daily hydration goal (mL). Default: 2500
     */
    var goal: Int
        get() = sp.getInt(KEY_GOAL, 2500)
        set(v) = sp.edit().putInt(KEY_GOAL, v).apply()

    /**
     * Amount added per “Drink” action (mL). Default: 300
     */
    var oneDrinkAmount: Int
        get() = sp.getInt(KEY_DRINK_AMOUNT, 300)
        set(v) = sp.edit().putInt(KEY_DRINK_AMOUNT, v).apply()

    /**
     * Reminder interval in minutes. Default: 120 (2 hours)
     */
    var reminderInterval: Int
        get() = sp.getInt(KEY_INTERVAL_MIN, 120)
        set(v) = sp.edit().putInt(KEY_INTERVAL_MIN, v).apply()
}
