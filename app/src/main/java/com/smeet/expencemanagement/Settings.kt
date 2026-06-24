package com.smeet.expencemanagement

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class Settings : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_settings)

        val auth= FirebaseAuth.getInstance()

        val database = com.smeet.expencemanagement.model.ExpenseDatabase.getDatabase(this)
        val repository = com.smeet.expencemanagement.repository.ExpenseRepository(database.expenseDao(),database.ScheduledBillDao())
        val factory = com.smeet.expencemanagement.viewmodel.ExpenseViewModelFactory(repository)
        val viewModel = androidx.lifecycle.ViewModelProvider(this, factory).get(com.smeet.expencemanagement.viewmodel.ExpenseViewModel::class.java)

        val wipeData=findViewById<android.widget.LinearLayout>(R.id.settingsWipeDataButton)

        val sharedPreference=getSharedPreferences("ExpensePref", Context.MODE_PRIVATE)

        val name=findViewById<TextView>(R.id.settingsUserName)
        val email=findViewById<TextView>(R.id.settingsUserEmail)
        val signout=findViewById<Button>(R.id.settingsSignOutButton)
        val currencySpinner=findViewById<Spinner>(R.id.settingsCurrencySpinner)
        val bottomNavigationView = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)

        val isNightMode=if(sharedPreference.contains("isDarkMode")){
            sharedPreference.getBoolean("isDarkMode",false)
        }
        else{
            val currentNightMode=resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }

        wipeData.setOnClickListener {

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Wipe All Data?")
                .setMessage("Are you absolutely sure you want to delete all your expenses? This action CANNOT be undone.")
                .setCancelable(false)
                .setNegativeButton("Cancel") { dialog, _->
                    dialog.dismiss()
                }
                .setPositiveButton("Yes,Delete Everything"){ dialog,_->
                    viewModel.deleteAllExpence()
                    viewModel.deleteAllScheduledBills()

                    android.widget.Toast.makeText(this,"All data wiped successfully.",android.widget.Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        val currencies = arrayOf(
            "₹ (INR)", "$ (USD)", "€ (EUR)", "£ (GBP)", "¥ (JPY)", "A$ (AUD)", "C$ (CAD)"
        )

        val spinnerAdapter=android.widget.ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,currencies)
        currencySpinner.adapter=spinnerAdapter

        val savedCurrency=sharedPreference.getString("currencySymbole","₹")?:"₹"
        val currencyIndex=currencies.indexOfFirst { it.startsWith(savedCurrency) }
        if(currencyIndex>=0){
            currencySpinner.setSelection(currencyIndex)
        }

        currencySpinner.onItemSelectedListener=object :android.widget.AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedcOption=currencies[position]
                val newCurrency=selectedcOption.split(" ")[0]
                sharedPreference.edit().putString("currencySymbole",newCurrency).apply()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {

            }
        }

        bottomNavigationView.selectedItemId = R.id.nav_settings

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> true // We are already here, do nothing

                R.id.nav_home -> {
                    // 2. FIXED: Change this Intent to open Home::class.java
                    startActivity(android.content.Intent(applicationContext, Home::class.java))

                    // THE MAGIC TRICK: This kills the slide animation so it looks seamless
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_analytics -> {
                    // 2. FIXED: Change this Intent to open Home::class.java
                    startActivity(android.content.Intent(applicationContext, Analytics::class.java))

                    // THE MAGIC TRICK: This kills the slide animation so it looks seamless
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

        val username=auth.currentUser
        val fullname=username?.displayName
        val firstname=fullname?.split(" ")?.get(0) ?:"User"

        name.text=firstname.toString()

        val emailName=username?.email

        email.text=emailName.toString()

        val gso=com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSigninClient=com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)

        signout.setOnClickListener {

            auth.signOut()
            googleSigninClient.signOut().addOnCompleteListener {
                val intent= Intent(this@Settings, login::class.java)

                intent.flags=android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_settings
    }
}