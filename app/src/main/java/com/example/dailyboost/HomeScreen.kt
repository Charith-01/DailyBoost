package com.example.dailyboost

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial

class HomeScreen : AppCompatActivity() {

    private lateinit var homeRvAdapter: HomeHabitsAdapter

    // Hydration UI refs
    private var switchHydration: SwitchMaterial? = null
    private var hydrationSub: TextView? = null

    // Android 13+ notification permission
    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            HydrationScheduler.enable(this, HydrationPrefs.intervalMinutes(this))
            updateHydrationRow()
            Toast.makeText(this, "Hydration reminders enabled", Toast.LENGTH_SHORT).show()
            HydrationTest.scheduleTestReminder(this, 30) // dev helper
        } else {
            switchHydration?.isChecked = false
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_screen)

        // Insets
        val appBar = findViewById<View>(R.id.appBar)
        ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
            insets
        }
        val scroll = findViewById<View>(R.id.scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, nb.bottom)
            insets
        }
        val fab = findViewById<View>(R.id.fabAdd)
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
        findViewById<View>(R.id.cardHydration).setOnClickListener {
            startActivity(Intent(this, HydrationSettingsActivity::class.java))
        }

        // Hydration init
        NotificationHelper.createChannel(this)
        switchHydration = findViewById(R.id.switchHydration)
        hydrationSub = findViewById(R.id.hydrationSub)
        switchHydration?.isChecked = HydrationPrefs.isEnabled(this)
        updateHydrationRow()

        switchHydration?.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        this, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    HydrationScheduler.enable(this, HydrationPrefs.intervalMinutes(this))
                    updateHydrationRow()
                    Toast.makeText(this, "Hydration reminders enabled", Toast.LENGTH_SHORT).show()
                    HydrationTest.scheduleTestReminder(this, 30) // dev helper
                }
            } else {
                HydrationScheduler.disable(this)
                updateHydrationRow()
                Toast.makeText(this, "Hydration reminders disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // ---------- Quick Mood: save on tap; open editor on long-press ----------
        findViewById<LinearLayout>(R.id.moodChips)?.let { row ->
            for (i in 0 until row.childCount) {
                val tv = row.getChildAt(i) as? TextView ?: continue

                // Quick save on tap
                tv.setOnClickListener {
                    val face = tv.text?.toString().orEmpty()
                    MoodStore.add(this, face)
                    Toast.makeText(this, "Mood saved $face", Toast.LENGTH_SHORT).show()
                    refreshMoodTrend()
                }

                // Long-press to open AddMood with pre-selected emoji and optional note
                tv.setOnLongClickListener {
                    val face = tv.text?.toString().orEmpty()
                    startActivity(
                        Intent(this, AddMoodActivity::class.java)
                            .putExtra(AddMoodActivity.EXTRA_PRESELECT_EMOJI, face)
                    )
                    true
                }
            }
        }

        // Open full Add Mood screen
        findViewById<TextView>(R.id.btnAddMood)?.setOnClickListener {
            startActivity(Intent(this, AddMoodActivity::class.java))
        }

        // Mood history
        findViewById<TextView>(R.id.btnMoodHistory)?.setOnClickListener {
            startActivity(Intent(this, MoodHistoryActivity::class.java))
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
        refreshMoodTrend()
    }

    override fun onResume() {
        super.onResume()
        refreshHomeStats()
        refreshHomePreview()
        updateHydrationRow()
        refreshMoodTrend()
    }

    private fun updateHydrationRow() {
        val enabled = HydrationPrefs.isEnabled(this)
        switchHydration?.isChecked = enabled
        hydrationSub?.text = if (enabled) {
            HydrationScheduler.nextLabel(this)
        } else {
            "Reminders off"
        }
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

    private fun refreshMoodTrend() {
        val tv = findViewById<TextView>(R.id.txtMoodTrend) ?: return
        val avg = MoodStore.averageLast7(this)
        tv.text = if (avg == null) {
            "📊 Weekly Mood Trend • No data yet"
        } else {
            val face = MoodStore.emojiForScore(avg)
            val label = String.format("%.1f", avg)
            "📊 Weekly Mood Trend • $face  $label / 5"
        }
    }

    // ---------------- Adapter (Home preview) ----------------
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

        inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val icon: TextView = v.findViewById(R.id.habitIcon)
            val title: TextView = v.findViewById(R.id.txtTitle)
            val progressLabel: TextView = v.findViewById(R.id.txtProgress)
            val bar: LinearProgressIndicator = v.findViewById(R.id.progressBar)
            val btnPlus: View = v.findViewById(R.id.btnIncrement)
            val chkDone: CheckBox = v.findViewById(R.id.chkDone)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_habit, parent, false)
            return Holder(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: Holder, position: Int) {
            val item = items[position]

            h.icon.text = emojiFor(item.title)
            h.title.text = item.title

            val goal = item.goalPerDay.coerceAtLeast(1)
            val prog = item.progressToday.coerceIn(0, goal)
            h.progressLabel.text = "$prog/$goal today"

            val pct = ((prog.toFloat() / goal) * 100).toInt()
            h.bar.max = 100
            h.bar.progress = pct

            if (item.type == HabitType.COUNT) {
                h.btnPlus.visibility = View.VISIBLE
                h.chkDone.visibility = View.GONE
                h.btnPlus.setOnClickListener { onIncrement(item.id) }
            } else {
                h.btnPlus.visibility = View.GONE
                h.chkDone.visibility = View.VISIBLE
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
