package com.example.timed_mobile

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

abstract class WifiSecurityActivity : AppCompatActivity() {

    // Enum to represent different Wi-Fi connection states
    private enum class WifiCheckResult {
        SECURE,          // Correct BSSID
        INSECURE_SSID,   // Correct SSID, but wrong BSSID (Potential Rogue AP)
        WRONG_NETWORK,   // Not a recognized network
        NO_WIFI,         // Not on a Wi-Fi network
        PERMISSION_NEEDED
    }

    companion object {
        private const val TAG = "WifiSecurityActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123

        // List of authorized BSSIDs (MAC Addresses). This is the primary security check.
        private val ALLOWED_WIFI_BSSIDS = listOf(
            "6c:a4:d1:c8:28:f8", //TIMED-AP2.4G //Timeduser12345!
            "00:13:10:85:fe:01", // Example BSSID for AndroidWifi
            "dc:9f:db:f7:40:91"  // Example BSSID for NAVACOM AP
        )

        // List of authorized SSIDs (Wi-Fi Names). Used for the rogue AP warning.
        private val ALLOWED_WIFI_SSIDS = listOf(
            "TIMED-AP2.4G",
            "AndroidWifi",
            "CITU_WILSTUDENT",
            "NAVACOM AP"
        )
    }

    private var pendingAction: (() -> Unit)? = null
    private var networkChangeReceiver: BroadcastReceiver? = null
    private var blockingDialog: AlertDialog? = null

    override fun onResume() {
        super.onResume()
        networkChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "Network state changed.")
                checkNetworkAndShowBlockerIfNeeded()
            }
        }
        registerReceiver(networkChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        checkNetworkAndShowBlockerIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkChangeReceiver)
        blockingDialog?.dismiss()
        blockingDialog = null
    }

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
                    // --- ENHANCED SECURITY CHECK ---
                    when (checkWifiStatus()) {
                        WifiCheckResult.SECURE -> { /* All good, no error */ }
                        WifiCheckResult.INSECURE_SSID -> {
                            errorMessage = "Security Warning: The current Wi-Fi has a correct name but is an unrecognized access point. Please disconnect immediately."
                        }
                        else -> { // WRONG_NETWORK, NO_WIFI, PERMISSION_NEEDED
                            errorMessage = "You are not connected to an authorized Wi-Fi network. Please connect to the company's Wi-Fi to continue."
                        }
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

    private fun showBlockingDialog(message: String) {
        if (blockingDialog != null && blockingDialog!!.isShowing) {
            return
        }
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_network_blocker, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message)
        val settingsButton = dialogView.findViewById<Button>(R.id.dialog_button_settings)

        messageTextView.text = message

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        builder.setCancelable(false)

        blockingDialog = builder.create()

        settingsButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        blockingDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        blockingDialog?.show()
    }

    fun performWifiChecksAndProceed(action: () -> Unit) {
        val wifiStatus = checkWifiStatus()

        when (wifiStatus) {
            WifiCheckResult.SECURE -> {
                Log.d(TAG, "All checks passed. Proceeding with action.")
                action()
            }
            WifiCheckResult.INSECURE_SSID -> {
                Log.w(TAG, "Potential rogue AP detected. Correct SSID, but wrong BSSID.")
                showInsecureSsidDialog()
            }
            WifiCheckResult.WRONG_NETWORK, WifiCheckResult.NO_WIFI -> {
                Log.w(TAG, "Not connected to an allowed Wi-Fi network.")
                showWifiRequiredDialog()
            }
            WifiCheckResult.PERMISSION_NEEDED -> {
                Log.d(TAG, "Location permission not granted. Requesting...")
                pendingAction = action
                requestLocationPermission()
            }
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

    private fun checkWifiStatus(): WifiCheckResult {
        if (!hasLocationPermission()) return WifiCheckResult.PERMISSION_NEEDED

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) return WifiCheckResult.NO_WIFI

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        if (network == null || capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return WifiCheckResult.NO_WIFI
        }

        val wifiInfo: WifiInfo? = wifiManager.connectionInfo
        val currentBssid = wifiInfo?.bssid
        val currentSsid = wifiInfo?.ssid?.replace("\"", "")

        if (currentBssid == null || currentSsid == null) {
            Log.w(TAG, "Could not retrieve BSSID or SSID.")
            return WifiCheckResult.WRONG_NETWORK
        }

        Log.d(TAG, "Current Wi-Fi - SSID: $currentSsid, BSSID: $currentBssid")

        // Primary check: Is the BSSID in the allowed list?
        if (ALLOWED_WIFI_BSSIDS.any { it.equals(currentBssid, ignoreCase = true) }) {
            return WifiCheckResult.SECURE
        }

        // Secondary check: If BSSID failed, is the SSID a known name?
        // This indicates a potential "evil twin" attack.
        if (ALLOWED_WIFI_SSIDS.any { it.equals(currentSsid, ignoreCase = true) }) {
            return WifiCheckResult.INSECURE_SSID
        }

        // If both checks fail, it's just a random, unauthorized network.
        return WifiCheckResult.WRONG_NETWORK
    }

    private fun showCustomInfoDialog(message: String, buttonText: String, isCancelable: Boolean, action: (() -> Unit)?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_network_blocker, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message)
        val actionButton = dialogView.findViewById<Button>(R.id.dialog_button_settings)

        messageTextView.text = message
        actionButton.text = buttonText

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        builder.setCancelable(isCancelable)

        val dialog = builder.create()

        actionButton.setOnClickListener {
            action?.invoke()
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showInsecureSsidDialog() {
        showCustomInfoDialog(
            message = "Security Warning: The current Wi-Fi network has the correct name but is not a secure, authorized access point. For your security, please disconnect and connect to the official company Wi-Fi.",
            buttonText = "OK",
            isCancelable = true,
            action = null
        )
    }

    private fun showWifiRequiredDialog() {
        showCustomInfoDialog(
            message = "You must be connected to an authorized company Wi-Fi network to perform this action.",
            buttonText = "OK",
            isCancelable = true,
            action = null
        )
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