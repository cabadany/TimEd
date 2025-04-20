package com.example.timed_mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // User is signed in, go to Home
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // No user, go to Login
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish()
    }
}