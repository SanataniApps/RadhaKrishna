package com.radhegopal.hdwallpapers

import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.nativead.*
import java.io.File
import java.io.FileOutputStream
import jp.wasabeef.glide.transformations.BlurTransformation
import com.bumptech.glide.request.RequestOptions

class FinalWallpaperActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var rootLayout: LinearLayout
    private lateinit var adContainer: FrameLayout
    private lateinit var wpContainer: RelativeLayout
    private lateinit var blurBackground: ImageView

    private lateinit var btnLeft: ImageButton
    private lateinit var btnRight: ImageButton
    private lateinit var btnFav: ImageButton
    private lateinit var blinkAnim: AlphaAnimation

    private var wallpaperList = ArrayList<WallpaperModel>()
    private var currentPos: Int = 0
    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null
    private var mNativeAd: NativeAd? = null

    private var backClickCount = 0
    private var swipeCount = 0

    private val shrinkHandler = Handler(Looper.getMainLooper())
    private val shrinkRunnable = Runnable { shrinkAndShowAd() }
    private var weightAnimator: ValueAnimator? = null

    private var loadingDialog: AlertDialog? = null
    private var isActionInProgress = false

    private val appLink by lazy { "https://play.google.com/store/apps/details?id=$packageName" }
    private val shareMessage by lazy { "\n\nDownload *Radha Krishna Wallpaper 4K* for more: $appLink" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_final_wallpaper)

        rootLayout = findViewById(R.id.rootLayout)
        adContainer = findViewById(R.id.nativeAdContainer)
        wpContainer = findViewById(R.id.wallpaperContainer)
        viewPager = findViewById(R.id.viewPagerFinal)
        blurBackground = findViewById(R.id.blurBackground)

        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        btnFav = findViewById(R.id.btnFav)

        blinkAnim = AlphaAnimation(0.4f, 1.0f).apply {
            duration = 800
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }

        loadInterstitialAd()
        loadRewardedAd()

        val receivedList = intent.getSerializableExtra("list") as? ArrayList<WallpaperModel>
        currentPos = intent.getIntExtra("pos", 0)
        if (receivedList != null) wallpaperList.addAll(receivedList)

        viewPager.adapter = FullScreenAdapter(wallpaperList) {
            if (viewPager.currentItem < wallpaperList.size - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            }
        }
        viewPager.setCurrentItem(currentPos, false)
        viewPager.offscreenPageLimit = 5

        updateArrows(currentPos)
        checkFavoriteStatus(wallpaperList[currentPos].url)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                if (currentPos != position) {
                    swipeCount++
                    if (swipeCount >= 6) {
                        showSwipeInterstitial()
                        swipeCount = 0
                    }
                }

                currentPos = position
                updateArrows(position)
                checkFavoriteStatus(wallpaperList[position].url)

                resetToFullScreen()
                shrinkHandler.removeCallbacks(shrinkRunnable)
                // YAHAN 6000L KO 4000L KIYA HAI
                shrinkHandler.postDelayed(shrinkRunnable, 4000L)
                (viewPager.adapter as? FullScreenAdapter)?.playVideoAt(position)
            }
        })
        viewPager.postDelayed({ (viewPager.adapter as? FullScreenAdapter)?.playVideoAt(currentPos) }, 300)
        // YAHAN BHI 6000L KO 4000L KIYA HAI
        shrinkHandler.postDelayed(shrinkRunnable, 4000L)
        setupClickListeners()
    }

    private fun showLoading(message: String) {
        hideLoading()
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#E60B121C"))
                cornerRadius = 35f
            }

            val progress = ProgressBar(this@FinalWallpaperActivity).apply {
                indeterminateTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFD700"))
            }
            val tv = TextView(this@FinalWallpaperActivity).apply {
                text = message
                setTextColor(android.graphics.Color.parseColor("#FFD700"))
                textSize = 14f
                setPadding(0, 20, 0, 0)
                gravity = android.view.Gravity.CENTER
            }
            addView(progress)
            addView(tv)
        }

        loadingDialog = AlertDialog.Builder(this)
            .setView(ll)
            .setCancelable(false)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                window?.setDimAmount(0f)
                window?.setGravity(android.view.Gravity.BOTTOM)
                val lp = window?.attributes
                lp?.y = 550
                window?.attributes = lp
            }
        loadingDialog?.show()
    }

    private fun hideLoading() {
        if (loadingDialog != null && loadingDialog!!.isShowing) {
            try {
                loadingDialog!!.dismiss()
            } catch (e: Exception) { e.printStackTrace() }
        }
        loadingDialog = null
    }

    private fun showCenterMessage(message: String) {
        val tv = TextView(this).apply {
            text = message
            setTextColor(android.graphics.Color.parseColor("#FFD700"))
            textSize = 15f
            setPadding(50, 30, 50, 30)
            gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#E60B121C"))
                cornerRadius = 40f
            }
        }
        val dialog = AlertDialog.Builder(this)
            .setView(tv)
            .setCancelable(false)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                window?.setDimAmount(0f)
                window?.setGravity(android.view.Gravity.BOTTOM)
                val lp = window?.attributes
                lp?.y = 550
                window?.attributes = lp
            }
        dialog.show()
        Handler(Looper.getMainLooper()).postDelayed({
            try { dialog.dismiss() } catch (e: Exception) {}
        }, 1500)
    }

    private fun showSwipeInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { loadInterstitialAd() }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) { mInterstitialAd = null }
            }
            mInterstitialAd?.show(this)
        }
    }

    private fun resetToFullScreen() {
        weightAnimator?.cancel()
        adContainer.visibility = View.GONE
        val adParams = adContainer.layoutParams as LinearLayout.LayoutParams
        adParams.weight = 0f
        adContainer.layoutParams = adParams
        val wpParams = wpContainer.layoutParams as LinearLayout.LayoutParams
        wpParams.weight = 10f
        wpContainer.layoutParams = wpParams
        val vpParams = viewPager.layoutParams as RelativeLayout.LayoutParams
        vpParams.setMargins(0, 0, 0, 0)
        viewPager.layoutParams = vpParams
        blurBackground.visibility = View.GONE
    }

    private fun shrinkAndShowAd() {
        if (adContainer.visibility == View.VISIBLE) return
        adContainer.visibility = View.VISIBLE
        loadNativeAd()

        blurBackground.visibility = View.VISIBLE
        blurBackground.alpha = 0f
        Glide.with(this).load(wallpaperList[currentPos].url).apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3))).into(blurBackground)

        val displayMetrics = resources.displayMetrics
        val targetMarginSides = (displayMetrics.widthPixels * 0.20).toInt()

        weightAnimator?.cancel()
        weightAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val adParams = adContainer.layoutParams as LinearLayout.LayoutParams
                adParams.weight = 4f * fraction
                adContainer.layoutParams = adParams

                val wpParams = wpContainer.layoutParams as LinearLayout.LayoutParams
                wpParams.weight = 10f - (4f * fraction)
                wpContainer.layoutParams = wpParams

                val vpParams = viewPager.layoutParams as RelativeLayout.LayoutParams
                val currentMargin = (targetMarginSides * fraction).toInt()
                vpParams.setMargins(currentMargin, 0, currentMargin, 0)
                viewPager.layoutParams = vpParams
                blurBackground.alpha = fraction
            }
            start()
        }
    }

    private fun updateArrows(pos: Int) {
        btnLeft.visibility = if (pos == 0) View.GONE else View.VISIBLE
        btnRight.visibility = if (pos == wallpaperList.size - 1) View.GONE else View.VISIBLE
        if (btnLeft.visibility == View.VISIBLE) btnLeft.startAnimation(blinkAnim) else btnLeft.clearAnimation()
        if (btnRight.visibility == View.VISIBLE) btnRight.startAnimation(blinkAnim) else btnRight.clearAnimation()
    }

    private fun checkFavoriteStatus(url: String) {
        val prefs = getSharedPreferences("FavDB", Context.MODE_PRIVATE)
        val favs = prefs.getStringSet("links", emptySet()) ?: emptySet()
        btnFav.setImageResource(if (favs.contains(url)) R.drawable.ic_heart_red else R.drawable.ic_heart_grey)
    }

    private fun toggleFavorite(url: String) {
        val prefs = getSharedPreferences("FavDB", Context.MODE_PRIVATE)
        val favs = prefs.getStringSet("links", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (favs.contains(url)) {
            favs.remove(url)
            btnFav.setImageResource(R.drawable.ic_heart_grey)
            showCenterMessage("Removed from Favorites 💔")
        } else {
            favs.add(url)
            btnFav.setImageResource(R.drawable.ic_heart_red)
            showCenterMessage("Added to Favorites ❤️")
        }
        prefs.edit().putStringSet("links", favs).apply()
    }

    private fun setupClickListeners() {
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnDownload = findViewById<ImageButton>(R.id.btnDownload)
        val btnSet = findViewById<View>(R.id.btnSetWallpaper)
        val btnWhatsAppStatus = findViewById<ImageButton>(R.id.btnWhatsAppStatus)
        val btnShare = findViewById<ImageButton>(R.id.btnShare)

        btnLeft.setOnClickListener { viewPager.setCurrentItem(viewPager.currentItem - 1, true) }
        btnRight.setOnClickListener { viewPager.setCurrentItem(viewPager.currentItem + 1, true) }

        btnFav.setOnClickListener {
            if (isActionInProgress) return@setOnClickListener
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            toggleFavorite(wallpaperList[currentPos].url)
        }

        btnBack.setOnClickListener { handleBackPressWithAd() }
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBackPressWithAd() }
        })

        btnDownload.setOnClickListener {
            if (isActionInProgress) return@setOnClickListener
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            isActionInProgress = true
            showRewardDialog { downloadProcess() }
        }

        btnWhatsAppStatus.setOnClickListener {
            if (isActionInProgress) return@setOnClickListener
            isActionInProgress = true
            showRewardDialog { shareToWhatsAppProcess() }
        }

        btnShare.setOnClickListener {
            if (isActionInProgress) return@setOnClickListener
            isActionInProgress = true
            showRewardDialog {
                val url = wallpaperList[currentPos].url
                if (wallpaperList[currentPos].category == "Videos") {
                    shareVideoFile(url)
                } else {
                    showLoading("Preparing Image...\nPlease Wait")
                    Glide.with(applicationContext).asBitmap().load(url).diskCacheStrategy(DiskCacheStrategy.ALL).into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            shareImageOnlyWithPreview(resource)
                        }
                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            runOnUiThread { hideLoading(); showCenterMessage("Failed to load image!"); isActionInProgress = false }
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
                }
            }
        }

        btnSet.setOnClickListener {
            if (isActionInProgress) return@setOnClickListener
            isActionInProgress = true
            if (wallpaperList[currentPos].category == "Videos") {
                setVideoAsWallpaper(wallpaperList[currentPos].url)
            } else {
                val options = arrayOf("Home Screen \uD83C\uDFE0", "Lock Screen \uD83D\uDD12", "Both \uD83D\uDCF1")
                val builder = AlertDialog.Builder(this)
                    .setTitle("Apply Wallpaper")
                    .setOnCancelListener { isActionInProgress = false }
                    .setItems(options) { _, which ->
                        setWallpaperProcess(wallpaperList[currentPos].url, which + 1)
                    }

                val dialog = builder.create()
                dialog.window?.setDimAmount(0f)
                dialog.window?.setGravity(android.view.Gravity.BOTTOM)
                val lp = dialog.window?.attributes
                lp?.y = 500
                dialog.window?.attributes = lp
                dialog.show()
            }
        }
    }

    private fun showSuccessUpsellDialog() {
        val dialogView = layoutInflater.inflate(R.layout.upsell_dialog_layout, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0f)
        dialog.setOnCancelListener { isActionInProgress = false }
        dialog.window?.setGravity(android.view.Gravity.BOTTOM)
        val lp = dialog.window?.attributes
        lp?.y = 500
        dialog.window?.attributes = lp

        val btnMaybeLater = dialogView.findViewById<Button>(R.id.btnMaybeLater)
        val btnSaveGallery = dialogView.findViewById<Button>(R.id.btnSaveGallery)

        btnMaybeLater.setOnClickListener { dialog.dismiss(); isActionInProgress = false }
        btnSaveGallery.setOnClickListener {
            dialog.dismiss()
            directRewardedDownload()
        }
        dialog.show()
    }

    private fun directRewardedDownload() {
        if (mRewardedAd != null) {
            showLoading("Loading HD Quality Ad...")
            var isRewardEarned = false

            mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    hideLoading()
                    mRewardedAd = null
                    loadRewardedAd()
                    if (isRewardEarned) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            downloadProcess()
                        }, 300)
                    } else {
                        isActionInProgress = false
                    }
                }
                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    hideLoading()
                    mRewardedAd = null
                    downloadProcess()
                }
            }
            mRewardedAd?.show(this) { _ ->
                isRewardEarned = true
            }
        } else {
            downloadProcess()
            loadRewardedAd()
        }
    }

    // YAHAN PAR LOCK SCREEN FIX APPLY KIYA HAI BINA HOME SCREEN KO CHHEDE
    private fun setWallpaperProcess(imageUrl: String, type: Int) {
        showLoading("Applying Wallpaper...\nPlease Wait")
        Glide.with(applicationContext).asBitmap().load(imageUrl).diskCacheStrategy(DiskCacheStrategy.ALL).into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                Thread {
                    val wm = WallpaperManager.getInstance(applicationContext)
                    try {
                        val displayMetrics = resources.displayMetrics
                        val screenWidth = displayMetrics.widthPixels
                        val screenHeight = displayMetrics.heightPixels

                        // --- HOME SCREEN LOGIC: Perfect Center Scrolling ---
                        // Launcher ki max scroll limit nikalna
                        val maxAllowedWidth = wm.desiredMinimumWidth.takeIf { it > screenWidth } ?: (screenWidth * 2)
                        val scale = screenHeight.toFloat() / resource.height.toFloat()
                        val projectedWidth = (resource.width * scale).toInt()

                        var homeRect: android.graphics.Rect? = null
                        if (projectedWidth > maxAllowedWidth) {
                            // Agar photo limit se zyada chaudi hai, toh use perfect Center se crop karenge
                            // Taaki wo sirf left align na ho, aur right ka hissa poora na kate
                            val cropRatio = maxAllowedWidth.toFloat() / screenHeight.toFloat()
                            val cropWidth = (resource.height * cropRatio).toInt()
                            val xOffset = (resource.width - cropWidth) / 2
                            homeRect = android.graphics.Rect(xOffset, 0, xOffset + cropWidth, resource.height)
                        }

                        // --- LOCK SCREEN LOGIC: Perfect Center Static ---
                        val screenRatio = screenWidth.toFloat() / screenHeight.toFloat()
                        val imageRatio = resource.width.toFloat() / resource.height.toFloat()

                        var lockRect: android.graphics.Rect? = null
                        if (imageRatio > screenRatio) {
                            val cropWidth = (resource.height * screenRatio).toInt()
                            val xOffset = (resource.width - cropWidth) / 2
                            lockRect = android.graphics.Rect(xOffset, 0, xOffset + cropWidth, resource.height)
                        } else {
                            val cropHeight = (resource.width / screenRatio).toInt()
                            val yOffset = (resource.height - cropHeight) / 2
                            lockRect = android.graphics.Rect(0, yOffset, resource.width, yOffset + cropHeight)
                        }

                        when (type) {
                            1 -> wm.setBitmap(resource, homeRect, true, WallpaperManager.FLAG_SYSTEM)
                            2 -> wm.setBitmap(resource, lockRect, true, WallpaperManager.FLAG_LOCK)
                            3 -> {
                                wm.setBitmap(resource, homeRect, true, WallpaperManager.FLAG_SYSTEM)
                                wm.setBitmap(resource, lockRect, true, WallpaperManager.FLAG_LOCK)
                            }
                        }
                        runOnUiThread {
                            hideLoading()
                            showCenterMessage("Wallpaper Applied! ✅")
                            Handler(Looper.getMainLooper()).postDelayed({
                                showSuccessUpsellDialog()
                            }, 1600)
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            hideLoading()
                            showCenterMessage("Failed to Apply! ❌")
                            isActionInProgress = false
                        }
                    }
                }.start()
            }
            override fun onLoadFailed(errorDrawable: Drawable?) {
                runOnUiThread { hideLoading(); showCenterMessage("Network Error! ❌"); isActionInProgress = false }
            }
            override fun onLoadCleared(placeholder: Drawable?) {}
        })
    }



    private fun setVideoAsWallpaper(videoUrl: String) {
        val prefs = getSharedPreferences("WallpaperCfg", MODE_PRIVATE)
        prefs.edit().putString("video_path", videoUrl).apply()
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, VideoWallpaperService::class.java))
        startActivity(intent)
        isActionInProgress = false
    }

    private fun downloadProcess() {
        val url = wallpaperList[currentPos].url
        if (wallpaperList[currentPos].category == "Videos") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            isActionInProgress = false
        } else {
            showLoading("Downloading HD...\nPlease Wait")
            Glide.with(applicationContext)
                .asBitmap()
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        Thread { saveImageToGallery(resource) }.start()
                    }
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        runOnUiThread { hideLoading(); showCenterMessage("Download Failed! ❌"); isActionInProgress = false }
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        try {
            val filename = "RadheGopal_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                runOnUiThread {
                    hideLoading()
                    showCenterMessage("Saved to Gallery! ✅")
                    isActionInProgress = false
                }
            } else {
                runOnUiThread {
                    hideLoading()
                    showCenterMessage("Save Failed! ❌")
                    isActionInProgress = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                hideLoading()
                showCenterMessage("Error Saving! ❌")
                isActionInProgress = false
            }
        }
    }

    private fun shareVideoFile(videoUrl: String) {
        showLoading("Preparing Video...\nPlease Wait")
        Thread {
            try {
                val input = java.net.URL(videoUrl).openStream()
                val file = File(cacheDir, "shared_vid.mp4")
                FileOutputStream(file).use { output -> input.copyTo(output) }
                val uri = FileProvider.getUriForFile(this@FinalWallpaperActivity, "$packageName.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Radhe Radhe! 🙏 $shareMessage")
                    clipData = ClipData.newRawUri("Video", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runOnUiThread {
                    hideLoading()
                    startActivity(Intent.createChooser(shareIntent, "Share Video via"))
                    isActionInProgress = false
                }
            } catch (e: Exception) {
                runOnUiThread { hideLoading(); showCenterMessage("Failed to share!"); isActionInProgress = false }
            }
        }.start()
    }

    private fun shareImageOnlyWithPreview(bitmap: Bitmap) {
        Thread {
            try {
                val file = File(cacheDir, "images").apply { mkdirs() }.let { File(it, "temp_image.png") }
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "Radhe Radhe! 🙏 $shareMessage")
                    clipData = ClipData.newRawUri("Wallpaper", contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runOnUiThread {
                    hideLoading()
                    startActivity(Intent.createChooser(shareIntent, "Share via"))
                    isActionInProgress = false
                }
            } catch (e: Exception) {
                runOnUiThread { hideLoading(); isActionInProgress = false }
            }
        }.start()
    }

    private fun shareToWhatsAppProcess() {
        val url = wallpaperList[currentPos].url
        if (wallpaperList[currentPos].category == "Videos") shareVideoToWhatsApp(url)
        else shareImageToWhatsApp(url)
    }

    private fun shareDirectToWhatsApp(fileUri: Uri, mimeType: String, textToShare: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_TEXT, textToShare)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) { showCenterMessage("WhatsApp not found!") }
        isActionInProgress = false
    }

    private fun shareImageToWhatsApp(imageUrl: String) {
        showLoading("Preparing Status...\nPlease Wait")
        Glide.with(applicationContext).asBitmap().load(imageUrl).diskCacheStrategy(DiskCacheStrategy.ALL).into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                Thread {
                    try {
                        val file = File(cacheDir, "whatsapp_temp_img.jpg")
                        FileOutputStream(file).use { out -> resource.compress(Bitmap.CompressFormat.JPEG, 100, out) }
                        val uri = FileProvider.getUriForFile(this@FinalWallpaperActivity, "$packageName.fileprovider", file)
                        val shareText = "Radhe Radhe! 🙏 $shareMessage"
                        runOnUiThread { hideLoading(); shareDirectToWhatsApp(uri, "image/jpeg", shareText) }
                    } catch (e: Exception) { runOnUiThread { hideLoading(); isActionInProgress = false } }
                }.start()
            }
            override fun onLoadFailed(errorDrawable: Drawable?) {
                runOnUiThread { hideLoading(); showCenterMessage("Failed to load image!"); isActionInProgress = false }
            }
            override fun onLoadCleared(placeholder: Drawable?) {}
        })
    }

    private fun shareVideoToWhatsApp(videoUrl: String) {
        showLoading("Preparing Video Status...\nPlease Wait")
        Thread {
            try {
                val input = java.net.URL(videoUrl).openStream()
                val file = File(cacheDir, "whatsapp_temp_vid.mp4")
                FileOutputStream(file).use { output -> input.copyTo(output) }
                val uri = FileProvider.getUriForFile(this@FinalWallpaperActivity, "$packageName.fileprovider", file)
                val shareText = "Radhe Radhe! 🙏 $shareMessage"
                runOnUiThread { hideLoading(); shareDirectToWhatsApp(uri, "video/mp4", shareText) }
            } catch (e: Exception) {
                runOnUiThread { hideLoading(); showCenterMessage("Error processing video!"); isActionInProgress = false }
            }
        }.start()
    }

    private fun loadNativeAd() {
        val adLoader = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
            .forNativeAd { ad ->
                mNativeAd?.destroy()
                mNativeAd = ad
                val adView = layoutInflater.inflate(R.layout.native_ad_layout, null) as NativeAdView
                populateNativeAdView(ad, adView)
                adContainer.removeAllViews()
                adContainer.addView(adView)
            }
            .withNativeAdOptions(NativeAdOptions.Builder().setVideoOptions(VideoOptions.Builder().setStartMuted(true).build()).build())
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.mediaView = adView.findViewById(R.id.ad_media)

        (adView.headlineView as TextView).text = nativeAd.headline
        adView.mediaView?.mediaContent = nativeAd.mediaContent

        if (nativeAd.body == null) adView.bodyView?.visibility = View.INVISIBLE
        else { adView.bodyView?.visibility = View.VISIBLE; (adView.bodyView as TextView).text = nativeAd.body }

        if (nativeAd.callToAction == null) adView.callToActionView?.visibility = View.INVISIBLE
        else { adView.callToActionView?.visibility = View.VISIBLE; (adView.callToActionView as Button).text = nativeAd.callToAction }

        if (nativeAd.icon == null) adView.iconView?.visibility = View.GONE
        else { adView.iconView?.visibility = View.VISIBLE; (adView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable) }

        adView.setNativeAd(nativeAd)
    }

    private fun handleBackPressWithAd() {
        backClickCount++
        if (backClickCount >= 3 && mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { finish() }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) { finish() }
            }
            mInterstitialAd?.show(this)
            backClickCount = 0
        } else {
            finish()
        }
    }

    private fun showRewardDialog(onEarned: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.premium_dialog_layout, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0f)
        dialog.setOnCancelListener { isActionInProgress = false }
        dialog.window?.setGravity(android.view.Gravity.BOTTOM)
        val lp = dialog.window?.attributes
        lp?.y = 500
        dialog.window?.attributes = lp

        val btnLater = dialogView.findViewById<Button>(R.id.btnLater)
        val btnWatchAd = dialogView.findViewById<Button>(R.id.btnWatchAd)

        btnLater.setOnClickListener {
            dialog.dismiss()
            isActionInProgress = false
        }

        btnWatchAd.setOnClickListener {
            dialog.dismiss()

            if (mRewardedAd != null) {
                showLoading("Loading Ad...")
                var isRewardEarned = false
                mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        hideLoading()
                        mRewardedAd = null
                        loadRewardedAd()
                        if (isRewardEarned) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onEarned()
                            }, 300)
                        } else {
                            isActionInProgress = false
                        }
                    }
                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        hideLoading()
                        mRewardedAd = null
                        onEarned()
                    }
                }
                mRewardedAd?.show(this) { _ -> isRewardEarned = true }
            } else {
                showCenterMessage("Unlocking for free! 🎁")
                onEarned()
                loadRewardedAd()
            }
        }
        dialog.show()
    }

    private fun loadInterstitialAd() {
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) { mInterstitialAd = null }
            override fun onAdLoaded(interstitialAd: InterstitialAd) { mInterstitialAd = interstitialAd }
        })
    }

    private fun loadRewardedAd() {
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) { mRewardedAd = null }
            override fun onAdLoaded(ad: RewardedAd) { mRewardedAd = ad }
        })
    }

    override fun onPause() {
        super.onPause()
        (viewPager.adapter as? FullScreenAdapter)?.releaseAll()
    }

    override fun onDestroy() {
        weightAnimator?.cancel()
        shrinkHandler.removeCallbacks(shrinkRunnable)
        hideLoading()
        mNativeAd?.destroy()
        (viewPager.adapter as? FullScreenAdapter)?.releaseAll()
        viewPager.adapter = null
        super.onDestroy()
    }
}

