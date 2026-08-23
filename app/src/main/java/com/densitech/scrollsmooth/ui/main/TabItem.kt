package com.densitech.scrollsmooth.ui.main

import com.densitech.scrollsmooth.R

/**
 * Every destination in the app. The five bottom tabs come first; the rest are pushed screens.
 *
 * Routes that carry an argument expose a `create(...)` helper so callers never hand-build a path.
 */
sealed class Screen(val route: String, val title: String, val resourceId: Int) {
    // Bottom tabs
    data object Home : Screen("home", "Home", R.drawable.ic_home_24)
    data object Browse : Screen("browse", "Browse", R.drawable.ic_shop_bag_24)
    data object Add : Screen("add", "Post", R.drawable.ic_add_circle_outline_24)
    data object Live : Screen("live", "Live", R.drawable.ic_live_24)
    data object Profile : Screen("profile", "Account", R.drawable.ic_person_24)

    // Pushed screens
    data object VideoTransformation : Screen("video_transformation", "Video Transformation", 0)
    data object Inbox : Screen("inbox", "Messages", 0)
    data object MyListings : Screen("my_listings", "My ads", 0)
    data object PostListing : Screen("post_listing", "Post an ad", 0)
    data object AccountSettings : Screen("account_settings", "Contact settings", 0)
    data object Login : Screen("login", "Sign in", 0)

    data object LiveRoom : Screen("live_room/{streamId}", "Live room", 0) {
        const val ARG_STREAM_ID = "streamId"
        fun create(streamId: String) = "live_room/$streamId"
    }

    data object Storefront : Screen("storefront/{sellerId}", "Seller", 0) {
        const val ARG_SELLER_ID = "sellerId"
        fun create(sellerId: String) = "storefront/$sellerId"
    }

    data object Chat : Screen("chat/{conversationId}", "Conversation", 0) {
        const val ARG_CONVERSATION_ID = "conversationId"

        /**
         * Conversation ids contain "::" and a listing id, so they are encoded before being put in
         * a route and decoded on the way out.
         */
        fun create(conversationId: String) = "chat/${java.net.URLEncoder.encode(conversationId, "UTF-8")}"

        fun decode(raw: String): String = java.net.URLDecoder.decode(raw, "UTF-8")
    }

    data object TagListings : Screen("tag_listings/{videoId}", "Tag your ads", 0) {
        const val ARG_VIDEO_ID = "videoId"
        fun create(videoId: String) = "tag_listings/$videoId"
    }
}
