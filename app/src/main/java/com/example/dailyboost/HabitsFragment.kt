package com.example.dailyboost

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class HabitsFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: HabitAdapter
    private val habits = mutableListOf<Habit>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_habits_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Keep status bar purple with light icons (like other screens)
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.primary)
        WindowCompat.getInsetsController(requireActivity().window, view)
            ?.isAppearanceLightStatusBars = false

        // TOP inset -> pad the AppBar so 58dp toolbar sits below status bar/cutout
        val appBar = view.findViewById<View>(R.id.appBar)
        ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, sb.top, v.paddingRight, v.paddingBottom)
            insets
        }

        rv = view.findViewById(R.id.rvHabits)

        // Bottom inset so items clear the gesture/nav bar & bottom nav
        ViewCompat.setOnApplyWindowInsetsListener(rv) { v, insets ->
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            // keeping your original additive approach
            v.updatePadding(bottom = v.paddingBottom + nb.bottom)
            insets
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        habits.clear()
        habits.addAll(HabitStore.loadHabits(requireContext()))

        adapter = HabitAdapter(
            items = habits,
            onIncrement = { id ->
                HabitStore.incrementCount(requireContext(), id)
                refresh()
            },
            onToggle = { id, done ->
                HabitStore.setYesNo(requireContext(), id, done)
                refresh()
            },
            onOpen = { id ->
                startActivity(
                    Intent(requireContext(), AddEditHabitActivity::class.java)
                        .putExtra(AddEditHabitActivity.EXTRA_HABIT_ID, id)
                )
            }
        )
        rv.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.fabAddHabit).setOnClickListener {
            startActivity(Intent(requireContext(), AddEditHabitActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        habits.clear()
        habits.addAll(HabitStore.loadHabits(requireContext()))
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
        val btnPlus: View = v.findViewById(R.id.btnIncrement)
        val chkDone: CheckBox = v.findViewById(R.id.chkDone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
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
            h.chkDone.setOnCheckedChangeListener { _, checked -> onToggle(item.id, checked) }
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
