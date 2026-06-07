package com.radhegopal.hdwallpapers

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class MyGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // 300MB fix limit. Iske upar ka data Glide khud 'Auto-Delete' kar dega.
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, 300 * 1024 * 1024))
    }
}