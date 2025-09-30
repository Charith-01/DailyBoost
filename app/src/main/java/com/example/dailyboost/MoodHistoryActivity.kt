package com.example.dailyboost

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.Calendar
import java.util.Locale

class MoodHistoryActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var emptyView: View
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipToday: Chip
    private lateinit var chipWeek: Chip
    private lateinit var chipAll: Chip

    private val adapter = MoodAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mood_history)

        // Toolbar (back arrow)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mood History"

        // Insets for the app bar and list
        val appBar = findViewById<View>(R.id.appBar)
        ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
            insets
        }
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, nb.bottom)
            insets
        }

        rv = findViewById(R.id.rvMoods)
        emptyView = findViewById(R.id.emptyView)
        chipGroup = findViewById(R.id.chipGroupFilters)
        chipToday = findViewById(R.id.chipToday)
        chipWeek = findViewById(R.id.chipWeek)
        chipAll = findViewById(R.id.chipAll)

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        // Default filter: Today
        chipToday.isChecked = true
        chipGroup.setOnCheckedStateChangeListener { _, _ -> applyFilter() }

        // First load
        applyFilter()
    }

    override fun onResume() {
        super.onResume()
        applyFilter()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun applyFilter() {
        val all = MoodStore.listAll(this)
        val filtered = when {
            chipToday.isChecked -> all.filter { isSameDay(it.timestamp, System.currentTimeMillis()) }
            chipWeek.isChecked  -> all.filter { isWithinLastDays(it.timestamp, 7) }
            else                -> all
        }
        adapter.submit(filtered)
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        // Optional: update subtitle with count
        val count = filtered.size
        val subtitle = when {
            chipToday.isChecked -> "Today • $count entr" + if (count == 1) "y" else "ies"
            chipWeek.isChecked  -> "Last 7 days • $count entr" + if (count == 1) "y" else "ies"
            else                -> "All time • $count entr" + if (count == 1) "y" else "ies"
        }
        findViewById<TextView>(R.id.subtitle).text = subtitle
    }

    private fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = ts1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = ts2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isWithinLastDays(ts: Long, days: Int): Boolean {
        val c = Calendar.getInstance()
        val now = c.timeInMillis
        c.add(Calendar.DAY_OF_YEAR, -days + 1) // include today
        val start = startOfDay(c.timeInMillis)
        return ts >= start && ts <= now
    }

    private fun startOfDay(ts: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    // ---------------- Adapter ----------------
    private inner class MoodAdapter : RecyclerView.Adapter<MoodAdapter.Holder>() {
        private val items = mutableListOf<MoodEntry>()
        private val timeFmt by lazy { DateFormat.getTimeFormat(this@MoodHistoryActivity) }
        private val dateFmt by lazy {
            android.text.format.DateFormat.getMediumDateFormat(this@MoodHistoryActivity)
        }

        fun submit(list: List<MoodEntry>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val emoji: TextView = v.findViewById(R.id.txtEmoji)
            val line1: TextView = v.findViewById(R.id.txtLine1) // time or date • time
            val line2: TextView = v.findViewById(R.id.txtLine2) // note or day label
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_mood_entry, parent, false)
            return Holder(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: Holder, position: Int) {
            val e = items[position]
            h.emoji.text = e.emoji

            val now = System.currentTimeMillis()
            val isToday = isSameDay(e.timestamp, now)

            val cal = Calendar.getInstance().apply { timeInMillis = e.timestamp }
            val timeText = timeFmt.format(cal.time)

            h.line1.text = if (isToday) {
                timeText
            } else {
                val date = dateFmt.format(cal.time)
                "$date • $timeText"
            }

            h.line2.text = when {
                !e.note.isNullOrBlank() -> e.note
                isToday -> "Today"
                isWithinLastDays(e.timestamp, 7) -> dayName(cal)
                else -> "Earlier"
            }
        }

        private fun dayName(c: Calendar): String {
            // e.g., Mon, Tue (locale-aware)
            return c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: ""
        }
    }
}
