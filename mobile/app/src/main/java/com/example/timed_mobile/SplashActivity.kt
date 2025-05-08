package com.example.timed_mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
// import android.net.wifi.ScanResult // No longer directly used for navigation decision
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
// import com.google.firebase.auth.FirebaseAuth // No longer primary check
// import com.google.firebase.firestore.FirebaseFirestore // No longer fetching from here

class SplashActivity : AppCompatActivity() {

    private lateinit var logo: ImageView
    private lateinit var appName: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var wifiManager: WifiManager
    // private lateinit var firestore: FirebaseFirestore // No longer needed here

    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        logo = findViewById(R.id.splash_logo)
        appName = findViewById(R.id.splash_app_name)
        progressBar = findViewById(R.id.splash_progress)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        // firestore = FirebaseFirestore.getInstance() // Not needed here anymore

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
                    // Simplified: directly check user session after animations
                    checkUserSessionAndProceed()
                }, 1500) // Delay after progress bar animation
            }, 500) // Delay after app name animation

        }, 800) // Delay after logo animation
    }

    // scanWifiAndProceed can be removed or simplified if not strictly needed for navigation
    // For this fix, we'll bypass its direct involvement in navigation decision.
    // private fun scanWifiAndProceed() { ... }

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
                // Logged in flag is true, but userId is missing. This is an inconsistent state.
                Log.e(TAG, "User logged in (SharedPreferences) but userId is null. Navigating to Login.")
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
            clear() // Clears all data from this SharedPreferences file
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

    private fun isLocationEnabled(): Boolean { // Keep this if other parts of app need it
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
                // Permission granted, animations should have been called or will be.
                // If startAnimations() wasn't called, call it.
                // For simplicity, we assume the flow from onCreate handles this.
                // If not, ensure startAnimations() or checkUserSessionAndProceed() is called.
                Log.d(TAG, "Location permission granted.")
                // If animations haven't started, this is where you might call startAnimations()
                // if it wasn't called in onCreate due to missing permission.
                // However, the current flow calls startAnimations() if permission exists,
                // or requestLocationPermission() which then leads here.
                // If startAnimations() was deferred, it should be called now.
                // For this fix, we assume startAnimations() is the main entry point after permission check.
                // The current structure in onCreate already handles this:
                // if (hasLocationPermission()) { startAnimations() } else { requestLocationPermission() }
                // So, if permission is granted here, startAnimations() would have been the path.
                // If it was denied then granted, startAnimations() should be called.
                // To be safe, if animations haven't started, trigger them.
                // This check is a bit redundant with current onCreate but safe.
                if (logo.animation == null) { // A simple check if animations haven't run
                    startAnimations()
                }

            } else {
                Toast.makeText(this, "Location permission denied. Some features might be limited.", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Location permission denied. Proceeding to user session check.")
                // Proceed to check user session even if permission is denied, as login doesn't strictly depend on it.
                Handler(Looper.getMainLooper()).postDelayed({
                    checkUserSessionAndProceed()
                }, 500)
            }
        }
    }

    // showWifiErrorDialog and showEnableLocationDialog can be kept if needed for other functionalities
    // but are not directly part of this login fix path.
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