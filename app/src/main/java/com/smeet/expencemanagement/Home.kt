package com.smeet.expencemanagement

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class Home : AppCompatActivity() {
    lateinit var auth: FirebaseAuth

    private lateinit var  totalbudget:TextView
    private lateinit var  sharedPreference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        auth= FirebaseAuth.getInstance()

        val user=auth.currentUser
        val fullname=user?.displayName
        val firstName=fullname?.split(" ")?.get(0)?:"User"

        totalbudget=findViewById<TextView>(R.id.totalBudgetText)

        val username=findViewById<TextView>(R.id.userNameText)

        username.setText(firstName)

        sharedPreference= getSharedPreferences("ExpensePref", Context.MODE_PRIVATE)

        val isFirstTime=sharedPreference.getBoolean("isFirstTime",true)

        val savedCurrency=sharedPreference.getString("currencySymbole","₹")

        if(isFirstTime==true){
            showConfigurationPopup(sharedPreference)
        }
        else{
            val savedBudget=sharedPreference.getInt("dailyBudget",500)

            totalbudget.setText("/ $savedCurrency$savedBudget")
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showConfigurationPopup(sharedPref: SharedPreferences){
        val input= EditText(this)
        input.inputType=android.text.InputType.TYPE_CLASS_NUMBER
        input.hint="e.g., 500"

        val container=android.widget.LinearLayout(this)
        container.orientation=android.widget.LinearLayout.VERTICAL
        container.setPadding(50,40,50,0)

        val spinner=android.widget.Spinner(this)
        val currencies = arrayOf(
            "₹ (INR)",
            "$ (USD)",
            "€ (EUR)",
            "£ (GBP)",
            "¥ (JPY)",
            "A$ (AUD)",
            "C$ (CAD)"
        )

        val adapter=android.widget.ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,currencies)
        spinner.adapter=adapter

        container.addView(spinner)
        container.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle("Welcome!")
            .setMessage("Set your currency and daily budget:")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("Save Budget"){ dialog,_->
                val typeText=input.text.toString()
                val finalBudget=if(typeText.isNotEmpty()) typeText.toInt() else 500

                val selectedOption = spinner.selectedItem.toString()
                val selectedCurrency=selectedOption.split(" ")[0]

                sharedPref.edit()
                    .putBoolean("isFirstTime",false)
                    .putInt("dailyBudget",finalBudget)
                    .putString("currencySymbole",selectedCurrency)
                    .apply ()

                totalbudget.setText("/ $selectedCurrency$finalBudget")
            }
            .show()
    }
}