package com.example.dailyboost

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.roundToInt

object MoodStore {

    private const val PREFS = "wellness_prefs"           // same bucket as the rest of app prefs
    private const val KEY_MOODS = "moods_json"           // stored as a JSON array

    // ---------- Public API ----------

    fun add(ctx: Context, emoji: String, note: String? = null): MoodEntry {
        val entry = MoodEntry(emoji = emoji, note = note)
        val arr = readArray(ctx)
        arr.put(toJson(entry))
        saveArray(ctx, arr)
        return entry
    }

    fun listAll(ctx: Context): List<MoodEntry> {
        val arr = readArray(ctx)
        val out = ArrayList<MoodEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            out.add(fromJson(obj))
        }
        // newest first
        return out.sortedByDescending { it.timestamp }
    }

    fun delete(ctx: Context, id: String): Boolean {
        val arr = readArray(ctx)
        var removed = false
        val next = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.optString("id") == id) {
                removed = true
            } else {
                next.put(obj)
            }
        }
        if (removed) saveArray(ctx, next)
        return removed
    }

    /** Average mood score (1–5) across the last 7 calendar days (including today). */
    fun averageLast7(ctx: Context): Float? {
        val all = listAll(ctx)
        if (all.isEmpty()) return null

        val todayKey = dayKey(System.currentTimeMillis())
        val sevenDaysKeys = HashSet<Int>().apply {
            val cal = Calendar.getInstance()
            for (i in 0..6) {
                add(dayKey(cal.timeInMillis))
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }

        var sum = 0f
        var count = 0
        for (e in all) {
            if (e.timestamp <= 0L) continue
            if (dayKey(e.timestamp) in sevenDaysKeys) {
                sum += scoreForEmoji(e.emoji).toFloat()
                count++
            }
        }
        return if (count == 0) null else (sum / count)
    }

    /** Round score to nearest face for display. */
    fun emojiForScore(avg: Float): String = when (avg.roundToInt().coerceIn(1, 5)) {
        1 -> "😭"
        2 -> "😞"
        3 -> "😐"
        4 -> "🙂"
        else -> "🤩"
    }

    fun scoreForEmoji(emoji: String): Int = when (emoji) {
        "😭" -> 1
        "😞" -> 2
        "😐" -> 3
        "🙂", "😊" -> 4
        "🤩", "❤️", "😁" -> 5
        else -> 3
    }

    // ---------- Internals ----------

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun readArray(ctx: Context): JSONArray {
        val raw = prefs(ctx).getString(KEY_MOODS, "[]") ?: "[]"
        return try { JSONArray(raw) } catch (_: Throwable) { JSONArray() }
    }

    private fun saveArray(ctx: Context, arr: JSONArray) {
        prefs(ctx).edit().putString(KEY_MOODS, arr.toString()).apply()
    }

    private fun toJson(e: MoodEntry) = JSONObject().apply {
        put("id", e.id)
        put("timestamp", e.timestamp)
        put("emoji", e.emoji)
        if (e.note != null) put("note", e.note)
    }

    private fun fromJson(o: JSONObject) = MoodEntry(
        id = o.optString("id"),
        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
        emoji = o.optString("emoji", "😐"),
        note = if (o.has("note")) o.optString("note") else null
    )

    /** Compact day key like 20250930 in device timezone. */
    private fun dayKey(ts: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1 // 0-based
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return y * 10000 + m * 100 + d
    }
}
