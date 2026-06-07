package com.radhegopal.hdwallpapers

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

class WallpaperAdapter(
    private val list: ArrayList<WallpaperModel>,
    private val nativeAds: ArrayList<NativeAd> = ArrayList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_WALLPAPER = 0
        private const val TYPE_AD = 1
        private const val AD_INTERVAL = 12
    }

    override fun getItemViewType(position: Int): Int {
        return if ((position + 1) % (AD_INTERVAL + 1) == 0 && nativeAds.isNotEmpty()) {
            TYPE_AD
        } else {
            TYPE_WALLPAPER
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.itemImage)
        val txtNew: TextView = view.findViewById(R.id.txtNew)
        val videoIcon: ImageView = view.findViewById(R.id.imgVideoIcon)
    }

    class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val adView: NativeAdView = view as NativeAdView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AD) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_native_ad, parent, false)
            AdViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wallpaper, parent, false)
            ViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_AD) {
            val adPos = (position / (AD_INTERVAL + 1)) % nativeAds.size
            populateNativeAd(nativeAds[adPos], (holder as AdViewHolder).adView)
        } else {
            val wallpaperIndex = position - (position / (AD_INTERVAL + 1))
            val model = list[wallpaperIndex]
            val wallpaperHolder = holder as ViewHolder

            // FIX: Purani image ko clear karo
            wallpaperHolder.img.setImageDrawable(null)
            Glide.with(wallpaperHolder.itemView.context).clear(wallpaperHolder.img)

            // ⭐ THE SMOOTH & FAST CONFIGURATION ⭐
            Glide.with(wallpaperHolder.itemView.context)
                .load(model.url)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Offline save rakhega
                .override(360, 540) // Perfect size jisse CPU hang na ho
                .centerCrop()
                .thumbnail(0.15f) // Pehle halka blur layega
                .placeholder(android.R.color.darker_gray) // Shimmer ki jagah gray color
                .transition(DrawableTransitionOptions.withCrossFade(300)) // Jhatka hatane ke liye wapas CrossFade
                .into(wallpaperHolder.img)

            // Background Preload (Agli image chipke se load hogi)
            if (wallpaperIndex + 1 < list.size) {
                Glide.with(wallpaperHolder.itemView.context)
                    .load(list[wallpaperIndex + 1].url)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .preload()
            }

            if (model.category == "Videos") {
                wallpaperHolder.videoIcon.visibility = View.VISIBLE
            } else {
                wallpaperHolder.videoIcon.visibility = View.GONE
            }

            wallpaperHolder.txtNew.visibility = if (model.isNew == "true") View.VISIBLE else View.GONE

            wallpaperHolder.itemView.setOnClickListener {
                val intent = Intent(wallpaperHolder.itemView.context, FinalWallpaperActivity::class.java)
                intent.putExtra("list", list)
                intent.putExtra("pos", wallpaperIndex)
                wallpaperHolder.itemView.context.startActivity(intent)
            }
        }
    }

    private fun populateNativeAd(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.mediaView = adView.findViewById(R.id.ad_media)

        (adView.headlineView as TextView).text = nativeAd.headline
        adView.mediaView?.mediaContent = nativeAd.mediaContent
        if (nativeAd.body == null) adView.bodyView?.visibility = View.INVISIBLE
        else { adView.bodyView?.visibility = View.VISIBLE; (adView.bodyView as TextView).text = nativeAd.body }

        (adView.callToActionView as Button).text = nativeAd.callToAction
        (adView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
        adView.setNativeAd(nativeAd)
    }

    override fun getItemCount(): Int {
        if (list.isEmpty()) return 0
        val adCount = if (nativeAds.isEmpty()) 0 else list.size / AD_INTERVAL
        return list.size + adCount
    }
}