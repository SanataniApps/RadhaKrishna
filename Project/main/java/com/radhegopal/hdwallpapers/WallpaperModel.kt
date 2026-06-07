package com.radhegopal.hdwallpapers

import java.io.Serializable

// Serializable zaroori hai taki swipe wale logic mein puri list ek sath ja sake
data class WallpaperModel(
    val url: String,
    val category: String,
    val isNew: String? = "false" // Naya field jo batayega ki photo nayi hai ya nahi
) : Serializable