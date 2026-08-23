package com.densitech.scrollsmooth.ui.video.model

data class VideoActionParams(
    val token: Int,
    val likeCount: Int,
    val commentCount: Int,
    val shareCount: Int,
    val isDownloaded: Boolean,
    /** Unread conversations, badged on the messages action. */
    val unreadMessageCount: Int = 0,
    /** How many ads this video is showing; hides the tag action when zero. */
    val taggedListingCount: Int = 0,
)
