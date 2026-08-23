package com.densitech.scrollsmooth.ui.commerce.model

/**
 * A live selling room. The stream itself is a looping video source in this build; everything
 * around it (viewer count, chat, the pinned product and the flash sale clock) is driven by
 * [com.densitech.scrollsmooth.ui.commerce.data.LiveRepository].
 */
data class LiveStream(
    val id: String,
    val sellerId: String,
    val title: String,
    val videoUrl: String,
    val topic: String,
    val productIds: List<String>,
    val pinnedProductId: String,
    val startingViewerCount: Int,
    val flashSaleSeconds: Int,
    val flashSaleDiscountPercent: Int,
)

enum class LiveChatKind { CHAT, JOIN, PURCHASE }

data class LiveChatMessage(
    val id: Long,
    val author: String,
    val emoji: String,
    val text: String,
    val kind: LiveChatKind = LiveChatKind.CHAT,
)
