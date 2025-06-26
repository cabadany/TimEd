package com.example.timed_mobile

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

abstract class WifiSecurityActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WifiSecurityActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
        private val ALLOWED_WIFI_SSIDS = listOf(
            "TIMED-AP2.4G",
            "CITU_WILSTUDENT",
            "NAVACOM AP"
        )
    }

    private var pendingAction: (() -> Unit)? = null
    private var networkChangeReceiver: BroadcastReceiver? = null
    private var blockingDialog: AlertDialog? = null

    override fun onResume() {
        super.onResume()
        // Register a receiver to listen for network changes
        networkChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "Network state changed.")
                checkNetworkAndShowBlockerIfNeeded()
            }
        }
        registerReceiver(networkChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        // Perform an initial check as soon as the activity is visible
        checkNetworkAndShowBlockerIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        // Unregister the receiver to avoid memory leaks
        unregisterReceiver(networkChangeReceiver)
        // Dismiss the dialog to prevent window leaks
        blockingDialog?.dismiss()
        blockingDialog = null
    }

    /**
     * Continuously checks the network state and shows a blocking dialog if invalid.
     */
    private fun checkNetworkAndShowBlockerIfNeeded() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        var errorMessage: String? = null

        if (!wifiManager.isWifiEnabled) {
            errorMessage = "Wi-Fi is disabled. Please enable Wi-Fi and connect to an authorized network."
        } else {
            val network = connectivityManager.activeNetwork
            if (network == null) {
                errorMessage = "No active network connection. Please connect to an authorized Wi-Fi network."
            } else {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                if (capabilities == null) {
                    errorMessage = "Cannot determine network status. Please connect to an authorized Wi-Fi network."
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    errorMessage = "Mobile data is not allowed. Please disable it and connect to an authorized Wi-Fi network."
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    if (!isCorrectWifiConnected()) {
                        val allowedSsidsString = ALLOWED_WIFI_SSIDS.joinToString(separator = ", ")
                        errorMessage = "You are connected to an unauthorized Wi-Fi network. Please connect to one of the following: $allowedSsidsString."
                    }
                } else {
                    errorMessage = "An unsupported network type is active. Please connect to an authorized Wi-Fi network."
                }
            }
        }

        if (errorMessage != null) {
            if (blockingDialog == null || !blockingDialog!!.isShowing) {
                showBlockingDialog(errorMessage)
            }
        } else {
            blockingDialog?.dismiss()
            blockingDialog = null
        }
    }

    /**
     * Shows a non-cancelable dialog that blocks the UI.
     */
    private fun showBlockingDialog(message: String) {
        blockingDialog?.dismiss() // Dismiss any old dialog
        val builder = AlertDialog.Builder(this)
            .setTitle("Network Requirement")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
        blockingDialog = builder.create()
        blockingDialog?.show()
    }

    /**
     * Performs a one-time check when a specific action (e.g., a button click) is initiated.
     */
    fun performWifiChecksAndProceed(action: () -> Unit) {
        if (!hasLocationPermission()) {
            Log.d(TAG, "Location permission not granted. Requesting...")
            pendingAction = action
            requestLocationPermission()
            return
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            Log.w(TAG, "Wi-Fi is disabled.")
            showWifiNotEnabledDialog()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && !isLocationServicesEnabled()) {
            Log.w(TAG, "Location services are disabled on Android 8.1+.")
            showEnableLocationDialogForWifi()
            return
        }

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            Log.w(TAG, "Action blocked: User is on mobile data.")
            showMobileDataNotAllowedDialog()
            return
        }

        if (isCorrectWifiConnected()) {
            Log.d(TAG, "All checks passed. Proceeding with action.")
            action()
        } else {
            Log.w(TAG, "Not connected to an allowed Wi-Fi SSID or no network.")
            showWifiRequiredDialog()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun isLocationServicesEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            try {
                Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF
            } catch (e: Settings.SettingNotFoundException) {
                false
            }
        }
    }

    @Suppress("deprecation")
    private fun isCorrectWifiConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return false
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            if (!networkInfo.isConnected || networkInfo.type != ConnectivityManager.TYPE_WIFI) return false
        }

        val currentSsidRaw = wifiManager.connectionInfo?.ssid
        if (currentSsidRaw == null || currentSsidRaw == "<unknown ssid>" || currentSsidRaw.isBlank()) {
            return false
        }
        val cleanedSsid = currentSsidRaw.trim('"')
        Log.d(TAG, "Current Wi-Fi SSID: $cleanedSsid")
        return ALLOWED_WIFI_SSIDS.contains(cleanedSsid)
    }

    private fun showWifiNotEnabledDialog() {
        AlertDialog.Builder(this)
            .setTitle("Wi-Fi Disabled")
            .setMessage("Wi-Fi must be enabled for this action. Please enable Wi-Fi and try again.")
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { _, _ -> startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMobileDataNotAllowedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Mobile Data Detected")
            .setMessage("This action requires a Wi-Fi connection. Please disable mobile data or connect to an authorized Wi-Fi network to continue.")
            .setCancelable(true)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showEnableLocationDialogForWifi() {
        AlertDialog.Builder(this)
            .setTitle("Enable Location Services")
            .setMessage("Location services are required to detect the Wi-Fi network name. Please enable location services and try again.")
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { _, _ -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showWifiRequiredDialog() {
        val allowedSsidsString = ALLOWED_WIFI_SSIDS.joinToString(separator = ", ")
        AlertDialog.Builder(this)
            .setTitle("Incorrect Wi-Fi Network")
            .setMessage("You must be connected to one of the following Wi-Fi networks to perform this action: $allowedSsidsString.")
            .setCancelable(true)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted. Retrying pending action.")
                pendingAction?.invoke()
            } else {
                Toast.makeText(this, "Location permission is required to verify your Wi-Fi connection.", Toast.LENGTH_LONG).show()
            }
            pendingAction = null
        }
    }
}