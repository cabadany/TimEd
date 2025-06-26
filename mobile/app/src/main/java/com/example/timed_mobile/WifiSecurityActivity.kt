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

    /**
     * A generic function to show a custom dialog for one-time info alerts.
     */
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

    private fun showWifiNotEnabledDialog() {
        showCustomInfoDialog(
            message = "Wi-Fi must be enabled for this action. Please enable Wi-Fi and try again.",
            buttonText = "Go to Settings",
            isCancelable = false,
            action = { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        )
    }

    private fun showMobileDataNotAllowedDialog() {
        showCustomInfoDialog(
            message = "This action requires a Wi-Fi connection. Please disable mobile data or connect to an authorized Wi-Fi network to continue.",
            buttonText = "OK",
            isCancelable = true,
            action = null
        )
    }

    private fun showEnableLocationDialogForWifi() {
        showCustomInfoDialog(
            message = "Location services are required to detect the Wi-Fi network name. Please enable location services and try again.",
            buttonText = "Go to Settings",
            isCancelable = false,
            action = { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
        )
    }

    private fun showWifiRequiredDialog() {
        val allowedSsidsString = ALLOWED_WIFI_SSIDS.joinToString(separator = ", ")
        showCustomInfoDialog(
            message = "You must be connected to one of the following Wi-Fi networks to perform this action: $allowedSsidsString.",
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