package com.example.timed_mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.animation.AnimatorInflater
import android.animation.Animator

class SplashActivity : AppCompatActivity() {

    private lateinit var logo: ImageView
    private lateinit var appName: TextView
    private lateinit var progressBar: ProgressBar

    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        logo = findViewById(R.id.splash_logo)
        appName = findViewById(R.id.splash_app_name)
        progressBar = findViewById(R.id.splash_progress)

        if (hasLocationPermission()) {
            startAnimations()
        } else {
            requestLocationPermission()
        }
    }

    private fun startAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        logo.startAnimation(zoomIn)

        val iconContainers = listOf(
            R.id.float_event_container,
            R.id.float_form_container,
            R.id.float_users_container,
            R.id.float_chart_container,
            R.id.float_clock_container,
            R.id.float_verified_container
        )

        for (id in iconContainers) {
            val view = findViewById<View>(id)
            val animator = AnimatorInflater.loadAnimator(this, R.animator.floating_icon_motion)
            animator.setTarget(view)
            animator.start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            appName.alpha = 1f
            appName.startAnimation(fadeIn)
            appName.startAnimation(slideUp)

            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.alpha = 1f
                progressBar.startAnimation(fadeIn)

                Handler(Looper.getMainLooper()).postDelayed({
                    checkUserSessionAndProceed()
                }, 5000)
            }, 1000)
        }, 1200)
    }

    private fun checkUserSessionAndProceed() {
        val sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)

        if (isLoggedIn) {
            Log.d(TAG, "User is logged in via SharedPreferences. Navigating to Home.")
            val userId = sharedPreferences.getString(LoginActivity.KEY_USER_ID, null)
            val email = sharedPreferences.getString(LoginActivity.KEY_EMAIL, "N/A")
            val firstName = sharedPreferences.getString(LoginActivity.KEY_FIRST_NAME, "User")
            val idNumber = sharedPreferences.getString(LoginActivity.KEY_ID_NUMBER, "N/A")
            val department = sharedPreferences.getString(LoginActivity.KEY_DEPARTMENT, "N/A")

            if (userId != null) {
                val intent = Intent(this, HomeActivity::class.java).apply {
                    putExtra("userId", userId)
                    putExtra("email", email)
                    putExtra("firstName", firstName)
                    putExtra("idNumber", idNumber)
                    putExtra("department", department)
                }
                startActivity(intent)
                finishAndFade()
            } else {
                Log.e(
                    TAG,
                    "User logged in (SharedPreferences) but userId is null. Navigating to Login."
                )
                clearLoginDataAndNavigateToLogin()
            }
        } else {
            Log.d(TAG, "No user logged in (SharedPreferences). Navigating to Login.")
            navigateToLogin()
        }
    }

    private fun clearLoginDataAndNavigateToLogin() {
        val sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finishAndFade()
    }

    private fun finishAndFade() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted.")
                if (logo.animation == null) {
                    startAnimations()
                }
            } else {
                Toast.makeText(
                    this,
                    "Location permission denied. Some features might be limited.",
                    Toast.LENGTH_LONG
                ).show()
                Log.w(TAG, "Location permission denied. Proceeding to user session check.")
                Handler(Looper.getMainLooper()).postDelayed({
                    checkUserSessionAndProceed()
                }, 500)
            }
        }
    }

    @Suppress("unused")
    private fun showEnableLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Location Services")
            .setMessage("Location services are needed for an optimal experience. Please enable it in settings.")
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Continue without") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "Proceeding without location-based features.",
                    Toast.LENGTH_SHORT
                ).show()
                checkUserSessionAndProceed()
            }
            .show()
    }
}