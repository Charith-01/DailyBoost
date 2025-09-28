package com.example.dailyboost

import org.json.JSONObject
import java.util.UUID

enum class HabitType { COUNT, YES_NO }

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var type: HabitType,
    var goalPerDay: Int,          // COUNT: target number; YES_NO: usually 1
    var progressToday: Int = 0,   // 0..goalPerDay
    var isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("type", type.name)
        put("goalPerDay", goalPerDay)
        put("progressToday", progressToday)
        put("isActive", isActive)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): Habit = Habit(
            id = obj.optString("id"),
            title = obj.optString("title"),
            type = HabitType.valueOf(obj.optString("type", HabitType.COUNT.name)),
            goalPerDay = obj.optInt("goalPerDay", 1),
            progressToday = obj.optInt("progressToday", 0),
            isActive = obj.optBoolean("isActive", true),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
