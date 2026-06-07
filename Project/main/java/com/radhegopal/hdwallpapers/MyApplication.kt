package com.radhegopal.hdwallpapers

import android.app.Application
import okhttp3.*
import java.io.IOException

class MyApplication : Application() {

    companion object {
        // App khulte hi data isme save ho jayega
        var appLevelJsonCache: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        // App icon par click hote hi ye background mein JSON fetch karna shuru kar dega
        prefetchJson()
    }

    private fun prefetchJson() {
        val jsonUrl = "https://cdn.jsdelivr.net/gh/s-n-t-ni-a-p/res-rk@main/wallpapers.json"
        val client = OkHttpClient()
        val request = Request.Builder().url(jsonUrl).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Agar fail hua toh koi dikkat nahi, Fragment baad mein apna try kar lega
            }
            override fun onResponse(call: Call, response: Response) {
                val data = response.body?.string()
                if (data != null) {
                    appLevelJsonCache = data
                }
            }
        })
    }
}