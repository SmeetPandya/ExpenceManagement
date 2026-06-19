package com.smeet.expencemanagement

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.smeet.expencemanagement.model.ExpenseDatabase
import com.smeet.expencemanagement.repository.ExpenseRepository
import com.smeet.expencemanagement.viewmodel.ExpenseViewModel
import com.smeet.expencemanagement.viewmodel.ExpenseViewModelFactory

class Analytics : AppCompatActivity() {

    private lateinit var viewModel: ExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_analytics)

        val database= ExpenseDatabase.getDatabase(this)
        val repository= ExpenseRepository(database.expenseDao())
        val factory= ExpenseViewModelFactory(repository)
        viewModel= ViewModelProvider(this,factory).get(ExpenseViewModel::class.java)

        val dateRangeButton=findViewById<ImageButton>(R.id.btnDateRange)
        val barChart=findViewById<com.github.mikephil.charting.charts.BarChart>(R.id.BarChart)
        val pieChart=findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.PieChart)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}