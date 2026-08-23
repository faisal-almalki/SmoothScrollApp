package com.densitech.scrollsmooth.ui.video.model

data class VideoActionParams(
    val token: Int,
    val likeCount: Int,
    val commentCount: Int,
    val shareCount: Int,
    val isDownloaded: Boolean,
    /** Number of items in the shopper's cart, badged on the cart action. */
    val cartItemCount: Int = 0,
    /** How many products this video is selling; hides the shop action when zero. */
    val taggedProductCount: Int = 0,
)
