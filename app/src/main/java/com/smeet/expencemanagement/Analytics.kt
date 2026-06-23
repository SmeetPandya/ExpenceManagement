package com.smeet.expencemanagement

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.MaterialDatePicker
import com.smeet.expencemanagement.model.Expence
import com.smeet.expencemanagement.model.ExpenseDatabase
import com.smeet.expencemanagement.repository.ExpenseRepository
import com.smeet.expencemanagement.viewmodel.ExpenseViewModel
import com.smeet.expencemanagement.viewmodel.ExpenseViewModelFactory

class Analytics : AppCompatActivity() {

    private lateinit var viewModel: ExpenseViewModel
    private var selectedStartDate: Long = System.currentTimeMillis() - 604800000L
    private var selectedEndDate: Long = System.currentTimeMillis()
    private var myExpenses: List<com.smeet.expencemanagement.model.Expence> = emptyList()

    private lateinit var sharedPreference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        val database = ExpenseDatabase.getDatabase(this)
        val repository = ExpenseRepository(database.expenseDao(),database.ScheduledBillDao())
        val factory = ExpenseViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(ExpenseViewModel::class.java)

        val dateRangeButton = findViewById<ImageButton>(R.id.btnDateRange)
        val barChart = findViewById<BarChart>(R.id.BarChart)
        val dropdown = findViewById<AutoCompleteTextView>(R.id.dropdownPieFilter)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        val pieOption=arrayOf("This Week" , "This Month", "This Year" , "Overall")

        val dropdownAdapter= ArrayAdapter(this,android.R.layout.simple_dropdown_item_1line,pieOption)
        dropdown.setAdapter(dropdownAdapter)

        dropdown.setText("This Month",false)

