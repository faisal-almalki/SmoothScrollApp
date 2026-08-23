package com.densitech.scrollsmooth.ui.video.model

import androidx.media3.exoplayer.source.MediaSource
import com.densitech.scrollsmooth.ui.commerce.model.Product
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.video.prefetch.PlayerPool

data class VideoItemParams(
    val playerPool: PlayerPool,
    val isActive: Boolean,
    val currentToken: Int,
    val currentMediaSource: MediaSource,
    val mediaInfo: MediaInfo,
    val isDownloaded: Boolean,
    /** Products this video sells. Empty means the video is not shoppable. */
    val taggedProducts: List<Product> = emptyList(),
    /** The creator selling them, resolved from the featured product. */
    val seller: Seller? = null,
    val isSellerLiveNow: Boolean = false,
    val cartItemCount: Int = 0,
)
