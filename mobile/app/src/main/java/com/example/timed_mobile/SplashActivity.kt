package com.example.timed_mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var logo: ImageView
    private lateinit var appName: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var wifiManager: WifiManager

    private val targetWifiName = "CITU_WILSTUDENT" // <-- Your target WiFi Name here
    private val LOCATION_PERMISSION_REQUEST_CODE = 123

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
                    scanWifiAndProceed()
                }, 1500)
            }, 500)

        }, 800)
    }

    /*With Strongest WIFI Scanning functions*/
    /*private fun scanWifiAndProceed() {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "WiFi is disabled. Please enable WiFi.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val success = wifiManager.startScan()
        if (!success) {
            Toast.makeText(this, "WiFi scan failed. Try again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Delay a bit to wait for scan to complete
        Handler(Looper.getMainLooper()).postDelayed({
            val scanResults: List<ScanResult> = wifiManager.scanResults

            if (scanResults.isEmpty()) {
                showWifiErrorDialog("No WiFi networks found. Please move closer to a router.")
                return@postDelayed
            }

            // Sort networks by signal strength (RSSI), strongest first
            val strongestWifi = scanResults.maxByOrNull { it.level }

            strongestWifi?.let { wifi ->
                val ssid = wifi.SSID.replace("\"", "")

                if (ssid == targetWifiName) {
                    navigateToNextScreen()
                } else {
                    showWifiErrorDialog("Strongest WiFi detected is \"$ssid\". You must be near \"$targetWifiName\" to use the app.")
                }
            } ?: run {
                showWifiErrorDialog("Unable to detect WiFi strength. Please try again.")
            }

        }, 2000) // 2 seconds delay to allow scan results
    }*/



/*Without Strongest WIFI Scanning functions*/
    private fun scanWifiAndProceed() {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "WiFi is disabled. Please enable WiFi.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val success = wifiManager.startScan()
        if (!success) {
            Toast.makeText(this, "WiFi scan failed. Try again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Delay a bit to wait for scan to complete
        Handler(Looper.getMainLooper()).postDelayed({
            val scanResults: List<ScanResult> = wifiManager.scanResults

            if (scanResults.isEmpty()) {
                showWifiErrorDialog("No WiFi networks found. Please move closer to a router.")
                return@postDelayed
            }

            // Check if the target WiFi is in range, regardless of signal strength
            val targetWifiFound = scanResults.any {
                it.SSID.replace("\"", "") == targetWifiName
            }

            if (targetWifiFound) {
                // Target WiFi is in range, proceed
                navigateToNextScreen()
            } else {
                // Target WiFi not found
                showWifiErrorDialog("\"$targetWifiName\" network not found. You must be near \"$targetWifiName\" to use this app.")
            }
        }, 2000) // 2 seconds delay to allow scan results
    }




    private fun navigateToNextScreen() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        val intent = if (currentUser != null) {
            Intent(this, HomeActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAnimations()
            } else {
                showWifiErrorDialog("Location permission is required to scan WiFi networks.")
            }
        }
    }

    private fun showWifiErrorDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("WiFi Connection Error")
        builder.setMessage(message)
        builder.setCancelable(false)
        builder.setPositiveButton("Close App") { _, _ ->
            finish()
        }
        builder.show()
    }
}