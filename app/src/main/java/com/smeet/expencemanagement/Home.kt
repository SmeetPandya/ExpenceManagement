package com.smeet.expencemanagement

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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
import com.google.firebase.auth.FirebaseAuth
import com.smeet.expencemanagement.model.Expence
import com.smeet.expencemanagement.viewmodel.ExpenseViewModel
import java.text.Bidi

class Home : AppCompatActivity() {
    lateinit var auth: FirebaseAuth

    private lateinit var totalbudget: TextView
    private lateinit var sharedPreference: SharedPreferences
    private lateinit var viewModel: ExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_home)

        // Initialize Room database, repository, and ViewModel
        val database = com.smeet.expencemanagement.model.ExpenseDatabase.getDatabase(this)
        val repository = com.smeet.expencemanagement.repository.ExpenseRepository(database.expenseDao())
        val factory = com.smeet.expencemanagement.viewmodel.ExpenseViewModelFactory(repository)
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory).get(com.smeet.expencemanagement.viewmodel.ExpenseViewModel::class.java)

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

        // Configure the RecyclerView layout manager
        val recyclerView = findViewById<RecyclerView>(R.id.expensesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter and handle inline Edit/Delete actions triggered from the UI
        val adapter = ExpenseAdapter(mutableListOf<Expence>(), savedCurrency) { expence, action ->
            if (action == "DELETE") {
                // Remove the selected expense from the database
                viewModel.delete(expence)
                Toast.makeText(this, "Expense Deleted", Toast.LENGTH_SHORT).show()
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

                val category = arrayOf("Food", "Transport", "Shopping", "Bills", "Other")
                val spinnerAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, category)
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
                        val amountDouble = amountText.toDouble()
                        val updatedExpense = expence.copy(amount = amountDouble, category = categoryValue, note = nameText)
                        viewModel.update(updatedExpense)

                        Toast.makeText(this, "Expense Updated.", Toast.LENGTH_SHORT).show()
                        bottomsheetDialog.dismiss()
                    }
                }
            }
        }

        recyclerView.adapter = adapter

        // Observe the LiveData list so the UI updates automatically on any database changes
        viewModel.allExpenses.asLiveData().observe(this) { expenceList ->
            adapter.updateData(expenceList)
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

            val category = arrayOf("Food", "Transport", "Shopping", "Bills", "Other")
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
                    val newExpence = Expence(amount = amountDouble, category = categoryValue, note = nameText)
                    viewModel.insert(newExpence)

                    Toast.makeText(this@Home, "Expense Saved.", Toast.LENGTH_SHORT).show()
                    bottomSheetDialog.dismiss()
                }
            }
        }
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
}