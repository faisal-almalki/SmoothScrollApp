package com.densitech.scrollsmooth.ui.commerce.data

import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.ListingCategory
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.commerce.model.VideoListingTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The marketplace as the UI sees it: the seeded ads plus anything the signed in user has posted,
 * plus the video to listing tags that make the feed shoppable.
 */
object ListingRepository {

    private const val KEY_MY_LISTINGS = "my_listings"
    private const val KEY_VIDEO_TAGS = "video_tags"

    private val _myListings = MutableStateFlow(
        CommerceStore.readOrNull<List<Listing>>(KEY_MY_LISTINGS) ?: emptyList()
    )

    private val _listings = MutableStateFlow(CommerceCatalog.seedListings() + _myListings.value)
    val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    /** Tags a seller attached while posting, keyed by video id. */
    private val _videoTags = MutableStateFlow(
        CommerceStore.readOrNull<Map<String, List<String>>>(KEY_VIDEO_TAGS) ?: emptyMap()
    )
    val videoTags: StateFlow<Map<String, List<String>>> = _videoTags.asStateFlow()

    fun listing(id: String?): Listing? =
        id?.let { wanted -> _listings.value.firstOrNull { it.id == wanted } }

    fun listingsByIds(ids: List<String>): List<Listing> = ids.mapNotNull { listing(it) }

    fun listingsOf(sellerId: String): List<Listing> = _listings.value.filter { it.sellerId == sellerId }

    fun seller(id: String?): Seller? = CommerceCatalog.seller(id) ?: SellerRepository.customSeller(id)

    fun post(listing: Listing) {
        val updated = _myListings.value.filterNot { it.id == listing.id } + listing
        persistMine(updated)
    }

    fun remove(listingId: String) {
        persistMine(_myListings.value.filterNot { it.id == listingId })
    }

    /** Marking an ad sold keeps it on the profile but takes it out of browse and the feed. */
    fun markSold(listingId: String, isSold: Boolean) {
        val updated = _myListings.value.map {
            if (it.id == listingId) it.copy(isSold = isSold) else it
        }
        persistMine(updated)
    }

    /**
     * Ads shown over a feed video. An explicit tag from the seller wins; otherwise the video is
     * tagged deterministically from its id so the whole demo feed is browsable.
     */
    fun listingsForVideo(videoId: String): List<Listing> {
        val explicit = _videoTags.value[videoId]
        if (!explicit.isNullOrEmpty()) {
            val resolved = listingsByIds(explicit).filterNot { it.isSold }
            if (resolved.isNotEmpty()) return resolved
        }
        val available = _listings.value.filterNot { it.isSold }
        val tag: VideoListingTag = CommerceCatalog.tagForVideo(videoId, available)
        return listingsByIds(tag.listingIds)
    }

    fun tagVideo(videoId: String, listingIds: List<String>) {
        val updated = _videoTags.value.toMutableMap()
        if (listingIds.isEmpty()) updated.remove(videoId) else updated[videoId] = listingIds
        _videoTags.value = updated
        CommerceStore.writeValue<Map<String, List<String>>>(KEY_VIDEO_TAGS, updated)
    }

    fun search(query: String, category: ListingCategory, city: String?): List<Listing> {
        val trimmed = query.trim()
        return _listings.value.filter { listing ->
            if (listing.isSold) return@filter false
            val matchesCategory = category == ListingCategory.ALL || listing.category == category.label
            val matchesCity = city == null || listing.city == city
            val matchesQuery = trimmed.isEmpty() ||
                listing.title.contains(trimmed, ignoreCase = true) ||
                listing.description.contains(trimmed, ignoreCase = true) ||
                listing.category.contains(trimmed, ignoreCase = true) ||
                listing.city.contains(trimmed, ignoreCase = true) ||
                seller(listing.sellerId)?.displayName?.contains(trimmed, ignoreCase = true) == true ||
                seller(listing.sellerId)?.handle?.contains(trimmed, ignoreCase = true) == true
            matchesCategory && matchesCity && matchesQuery
        }
    }

    /** Browse ordering: newest first, which is what a classifieds board is for. */
    fun newest(): List<Listing> =
        _listings.value.filterNot { it.isSold }.sortedByDescending { it.postedAtMillis }

    private fun persistMine(updated: List<Listing>) {
        _myListings.value = updated
        CommerceStore.writeValue(KEY_MY_LISTINGS, updated)
        _listings.value = CommerceCatalog.seedListings() + updated
    }
}
