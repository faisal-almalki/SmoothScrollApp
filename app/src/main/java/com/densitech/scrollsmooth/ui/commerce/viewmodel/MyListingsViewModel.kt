package com.densitech.scrollsmooth.ui.commerce.viewmodel

import androidx.lifecycle.ViewModel
import com.densitech.scrollsmooth.ui.commerce.data.CommerceCatalog
import com.densitech.scrollsmooth.ui.commerce.data.ListingRepository
import com.densitech.scrollsmooth.ui.commerce.data.MessageRepository
import com.densitech.scrollsmooth.ui.commerce.data.SellerRepository
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.ListingCategory
import com.densitech.scrollsmooth.ui.commerce.model.ListingCondition
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.commerce.model.stableHash
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The selling side: your profile, your contact preferences, the ads you have posted and which of
 * your videos each ad is attached to.
 */
class MyListingsViewModel() : ViewModel() {

    val me = SellerRepository.me
    val allListings = ListingRepository.listings
    val conversations = MessageRepository.conversations

    /** Ads picked while posting a video, before the video is published. */
    private val _pendingVideoTags = MutableStateFlow<List<String>>(emptyList())
    val pendingVideoTags = _pendingVideoTags.asStateFlow()

    fun myListings(): List<Listing> = ListingRepository.listingsOf(SellerRepository.MY_SELLER_ID)

    fun cities(): List<String> = CommerceCatalog.cities

    fun seller(id: String?): Seller? = ListingRepository.seller(id)

    fun listingsOf(sellerId: String): List<Listing> = ListingRepository.listingsOf(sellerId)

    /** Total views across your live ads, the number a classifieds seller actually watches. */
    fun myTotalViews(): Int = myListings().filterNot { it.isSold }.sumOf { it.viewCount }

    fun myEnquiryCount(): Int =
        conversations.value.count { SellerRepository.isMe(it.sellerId) }

    fun updateProfile(displayName: String, handle: String, bio: String, emoji: String, city: String) =
        SellerRepository.updateProfile(displayName, handle, bio, emoji, city)

    fun updateContactPreferences(phoneNumber: String, allowCalls: Boolean, allowMessages: Boolean) =
        SellerRepository.updateContactPreferences(phoneNumber, allowCalls, allowMessages)

    fun postListing(
        title: String,
        description: String,
        priceCents: Long,
        isNegotiable: Boolean,
        category: ListingCategory,
        condition: ListingCondition,
        emoji: String,
        city: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Listing {
        val listing = Listing(
            id = newListingId(title, nowMillis),
            sellerId = SellerRepository.MY_SELLER_ID,
            title = title.trim(),
            description = description.trim(),
            priceCents = priceCents,
            isNegotiable = isNegotiable,
            category = (if (category == ListingCategory.ALL) ListingCategory.OTHER else category).label,
            emoji = emoji.ifBlank { "📦" },
            city = city.ifBlank { me.value.city },
            condition = condition,
            postedAtMillis = nowMillis,
            viewCount = 0,
            isSold = false,
        )
        ListingRepository.post(listing)
        return listing
    }

    fun removeListing(listingId: String) = ListingRepository.remove(listingId)

    fun markSold(listingId: String, isSold: Boolean) = ListingRepository.markSold(listingId, isSold)

    fun togglePendingTag(listingId: String) {
        val current = _pendingVideoTags.value
        _pendingVideoTags.value = if (current.contains(listingId)) {
            current - listingId
        } else {
            (current + listingId).take(MAX_TAGS_PER_VIDEO)
        }
    }

    fun clearPendingTags() {
        _pendingVideoTags.value = emptyList()
    }

    /** Attaches the chosen ads to a published video so the feed can show them. */
    fun publishTags(videoId: String) {
        ListingRepository.tagVideo(videoId, _pendingVideoTags.value)
        _pendingVideoTags.value = emptyList()
    }

    private fun newListingId(title: String, nowMillis: Long): String {
        val slug = title.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(24)
            .ifBlank { "ad" }
        return "l_me_${slug}_${stableHash(nowMillis) % 10_000}"
    }

    private companion object {
        const val MAX_TAGS_PER_VIDEO = 6
    }
}
