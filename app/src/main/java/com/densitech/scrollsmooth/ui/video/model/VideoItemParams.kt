package com.densitech.scrollsmooth.ui.video.model

import androidx.media3.exoplayer.source.MediaSource
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.video.prefetch.PlayerPool

data class VideoItemParams(
    val playerPool: PlayerPool,
    val isActive: Boolean,
    val currentToken: Int,
    val currentMediaSource: MediaSource,
    val mediaInfo: MediaInfo,
    val isDownloaded: Boolean,
    /** Ads this video is showing. Empty means the video carries no tag. */
    val taggedListings: List<Listing> = emptyList(),
    /** The seller behind them, resolved from the first tagged ad. */
    val seller: Seller? = null,
    val isSellerLiveNow: Boolean = false,
    val unreadMessageCount: Int = 0,
)
