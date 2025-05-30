package com.example.timed_mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
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
    // SSIDs are often quoted, ensure they match exactly how the system reports them.
    private val ALLOWED_WIFI_SSIDS = listOf(
        "\"TIMED-AP2.4G\"",
        "\"CITU_WILSTUDENT\"",
        "\"CITU_WILCORPO2.4GHz\"",
        "\"NAVACOM AP\"",
        "\"GlobeAtHome_b4338_2.4\""
    )
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
                    performWifiCheckAndProceed()
                }, 5000) // Initial delay for animations to complete
            }, 1000)
        }, 1200)
    }

    @Suppress("deprecation") // For WifiInfo.getSSID on older APIs, and NetworkInfo for older APIs
    private fun isCorrectWifiConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) {
            Log.w(TAG, "Wi-Fi is disabled.")
            return false
        }

        val currentSsid: String?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 and above
            val network = connectivityManager.activeNetwork ?: return false.also { Log.d(TAG, "No active network.") }
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false.also { Log.d(TAG, "No network capabilities.") }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiInfo = wifiManager.connectionInfo
                currentSsid = wifiInfo?.ssid
                Log.d(TAG, "Current Wi-Fi SSID (API >= Q): $currentSsid")
            } else {
                Log.d(TAG, "Not connected to a Wi-Fi network (API >= Q).")
                return false
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android M, N, O, P
            val network = connectivityManager.activeNetwork ?: return false.also { Log.d(TAG, "No active network.") }
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false.also { Log.d(TAG, "No network capabilities.") }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiInfo = wifiManager.connectionInfo
                currentSsid = wifiInfo?.ssid
                Log.d(TAG, "Current Wi-Fi SSID (API M-P): $currentSsid")
            } else {
                Log.d(TAG, "Not connected to a Wi-Fi network (API M-P).")
                return false
            }
        } else { // Below Android M
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false.also { Log.d(TAG, "No active network info (legacy).") }
            if (networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                val wifiInfo = wifiManager.connectionInfo
                currentSsid = wifiInfo?.ssid
                Log.d(TAG, "Current Wi-Fi SSID (legacy): $currentSsid")
            } else {
                Log.d(TAG, "Not connected to a Wi-Fi network (legacy).")
                return false
            }
        }

        return if (currentSsid != null && ALLOWED_WIFI_SSIDS.contains(currentSsid)) {
            Log.d(TAG, "Connected to an allowed Wi-Fi SSID: $currentSsid")
            true
        } else {
            Log.d(TAG, "SSID '$currentSsid' is not in the allowed list: $ALLOWED_WIFI_SSIDS")
            false
        }
    }

    private fun performWifiCheckAndProceed() {
        if (isCorrectWifiConnected()) {
            Log.d(TAG, "Connected to an allowed Wi-Fi SSID.")
            checkUserSessionAndProceed()
        } else {
            Log.w(TAG, "Not connected to an allowed Wi-Fi SSID. Or Wi-Fi is off/not connected.")
            showWifiRequiredDialog()
        }
    }

    private fun showWifiRequiredDialog() {
        val allowedSsidsString = ALLOWED_WIFI_SSIDS.joinToString(separator = " or ") { it.replace("\"", "") }
        AlertDialog.Builder(this)
            .setTitle("Wi-Fi Connection Required")
            .setMessage("This application requires a connection to the \"$allowedSsidsString\" Wi-Fi network to function. Please connect to one of the specified Wi-Fi networks and try again.")
            .setCancelable(false) // User cannot dismiss by tapping outside
            .setPositiveButton("Retry") { dialog, _ ->
                dialog.dismiss()
                // Add a small delay before retrying to allow user to potentially switch Wi-Fi
                Handler(Looper.getMainLooper()).postDelayed({
                    performWifiCheckAndProceed()
                }, 1000)
            }
            .setNegativeButton("Exit") { dialog, _ ->
                dialog.dismiss()
                finishAffinity() // Exits the application
            }
            .show()
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
                if (logo.animation == null) { // Ensure animations haven't started
                    startAnimations()
                }
            } else {
                Toast.makeText(
                    this,
                    "Location permission denied. Wi-Fi check might be affected.",
                    Toast.LENGTH_LONG
                ).show()
                Log.w(TAG, "Location permission denied. Proceeding to Wi-Fi check and user session check.")
                // Still proceed to Wi-Fi check, as it's a primary gate.
                // The Wi-Fi check itself might fail to get SSID without location on newer Androids.
                Handler(Looper.getMainLooper()).postDelayed({
                    if (logo.animation == null) { // Ensure animations haven't started if permission was denied quickly
                        startAnimations() // Start animations, then it will hit performWifiCheckAndProceed
                    } else {
                        performWifiCheckAndProceed() // If animations already ran, just check Wi-Fi
                    }
                }, 500)
            }
        }
    }

    @Suppress("unused")
    private fun showEnableLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Location Services")
            .setMessage("Location services are needed for an optimal experience and for Wi-Fi scanning on newer Android versions. Please enable it in settings.")
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                // Consider re-checking Wi-Fi after returning from settings, or guiding the user.
                // For simplicity, current flow relies on user retrying via the Wi-Fi dialog if needed.
            }
            .setNegativeButton("Continue without") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "Proceeding without location. Wi-Fi check might be affected.",
                    Toast.LENGTH_SHORT
                ).show()
                performWifiCheckAndProceed()
            }
            .show()
    }
}