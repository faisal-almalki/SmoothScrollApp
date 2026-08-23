package com.densitech.scrollsmooth.ui.commerce.model

/**
 * A live room. The stream itself is a looping video source in this build; the viewer count and
 * the chat are driven by [com.densitech.scrollsmooth.ui.commerce.data.LiveRepository].
 *
 * Nothing is sold in the room: a viewer who wants an ad contacts the seller, same as anywhere
 * else in the app.
 */
data class LiveStream(
    val id: String,
    val sellerId: String,
    val title: String,
    val videoUrl: String,
    val topic: String,
    val listingIds: List<String>,
    val pinnedListingId: String,
    val startingViewerCount: Int,
)

enum class LiveChatKind { CHAT, JOIN, CONTACT }

data class LiveChatMessage(
    val id: Long,
    val author: String,
    val emoji: String,
    val text: String,
    val kind: LiveChatKind = LiveChatKind.CHAT,
)
