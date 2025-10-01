package com.example.dailyboost

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var homeRvAdapter: HomeHabitsAdapter
    private var switchHydration: SwitchMaterial? = null
    private var hydrationSub: TextView? = null

    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val ctx = requireContext()
        if (granted) {
            HydrationScheduler.enable(ctx, HydrationPrefs.intervalMinutes(ctx))
            updateHydrationRow()
            Toast.makeText(ctx, "Hydration reminders enabled", Toast.LENGTH_SHORT).show()
            HydrationTest.scheduleTestReminder(ctx, 30)
        } else {
            switchHydration?.isChecked = false
            Toast.makeText(ctx, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_home_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Status bar styling matches AppBar
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.primary)
        WindowCompat.getInsetsController(requireActivity().window, view)
            ?.isAppearanceLightStatusBars = false

        // TOP inset -> pad the AppBar below the cutout/status bar
        val appBar = view.findViewById<View>(R.id.appBar)
        ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // --- Navigation shortcuts ---
        val fab = view.findViewById<View>(R.id.fabAdd)
        fab.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditHabitActivity::class.java))
        }
        view.findViewById<TextView>(R.id.btnSeeAllHabits)?.setOnClickListener {
            (activity as? MainActivity)?.switchToTab(R.id.tab_habits)
        }
        view.findViewById<View>(R.id.cardHydration).setOnClickListener {
            startActivity(Intent(requireContext(), HydrationSettingsActivity::class.java))
        }

        // Hydration
        NotificationHelper.createChannel(requireContext())
        switchHydration = view.findViewById(R.id.switchHydration)
        hydrationSub = view.findViewById(R.id.hydrationSub)
        switchHydration?.isChecked = HydrationPrefs.isEnabled(requireContext())
        updateHydrationRow()

        switchHydration?.setOnCheckedChangeListener { _, checked ->
            val ctx = requireContext()
            if (checked) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    HydrationScheduler.enable(ctx, HydrationPrefs.intervalMinutes(ctx))
                    updateHydrationRow()
                    Toast.makeText(ctx, "Hydration reminders enabled", Toast.LENGTH_SHORT).show()
                    HydrationTest.scheduleTestReminder(ctx, 30)
                }
            } else {
                HydrationScheduler.disable(ctx)
                updateHydrationRow()
                Toast.makeText(ctx, "Hydration reminders disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // -----------------------------
        // QUICK MOOD: "Add mood" removed as requested
        // -----------------------------

        // Keep "See all" working → open MoodHistoryActivity
        view.findViewById<TextView>(R.id.btnMoodHistory)?.setOnClickListener {
            startActivity(Intent(requireContext(), MoodHistoryActivity::class.java))
        }

        // Habits preview list
        val rvHome = view.findViewById<RecyclerView>(R.id.rvHabitsToday)
        rvHome.layoutManager = LinearLayoutManager(requireContext())
        homeRvAdapter = HomeHabitsAdapter(
            onIncrement = { id ->
                HabitStore.incrementCount(requireContext(), id)
                refreshHomeStats(); refreshHomePreview()
            },
            onToggle = { id, done ->
                HabitStore.setYesNo(requireContext(), id, done)
                refreshHomeStats(); refreshHomePreview()
            },
            onOpen = { id ->
                startActivity(
                    Intent(requireContext(), AddEditHabitActivity::class.java)
                        .putExtra(AddEditHabitActivity.EXTRA_HABIT_ID, id)
                )
            }
        )
        rvHome.adapter = homeRvAdapter

        // Initial data
        refreshHomeStats()
        refreshHomePreview()
        refreshMoodTrend()
        renderComposeChart()
    }

    override fun onResume() {
        super.onResume()
        refreshHomeStats()
        refreshHomePreview()
        updateHydrationRow()
        refreshMoodTrend()
        renderComposeChart()

        // Keep status bar style consistent
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.primary)
        WindowCompat.getInsetsController(requireActivity().window, view ?: return)
            ?.isAppearanceLightStatusBars = false
    }

    private fun updateHydrationRow() {
        val ctx = requireContext()
        val enabled = HydrationPrefs.isEnabled(ctx)
        switchHydration?.isChecked = enabled
        hydrationSub?.text = if (enabled) HydrationScheduler.nextLabel(ctx) else "Reminders off"
    }

    private fun refreshHomeStats() {
        val ctx = requireContext()
        HabitStore.resetIfNewDay(ctx)

        val percent = HabitStore.computeTodayPercent(ctx)
        view?.findViewById<LinearProgressIndicator>(R.id.progressToday)
            ?.apply { max = 100; progress = percent }

        val (done, total) = HabitStore.todayCounts(ctx)
        view?.findViewById<TextView>(R.id.todayRightLabel)?.text = "$done/$total habits"

        val streak = HabitStore.getStreak(ctx)
        val stats = view?.findViewById<TextView>(R.id.txtTodayStats)
        stats?.text = when {
            total == 0 -> "Add your first habit to begin"
            streak > 0 -> "🔥 $streak-day streak • Keep going!"
            else -> if (done == total && total > 0)
                "🎉 All habits done today! Streak starts tomorrow"
            else "🔥 Keep going!"
        }
    }

    private fun refreshHomePreview() {
        val topHabits = HabitStore.loadHabits(requireContext())
            .filter { it.isActive }
            .take(10)
        homeRvAdapter.submit(topHabits)
    }

    private fun refreshMoodTrend() {
        val tv = view?.findViewById<TextView>(R.id.txtMoodTrend) ?: return
        val avg = MoodStore.averageLast7(requireContext())
        tv.text = if (avg == null) {
            "📊 Weekly Mood Trend • No data yet"
        } else {
            val face = MoodStore.emojiForScore(avg)
            val label = String.format("%.1f", avg)
            "📊 Weekly Mood Trend • $face  $label / 5"
        }
    }

    /** Renders the 7-day mood chart into the ComposeView. */
    private fun renderComposeChart() {
        val composeView =
            view?.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.moodComposeView)
                ?: return

        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        val data = MoodStore.listAll(requireContext())
        composeView.setContent {
            MoodBarChart7Day(data)
        }
    }

    // ---------- Compose chart ----------
    @Composable
    private fun MoodBarChart7Day(data: List<MoodEntry>) {
        val days = 7
        val labels = mutableListOf<String>()
        val scores = FloatArray(days) { 0f }

        // Window & labels (oldest -> newest)
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
        val keys = IntArray(days)
        repeat(days) { i ->
            keys[i] = cal.get(Calendar.YEAR) * 10000 +
                    (cal.get(Calendar.MONTH) + 1) * 100 +
                    cal.get(Calendar.DAY_OF_MONTH)
            labels.add(dayFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Bucket moods to day
        val byDay = HashMap<Int, MutableList<Int>>()
        for (e in data) {
            val c = Calendar.getInstance().apply { timeInMillis = e.timestamp }
            val k = c.get(Calendar.YEAR) * 10000 +
                    (c.get(Calendar.MONTH) + 1) * 100 +
                    c.get(Calendar.DAY_OF_MONTH)
            if (keys.contains(k)) {
                byDay.getOrPut(k) { mutableListOf() }.add(MoodStore.scoreForEmoji(e.emoji))
            }
        }

        // Average per day (0..5)
        repeat(days) { i ->
            scores[i] = byDay[keys[i]]?.let { it.sum().toFloat() / it.size } ?: 0f
        }

        val primary = colorResource(id = R.color.primary)
        val emptyBar = Color(0xFFE9E5F5)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(days) { i ->
                    val h = (scores[i] / 5f).coerceIn(0f, 1f)
                    val barHeight = (h * 140f).dp
                    val barColor = if (h == 0f) emptyBar else primary

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .background(barColor)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = labels[i],
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    // -------- Adapter (Home preview) --------
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

            h.bar.max = 100
            h.bar.progress = ((prog.toFloat() / goal) * 100).toInt()

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
            title.contains("gym", true) -> "🏋️"
            title.contains("read", true) -> "📚"
            title.contains("walk", true) -> "🚶"
            title.contains("meditat", true) -> "🧘"
            else -> "✅"
        }
    }
}
