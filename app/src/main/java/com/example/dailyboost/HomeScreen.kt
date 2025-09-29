package com.example.dailyboost

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View // ADDED: for findViewById<View>
import android.widget.CheckBox
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial

class HomeScreen : AppCompatActivity() {

    private lateinit var homeRvAdapter: HomeHabitsAdapter

    // Hydration UI refs
    private var switchHydration: SwitchMaterial? = null
    private var hydrationSub: TextView? = null

    // Runtime permission (Android 13+)
    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            HydrationScheduler.enable(this, HydrationPrefs.intervalMinutes(this))
            updateHydrationRow()
            Toast.makeText(this, "Hydration reminders enabled", Toast.LENGTH_SHORT).show()

            // DEV: quick test notification after 30s (remove later if you want)
            HydrationTest.scheduleTestReminder(this, 30)
            Toast.makeText(this, "Test reminder in 30s (dev only)", Toast.LENGTH_SHORT).show()
        } else {
            switchHydration?.isChecked = false
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

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

        // Open Hydration settings when the whole card is tapped
        findViewById<View>(R.id.cardHydration).setOnClickListener {
            startActivity(Intent(this, HydrationSettingsActivity::class.java))
        }

        // ===== Hydration: init channel, refs, and toggle =====
        NotificationHelper.createChannel(this)
        switchHydration = findViewById(R.id.switchHydration)
        hydrationSub = findViewById(R.id.hydrationSub)

        // Apply saved state
        switchHydration?.isChecked = HydrationPrefs.isEnabled(this)
        updateHydrationRow()

        // Toggle behavior
        switchHydration?.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                // Android 13+ needs POST_NOTIFICATIONS permission
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

                    // DEV: quick test notification after 30s (remove later if you want)
                    HydrationTest.scheduleTestReminder(this, 30)
                    Toast.makeText(this, "Test reminder in 30s", Toast.LENGTH_SHORT).show()
                }
            } else {
                HydrationScheduler.disable(this)
                updateHydrationRow()
                Toast.makeText(this, "Hydration reminders disabled", Toast.LENGTH_SHORT).show()
            }
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
        updateHydrationRow() // updates subtitle after returning from settings
    }

    // ---- Hydration helpers ----
    private fun updateHydrationRow() {
        val enabled = HydrationPrefs.isEnabled(this)
        switchHydration?.isChecked = enabled
        hydrationSub?.text = if (enabled) {
            HydrationScheduler.nextLabel(this)
        } else {
            "Reminders off"
        }
    }

    // ---- Existing code below ----
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