        dropdown.setOnItemClickListener { parent, _, position, _ ->
            val selectedFilter=parent.getItemAtPosition(position).toString()

            val filteredList=filterExpensesForPieChart(selectedFilter,myExpenses)

            updatePieChart(filteredList)
        }


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
                R.id.nav_reminders -> {
                    startActivity(android.content.Intent(applicationContext, Reminder::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        barChart.setNoDataText("No expenses for these dates ")
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        barChart.setNoDataTextColor(if (isDarkMode) Color.WHITE else Color.BLACK)

        viewModel.allExpenses.asLiveData().observe(this) { expenses ->
            if (expenses != null) {
                myExpenses = expenses
                updateBarChart(myExpenses)

                val initialFilter = dropdown.text.toString()
                val filteredList = filterExpensesForPieChart(initialFilter, myExpenses)
                updatePieChart(filteredList)
            }
        }

        dateRangeButton.setOnClickListener {
            val builder = MaterialDatePicker.Builder.dateRangePicker()
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

    private fun filterExpensesForPieChart(filterType: String,allExpence: List<com.smeet.expencemanagement.model.Expence>):List<com.smeet.expencemanagement.model.Expence>{
        val calendar=java.util.Calendar.getInstance()
        val currentTime=calendar.timeInMillis

        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        val startTime=when(filterType){
            "This Week" ->{
                calendar.set(java.util.Calendar.DAY_OF_WEEK,calendar.firstDayOfWeek)
                calendar.timeInMillis
            }
            "This Month" ->{
                calendar.set(java.util.Calendar.DAY_OF_MONTH,1)
                calendar.timeInMillis
            }
            "This Year" ->{
                calendar.set(java.util.Calendar.DAY_OF_YEAR,1)
                calendar.timeInMillis
            }
            "Overall" ->{
                return allExpence
            }
            else -> 0L
        }

        return allExpence.filter { it.date in startTime..currentTime }
    }

    private fun updatePieChart(filteredExpence: List<com.smeet.expencemanagement.model.Expence>){
        val categoryPiles=filteredExpence.groupBy { it.category }

        val categoryTotal=categoryPiles.mapValues { pile->
            pile.value.sumOf { it.amount.toDouble() }
        }

        val pieChart = findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.PieChart)
        val sharedPreferences = getSharedPreferences("ExpensePref", MODE_PRIVATE)
        val saveCurrency = sharedPreferences.getString("currencySymbole", "₹") ?: "₹"

        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK

        pieChart.setNoDataText("No Data Found")
        pieChart.setNoDataTextColor(textColor)

        val entries= ArrayList<com.github.mikephil.charting.data.PieEntry>()
        for((categoryName,total) in categoryTotal){
            if(total>0){
                entries.add(com.github.mikephil.charting.data.PieEntry(total.toFloat(),categoryName))
            }
        }

        if(entries.isEmpty()){
            pieChart.clear()
            return
        }

        val dataSet=com.github.mikephil.charting.data.PieDataSet(entries,"")

        val colors = ArrayList<Int>()
        colors.addAll(com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS.toList())
        colors.addAll(com.github.mikephil.charting.utils.ColorTemplate.PASTEL_COLORS.toList())
        dataSet.colors = colors

        dataSet.setDrawValues(true)
        dataSet.valueTextSize = 10f

        dataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getPieLabel(value: Float, pieEntry: com.github.mikephil.charting.data.PieEntry?): String {
                return if (value % 1 == 0f) "$saveCurrency${value.toInt()}" else "$saveCurrency$value"
            }
        }

        pieChart.data = com.github.mikephil.charting.data.PieData(dataSet)

        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = true
        pieChart.legend.textColor = textColor
        pieChart.legend.isWordWrapEnabled = true
        pieChart.legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
        pieChart.legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
        pieChart.legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
        pieChart.legend.yEntrySpace = 8f
        pieChart.setExtraOffsets(0f, 0f, 0f, 5f)
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 38f
        pieChart.transparentCircleRadius = 55f
        pieChart.setHoleColor(android.graphics.Color.TRANSPARENT)

        val totalSpent = categoryTotal.values.sum()
        val formattedTotal = if (totalSpent % 1 == 0.0) "${totalSpent.toInt()}" else "$totalSpent"
        pieChart.centerText = "Total Spent\n$saveCurrency$formattedTotal"
        pieChart.setCenterTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary))
        pieChart.setCenterTextSize(10f)

        pieChart.setDrawEntryLabels(false)

        pieChart.notifyDataSetChanged()

        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    @android.annotation.SuppressLint("UseKtx")
    private fun updateBarChart(expenses: List<com.smeet.expencemanagement.model.Expence>) {
        val barChart = findViewById<com.github.mikephil.charting.charts.BarChart>(R.id.BarChart)

        val sharedPreferences = getSharedPreferences("ExpensePref", MODE_PRIVATE)
        val dailyBudgetLimit = sharedPreferences.getInt("dailyBudget", 500).toFloat()
        val saveCurrency=sharedPreferences.getString("currencySymbole","₹")?:"₹"

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
        dataSet.valueTextSize = 10f
        dataSet.valueTextColor = textColor
        dataSet.valueTypeface = cleanFont

        dataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getBarLabel(barEntry: com.github.mikephil.charting.data.BarEntry?): String {
                val value = barEntry?.y ?: 0f
                return if (value % 1 == 0f) "$saveCurrency${value.toInt()}" else "$saveCurrency$value"
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

        val limitLine=com.github.mikephil.charting.components.LimitLine(dailyBudgetLimit, "")
        limitLine.lineWidth = 2f
        limitLine.enableDashedLine(15f, 10f, 0f)
        limitLine.lineColor = overBudgetColor
        axisLeft.addLimitLine(limitLine)
        // 3. Add 20% extra empty space to the top of the chart for breathing room
        axisLeft.spaceTop = 20f

        axisLeft.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                return if (value % 1 == 0f) "₹${value.toInt()}" else "$saveCurrency$value"
            }
        }

        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.setDrawBorders(false)

        val legend=barChart.legend
        legend.isEnabled=true

        val limitEntry = com.github.mikephil.charting.components.LegendEntry()
        limitEntry.label = "Daily Limit (₹${dailyBudgetLimit.toInt()})"
        limitEntry.formColor = overBudgetColor
        limitEntry.form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
        limitEntry.formLineWidth = 2f
        limitEntry.formLineDashEffect = android.graphics.DashPathEffect(floatArrayOf(15f, 10f), 0f)

        legend.setCustom(listOf(limitEntry))

        legend.textColor = textColor
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
        legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)

        barChart.setExtraOffsets(0f, 0f, 0f, 10f)

        barChart.animateY(1000)
        barChart.invalidate()
    }
}