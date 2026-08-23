package com.densitech.scrollsmooth.ui.commerce.model

import kotlinx.serialization.Serializable

/**
 * A classified ad. There is no checkout in this app: a listing exists to be found and to put a
 * buyer in touch with the person selling it, so it carries only what a buyer needs before they
 * decide to make contact.
 */
@Serializable
data class Listing(
    val id: String,
    val sellerId: String,
    val title: String,
    val description: String,
    val priceCents: Long,
    val isNegotiable: Boolean = true,
    val category: String,
    val emoji: String,
    val city: String,
    val condition: ListingCondition = ListingCondition.USED,
    val postedAtMillis: Long = 0L,
    val viewCount: Int = 0,
    val isSold: Boolean = false,
    val imageUrl: String? = null,
) {
    val isFree: Boolean get() = priceCents <= 0L
}

@Serializable
enum class ListingCondition(val label: String) {
    NEW("New"),
    LIKE_NEW("Like new"),
    USED("Used"),
}

@Serializable
enum class ListingCategory(val label: String, val emoji: String) {
    ALL("All", "🔥"),
    CARS("Cars", "🚗"),
    ELECTRONICS("Electronics", "📱"),
    FURNITURE("Furniture", "🛋"),
    FASHION("Fashion", "👗"),
    PROPERTY("Property", "🏠"),
    PETS("Pets", "🐈"),
    OTHER("Other", "📦"),
}

/**
 * The link between a feed video and the ads it is showing. The first tag is the pill that appears
 * over the player.
 */
data class VideoListingTag(
    val videoId: String,
    val listingIds: List<String>,
)
