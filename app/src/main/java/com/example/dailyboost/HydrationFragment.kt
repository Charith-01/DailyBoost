package com.example.dailyboost

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HydrationFragment : Fragment() {

    private lateinit var data: HydrationData
    private lateinit var gauge: ArcGaugeView
    private lateinit var txtAmount: TextView
    private lateinit var txtGoal: TextView
    private lateinit var btnDrink: Button
    private lateinit var btnSettings: FloatingActionButton

    private val timeFmt = SimpleDateFormat("HH:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.activity_hydration_fragment, container, false)

        // Initialize stored hydration data
        data = HydrationData(requireContext())
        data.ensureNewDayReset()

        // Bind UI components
        gauge = v.findViewById(R.id.gauge)
        txtAmount = v.findViewById(R.id.txtAmount)
        txtGoal = v.findViewById(R.id.txtGoal)
        btnDrink = v.findViewById(R.id.btnDrink)
        btnSettings = v.findViewById(R.id.btnSettings)

        // Update UI for current totals
        bindTotals()

        // Add water button
        btnDrink.setOnClickListener {
            val addAmount = data.oneDrinkAmount
            data.totalToday = (data.totalToday + addAmount).coerceAtMost(data.goal)
            bindTotals()
            Toast.makeText(
                requireContext(),
                "Added $addAmount mL of water 💧",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Open hydration settings activity
        btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), HydrationSettingsActivity::class.java))
        }

        return v
    }

    override fun onResume() {
        super.onResume()
        // Ensure data is refreshed when returning from settings
        data.ensureNewDayReset()
        bindTotals()
    }

    // Update gauge and goal text
    private fun bindTotals() {
        txtAmount.text = "${data.totalToday}"
        txtGoal.text = "/${data.goal} mL"
        val percent = if (data.goal <= 0) 0 else (data.totalToday * 100 / data.goal)
        gauge.setProgress(percent, animate = true)
    }
}
