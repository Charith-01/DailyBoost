package com.example.dailyboost.ui.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dailyboost.MoodStore
import com.example.dailyboost.MoodEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MoodChart(data: List<MoodEntry>) {
    val days = 7
    val labels = mutableListOf<String>()
    val scores = FloatArray(days) { 0f }

    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayKeys = IntArray(days)

    for (i in 0 until days) {
        dayKeys[i] = cal.get(Calendar.YEAR) * 10000 +
                (cal.get(Calendar.MONTH)+1)*100 +
                cal.get(Calendar.DAY_OF_MONTH)
        labels.add(dayFormat.format(cal.time))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    val buckets = HashMap<Int, MutableList<Int>>()
    for (e in data) {
        val k = (Calendar.getInstance().apply { timeInMillis = e.timestamp }).let {
            it.get(Calendar.YEAR)*10000 + (it.get(Calendar.MONTH)+1)*100 + it.get(Calendar.DAY_OF_MONTH)
        }
        if (dayKeys.contains(k)) {
            buckets.getOrPut(k) { mutableListOf() }.add(MoodStore.scoreForEmoji(e.emoji))
        }
    }
    for (i in 0 until days) {
        val list = buckets[dayKeys[i]]
        if (!list.isNullOrEmpty()) scores[i] = list.sum().toFloat()/list.size
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until days) {
            val h = (scores[i] / 5f).coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((h * 150).dp)
                        .background(if (h == 0f) Color.LightGray else MaterialTheme.colorScheme.primary)
                )
                Text(labels[i], style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
