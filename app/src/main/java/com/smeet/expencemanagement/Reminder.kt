package com.smeet.expencemanagement

import android.icu.util.Calendar
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

        val adapter = ScheduledBillAdapter(
            billList = mutableListOf(),
            currencySymbol = currencySymbol,
            onMarkAsPaid = { paidBill ->
                val today= System.currentTimeMillis()

                val newExpense = com.smeet.expencemanagement.model.Expence(
                    amount = paidBill.amount,
                    category = paidBill.category,
                    note = paidBill.billName + " (Scheduled)",
                    date = today
                )
                viewModel.insert(newExpense)

                val updatedBill = paidBill.copy(isPaid = true, exactDatePaid = today)
                viewModel.updateScheduledBill(updatedBill)

                android.widget.Toast.makeText(this, "${paidBill.billName} marked as paid!", android.widget.Toast.LENGTH_SHORT).show()
            },
            onEditClick = { billToEdit ->
                showAddBillBottomSheet(billToEdit)
            },
            onDeleteClick = { billToDelete ->

                val deleteBackup=billToDelete.copy()
                viewModel.deleteScheduleBills(billToDelete)

                com.google.android.material.snackbar.Snackbar.make(
                    recyclerView,
                    "Bill Deleted",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).apply {
                    setAnchorView(findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddExpense))

                    setBackgroundTint(androidx.core.content.ContextCompat.getColor(this@Reminder, R.color.bg_card))
                    setTextColor(androidx.core.content.ContextCompat.getColor(this@Reminder, R.color.text_primary))
                    setActionTextColor(androidx.core.content.ContextCompat.getColor(this@Reminder, R.color.brand_primary))

                    setAction("Undo"){
                        viewModel.insertScheduleBills(deleteBackup)
                        Toast.makeText(this@Reminder,"Bill Restored", Toast.LENGTH_SHORT).show()
                    }
                    show()
                }
            }
        )

        recyclerView.adapter=adapter

        val emptyState=findViewById<android.widget.TextView>(R.id.emptyStateBills)
        val tvTotalBillAmount=findViewById<android.widget.TextView>(R.id.tvTotalBillsAmount)
        val tvRemainingAmount=findViewById<android.widget.TextView>(R.id.tvRemainingAmount)
        val Title=findViewById<TextView>(R.id.listTitle)

        val calender= java.util.Calendar.getInstance()

        val monthYearFormater=java.text.SimpleDateFormat("MMM yyyy",java.util.Locale.getDefault())
        val formattedMonthYear=monthYearFormater.format(calender.time)

        Title.text="Bills & Overdue for $formattedMonthYear"

        viewModel.allScheduledBill.asLiveData().observe(this){billList->

            val currentCalender=java.util.Calendar.getInstance()
            val currentMonth=currentCalender.get(java.util.Calendar.MONTH)
            val currentYear=currentCalender.get(java.util.Calendar.YEAR)

            val visibleBill = billList.filter { bill ->
                val billCalendar = java.util.Calendar.getInstance()
                billCalendar.timeInMillis = bill.dueDate

                val billMonth = billCalendar.get(java.util.Calendar.MONTH)
                val billYear = billCalendar.get(java.util.Calendar.YEAR)

                val isPast = (billYear < currentYear) || (billYear == currentYear && billMonth < currentMonth)
                val isCurrent = (billYear == currentYear) && (billMonth == currentMonth)

                isCurrent || (isPast && !bill.isPaid)
            }

            adapter.updateData(visibleBill)


            if(visibleBill.isEmpty()){
                emptyState.visibility=android.view.View.VISIBLE
                recyclerView.visibility=android.view.View.GONE
            }
            else{
                emptyState.visibility=android.view.View.GONE
                recyclerView.visibility=android.view.View.VISIBLE
            }

            val totalScheduled = visibleBill.filter { bill->
                val bc = java.util.Calendar.getInstance()
                bc.timeInMillis = bill.dueDate
                val bcMonth = bc.get(java.util.Calendar.MONTH)
                val bcYear = bc.get(java.util.Calendar.YEAR)

                (bcYear == currentYear) && (bcMonth == currentMonth)
            }.sumOf { it.amount }

            val totalRemaining = visibleBill.filter { !it.isPaid }.sumOf { it.amount }

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

    private fun showAddBillBottomSheet(billToEdit: ScheduledBill?=null){
        val bottomSheetDialog= BottomSheetDialog(this)

        val view=layoutInflater.inflate(R.layout.bottom_sheet_add_scheduled_bill,null)
        bottomSheetDialog.setContentView(view)

        bottomSheetDialog.show()

        val btnPickDate=view.findViewById<Button>(R.id.btnPickDueDate)
        val tvSelectDate=view.findViewById<TextView>(R.id.tvSelectedDueDate)

        btnPickDate.setOnClickListener {

            val constraintBuilder=com.google.android.material.datepicker.CalendarConstraints.Builder()
                .setValidator(com.google.android.material.datepicker.DateValidatorPointForward.now())

            val dataPicker= MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Due Date")
                .setCalendarConstraints(constraintBuilder.build())
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

        if(billToEdit!=null){
            inputName.setText(billToEdit.billName)
            val formattedAmount = if (billToEdit.amount % 1 == 0.0) billToEdit.amount.toInt().toString() else billToEdit.amount.toString()
            inputAmount.setText(formattedAmount)

            val categoryPosition=categories.indexOf(billToEdit.category)
            if(categoryPosition>=0){
                spinerCategory.setSelection(categoryPosition)
            }

            selectedDueDate=billToEdit.dueDate

            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            tvSelectDate.text = sdf.format(java.util.Date(selectedDueDate))

            btnSave.text="Update"
        }
        else{
            selectedDueDate=0L
            btnSave.setText("Save")
        }

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

            if(billToEdit==null){
                val newBill= ScheduledBill(
                    billName = nameText,
                    amount = amountText.toDouble(),
                    category = categoryValue,
                    dueDate = selectedDueDate
                )

                viewModel.insertScheduleBills(newBill)
                android.widget.Toast.makeText(this,"Payment Scheduled.", Toast.LENGTH_SHORT).show()
            }
            else{
                val updatedBill=billToEdit.copy(
                    billName = nameText,
                    amount = amountText.toDouble(),
                    category = categoryValue,
                    dueDate = selectedDueDate
                )
                viewModel.updateScheduledBill(updatedBill)
                android.widget.Toast.makeText(this,"Bill Updated.", android.widget.Toast.LENGTH_SHORT).show()
            }

            bottomSheetDialog.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_reminders
    }
}