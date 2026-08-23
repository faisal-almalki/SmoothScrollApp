package com.densitech.scrollsmooth.ui.commerce.model

/**
 * The link between a feed video and the products it sells. A video may carry several tags; the
 * first one is the pill that shows over the player.
 */
data class VideoProductTag(
    val videoId: String,
    val productIds: List<String>,
) {
    val featuredProductId: String? get() = productIds.firstOrNull()
}
