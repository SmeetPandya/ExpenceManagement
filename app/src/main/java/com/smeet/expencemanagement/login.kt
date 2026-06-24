package com.smeet.expencemanagement

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth

class login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth


    private lateinit var googleSignnClient:com.google.android.gms.auth.api.signin.GoogleSignInClient

    private val googleLauncher=registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ){result ->
        if(result.resultCode==RESULT_OK){
            val task=com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try{
                val account=task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            }
            catch (e: Exception){
                Toast.makeText(this,"Google Error:${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth= FirebaseAuth.getInstance()

        val gso=com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignnClient=com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this,gso)


        val emailInput=findViewById<EditText>(R.id.emailEditText)
        val passwordInput=findViewById<EditText>(R.id.passwordEditText)
        val signupText=findViewById<TextView>(R.id.signUpText)
        val loginbutton=findViewById<Button>(R.id.loginButton)
        val forgotPassword=findViewById<TextView>(R.id.forgotPasswordText)
        val googleButton=findViewById<Button>(R.id.googleButton)


        signupText.setOnClickListener {
            val intent= Intent(this@login, Register::class.java)
            startActivity(intent)
        }

        loginbutton.setOnClickListener {
            val email=emailInput.text.toString().trim()
            val password=passwordInput.text.toString().trim()

            if(email.isEmpty() || password.isEmpty() ){
                Toast.makeText(this,"Please enter both email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginbutton.isEnabled=false
            loginbutton.setText("Logging in...")

            auth.signInWithEmailAndPassword(email,password).addOnCompleteListener (this){ task ->
                if(task.isSuccessful){
                    Toast.makeText(this,"Login Successfully!", Toast.LENGTH_SHORT).show()

                    val intent= Intent(this@login, Home::class.java)
                    startActivity(intent)

                    finish()
                }
                else{
                    loginbutton.isEnabled = true
                    loginbutton.text = "Login"
                    Toast.makeText(this,"Login Failed:${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        forgotPassword.setOnClickListener {
            val email=emailInput.text.toString().trim()

            if(email.isEmpty()){
                Toast.makeText(this,"Please enter email address above first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email).addOnCompleteListener (this){ task ->
                if(task.isSuccessful){
                    Toast.makeText(this,"Reset link sent! Please check your inbox.", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this,"Error:${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        googleButton.setOnClickListener {
            val signinIntent=googleSignnClient.signInIntent
            googleLauncher.launch(signinIntent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String){
        val credential=com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken,null)

        auth.signInWithCredential(credential).addOnCompleteListener (this){ task ->
            if(task.isSuccessful){
                Toast.makeText(this,"Google SignIn Successfully!", Toast.LENGTH_SHORT).show()

                val intent= Intent(this@login, Home::class.java)
                startActivity(intent)

                finish()
            }
            else{
                Toast.makeText(this,"Firebase Auth Error:${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}