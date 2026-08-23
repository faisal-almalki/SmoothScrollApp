package com.densitech.scrollsmooth.ui.commerce.viewmodel

import androidx.lifecycle.ViewModel
import com.densitech.scrollsmooth.ui.commerce.data.ListingRepository
import com.densitech.scrollsmooth.ui.commerce.data.SellerRepository
import com.densitech.scrollsmooth.ui.commerce.model.Listing
import com.densitech.scrollsmooth.ui.commerce.model.ListingCategory
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Browsing state for the Browse tab and for any surface that needs to open a listing sheet.
 */
class BrowseViewModel() : ViewModel() {

    val allListings = ListingRepository.listings
    val following = SellerRepository.following

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _category = MutableStateFlow(ListingCategory.ALL)
    val category = _category.asStateFlow()

    /** Null means every city. */
    private val _city = MutableStateFlow<String?>(null)
    val city = _city.asStateFlow()

    /** The listing whose detail sheet is open, if any. */
    private val _openListing = MutableStateFlow<Listing?>(null)
    val openListing = _openListing.asStateFlow()

    private val _results = MutableStateFlow(ListingRepository.newest())
    val results = _results.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
        refresh()
    }

    fun onCategoryChange(value: ListingCategory) {
        _category.value = value
        refresh()
    }

    fun onCityChange(value: String?) {
        _city.value = value
        refresh()
    }

    fun clearQuery() {
        _query.value = ""
        refresh()
    }

    fun refresh() {
        _results.value = ListingRepository
            .search(_query.value, _category.value, _city.value)
            .sortedByDescending { it.postedAtMillis }
    }

    fun openListing(listing: Listing) {
        _openListing.value = listing
    }

    fun closeListing() {
        _openListing.value = null
    }

    fun listing(id: String?): Listing? = ListingRepository.listing(id)

    fun seller(id: String?): Seller? = ListingRepository.seller(id)

    fun listingsOf(sellerId: String): List<Listing> = ListingRepository.listingsOf(sellerId)

    fun listingsForVideo(videoId: String): List<Listing> = ListingRepository.listingsForVideo(videoId)

    fun toggleFollow(sellerId: String) = SellerRepository.toggleFollow(sellerId)
}
