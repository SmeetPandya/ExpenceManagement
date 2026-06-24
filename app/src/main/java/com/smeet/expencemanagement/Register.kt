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

    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient

    private val googleLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Toast.makeText(this, "Google Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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


        // 1. Setup the Google Sign-In Options
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)

        // 2. Hook up the Google Button
        val googleButton = findViewById<Button>(R.id.googleRegisterButton) // Make sure this ID matches your Register XML!

        googleButton.setOnClickListener {
            val signinIntent = googleSignInClient.signInIntent
            googleLauncher.launch(signinIntent)
        }

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

            registerButton.isEnabled=false
            registerButton.setText("Creating Account")

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
                    registerButton.isEnabled = true
                    registerButton.text = "Register"
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

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Changed the toast to reflect Registration
                Toast.makeText(this, "Google Registration Successful!", Toast.LENGTH_SHORT).show()

                // Changed the Intent to use this@Register
                val intent = Intent(this@Register, Home::class.java)
                startActivity(intent)

                finish()
            } else {
                Toast.makeText(this, "Firebase Auth Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}