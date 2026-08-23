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
    data object Shop : Screen("shop", "Shop", R.drawable.ic_shop_bag_24)
    data object Add : Screen("add", "Add", R.drawable.ic_add_circle_outline_24)
    data object Live : Screen("live", "Live", R.drawable.ic_live_24)
    data object Profile : Screen("profile", "Profile", R.drawable.ic_person_24)

    // Pushed screens
    data object VideoTransformation : Screen("video_transformation", "Video Transformation", 0)
    data object Cart : Screen("cart", "Cart", 0)
    data object Checkout : Screen("checkout", "Checkout", 0)
    data object Orders : Screen("orders", "Orders", 0)
    data object CreatorStudio : Screen("creator_studio", "Creator Studio", 0)
    data object AddProduct : Screen("add_product", "List a product", 0)

    data object LiveRoom : Screen("live_room/{streamId}", "Live room", 0) {
        const val ARG_STREAM_ID = "streamId"
        fun create(streamId: String) = "live_room/$streamId"
    }

    data object Storefront : Screen("storefront/{sellerId}", "Storefront", 0) {
        const val ARG_SELLER_ID = "sellerId"
        fun create(sellerId: String) = "storefront/$sellerId"
    }

    data object OrderPlaced : Screen("order_placed/{orderId}", "Order placed", 0) {
        const val ARG_ORDER_ID = "orderId"
        fun create(orderId: String) = "order_placed/$orderId"
    }

    data object TagProducts : Screen("tag_products/{videoId}", "Tag products", 0) {
        const val ARG_VIDEO_ID = "videoId"
        fun create(videoId: String) = "tag_products/$videoId"
    }
}
