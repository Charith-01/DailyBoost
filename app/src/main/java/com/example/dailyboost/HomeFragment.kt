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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.util.Calendar
import java.util.Locale
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class HomeFragment : Fragment() {

    private lateinit var homeRvAdapter: HomeHabitsAdapter
    private var switchHydration: SwitchMaterial? = null
    private var hydrationSub: TextView? = null

    // ▶️ Music UI references
    private var musicNow: TextView? = null
    private var musicToggle: TextView? = null
    private var pulse: LinearProgressIndicator? = null
    private var btnPrev: TextView? = null
    private var btnIconPlay: TextView? = null
    private var btnNext: TextView? = null

    // ▶️ ExoPlayer instance
    private var player: ExoPlayer? = null
    private var currentStreamName: String? = null

    // ▶️ Local RAW streams (order used for prev/next)
    // Place files in: app/src/main/res/raw/  -> lofi.mp3, nature.mp3, rain.mp3, piano.mp3
    private val streams = linkedMapOf(
        "Lo-Fi" to R.raw.lofi,
        "Nature" to R.raw.nature,
        "Rain" to R.raw.rain,
        "Piano" to R.raw.piano
    )
    private val streamKeys by lazy { streams.keys.toList() }

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
        super.onViewCreated(view, savedInstanceState)

        // Status bar styling
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.primary)
        WindowCompat.getInsetsController(requireActivity().window, view)
            ?.isAppearanceLightStatusBars = false

        // Dynamic greeting
        val greetingView = view.findViewById<TextView>(R.id.greeting)
        val user = AuthPrefs.getUser(requireContext())
        val name = user?.fullName?.trim().takeUnless { it.isNullOrEmpty() } ?: "User"
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingWord = when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Hello"
        }
        greetingView.text = "$greetingWord, $name 👋"

        // Handle top inset
        val appBar = view.findViewById<View>(R.id.appBar)
        ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // (FAB removed — no fabAdd code)

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
        // QUICK MOOD
        // -----------------------------
        view.findViewById<LinearLayout>(R.id.moodChips)?.let { row ->
            for (i in 0 until row.childCount) {
                val tv = row.getChildAt(i) as? TextView ?: continue

                // Quick save on tap
                tv.setOnClickListener {
                    val face = tv.text?.toString().orEmpty()
                    MoodStore.add(requireContext(), face)
                    Toast.makeText(requireContext(), "Mood saved $face", Toast.LENGTH_SHORT).show()
                    refreshMoodTrend()
                    renderComposeChart()
                }

                // Long press → open Mood tab
                tv.setOnLongClickListener {
                    (activity as? MainActivity)?.switchToTab(R.id.tab_mood)
                    true
                }
            }
        }

        // Mood history
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

        // -----------------------------
        // 🎵 Stress-release Music (local)
        // -----------------------------
        musicNow = view.findViewById(R.id.txtMusicNow)
        musicToggle = view.findViewById(R.id.btnOpenPlayer)
        pulse = view.findViewById(R.id.progressPulse)
        btnPrev = view.findViewById(R.id.btnPrev)
        btnIconPlay = view.findViewById(R.id.btnIconPlay)
        btnNext = view.findViewById(R.id.btnNext)

        initPlayer()

        view.findViewById<View>(R.id.cardMusic)?.setOnClickListener { togglePlayPause() }

        // Emoji chips → start tracks
        view.findViewById<TextView>(R.id.chipLoFi)?.setOnClickListener { startStream("Lo-Fi") }
        view.findViewById<TextView>(R.id.chipNature)?.setOnClickListener { startStream("Nature") }
        view.findViewById<TextView>(R.id.chipRain)?.setOnClickListener { startStream("Rain") }
        view.findViewById<TextView>(R.id.chipPiano)?.setOnClickListener { startStream("Piano") }

        // Transport controls
        musicToggle?.setOnClickListener { togglePlayPause() }
        btnIconPlay?.setOnClickListener { togglePlayPause() }
        btnPrev?.setOnClickListener { prevStream() }
        btnNext?.setOnClickListener { nextStream() }
    }

    override fun onResume() {
        super.onResume()
        refreshHomeStats()
        refreshHomePreview()
        updateHydrationRow()
        refreshMoodTrend()
        renderComposeChart()

        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.primary)
        WindowCompat.getInsetsController(requireActivity().window, view ?: return)
            ?.isAppearanceLightStatusBars = false

        view?.findViewById<TextView>(R.id.greeting)?.let { tv ->
            val user = AuthPrefs.getUser(requireContext())
            val name = user?.fullName?.trim().takeUnless { it.isNullOrEmpty() } ?: "User"
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greetingWord = when (hour) {
                in 5..11 -> "Good Morning"
                in 12..16 -> "Good Afternoon"
                in 17..21 -> "Good Evening"
                else -> "Hello"
            }
            tv.text = "$greetingWord, $name 👋"
        }
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
        updateMusicToggleLabel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        musicNow = null
        musicToggle = null
        pulse = null
        btnPrev = null
        btnIconPlay = null
        btnNext = null
    }

    // ----------------- Home stats / hydration / mood -----------------

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
            val label = String.format(Locale.getDefault(), "%.1f", avg)
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
        composeView.setContent { MoodBarChart7Day(data) }
    }

    // ---------- Compose chart (LINE chart) ----------
    @Composable
    private fun MoodBarChart7Day(data: List<MoodEntry>) {
        val days = 7
        val labels = mutableListOf<String>()
        val scores = FloatArray(days) { 0f }

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

        repeat(days) { i ->
            scores[i] = byDay[keys[i]]?.let { it.sum().toFloat() / it.size } ?: 0f
        }

        val primary = colorResource(id = R.color.primary)
        val gridColor = Color(0xFFE9E5F5)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Chart area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid lines
                    val levels = floatArrayOf(0f, 2.5f, 5f)
                    levels.forEach { lvl ->
                        val y = h - (lvl / 5f) * h
                        drawLine(
                            color = gridColor,
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(w, y),
                            strokeWidth = 2f
                        )
                    }

                    val step = if (days > 1) w / (days - 1) else 0f

                    // Line path
                    val path = Path()
                    for (i in 0 until days) {
                        val x = step * i
                        val y = h - (scores[i] / 5f).coerceIn(0f, 1f) * h
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(path = path, color = primary, style = Stroke(width = 6f))

                    // Points
                    for (i in 0 until days) {
                        val x = step * i
                        val y = h - (scores[i] / 5f).coerceIn(0f, 1f) * h
                        drawCircle(
                            color = primary,
                            radius = 8f,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(days) { i ->
                    Text(text = labels[i], style = MaterialTheme.typography.labelSmall)
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
            title.contains("meditation", true) -> "🧘"
            else -> "✅"
        }
    }

    // -------------------- MUSIC (ExoPlayer) helpers --------------------
    private fun initPlayer() {
        if (player != null) return
        player = ExoPlayer.Builder(requireContext()).build().also { p ->
            p.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateMusicToggleLabel()
                }
            })
        }
        updateMusicToggleLabel()
        musicNow?.text = "Pick a sound to start"
        setSelectedChip(null)
    }

    private fun startStream(name: String) {
        val resId = streams[name] ?: return
        val uri = "android.resource://${requireContext().packageName}/$resId"

        val p = player ?: run { initPlayer(); player!! }
        val mediaItem = MediaItem.fromUri(uri)
        p.setMediaItem(mediaItem)
        p.prepare()
        p.playWhenReady = true

        currentStreamName = name
        musicNow?.text = "Now playing • $name"
        setSelectedChip(name)
        updateMusicToggleLabel()
        Toast.makeText(requireContext(), "Playing $name", Toast.LENGTH_SHORT).show()
    }

    private fun togglePlayPause() {
        val p = player ?: run { initPlayer(); player!! }
        if (p.mediaItemCount == 0) {
            startStream("Lo-Fi")
            return
        }
        p.playWhenReady = !p.isPlaying
        updateMusicToggleLabel()
    }

    private fun prevStream() {
        if (streamKeys.isEmpty()) return
        val idx = currentStreamName?.let { streamKeys.indexOf(it) } ?: 0
        val prev = if (idx <= 0) streamKeys.last() else streamKeys[idx - 1]
        startStream(prev)
    }

    private fun nextStream() {
        if (streamKeys.isEmpty()) return
        val idx = currentStreamName?.let { streamKeys.indexOf(it) } ?: -1
        val next = if (idx >= streamKeys.lastIndex) streamKeys.first() else streamKeys[idx + 1]
        startStream(next)
    }

    private fun updateMusicToggleLabel() {
        val p = player
        val label = when {
            p == null || p.mediaItemCount == 0 -> "Play"
            p.isPlaying -> "Pause"
            else -> "Play"
        }
        musicToggle?.text = label
        btnIconPlay?.text = if (label == "Pause") "⏸" else "▶"
        pulse?.visibility = if (p?.isPlaying == true) View.VISIBLE else View.GONE
    }

    // Highlight the selected chip
    private fun setSelectedChip(name: String?) {
        val chips = listOf(
            view?.findViewById<TextView>(R.id.chipLoFi) to "Lo-Fi",
            view?.findViewById<TextView>(R.id.chipNature) to "Nature",
            view?.findViewById<TextView>(R.id.chipRain) to "Rain",
            view?.findViewById<TextView>(R.id.chipPiano) to "Piano"
        )
        chips.forEach { (chip, label) ->
            chip ?: return@forEach
            if (label == name) {
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
            } else {
                chip.setBackgroundResource(R.drawable.bg_mood_chip)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
        }
    }
}
