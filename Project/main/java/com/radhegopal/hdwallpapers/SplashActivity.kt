package com.radhegopal.hdwallpapers

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen setup
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_splash)

        // --- INTERNET CHECK LOGIC ---
        if (!isInternetAvailable()) {
            showNoInternetDialog()
        } else {
            startSplashAnimation()
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle("इंटरनेट बंद है! ⚠️")
            .setMessage("राधे गोपाल ऐप चलाने के लिए इंटरनेट चालू करना ज़रूरी है। कृपया अपना इंटरनेट ऑन करें और ऐप दोबारा खोलें।")
            .setCancelable(false)
            .setPositiveButton("बाहर जाएं (Exit)") { _, _ ->
                finishAffinity()
            }
            .show()
    }

    private fun startSplashAnimation() {
        val splashLogo = findViewById<ImageView>(R.id.splashLogo)
        val splashTitle = findViewById<TextView>(R.id.splashTitle)

        // Fade-In Animation
        splashLogo.alpha = 0f
        splashTitle.alpha = 0f

        splashLogo.animate().setDuration(1500).alpha(1f)
        splashTitle.animate().setDuration(1500).alpha(1f)

        // 2.5 Second baad MainActivity par jana
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2500)
    }
}