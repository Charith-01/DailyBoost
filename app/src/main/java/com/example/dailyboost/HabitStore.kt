package com.example.dailyboost

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HabitStore {

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    fun loadHabits(context: Context): MutableList<Habit> {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(Constants.KEY_HABITS, "[]"))
        val out = mutableListOf<Habit>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: JSONObject()
            out += Habit.fromJson(obj)
        }
        return out
    }

    fun saveHabits(context: Context, habits: List<Habit>) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray().apply { habits.forEach { put(it.toJson()) } }
        prefs.edit().putString(Constants.KEY_HABITS, arr.toString()).apply()
    }

    fun addHabit(context: Context, habit: Habit) {
        val list = loadHabits(context)
        list += habit
        saveHabits(context, list)
    }

    fun updateHabit(context: Context, habit: Habit) {
        val list = loadHabits(context)
        val idx = list.indexOfFirst { it.id == habit.id }
        if (idx >= 0) {
            list[idx] = habit
            saveHabits(context, list)
        }
    }

    fun deleteHabit(context: Context, habitId: String) {
        val list = loadHabits(context).filterNot { it.id == habitId }
        saveHabits(context, list)
    }

    // -------------------------------------------------------------------------
    // Progress mutations
    // -------------------------------------------------------------------------

    fun incrementCount(context: Context, habitId: String, delta: Int = 1) {
        val list = loadHabits(context)
        val h = list.find { it.id == habitId } ?: return
        if (h.type == HabitType.COUNT) {
            val goal = h.goalPerDay.coerceAtLeast(1)
            h.progressToday = (h.progressToday + delta).coerceAtMost(goal)
            saveHabits(context, list)
        }
    }

    fun setYesNo(context: Context, habitId: String, done: Boolean) {
        val list = loadHabits(context)
        val h = list.find { it.id == habitId } ?: return
        if (h.type == HabitType.YES_NO) {
            h.progressToday = if (done) 1 else 0
            saveHabits(context, list)
        }
    }

    // -------------------------------------------------------------------------
    // Aggregates
    // -------------------------------------------------------------------------

    /** Average completion across active habits (0..100). */
    fun computeTodayPercent(context: Context): Int {
        val actives = loadHabits(context).filter { it.isActive }
        if (actives.isEmpty()) return 0
        val ratios = actives.map {
            val goal = it.goalPerDay.coerceAtLeast(1)
            val prog = it.progressToday.coerceIn(0, goal)
            prog.toFloat() / goal
        }
        return (ratios.average() * 100).toInt()
    }

    /** Returns Pair(completed, totalActive) for "X/Y habits". */
    fun todayCounts(context: Context): Pair<Int, Int> {
        val actives = loadHabits(context).filter { it.isActive }
        val total = actives.size
        val done = actives.count {
            val goal = it.goalPerDay.coerceAtLeast(1)
            it.progressToday >= goal
        }
        return done to total
    }

    // -------------------------------------------------------------------------
    // Daily rollover + streak
    // -------------------------------------------------------------------------

    /**
     * Call on app open / Home. If the calendar date changed:
     * 1) Decide if *yesterday* was fully completed (all active habits reached goal).
     * 2) Update streak accordingly.
     * 3) Reset today's progress to 0 and store the new date.
     */
    fun resetIfNewDay(context: Context) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayString()
        val last = prefs.getString(Constants.KEY_LAST_OPEN_DATE, null)
        if (last == today) return

        val list = loadHabits(context)

        // Yesterday = the data we currently hold in progressToday.
        val actives = list.filter { it.isActive }
        val yesterdayComplete = actives.isNotEmpty() && actives.all { h ->
            val goal = h.goalPerDay.coerceAtLeast(1)
            h.progressToday >= goal
        }

        val currentStreak = prefs.getInt(Constants.KEY_STREAK, 0)
        val newStreak = if (yesterdayComplete) currentStreak + 1 else 0
        prefs.edit().putInt(Constants.KEY_STREAK, newStreak).apply()

        // New day → clear progress
        list.forEach { it.progressToday = 0 }
        saveHabits(context, list)

        prefs.edit().putString(Constants.KEY_LAST_OPEN_DATE, today).apply()
    }

    fun getStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(Constants.KEY_STREAK, 0)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun todayString(): String {
        val sdf = SimpleDateFormat(Constants.DATE_PATTERN, Locale.US)
        return sdf.format(Date())
    }
}
