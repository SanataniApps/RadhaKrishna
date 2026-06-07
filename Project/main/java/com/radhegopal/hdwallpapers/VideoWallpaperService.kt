package com.radhegopal.hdwallpapers

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player

class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine() {
        private var exoPlayer: ExoPlayer? = null

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) exoPlayer?.play() else exoPlayer?.pause()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
// VideoWallpaperService.kt mein sirf ye line check karein:
            exoPlayer = ExoPlayer.Builder(this@VideoWallpaperService).build().apply {
                setVideoSurfaceHolder(holder)
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f // Isse wallpaper hamesha silent rahega

                val prefs = getSharedPreferences("WallpaperCfg", MODE_PRIVATE)
                val videoPath = prefs.getString("video_path", "")
                if (!videoPath.isNullOrEmpty()) {
                    setMediaItem(MediaItem.fromUri(videoPath))
                    prepare()
                    playWhenReady = true
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            exoPlayer?.release()
            exoPlayer = null
        }
    }
}