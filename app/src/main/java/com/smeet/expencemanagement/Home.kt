package com.smeet.expencemanagement

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.Image
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import com.google.firebase.auth.FirebaseAuth
import com.smeet.expencemanagement.model.Expence
import com.smeet.expencemanagement.viewmodel.ExpenseViewModel
import java.text.Bidi

class Home : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var totalbudget: TextView
    private lateinit var sharedPreference: SharedPreferences
    private lateinit var viewModel: ExpenseViewModel

    private var currentlyViewingDate: Long = System.currentTimeMillis()
    private var allMyExpenses: List<Expence> = emptyList()

    private lateinit var emptyState: TextView
    private lateinit var amountWidget: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExpenseAdapter
    private var TotalAmount: Double = 0.0


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_home)



        // Initialize Room database, repository, and ViewModel
        val database = com.smeet.expencemanagement.model.ExpenseDatabase.getDatabase(this)
        val repository = com.smeet.expencemanagement.repository.ExpenseRepository(database.expenseDao(),database.ScheduledBillDao())
        val factory = com.smeet.expencemanagement.viewmodel.ExpenseViewModelFactory(repository)
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory).get(com.smeet.expencemanagement.viewmodel.ExpenseViewModel::class.java)


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val workRequest=androidx.work.PeriodicWorkRequestBuilder<BillReminderWorker>(
            1,java.util.concurrent.TimeUnit.DAYS
        ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyBillReminder",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP, // KEEP prevents resetting the timer if they open the app
            workRequest
        )

        val bottomnavigationView1=findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)

        bottomnavigationView1.selectedItemId=R.id.nav_home

        bottomnavigationView1.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // We are already here, do nothing

                R.id.nav_settings -> {
                    startActivity(android.content.Intent(applicationContext, Settings::class.java))
                    // THE MAGIC TRICK: This kills the slide animation so it looks seamless
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_analytics ->{
                    val intent= Intent(this, Analytics::class.java)
                    startActivity(intent)
                    overridePendingTransition(0,0)
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

        // Fetch current user's first name from Firebase Authentication
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val fullname = user?.displayName
        val firstName = fullname?.split(" ")?.get(0) ?: "User"

        totalbudget = findViewById<TextView>(R.id.totalBudgetText)
        val username = findViewById<TextView>(R.id.userNameText)
        username.text = firstName

        // Set up SharedPreferences for onboarding and budget settings
        sharedPreference = getSharedPreferences("ExpensePref", Context.MODE_PRIVATE)
        val isFirstTime = sharedPreference.getBoolean("isFirstTime", true)
        val savedCurrency = sharedPreference.getString("currencySymbole", "₹") ?: "₹"

        if (isFirstTime) {
            // Show welcome popup if the app is opened for the very first time
            showConfigurationPopup(sharedPreference)
        } else {
            // Display the previously saved budget and currency values
            val savedBudget = sharedPreference.getInt("dailyBudget", 500)
            totalbudget.text = "/ $savedCurrency$savedBudget"
        }

        val editButton=findViewById<ImageView>(R.id.editBudgetIcon)

        editButton.setOnClickListener {
            // 1. Fetch current values
            val getBudget = sharedPreference.getInt("dailyBudget", 500)
            val currentCurrency = sharedPreference.getString("currencySymbole", "₹") ?: "₹"

            // 2. Calculate perfect padding (Converts 24dp to exact screen pixels)
            val paddingPx = (24 * resources.displayMetrics.density).toInt()

            // 3. Create a Container to act as a bumper
            val container = android.widget.FrameLayout(this)
            container.setPadding(paddingPx, paddingPx / 2, paddingPx, 0)

            // 4. Create the modern "Outlined" Wrapper programmatically
            val textInputLayout = com.google.android.material.textfield.TextInputLayout(
                this,
                null,
                com.google.android.material.R.attr.textInputOutlinedStyle
            )
            textInputLayout.hint = "Daily Budget"
            textInputLayout.layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )

            // 5. Create the actual Text Input Box
            val input = com.google.android.material.textfield.TextInputEditText(textInputLayout.context)
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            input.setText(getBudget.toString())

            // 6. Assemble the pieces (Input goes into Wrapper, Wrapper goes into Container)
            textInputLayout.addView(input)
            container.addView(textInputLayout)

            // 7. Show the Dialog
            MaterialAlertDialogBuilder(this)
                .setTitle("Edit Daily Budget")
                .setView(container) // Pass our perfectly padded container here
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save") { dialog, _ ->

                    val typeText = input.text.toString()
                    val finalBudget = if (typeText.isNotEmpty()) typeText.toInt() else getBudget

                    // Save the new budget
                    sharedPreference.edit().putInt("dailyBudget", finalBudget).apply()

                    // Update UI
                    totalbudget.text = "/ $currentCurrency$finalBudget"
                    Toast.makeText(this, "Budget Updated", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Configure the RecyclerView layout manager
        recyclerView = findViewById<RecyclerView>(R.id.expensesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter and handle inline Edit/Delete actions triggered from the UI
        adapter = ExpenseAdapter(mutableListOf<Expence>(), savedCurrency) { expence, action ->
            if (action == "DELETE") {
                val deletedExpenceBackup=expence.copy()
                viewModel.delete(expence)
                com.google.android.material.snackbar.Snackbar.make(
                    recyclerView,
                    "Expense Deleted",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).apply {
                    setAnchorView(findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddExpense))

                    setBackgroundTint(androidx.core.content.ContextCompat.getColor(this@Home, R.color.bg_card))
                    setTextColor(androidx.core.content.ContextCompat.getColor(this@Home, R.color.text_primary))
                    setActionTextColor(androidx.core.content.ContextCompat.getColor(this@Home, R.color.brand_primary))

                    setAction("Undo"){
                        viewModel.insert(deletedExpenceBackup)
                        Toast.makeText(this@Home, "Expense restored", Toast.LENGTH_SHORT).show()
                    }
                    show()
                }
            } else if (action == "EDIT") {
                // Open the bottom sheet to edit an existing expense
                val bottomsheetDialog = BottomSheetDialog(this)
                val bottomsheetView = layoutInflater.inflate(R.layout.bottom_sheet_add_expense, null)
                bottomsheetDialog.setContentView(bottomsheetView)
                bottomsheetDialog.show()

                val dialogName = bottomsheetView.findViewById<EditText>(R.id.inputExpenseName)
                val dialogAmount = bottomsheetView.findViewById<EditText>(R.id.inputExpenseAmount)
                val dialogButton = bottomsheetView.findViewById<Button>(R.id.btnSaveExpense)
                val dropdown = bottomsheetView.findViewById<Spinner>(R.id.spinnerCategory)

                val category = arrayOf("Food and Dining", "Transportation","Housing and Utilities","Shopping","Health & Fitness","Entertainment & Leisure","Personal Care","Education","Finance & Debt","Other")
                val spinnerAdapter = android.widget.ArrayAdapter(this@Home, android.R.layout.simple_spinner_dropdown_item, category)
                dropdown.adapter = spinnerAdapter

                // Pre-fill the form with the selected expense's details
                dialogName.setText(expence.note)
                dialogAmount.setText(expence.amount.toString())
                dialogButton.text = "Update Expense"
                val spinnerPosition = spinnerAdapter.getPosition(expence.category)
                dropdown.setSelection(spinnerPosition)

                dialogButton.setOnClickListener {
                    val nameText = dialogName.text.toString()
                    val amountText = dialogAmount.text.toString()
                    val categoryValue = dropdown.selectedItem.toString()

                    if (nameText.isEmpty() || amountText.isEmpty()) {
                        Toast.makeText(this, "Please fill both fields to update", Toast.LENGTH_SHORT).show()
                    } else {
                        // Package the modified values and update the database record

                        val amountDouble=amountText.toDouble()
                        val currentBudget=sharedPreference.getInt("dailyBudget",500)

                        val projectTotal=(TotalAmount-expence.amount)+amountDouble

                        if(projectTotal>currentBudget){
                            MaterialAlertDialogBuilder(this)
                                .setTitle("Over Budget Warning!")
                                .setMessage("This expense will put you over your daily limit.")
                                .setCancelable(false)
                                .setNegativeButton("Cancel") { dialog, _->
                                    dialog.dismiss()
                                }
                                .setPositiveButton("Yes,Save") {dialog ,_->
                                    val updatedExpense = expence.copy(amount = amountDouble, category = categoryValue, note = nameText)
                                    viewModel.update(updatedExpense)

                                    Toast.makeText(this, "Expense Updated.", Toast.LENGTH_SHORT).show()
                                    bottomsheetDialog.dismiss()
                                }
                                .show()
                        }
                        else{
                            val updatedExpense = expence.copy(amount = amountDouble, category = categoryValue, note = nameText)
                            viewModel.update(updatedExpense)

                            Toast.makeText(this, "Expense Updated.", Toast.LENGTH_SHORT).show()
                            bottomsheetDialog.dismiss()
                        }
                    }
                }
            }
        }

        recyclerView.adapter = adapter

        // Observe the LiveData list so the UI updates automatically on any database changes

        emptyState=findViewById<TextView>(R.id.emptyStateText)
        amountWidget=findViewById<TextView>(R.id.usedBudgetText)

        val filterButton=findViewById<ImageView>(R.id.btnFilterDate)

        filterButton.setOnClickListener {

            val constraintsBuilder = com.google.android.material.datepicker.CalendarConstraints.Builder()
                .setValidator(com.google.android.material.datepicker.DateValidatorPointBackward.now())

            val builder = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            builder.setTitleText("Select a Day")
            builder.setSelection(currentlyViewingDate)

            builder.setCalendarConstraints(constraintsBuilder.build())

            val picker = builder.build()

            picker.addOnPositiveButtonClickListener { selection ->
                currentlyViewingDate = selection
                refreshListAndBudget()
            }

            picker.show(supportFragmentManager, "DATE_PICKER")
        }


        viewModel.allExpenses.asLiveData().observe(this) { expenceList ->

            // 1. Calculate the exact milliseconds when "Today" starts and ends
            allMyExpenses = expenceList // Save the master list so the filter button can use it later!

            refreshListAndBudget()
        }


        // Handle the Floating Action Button click to add a new expense
        val addButton = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddExpense)

        addButton.setOnClickListener {
            // Open a blank bottom sheet for a new entry
            val bottomSheetDialog = BottomSheetDialog(this)
            val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_add_expense, null)
            bottomSheetDialog.setContentView(bottomSheetView)
            bottomSheetDialog.show()

            val dialogName = bottomSheetView.findViewById<EditText>(R.id.inputExpenseName)
            val dialogAmount = bottomSheetView.findViewById<EditText>(R.id.inputExpenseAmount)
            val dialogButton = bottomSheetView.findViewById<Button>(R.id.btnSaveExpense)
            val dropdown = bottomSheetView.findViewById<Spinner>(R.id.spinnerCategory)

            val category = arrayOf("Food and Dining", "Transportation", "Housing and Utilities", "Shopping", "Health & Fitness", "Entertainment & Leisure", "Personal Care", "Education", "Finance & Debt", "Other")
            val spinnerAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, category)
            dropdown.adapter = spinnerAdapter

            dialogButton.text = "Save Expence"

            dialogButton.setOnClickListener {
                val nameText = dialogName.text.toString()
                val amountText = dialogAmount.text.toString()
                val categoryValue = dropdown.selectedItem.toString()

                if (nameText.isEmpty() || amountText.isEmpty()) {
                    Toast.makeText(this, "Please fill both fields to add the expense", Toast.LENGTH_SHORT).show()
                } else {
                    // Create a brand new expense object and insert it into the database
                    val amountDouble = amountText.toDouble()
                    val currentBudget=sharedPreference.getInt("dailyBudget",500)
                    val projectTotal=TotalAmount+amountDouble

                    if(projectTotal>currentBudget){
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Over Budget Warning!")
                            .setMessage("This expense will put you over your daily limit.")
                            .setCancelable(false)
                            .setNegativeButton("Cancel") { dialog, _->
                                dialog.dismiss()
                            }
                            .setPositiveButton("Yes,Save") {dialog ,_->
                                val newExpence = Expence(amount = amountDouble, category = categoryValue, note = nameText)
                                viewModel.insert(newExpence)
                                Toast.makeText(this@Home, "Expense Saved.", Toast.LENGTH_SHORT).show()
                                bottomSheetDialog.dismiss()
                            }
                            .show()
                    }
                    else{
                        val newExpence = Expence(amount = amountDouble, category = categoryValue, note = nameText)
                        viewModel.insert(newExpence)
                        Toast.makeText(this@Home, "Expense Saved.", Toast.LENGTH_SHORT).show()
                        bottomSheetDialog.dismiss()
                    }
                }
            }
        }
    }

    private fun refreshListAndBudget(){
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentlyViewingDate // Force the calendar to use our selected date!

        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        // 2. The Filter: Create a new mini-list containing ONLY today's expenses
        val todaysExpenses = allMyExpenses.filter { expence ->
            expence.date in startOfDay..endOfDay
        }

        // 3. Update the UI and Math using ONLY the mini-list (todaysExpenses)
        adapter.updateData(todaysExpenses)

        TotalAmount = todaysExpenses.sumOf { it.amount }
        amountWidget.text = TotalAmount.toString()

        val recentTitle = findViewById<TextView>(R.id.recentTitle)

        val fabAddExpense = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddExpense)

        // 2. SMART TITLE LOGIC: Check if the currently viewed date is actually "Today"
        if (android.text.format.DateUtils.isToday(currentlyViewingDate)) {
            // If it is today, show the normal title
            recentTitle.text = "Today's Expenses"
            fabAddExpense.visibility=android.view.View.VISIBLE
        } else {
            // If it is NOT today, format the date (e.g., "18 Jun 2026") and show that!
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            val formattedDate = sdf.format(java.util.Date(currentlyViewingDate))
            recentTitle.text = "Expenses on $formattedDate"

            fabAddExpense.visibility=android.view.View.GONE
        }

        if(todaysExpenses.isEmpty()){
            emptyState.text = "No expenses."
            emptyState.visibility=android.view.View.VISIBLE
            recyclerView.visibility=android.view.View.GONE
        }
        else{
            emptyState.visibility=android.view.View.GONE
            recyclerView.visibility=android.view.View.VISIBLE
        }

        // 1. Find the Progress Bar
        val budgetProgressBar = findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.budgetProgressBar)

        // 2. Get the user's set limit
        val currentBudget = sharedPreference.getInt("dailyBudget", 500)

        // 3. Calculate the percentage
        val progressPercentage = ((TotalAmount / currentBudget) * 100).toInt()

        // 4. Prevent the bar from crashing if they go over 100%
        val safeProgress = if (progressPercentage > 100) 100 else progressPercentage

        // 5. COLOR SHIFT LOGIC: Check if they went over budget!
        if (TotalAmount > currentBudget) {
            // Turn it Red (using the money_expense color from your colors.xml)
            budgetProgressBar.setIndicatorColor(getColor(R.color.money_expense))
        } else {
            // Keep it Blue (using your normal brand_primary color)
            budgetProgressBar.setIndicatorColor(getColor(R.color.brand_primary))
        }

        // 6. Animate the bar!
        budgetProgressBar.setProgressCompat(safeProgress, true)
    }

    // Show a configuration dialog for first-time users to set currency and budget limits
    private fun showConfigurationPopup(sharedPref: SharedPreferences) {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "e.g., 500"

        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.setPadding(50, 40, 50, 0)

        val spinner = android.widget.Spinner(this)
        val currencies = arrayOf(
            "₹ (INR)",
            "$ (USD)",
            "€ (EUR)",
            "£ (GBP)",
            "¥ (JPY)",
            "A$ (AUD)",
            "C$ (CAD)"
        )

        val spinnerAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, currencies)
        spinner.adapter = spinnerAdapter

        container.addView(spinner)
        container.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle("Welcome!")
            .setMessage("Set your currency and daily budget:")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("Save Budget") { dialog, _ ->
                val typeText = input.text.toString()
                val finalBudget = if (typeText.isNotEmpty()) typeText.toInt() else 500

                val selectedOption = spinner.selectedItem.toString()
                val selectedCurrency = selectedOption.split(" ")[0]

                // Save user selections locally so the prompt doesn't show again
                sharedPref.edit()
                    .putBoolean("isFirstTime", false)
                    .putInt("dailyBudget", finalBudget)
                    .putString("currencySymbole", selectedCurrency)
                    .apply()

                totalbudget.text = "/ $selectedCurrency$finalBudget"
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_home
    }
}
