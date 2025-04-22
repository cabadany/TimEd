package com.example.timed_mobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var logo: ImageView
    private lateinit var appName: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        // Initialize views
        logo = findViewById(R.id.splash_logo)
        appName = findViewById(R.id.splash_app_name)
        progressBar = findViewById(R.id.splash_progress)

        // Start animation sequence
        startAnimations()
    }

    private fun startAnimations() {
        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        // Apply zoom and fade animations to logo
        logo.startAnimation(zoomIn)

        // Delayed animations for app name (appear after logo animation)
        Handler(Looper.getMainLooper()).postDelayed({
            // Make app name visible and apply animations
            appName.alpha = 1f
            appName.startAnimation(fadeIn)
            appName.startAnimation(slideUp)

            // After another delay, show progress bar
            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.alpha = 1f
                progressBar.startAnimation(fadeIn)

                // After animations complete, check authentication and navigate
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToNextScreen()
                }, 1500) // 1.5 second delay before navigation

            }, 500) // 0.5 second delay before showing progress bar

        }, 800) // 0.8 second delay before app name animation
    }

    private fun navigateToNextScreen() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        val intent = if (currentUser != null) {
            // User is signed in, go to Home
            Intent(this, LoginActivity::class.java)
        } else {
            // No user, go to Login
            Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish() // Close splash activity

        // Add exit animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}