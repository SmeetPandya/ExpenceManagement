package com.smeet.expencemanagement

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smeet.expencemanagement.model.ExpenseDatabase
import com.smeet.expencemanagement.repository.ExpenseRepository
import com.smeet.expencemanagement.viewmodel.ExpenseViewModel
import com.smeet.expencemanagement.viewmodel.ExpenseViewModelFactory

class Analytics : AppCompatActivity() {

    private lateinit var viewModel: ExpenseViewModel
    private var selectedStartDate: Long = 0L
    private var selectedEndDate: Long = System.currentTimeMillis()
    private var myExpenses: List<com.smeet.expencemanagement.model.Expence> = emptyList()

    private lateinit var sharedPreference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        val database = ExpenseDatabase.getDatabase(this)
        val repository = ExpenseRepository(database.expenseDao())
        val factory = ExpenseViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(ExpenseViewModel::class.java)

        val dateRangeButton = findViewById<ImageButton>(R.id.btnDateRange)
        val barChart = findViewById<com.github.mikephil.charting.charts.BarChart>(R.id.BarChart)
        val pieChart = findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.PieChart)
        val dropdown = findViewById<AutoCompleteTextView>(R.id.dropdownPieFilter)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNavigationView.selectedItemId = R.id.nav_analytics
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_analytics -> true
                R.id.nav_home -> {
                    val intent = Intent(this, Home::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, Settings::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        barChart.setNoDataText("No expenses for these dates \uD83D\uDCB8")
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        barChart.setNoDataTextColor(if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK)

        viewModel.allExpenses.asLiveData().observe(this) { expenses ->
            if (expenses != null) {
                myExpenses = expenses
                updateBarChart(myExpenses)
            }
        }

        dateRangeButton.setOnClickListener {
            val builder = com.google.android.material.datepicker.MaterialDatePicker.Builder.dateRangePicker()
            builder.setTitleText("Select Dates")
            builder.setTheme(com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialCalendar)

            val picker = builder.build()

            picker.addOnPositiveButtonClickListener { selection ->
                selectedStartDate = selection.first
                selectedEndDate = selection.second
                updateBarChart(myExpenses)
            }
            picker.show(supportFragmentManager, "DATE_PICKER")
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_analytics
    }

    @android.annotation.SuppressLint("UseKtx")
    private fun updateBarChart(expenses: List<com.smeet.expencemanagement.model.Expence>) {
        val barChart = findViewById<com.github.mikephil.charting.charts.BarChart>(R.id.BarChart)

        val sharedPreferences = getSharedPreferences("ExpensePref", MODE_PRIVATE)
        val dailyBudgetLimit = sharedPreferences.getInt("dailyBudget", 500).toFloat()

        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        val gridColor = if (isDarkMode) android.graphics.Color.parseColor("#333333") else android.graphics.Color.parseColor("#E0E0E0")
        val cleanFont = android.graphics.Typeface.SANS_SERIF

        val adjustedEndDate = selectedEndDate + 86399999L

        val filteredExpenses = expenses.filter { expence ->
            expence.date in selectedStartDate..adjustedEndDate
        }

        val dateFormat = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
        val dailySums = filteredExpenses.groupBy { expence ->
            dateFormat.format(java.util.Date(expence.date))
        }.mapValues { entry ->
            entry.value.sumOf { it.amount.toDouble() }
        }

        val entries = ArrayList<com.github.mikephil.charting.data.BarEntry>()
        val labels = ArrayList<String>()
        val barColors = ArrayList<Int>()

        val normalColor = android.graphics.Color.parseColor("#5C6BC0")
        val overBudgetColor = android.graphics.Color.parseColor("#E53935")

        var index = 0f
        for ((dateString, total) in dailySums) {
            val totalFloat = total.toFloat()
            entries.add(com.github.mikephil.charting.data.BarEntry(index, totalFloat))
            labels.add(dateString)

            if (totalFloat > dailyBudgetLimit) {
                barColors.add(overBudgetColor)
            } else {
                barColors.add(normalColor)
            }

            index += 1f
        }

        if (entries.isEmpty()) {
            barChart.axisLeft.removeAllLimitLines()
            barChart.clear()
            return
        }

        val dataSet = com.github.mikephil.charting.data.BarDataSet(entries, "")
        dataSet.colors = barColors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = textColor
        dataSet.valueTypeface = cleanFont

        dataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getBarLabel(barEntry: com.github.mikephil.charting.data.BarEntry?): String {
                val value = barEntry?.y ?: 0f
                return if (value % 1 == 0f) "₹${value.toInt()}" else "₹$value"
            }
        }

        val barData = com.github.mikephil.charting.data.BarData(dataSet)
        barData.barWidth = 0.5f
        barChart.data = barData

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.isGranularityEnabled = true
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textColor = textColor
        xAxis.typeface = cleanFont

        val axisLeft = barChart.axisLeft
        axisLeft.axisMinimum = 0f
        axisLeft.textColor = textColor
        axisLeft.gridColor = gridColor
        axisLeft.setDrawAxisLine(false)
        axisLeft.typeface = cleanFont

        axisLeft.removeAllLimitLines()
        val limitLine = com.github.mikephil.charting.components.LimitLine(dailyBudgetLimit, "Daily Limit (₹${dailyBudgetLimit.toInt()})")
        limitLine.lineWidth = 2f
        limitLine.enableDashedLine(15f, 10f, 0f)
        limitLine.labelPosition = com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_TOP
        limitLine.textSize = 10f
        limitLine.textColor = textColor
        limitLine.lineColor = overBudgetColor
        axisLeft.addLimitLine(limitLine)

        axisLeft.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                return if (value % 1 == 0f) "₹${value.toInt()}" else "₹$value"
            }
        }

        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.setDrawBorders(false)
        barChart.legend.isEnabled = false // Completely disables the Legend

        barChart.animateY(1000)
        barChart.invalidate()
    }
}