package com.example.dailyboost

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class HabitsActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: HabitAdapter
    private var habits = mutableListOf<Habit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_habits)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        rv = findViewById(R.id.rvHabits)

        // Keep toolbar readable (opaque status bar over brand color)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, null)

        ViewCompat.setOnApplyWindowInsetsListener(rv) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = v.paddingBottom + sys.bottom)
            insets
        }

        rv.layoutManager = LinearLayoutManager(this)
        habits = HabitStore.loadHabits(this)

        adapter = HabitAdapter(
            items = habits,
            onIncrement = { id ->
                HabitStore.incrementCount(this, id)
                refresh()
            },
            onToggle = { id, done ->
                HabitStore.setYesNo(this, id, done)
                refresh()
            },
            onOpen = { id ->
                startActivity(
                    Intent(this, AddEditHabitActivity::class.java)
                        .putExtra(AddEditHabitActivity.EXTRA_HABIT_ID, id)
                )
            }
        )
        rv.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddHabit).setOnClickListener {
            startActivity(Intent(this, AddEditHabitActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        habits.clear()
        habits.addAll(HabitStore.loadHabits(this))
        adapter.notifyDataSetChanged()
    }
}

private class HabitAdapter(
    private val items: List<Habit>,
    private val onIncrement: (String) -> Unit,
    private val onToggle: (String, Boolean) -> Unit,
    private val onOpen: (String) -> Unit
) : RecyclerView.Adapter<HabitAdapter.Holder>() {

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val icon: TextView = v.findViewById(R.id.habitIcon)
        val title: TextView = v.findViewById(R.id.txtTitle)
        val progressLabel: TextView = v.findViewById(R.id.txtProgress)
        val bar: LinearProgressIndicator = v.findViewById(R.id.progressBar)
        val btnPlus: View = v.findViewById(R.id.btnIncrement)   // <-- now View
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
