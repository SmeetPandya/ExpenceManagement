package com.smeet.expencemanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest


class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth= FirebaseAuth.getInstance()

        val nameInput=findViewById<EditText>(R.id.nameEditText)
        val emailInput=findViewById<EditText>(R.id.emailEditText)
        val passwordInput=findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordInput=findViewById<EditText>(R.id.confirmPasswordEditText)
        val registerButton=findViewById<Button>(R.id.btnRegister)
        val loginbutton=findViewById<TextView>(R.id.loginRedirectText)


        registerButton.setOnClickListener{
            val name=nameInput.text.toString().trim()
            val email=emailInput.text.toString().trim()
            val password=passwordInput.text.toString().trim()
            val confirmPassword=confirmPasswordInput.text.toString().trim()

            if(name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()){
                Toast.makeText(this,"Please fill out all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(password != confirmPassword){
                Toast.makeText(this,"Password do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(password.length < 6 ){
                Toast.makeText(this,"Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            }

            auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener (this){ task ->

                if(task.isSuccessful){
                    val user=auth.currentUser

                    val profileUpdate= UserProfileChangeRequest.Builder().setDisplayName(name).build()

                    user?.updateProfile(profileUpdate)?.addOnCompleteListener {
                        Toast.makeText(this,"Account Created Successfully!", Toast.LENGTH_SHORT).show()

                        val intent= Intent(this@Register, Home::class.java)
                        startActivity(intent)

                        finish()
                    }
                }
                else{
                    Toast.makeText(this,"Registration Failed:${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }

        }

        loginbutton.setOnClickListener {
            val intent= Intent(this@Register, login::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}