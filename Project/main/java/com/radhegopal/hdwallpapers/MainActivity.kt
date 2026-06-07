package com.radhegopal.hdwallpapers

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.play.core.review.ReviewManagerFactory

// IN-APP UPDATE IMPORTS
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bannerContainer: FrameLayout
    private var mAdView: AdView? = null

    // NATIVE ADS LIST
    val nativeAdsList = ArrayList<NativeAd>()

    // IN-APP UPDATE VARIABLES
    private lateinit var appUpdateManager: AppUpdateManager
    private val UPDATE_TYPE = AppUpdateType.FLEXIBLE // Background download ke liye

    // App crash na ho iske liye modern Launcher
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            // Agar user update cancel kar de, toh koi baat nahi app chalti rahegi
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_main)

        // --- ADMOB ENGINE START ---
        MobileAds.initialize(this) {}
        bannerContainer = findViewById(R.id.bannerContainer)

        loadAdaptiveBanner()
        loadNativeAds()

        // --- IN-APP UPDATE INITIALIZE & CHECK ---
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForGooglePlayAppUpdate()

        val appBar = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBar)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar) { view, insets ->
            val statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBarInsets.top + 10, 0, 0)
            insets
        }

        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        val categories = arrayOf("Home", "Videos", "Radha Krishna", "Krishna", "Radha", "Others", "Favorite")

        viewPager.offscreenPageLimit = categories.size

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = categories.size
            override fun createFragment(position: Int): androidx.fragment.app.Fragment {
                return WallpaperFragment.newInstance(categories[position])
            }
        }

        viewPager.setPageTransformer { page, position ->
            val r = 1 - kotlin.math.abs(position)
            page.scaleY = 0.85f + r * 0.15f
            page.alpha = 0.5f + r * 0.5f
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val catName = categories[position]
            if (catName == "Home") {
                tab.setIcon(R.drawable.ic_home)
            } else {
                tab.text = catName
            }
        }.attach()

        for (i in 0 until tabLayout.tabCount) {
            val tabView = (tabLayout.getChildAt(0) as android.view.ViewGroup).getChildAt(i)
            val params = tabView.layoutParams as android.widget.LinearLayout.LayoutParams
            params.setMargins(4, 0, 4, 0)
            tabView.layoutParams = params
            tabView.requestLayout()
        }

        toolbar.setNavigationOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { viewPager.setCurrentItem(0, true) }
                R.id.nav_fav -> { viewPager.setCurrentItem(categories.size - 1, true) }
                R.id.nav_share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Radha Krishna 4K Wallpapers & Status App download karein: https://play.google.com/store/apps/details?id=$packageName")
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share via"))
                }
                R.id.nav_rate -> { askForInAppReview() }
                R.id.nav_more_apps -> {
                    val devName = "Sanatani+Apps"
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:$devName")))
                    } catch (e: android.content.ActivityNotFoundException) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=$devName")))
                    }
                }
                R.id.nav_privacy -> {
                    val url = "https://sanataniapps.blogspot.com/p/radhe-gopal-hd-wallpapers.html"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                R.id.nav_about -> { showAboutAppDialog() }
                R.id.nav_exit -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    showExitDialog()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    showExitDialog()
                }
            }
        })

        // --- FIREBASE MESSAGING ---
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                android.util.Log.d("FCM_TOKEN", "BHAI AAPKA TOKEN YE RAHA: \n$token")
            }
        }

        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_users").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                android.util.Log.d("FCM_TOPIC", "Successfully Subscribed to all_users")
            }
        }

        // ⭐ NOTIFICATION LOGIC CALLED HERE (Old direct request removed) ⭐
        setupNotificationsSmartly()
    }

    private fun setupNotificationsSmartly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            // App kitni baar khuli hai, uska record yahan banega
            val prefs = getSharedPreferences("AppConfig", MODE_PRIVATE)
            val launchCount = prefs.getInt("launch_count", 0) + 1
            prefs.edit().putInt("launch_count", launchCount).apply()

            // ⭐ MASTER FIX: 3 second (3000ms) ka delay diya hai.
            // 3 second mein JSON aur images aaram se load ho jayenge bina ruke!
            Handler(Looper.getMainLooper()).postDelayed({

                // Check karo permission mili hai ya nahi
                val isPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

                if (!isPermissionGranted) {
                    if (launchCount == 1) {
                        // Pehla Launch: 3 Second baad Android ka apna Default Popup aayega
                        // Tab tak peeche images load ho chuki hongi!
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            101
                        )
                    }
                    else if (launchCount % 3 == 0) {
                        // Har 3rd Launch: Custom Popup aayega (Agar pehle mana kar diya tha)
                        AlertDialog.Builder(this)
                            .setTitle("Notification Permission")
                            .setMessage("Don't miss the New Updates, Please turn On the notification button.")
                            .setPositiveButton("Allow") { _, _ ->
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                startActivity(intent)
                            }
                            .setNegativeButton("Later") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
            }, 1000) // 1000 milliseconds = 3 seconds delay
        }
    }


    // --- GOOGLE PLAY IN-APP UPDATE LOGIC ---
    private fun checkForGooglePlayAppUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(UPDATE_TYPE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(UPDATE_TYPE).build()
                )
            }
        }
    }

    // Jab update background mein download ho jaye toh ye listener chalega
    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showSnackBarForCompleteUpdate()
        }
    }

    private fun showSnackBarForCompleteUpdate() {
        Snackbar.make(
            findViewById(android.R.id.content),
            "Naya update download ho gaya hai! \uD83D\uDE80",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("INSTALL") {
                appUpdateManager.completeUpdate()
            }
            show()
        }
    }

    // --- IN-APP REVIEW API ---
    private fun askForInAppReview() {
        val manager = ReviewManagerFactory.create(this)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(this@MainActivity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    Toast.makeText(this@MainActivity, "Thank you for your review! 🙏", Toast.LENGTH_SHORT).show()
                }
            } else {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                } catch (e: android.content.ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                }
            }
        }
    }

    // --- ADS HELPER FUNCTIONS ---
    private fun loadAdaptiveBanner() {
        mAdView = AdView(this)
        mAdView?.adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ID hai, apni laga lena zaroorat padne par
        bannerContainer.removeAllViews()
        bannerContainer.addView(mAdView)
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val adWidth = (outMetrics.widthPixels / outMetrics.density).toInt()
        mAdView?.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth))
        mAdView?.loadAd(AdRequest.Builder().build())
    }

    private fun loadNativeAds() {
        val adLoader = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110") // Test ID
            .forNativeAd { ad -> nativeAdsList.add(ad) }
            .build()
        adLoader.loadAds(AdRequest.Builder().build(), 3)
    }

    fun refreshAds() {
        nativeAdsList.clear()
        loadNativeAds()
    }

    private fun showAboutAppDialog() {
        AlertDialog.Builder(this)
            .setTitle("Radha Krishna 4k Wallpapers & Status")
            .setMessage("Jai Shree Krishna! 🙏\n\n Radhe Radhe 🙏, Welcome to the app, your destination for premium quality devotional 4k wallpapers and videos.\n\nDiscover new status updates daily, download them, and share directly to your WhatsApp!\n\nVersion 1.1")
            .setPositiveButton("Jai Shree Radhe", null)
            .show()
    }

    private fun showExitDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("No", null)
            .show()
    }

    // --- LIFECYCLE METHODS ---
    override fun onResume() {
        super.onResume()
        mAdView?.resume()

        // Listener attach karna (Update ke liye)
        appUpdateManager.registerListener(installStateUpdatedListener)

        // Agar user back aata hai aur app update background mein ho chuka hai
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showSnackBarForCompleteUpdate()
            }
        }
    }

    override fun onPause() {
        mAdView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        mAdView?.destroy()
        nativeAdsList.forEach { it.destroy() }

        // Listener hatana (Memory leak rokne ke liye)
        appUpdateManager.unregisterListener(installStateUpdatedListener)

        super.onDestroy()
    }
}