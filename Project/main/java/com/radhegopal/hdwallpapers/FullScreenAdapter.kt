package com.radhegopal.hdwallpapers

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy // NAYA
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource // NAYA
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource // NAYA
import com.google.android.exoplayer2.upstream.cache.CacheDataSource // NAYA

// DHYAN DEIN: Yahan onVideoEnded add kiya hai Auto-Scroll ke liye
class FullScreenAdapter(
    private val list: ArrayList<WallpaperModel>,
    private val onVideoEnded: () -> Unit
) : RecyclerView.Adapter<FullScreenAdapter.ViewHolder>() {

    val playersMap = HashMap<Int, ExoPlayer>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgFullScreen)
        val playerView: StyledPlayerView = view.findViewById(R.id.videoPlayerFullScreen)
        val uiContainer: RelativeLayout = view.findViewById(R.id.customUiContainer)
        val btnPlayPause: ImageView = view.findViewById(R.id.btnCustomPlayPause)
        val seekBar: SeekBar = view.findViewById(R.id.customSeekBar)
        var isVideo = false
        val handler = Handler(Looper.getMainLooper())
        var updateProgressAction: Runnable? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_full_screen, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = list[position]
        holder.isVideo = (model.category == "Videos")

        // 1. GLIDE CACHING (Image caching super fast karega)
        Glide.with(holder.itemView.context)
            .load(model.url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .fitCenter()
            .into(holder.img)

        if (holder.isVideo) {
            holder.playerView.visibility = View.VISIBLE
            val player = ExoPlayer.Builder(holder.itemView.context).build()
            playersMap[position] = player
            holder.playerView.player = player

            // 2. EXOPLAYER CACHING LOGIC
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(VideoCache.getInstance(holder.itemView.context))
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(model.url))

            player.setMediaSource(mediaSource)
            player.prepare()

            // Auto-Scroll ke liye Repeat mode OFF karna zaroori hai
            player.repeatMode = Player.REPEAT_MODE_OFF
            player.playWhenReady = false

            // YouTube Like UI Logic (Pehle jaisa hi hai)
            holder.playerView.setOnClickListener {
                if (holder.uiContainer.visibility == View.VISIBLE) {
                    holder.uiContainer.visibility = View.GONE
                } else {
                    holder.uiContainer.visibility = View.VISIBLE
                    if (player.isPlaying) {
                        holder.handler.postDelayed({ holder.uiContainer.visibility = View.GONE }, 2500)
                    }
                }
            }

            holder.btnPlayPause.setOnClickListener {
                if (player.isPlaying) {
                    player.pause()
                    holder.btnPlayPause.setImageResource(R.drawable.ic_play_white)
                    holder.handler.removeCallbacksAndMessages(null)
                } else {
                    player.play()
                    holder.btnPlayPause.setImageResource(R.drawable.ic_pause_white)
                    holder.handler.postDelayed({ holder.uiContainer.visibility = View.GONE }, 1000)
                }
            }

            holder.updateProgressAction = object : Runnable {
                override fun run() {
                    if (player.isPlaying) {
                        holder.seekBar.max = player.duration.toInt()
                        holder.seekBar.progress = player.currentPosition.toInt()
                    }
                    holder.handler.postDelayed(this, 500)
                }
            }

            holder.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) player.seekTo(progress.toLong())
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // 3. AUTO-SCROLL TRIGGER KAREIN
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) holder.img.visibility = View.GONE

                    // JAISE HI VIDEO KHATAM HO, ACTIVITY KO BATAO
                    if (state == Player.STATE_ENDED) {
                        onVideoEnded.invoke()
                    }
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        holder.btnPlayPause.setImageResource(R.drawable.ic_pause_white)
                        holder.handler.post(holder.updateProgressAction!!)
                    } else {
                        holder.btnPlayPause.setImageResource(R.drawable.ic_play_white)
                    }
                }
            })

        } else {
            holder.playerView.visibility = View.GONE
            holder.uiContainer.visibility = View.GONE
            holder.img.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = list.size

    fun playVideoAt(position: Int) {
        for ((key, player) in playersMap) {
            if (key != position) {
                player.pause()
                player.seekTo(0)
            }
        }
        playersMap[position]?.play()
    }

    fun releaseAll() {
        for (player in playersMap.values) {
            player.stop()
            player.release()
        }
        playersMap.clear()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.handler.removeCallbacksAndMessages(null)
        val pos = holder.absoluteAdapterPosition
        playersMap[pos]?.release()
        playersMap.remove(pos)
    }
}