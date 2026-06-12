package com.smeet.expencemanagement

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    val time = 4000L

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1. Move the view lookup here (after setContentView)
        val logoimage = findViewById<ImageView>(R.id.imageView)

        auth= FirebaseAuth.getInstance()

        // 2. Move the Glide call here
        Glide.with(this).asGif().load(R.drawable.anim_logo).into(logoimage)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



            Handler(Looper.getMainLooper()).postDelayed({

                val user=auth.currentUser

                if(user != null ){
                    val intent = Intent(this@MainActivity,Home::class.java)
                    startActivity(intent)

                    finish()
                }
                else {
                    val intent = Intent(this@MainActivity, Register::class.java)
                    startActivity(intent)
                    finish()
                }
            }, time)
    }
}