package com.example.dailyboost

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class HomeScreen : AppCompatActivity() {

    private lateinit var homeRvAdapter: HomeHabitsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_screen)

        // Insets
        val appBar = findViewById<android.view.View>(R.id.appBar)
        ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
            insets
        }
        val scroll = findViewById<android.view.View>(R.id.scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, nb.bottom)
            insets
        }
        val fab = findViewById<android.view.View>(R.id.fabAdd)
        ViewCompat.setOnApplyWindowInsetsListener(fab) { v, insets ->
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, nb.bottom)
            insets
        }

        // Navigation
        fab.setOnClickListener {
            startActivity(Intent(this, AddEditHabitActivity::class.java))
        }
        findViewById<TextView>(R.id.btnSeeAllHabits)?.setOnClickListener {
            startActivity(Intent(this, HabitsActivity::class.java))
        }

        // Today's Habits preview
        val rvHome = findViewById<RecyclerView>(R.id.rvHabitsToday)
        rvHome.layoutManager = LinearLayoutManager(this)
        homeRvAdapter = HomeHabitsAdapter(
            onIncrement = { id ->
                HabitStore.incrementCount(this, id)
                refreshHomeStats(); refreshHomePreview()
            },
            onToggle = { id, done ->
                HabitStore.setYesNo(this, id, done)
                refreshHomeStats(); refreshHomePreview()
            },
            onOpen = { id ->
                startActivity(
                    Intent(this, AddEditHabitActivity::class.java)
                        .putExtra(AddEditHabitActivity.EXTRA_HABIT_ID, id)
                )
            }
        )
        rvHome.adapter = homeRvAdapter

        // First load
        refreshHomeStats()
        refreshHomePreview()
    }

    override fun onResume() {
        super.onResume()
        refreshHomeStats()
        refreshHomePreview()
    }

    private fun refreshHomeStats() {
        HabitStore.resetIfNewDay(this)

        val percent = HabitStore.computeTodayPercent(this)
        findViewById<LinearProgressIndicator>(R.id.progressToday).apply {
            max = 100; progress = percent
        }

        val (done, total) = HabitStore.todayCounts(this)
        findViewById<TextView>(R.id.todayRightLabel)?.text = "$done/$total habits"

        val streak = HabitStore.getStreak(this)
        val stats = findViewById<TextView>(R.id.txtTodayStats)
        stats.text = when {
            total == 0 -> "Add your first habit to begin"
            streak > 0 -> "🔥 $streak-day streak • Keep going!"
            else -> if (done == total && total > 0)
                "🎉 All habits done today! Streak starts tomorrow"
            else "🔥 Keep going!"
        }
    }

    private fun refreshHomePreview() {
        val topHabits = HabitStore.loadHabits(this)
            .filter { it.isActive }
            .take(10)
        homeRvAdapter.submit(topHabits)
    }

    // Adapter for Home "Today's Habits" preview
    private class HomeHabitsAdapter(
        private val onIncrement: (String) -> Unit,
        private val onToggle: (String, Boolean) -> Unit,
        private val onOpen: (String) -> Unit
    ) : RecyclerView.Adapter<HomeHabitsAdapter.Holder>() {

        private val items = mutableListOf<Habit>()

        fun submit(list: List<Habit>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        inner class Holder(v: android.view.View) : RecyclerView.ViewHolder(v) {
            val icon: TextView = v.findViewById(R.id.habitIcon)
            val title: TextView = v.findViewById(R.id.txtTitle)
            val progressLabel: TextView = v.findViewById(R.id.txtProgress)
            val bar: LinearProgressIndicator = v.findViewById(R.id.progressBar)
            val btnPlus: MaterialButton = v.findViewById(R.id.btnIncrement)
            val chkDone: CheckBox = v.findViewById(R.id.chkDone)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Holder {
            val v = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_habit, parent, false)
            return Holder(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: Holder, position: Int) {
            val item = items[position]

            // Dynamic emoji (simple mapping by title; replace with a field if you add one)
            h.icon.text = emojiFor(item.title)

            h.title.text = item.title

            val goal = item.goalPerDay.coerceAtLeast(1)
            val prog = item.progressToday.coerceIn(0, goal)
            h.progressLabel.text = "$prog/$goal today"

            val pct = ((prog.toFloat() / goal) * 100).toInt()
            h.bar.max = 100
            h.bar.progress = pct

            if (item.type == HabitType.COUNT) {
                h.btnPlus.visibility = android.view.View.VISIBLE
                h.chkDone.visibility = android.view.View.GONE
                h.btnPlus.setOnClickListener { onIncrement(item.id) }
            } else {
                h.btnPlus.visibility = android.view.View.GONE
                h.chkDone.visibility = android.view.View.VISIBLE
                h.chkDone.setOnCheckedChangeListener(null)
                h.chkDone.isChecked = (prog >= goal)
                h.chkDone.setOnCheckedChangeListener { _, checked ->
                    onToggle(item.id, checked)
                }
            }

            h.itemView.setOnClickListener { onOpen(item.id) }
        }

        private fun emojiFor(title: String): String = when {
            title.contains("water", true) -> "💧"
            title.contains("gym", true)   -> "🏋️"
            title.contains("read", true)  -> "📚"
            title.contains("walk", true)  -> "🚶"
            title.contains("meditat", true)-> "🧘"
            else                          -> "✅"
        }
    }
}
