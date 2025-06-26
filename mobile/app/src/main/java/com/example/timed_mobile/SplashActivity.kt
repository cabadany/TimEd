package com.example.timed_mobile

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
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

class SplashActivity : AppCompatActivity() {

    private lateinit var logo: ImageView
    private lateinit var appName: TextView
    private lateinit var progressBar: ProgressBar

    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private val TAG = "SplashActivity"

    // Store SSIDs without quotes. Comparison will handle trimming quotes from system-reported SSID.
    private val ALLOWED_WIFI_SSIDS = listOf(
        "TIMED-AP2.4G",
        "CITU_WILSTUDENT",
        //"CITU_WILCORPO2.4GHz"//,
        "NAVACOM AP",//,
        "AndroidWifi",
        "GlobeAtHome_b4338_2.4",
        "DILI NI WIFI"
        //"GlobeAtHome_b4338_2.4"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        logo = findViewById(R.id.splash_logo)
        appName = findViewById(R.id.splash_app_name)
        progressBar = findViewById(R.id.splash_progress)

        if (hasLocationPermission()) {
            // If permission is already granted, animations can start.
            // The Wi-Fi and other checks will happen after initial animation delays.
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
                    performChecksAndProceed() // Renamed for clarity
                }, 5000) // Delay for animations before critical checks
            }, 1000)
        }, 1200)
    }

    private fun performChecksAndProceed() {
        // 1. Check if Wi-Fi hardware is enabled
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            Log.w(TAG, "Wi-Fi is disabled.")
            showWifiNotEnabledDialog()
            return
        }

        // 2. Check if Location Services are enabled (crucial for SSID on Android 8.1+ (API 27+))
        // This check is meaningful only if we have location permission.
        // If location permission was denied, isCorrectWifiConnected() will likely fail to get SSID.
        if (hasLocationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && !isLocationServicesEnabled()) {
            Log.w(TAG, "Location services are disabled on Android 8.1+.")
            showEnableLocationDialogForWifi()
            return
        }

        // 3. Now, attempt to check the connected Wi-Fi SSID
        if (isCorrectWifiConnected()) {
            Log.d(TAG, "Connected to an allowed Wi-Fi SSID.")
            checkUserSessionAndProceed()
        } else {
            Log.w(TAG, "Not connected to an allowed Wi-Fi SSID, Wi-Fi/Location off, or failed to determine SSID.")
            showWifiRequiredDialog()
        }
    }

    private fun isLocationServicesEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // Android 9 (API 28) and above
            locationManager.isLocationEnabled
        } else { // For Android 8.1 (API 27) and older.
            try {
                val locationMode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
                Log.d(TAG, "Location Mode (API < P): $locationMode")
                locationMode != Settings.Secure.LOCATION_MODE_OFF
            } catch (e: Settings.SettingNotFoundException) {
                Log.e(TAG, "Location setting (LOCATION_MODE) not found", e)
                // If setting is not found:
                // On API 27 (O_MR1), <unknown ssid> is a concern if location service is off.
                // If we can't verify it's ON, it's safer to assume it might be an issue.
                // For versions older than API 27, this setting was less directly tied to SSID retrieval if permission was granted.
                return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) {
                    false // Pessimistic for API 27 if we can't determine
                } else {
                    true  // Optimistic for < API 27
                }
            }
        }
    }

    @Suppress("deprecation") // For WifiInfo.getSSID and activeNetworkInfo
    private fun isCorrectWifiConnected(): Boolean {
        // This function assumes Wi-Fi is enabled. Location services check is done prior if needed.
        // Location permission is crucial: if not granted, SSID retrieval will likely fail.
        if (!hasLocationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.w(TAG, "Location permission not granted. SSID check will likely fail or return <unknown ssid>.")
            // No need to return false immediately, let the SSID retrieval attempt proceed and fail naturally
            // as it would give <unknown ssid> which is handled below.
        }

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Step 1: Confirm connection to a Wi-Fi network
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6.0 (API 23) and above
            val network = connectivityManager.activeNetwork
            if (network == null) {
                Log.d(TAG, "No active network (API >= M).")
                return false
            }
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.d(TAG, "Not connected to a Wi-Fi transport or no capabilities (API >= M).")
                return false
            }
        } else { // Below Android M (API < 23)
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo == null || !networkInfo.isConnected || networkInfo.type != ConnectivityManager.TYPE_WIFI) {
                Log.d(TAG, "Not connected to a Wi-Fi network (API < M).")
                return false
            }
        }

        // Step 2: If connected to Wi-Fi, get and check SSID
        val wifiInfo = wifiManager.connectionInfo
        val currentSsidRaw = wifiInfo?.ssid // Can be null or "<unknown ssid>"
        Log.d(TAG, "Attempted to get SSID. Raw value: $currentSsidRaw")

        if (currentSsidRaw == null || currentSsidRaw == "<unknown ssid>" || currentSsidRaw == "0x" || currentSsidRaw.isBlank()) {
            Log.w(TAG, "Retrieved SSID is invalid (e.g., null, \"<unknown ssid>\", blank). Common causes: Location permission denied, Location Services off (Android 8.1+), or not truly associated with a Wi-Fi AP.")
            return false
        }

        val cleanedSsid = currentSsidRaw.trim('"') // Remove surrounding quotes, if any

        return if (ALLOWED_WIFI_SSIDS.contains(cleanedSsid)) {
            Log.d(TAG, "Allowed Wi-Fi detected: $cleanedSsid (Raw: $currentSsidRaw)")
            true
        } else {
            Log.d(TAG, "SSID '$cleanedSsid' (Raw: $currentSsidRaw) is not in the allowed list: $ALLOWED_WIFI_SSIDS")
            false
        }
    }

    private fun showWifiNotEnabledDialog() {
        AlertDialog.Builder(this)
            .setTitle("Wi-Fi Disabled")
            .setMessage("Wi-Fi is currently disabled. This application requires an active Wi-Fi connection to a specific network. Please enable Wi-Fi.")
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                // Add a delay before re-checking to allow user to enable Wi-Fi and for system to update.
                Handler(Looper.getMainLooper()).postDelayed({
                    performChecksAndProceed()
                }, 3000) // Increased delay for user action
            }
            .setNegativeButton("Exit") { dialog, _ ->
                dialog.dismiss()
                finishAffinity()
            }
            .show()
    }

    private fun showEnableLocationDialogForWifi() {
        AlertDialog.Builder(this)
            .setTitle("Enable Location Services")
            .setMessage("Location services are required to detect the Wi-Fi network name on this version of Android. Please enable location services in your device settings and try again.")
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                Handler(Looper.getMainLooper()).postDelayed({
                    performChecksAndProceed()
                }, 3000) // Increased delay for user action
            }
            .setNegativeButton("Exit") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Location services are needed for Wi-Fi check. Exiting.", Toast.LENGTH_LONG).show()
                finishAffinity()
            }
            .show()
    }


    private fun showWifiRequiredDialog() {
        val allowedSsidsString = ALLOWED_WIFI_SSIDS.joinToString(separator = ", ")
        AlertDialog.Builder(this)
            .setTitle("Wi-Fi Connection Required")
            .setMessage("This application requires a connection to one of the following Wi-Fi networks: $allowedSsidsString. Please connect to an allowed network and ensure Location Services are enabled if prompted, then try again.")
            .setCancelable(false)
            .setPositiveButton("Retry") { dialog, _ ->
                dialog.dismiss()
                Handler(Looper.getMainLooper()).postDelayed({
                    performChecksAndProceed()
                }, 1000)
            }
            .setNegativeButton("Exit") { dialog, _ ->
                dialog.dismiss()
                finishAffinity()
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
                // Animations might have already started if permission was granted very quickly
                // or if this is a re-check after a previous denial where animations were skipped.
                // The check `logo.animation == null` helps decide if startAnimations() is needed
                // or if we can proceed to checks directly if animations already ran.
                // However, the current flow ensures performChecksAndProceed is called after animation delays.
                // So, if permission is granted here, we just ensure animations run their course.
                if (logo.animation == null) { // Check if animations need to be initiated
                    startAnimations()
                } else {
                    // Animations already started (e.g. permission dialog was quick).
                    // The scheduled performChecksAndProceed will run.
                    // Or, if we want to expedite after permission grant:
                    // Handler(Looper.getMainLooper()).postDelayed({ performChecksAndProceed() }, 500)
                    // For simplicity, let the original animation flow handle it.
                }
            } else {
                Toast.makeText(
                    this,
                    "Location permission denied. Wi-Fi check may not be accurate or fail.",
                    Toast.LENGTH_LONG
                ).show()
                Log.w(TAG, "Location permission denied. Proceeding with checks, but SSID retrieval might fail.")
                // Still proceed. If animations haven't started, start them.
                // The Wi-Fi check will then occur and likely fail to get specific SSID,
                // leading to the "Wi-Fi Required" dialog which has a retry option.
                Handler(Looper.getMainLooper()).postDelayed({
                    if (logo.animation == null) {
                        startAnimations()
                    } else {
                        // If animations already ran (e.g. quick denial), directly attempt checks
                        performChecksAndProceed()
                    }
                }, 500)
            }
        }
    }

    // This original showEnableLocationDialog is kept in case it's used elsewhere,
    // but for Wi-Fi specific needs, showEnableLocationDialogForWifi is now used.
    // If it's truly unused, it can be removed.
    @Suppress("unused")
    private fun showEnableLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Location Services")
            .setMessage("Location services are needed for an optimal experience and for Wi-Fi scanning on newer Android versions. Please enable it in settings.")
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Continue without") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "Proceeding without location. Wi-Fi check might be affected.",
                    Toast.LENGTH_SHORT
                ).show()
                performChecksAndProceed() // Or the next step in a more general flow
            }
            .show()
    }
}
