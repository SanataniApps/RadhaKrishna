package com.radhegopal.hdwallpapers

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.facebook.shimmer.ShimmerFrameLayout
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class WallpaperFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WallpaperAdapter
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var list = ArrayList<WallpaperModel>()
    private var categoryName: String? = null
    private lateinit var prefs: SharedPreferences // OFFLINE CACHE KE LIYE

    private val jsonUrl = "https://cdn.jsdelivr.net/gh/s-n-t-ni-a-p/res-rk@main/wallpapers.json"

    companion object {
        var globalJsonCache: String? = null

        fun newInstance(category: String): WallpaperFragment {
            val fragment = WallpaperFragment()
            val args = Bundle()
            args.putString("cat", category)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_wallpaper, container, false)
        categoryName = arguments?.getString("cat")

        // Initialize SharedPreferences
        prefs = requireContext().getSharedPreferences("AppOfflineCache", Context.MODE_PRIVATE)

        shimmerLayout = view.findViewById(R.id.shimmerLayout)
        recyclerView = view.findViewById(R.id.recyclerFragment)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        // PERFORMANCE BOOSTER
        recyclerView.setItemViewCacheSize(20)
        recyclerView.setHasFixedSize(true)

        val mainActivity = activity as? MainActivity

        val layoutManager = GridLayoutManager(context, 3)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val hasAds = mainActivity?.nativeAdsList?.isNotEmpty() ?: false
                // ⭐ UPDATE: (position + 1) % 6 == 0 ka matlab hai har 5 images ke baad 1 Ad
                return if (hasAds && (position + 1) % 6 == 0) 3 else 1
            }
        }
        recyclerView.layoutManager = layoutManager

        // ⭐ PRO APPROACH: Default animation hata do taaki images turant screen par aayein
        recyclerView.itemAnimator = null
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(20)


        if (mainActivity != null) {
            adapter = WallpaperAdapter(list, mainActivity.nativeAdsList)
            recyclerView.adapter = adapter
        }

        swipeRefresh.setOnRefreshListener {
            mainActivity?.refreshAds()
            fetchOnlineJSON(forceRefresh = true) // Swipe karne par internet se layega
        }

        val btnGoTop = view.findViewById<FloatingActionButton>(R.id.btnGoTop)
        btnGoTop.setOnClickListener { recyclerView.smoothScrollToPosition(0) }
        btnGoTop.hide()

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (recyclerView.computeVerticalScrollOffset() > 500) btnGoTop.show() else btnGoTop.hide()
            }
        })

        return view
    }

    override fun onResume() {
        super.onResume()
        if (list.isNotEmpty()) {
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            return
        }

        if (categoryName == "Favorite") {
            showFavoritesOnly()
        } else {
            loadDataSmartly()
        }
    }

    // ⭐ MARKET APP SECRET: OFFLINE FIRST LOGIC ⭐
    private fun loadDataSmartly() {
        val cachedJson = prefs.getString("offline_json", null)

        if (cachedJson != null) {
            // Agar purana data saved hai, toh turant dikhao! (NO SHIMMER)
            processJsonData(cachedJson)
            // Phir background mein chupchap naya data fetch kar lo
            fetchOnlineJSON(forceRefresh = false)
        } else {
            // Pehli baar app install hui hai, tabhi Shimmer dikhega
            shimmerLayout.startShimmer()
            shimmerLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            fetchOnlineJSON(forceRefresh = true)
        }
    }

    private fun fetchOnlineJSON(forceRefresh: Boolean = true) {

        // ⭐ ZERO-SECOND START LOGIC ⭐
        // Agar MyApplication ne pehle hi data background mein laa diya hai, toh turant dikhao!
        if (!forceRefresh && MyApplication.appLevelJsonCache != null) {
            processJsonData(MyApplication.appLevelJsonCache!!)
            // Data dikhane ke baad background mein fresh check karne do
        }

        // Agar forceRefresh hai ya pre-fetch fail ho gaya tha, toh normal OKHttp chalega
        val cacheSize = (5 * 1024 * 1024).toLong()
        val cache = okhttp3.Cache(requireContext().cacheDir, cacheSize)

        val client = OkHttpClient.Builder()
            .cache(cache)
            .build()

        val request = Request.Builder()
            .url(jsonUrl)
            .cacheControl(CacheControl.Builder().maxStale(1, java.util.concurrent.TimeUnit.DAYS).build())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread { swipeRefresh.isRefreshing = false }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    // Update the global cache
                    MyApplication.appLevelJsonCache = responseData

                    if (::prefs.isInitialized) {
                        prefs.edit().putString("offline_json", responseData).apply()
                    }

                    if (forceRefresh || list.isEmpty()) {
                        processJsonData(responseData)
                    }
                }
                activity?.runOnUiThread { swipeRefresh.isRefreshing = false }
            }
        })
    }

    private fun processJsonData(responseData: String) {
        try {
            val jsonArray = JSONArray(responseData)
            val tempList = ArrayList<WallpaperModel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val url = obj.getString("url")
                val category = obj.getString("category")
                val isNew = if (obj.has("isNew")) obj.getString("isNew") else "false"

                if (categoryName == "Home" || category.equals(categoryName, ignoreCase = true)) {
                    tempList.add(WallpaperModel(url, category, isNew))
                }
            }

            val uniqueList = tempList.distinctBy { it.url }

            // NEW wali images top par aur shuffled
            val newWallpapers = uniqueList.filter { it.isNew == "true" }.shuffled()
            // Baaki images niche aur shuffled
            val oldWallpapers = uniqueList.filter { it.isNew != "true" }.shuffled()

            tempList.clear()
            tempList.addAll(newWallpapers)
            tempList.addAll(oldWallpapers)

            activity?.runOnUiThread {
                if (isAdded) {
                    list.clear()
                    list.addAll(tempList)
                    adapter.notifyDataSetChanged()
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showFavoritesOnly() {
        val sharedPreferences = requireContext().getSharedPreferences("FavDB", Context.MODE_PRIVATE)
        val favLinks = sharedPreferences.getStringSet("links", emptySet())?.toMutableSet() ?: mutableSetOf()

        val cachedJson = prefs.getString("offline_json", null)
        if (cachedJson != null) {
            processFavoritesData(cachedJson, favLinks)
        } else {
            fetchOnlineJSON(forceRefresh = true)
        }
    }

    private fun processFavoritesData(responseData: String, favLinks: Set<String>) {
        try {
            val jsonArray = JSONArray(responseData)
            val tempList = ArrayList<WallpaperModel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val url = obj.getString("url")
                if (favLinks.contains(url)) {
                    tempList.add(WallpaperModel(url, obj.optString("category", "Home"), obj.optString("isNew", "false")))
                }
            }
            activity?.runOnUiThread {
                if (isAdded) {
                    list.clear()
                    list.addAll(tempList)
                    adapter.notifyDataSetChanged()
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    swipeRefresh.isRefreshing = false
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}