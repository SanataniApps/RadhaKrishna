package com.radhegopal.hdwallpapers

import android.content.Context
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

object VideoCache {
    private var simpleCache: SimpleCache? = null

    fun getInstance(context: Context): SimpleCache {
        if (simpleCache == null) {
            // 100 MB ka maximum cache limit (Purane video khud delete hote rahenge)
            val cacheFolder = File(context.cacheDir, "exo_video_cache")
            val cacheEvictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024)
            val databaseProvider = StandaloneDatabaseProvider(context)
            simpleCache = SimpleCache(cacheFolder, cacheEvictor, databaseProvider)
        }
        return simpleCache!!
    }
}