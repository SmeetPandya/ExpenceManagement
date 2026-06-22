package com.smeet.expencemanagement

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.smeet.expencemanagement.model.ExpenseDatabase
import com.smeet.expencemanagement.model.ScheduledBill
import com.smeet.expencemanagement.repository.ExpenseRepository
import com.smeet.expencemanagement.viewmodel.ExpenseViewModel
import com.smeet.expencemanagement.viewmodel.ExpenseViewModelFactory

class Reminder : AppCompatActivity() {

    private var selectedDueDate: Long = 0L

    private lateinit var viewModel: ExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reminder)

        val database= ExpenseDatabase.getDatabase(this)
        val repository= ExpenseRepository(database.expenseDao(),database.ScheduledBillDao())
        val factory= ExpenseViewModelFactory(repository)
        viewModel= ViewModelProvider(this,factory).get(ExpenseViewModel::class.java)


        val sharedPreferences=getSharedPreferences("ExpensePref",MODE_PRIVATE)
        val currencySymbol=sharedPreferences.getString("currencySymbole","₹") ?:"₹"

        val recyclerView=findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.billsRecyclerView)
        recyclerView.layoutManager=androidx.recyclerview.widget.LinearLayoutManager(this)

        val adapter= ScheduledBillAdapter(mutableListOf(),currencySymbol){paidBill ->
            val newExpense = com.smeet.expencemanagement.model.Expence(
                amount = paidBill.amount,
                category = paidBill.category,
                note = paidBill.billName + " (Scheduled)",
                date = System.currentTimeMillis()
            )
            viewModel.insert(newExpense)

            val updatedBill = paidBill.copy(isPaid = true)
            viewModel.updateScheduledBill(updatedBill)

            android.widget.Toast.makeText(this, "${paidBill.billName} marked as paid!", android.widget.Toast.LENGTH_SHORT).show()
        }

        recyclerView.adapter=adapter

        val emptyState=findViewById<android.widget.TextView>(R.id.emptyStateBills)
        val tvTotalBillAmount=findViewById<android.widget.TextView>(R.id.tvTotalBillsAmount)
        val tvRemainingAmount=findViewById<android.widget.TextView>(R.id.tvRemainingAmount)

        viewModel.allScheduledBill.asLiveData().observe(this){billList->

            adapter.updateData(billList)

            if(billList.isEmpty()){
                emptyState.visibility=android.view.View.VISIBLE
                recyclerView.visibility=android.view.View.GONE
            }
            else{
                emptyState.visibility=android.view.View.GONE
                recyclerView.visibility=android.view.View.VISIBLE
            }

            val totalScheduled = billList.sumOf { it.amount }
            val totalRemaining = billList.filter { !it.isPaid }.sumOf { it.amount }

            val formattedTotal = if (totalScheduled % 1 == 0.0) totalScheduled.toInt().toString() else totalScheduled.toString()
            val formattedRemaining = if (totalRemaining % 1 == 0.0) totalRemaining.toInt().toString() else totalRemaining.toString()

            tvTotalBillAmount.text = "$currencySymbol$formattedTotal"
            tvRemainingAmount.text = "$currencySymbol$formattedRemaining"
        }


        val bottomNavigationView=findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)

        bottomNavigationView.selectedItemId = R.id.nav_reminders

        bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId){
                R.id.nav_reminders -> true
                R.id.nav_home -> {
                    startActivity(android.content.Intent(applicationContext, Home::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_analytics -> {
                    startActivity(android.content.Intent(applicationContext, Analytics::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_settings -> {
                    startActivity(android.content.Intent(applicationContext, Settings::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        val fabAddBill=findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddExpense)

        fabAddBill.setOnClickListener {
            showAddBillBottomSheet()
        }
    }

    private fun showAddBillBottomSheet(){
        val bottomSheetDialog= BottomSheetDialog(this)

        val view=layoutInflater.inflate(R.layout.bottom_sheet_add_scheduled_bill,null)
        bottomSheetDialog.setContentView(view)

        bottomSheetDialog.show()

        val btnPickDate=view.findViewById<Button>(R.id.btnPickDueDate)
        val tvSelectDate=view.findViewById<TextView>(R.id.tvSelectedDueDate)

        btnPickDate.setOnClickListener {
            val dataPicker= MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Due Date")
                .build()

            dataPicker.show(supportFragmentManager,"DATE_PICKER")

            dataPicker.addOnPositiveButtonClickListener { selection ->
                selectedDueDate=selection

                val sdf=java.text.SimpleDateFormat("dd MMM yyyy",java.util.Locale.getDefault())
                tvSelectDate.text=sdf.format(java.util.Date(selection))
            }

        }

        val inputName=view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputScheduledName)
        val inputAmount=view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputScheduledAmount)
        val spinerCategory=view.findViewById<android.widget.Spinner>(R.id.spinnerScheduledCategory)
        val btnSave=view.findViewById<Button>(R.id.btnSaveScheduledBill)

        val categories=arrayOf("Food and Dining", "Transportation","Housing and Utilities","Shopping","Health & Fitness","Entertainment & Leisure","Personal Care","Education","Finance & Debt","Other")
        spinerCategory.adapter=android.widget.ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,categories)

        btnSave.setOnClickListener {
            val nameText=inputName.text.toString().trim()
            val amountText=inputAmount.text.toString().trim()
            val categoryValue=spinerCategory.selectedItem.toString()

            if(nameText.isEmpty() || amountText.isEmpty()){
                android.widget.Toast.makeText(this,"Please enter a name and amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(selectedDueDate ==0L){
                android.widget.Toast.makeText(this,"Please select a due date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newBill= ScheduledBill(
                billName = nameText,
                amount = amountText.toDouble(),
                category = categoryValue,
                dueDate = selectedDueDate
            )

            viewModel.insertScheduleBills(newBill)
            android.widget.Toast.makeText(this,"Payment Scheduled.", Toast.LENGTH_SHORT).show()

            bottomSheetDialog.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_reminders
    }
}