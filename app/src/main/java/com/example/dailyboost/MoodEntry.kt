package com.example.dailyboost

import java.util.UUID

data class MoodEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val emoji: String,
    val note: String? = null
)