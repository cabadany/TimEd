package com.example.timed_mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SplashActivity : AppCompatActivity() {

    private lateinit var logo: ImageView
    private lateinit var appName: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var wifiManager: WifiManager

    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        logo = findViewById(R.id.splash_logo)
        appName = findViewById(R.id.splash_app_name)
        progressBar = findViewById(R.id.splash_progress)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

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

        Handler(Looper.getMainLooper()).postDelayed({
            appName.alpha = 1f
            appName.startAnimation(fadeIn)
            appName.startAnimation(slideUp)

            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.alpha = 1f
                progressBar.startAnimation(fadeIn)

                Handler(Looper.getMainLooper()).postDelayed({
                    checkUserSessionAndProceed()
                }, 1500)
            }, 500)
        }, 800)
    }

    private fun checkUserSessionAndProceed() {
        if (!isConnectedToTargetWifi()) {
            Toast.makeText(
                this,
                "Please connect to the authorized Wi-Fi network to proceed.",
                Toast.LENGTH_LONG
            ).show()
            Log.w(TAG, "Access blocked: Not connected to required Wi-Fi.")
            return
        }

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
                Log.e(TAG, "User logged in (SharedPreferences) but userId is null. Navigating to Login.")
                clearLoginDataAndNavigateToLogin()
            }
        } else {
            Log.d(TAG, "No user logged in (SharedPreferences). Navigating to Login.")
            navigateToLogin()
        }
    }

    private fun isConnectedToTargetWifi(): Boolean {
        val wifiInfo = wifiManager.connectionInfo
        val currentSSID = wifiInfo.ssid?.replace("\"", "") ?: ""
        val targetSSID = "GlobeAtHome_b4338_2.4"
        Log.d(TAG, "Current SSID: $currentSSID")
        return currentSSID == targetSSID
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

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
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
                Toast.makeText(this, "Location permission denied. Some features might be limited.", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Location permission denied. Proceeding to user session check.")
                Handler(Looper.getMainLooper()).postDelayed({
                    checkUserSessionAndProceed()
                }, 500)
            }
        }
    }

    @Suppress("unused")
    private fun showWifiErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("WiFi Information")
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton("OK", null)
            .show()
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
                Toast.makeText(this, "Proceeding without location-based features.", Toast.LENGTH_SHORT).show()
                checkUserSessionAndProceed()
            }
            .show()
    }
}