package com.densitech.scrollsmooth.ui.commerce.viewmodel

import androidx.lifecycle.ViewModel
import com.densitech.scrollsmooth.ui.commerce.data.CommerceCatalog
import com.densitech.scrollsmooth.ui.commerce.data.OrderRepository
import com.densitech.scrollsmooth.ui.commerce.data.ProductRepository
import com.densitech.scrollsmooth.ui.commerce.data.SellerRepository
import com.densitech.scrollsmooth.ui.commerce.model.Order
import com.densitech.scrollsmooth.ui.commerce.model.Product
import com.densitech.scrollsmooth.ui.commerce.model.ProductCategory
import com.densitech.scrollsmooth.ui.commerce.model.ProductOption
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.commerce.model.stableHash
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * The selling side of the app: your own shop, the products you have listed, what they earned,
 * and which of your videos each product is attached to.
 */
@HiltViewModel
class CreatorViewModel @Inject constructor() : ViewModel() {

    val me = SellerRepository.me
    val allProducts = ProductRepository.products
    val orders = OrderRepository.orders

    /** Products the creator picked while publishing a video, before the video is posted. */
    private val _pendingVideoTags = MutableStateFlow<List<String>>(emptyList())
    val pendingVideoTags = _pendingVideoTags.asStateFlow()

    private val _lastListedProduct = MutableStateFlow<Product?>(null)
    val lastListedProduct = _lastListedProduct.asStateFlow()

    fun myProducts(): List<Product> = ProductRepository.productsOf(SellerRepository.MY_SELLER_ID)

    fun myOrders(): List<Order> = OrderRepository.ordersForSeller(SellerRepository.MY_SELLER_ID)

    fun myGrossRevenueCents(): Long =
        OrderRepository.grossRevenueCentsForSeller(SellerRepository.MY_SELLER_ID)

    fun myUnitsSold(): Int = OrderRepository.unitsSoldForSeller(SellerRepository.MY_SELLER_ID)

    /** The platform takes a flat commission; creators see net, not gross, on the dashboard. */
    fun myNetPayoutCents(): Long =
        myGrossRevenueCents() - (myGrossRevenueCents() * COMMISSION_BASIS_POINTS / 10_000L)

    fun updateShop(displayName: String, handle: String, bio: String, emoji: String) =
        SellerRepository.updateShop(displayName, handle, bio, emoji)

    fun seller(id: String?): Seller? = ProductRepository.seller(id)

    fun sellers(): List<Seller> = CommerceCatalog.sellers

    fun productsOf(sellerId: String): List<Product> = ProductRepository.productsOf(sellerId)

    fun listProduct(
        title: String,
        description: String,
        priceCents: Long,
        compareAtPriceCents: Long?,
        category: ProductCategory,
        emoji: String,
        stock: Int,
        optionName: String,
        optionValues: List<String>,
        nowMillis: Long = System.currentTimeMillis(),
    ): Product {
        val product = Product(
            id = newProductId(title, nowMillis),
            sellerId = SellerRepository.MY_SELLER_ID,
            title = title.trim(),
            description = description.trim(),
            priceCents = priceCents,
            compareAtPriceCents = compareAtPriceCents?.takeIf { it > priceCents },
            category = (if (category == ProductCategory.ALL) ProductCategory.FASHION else category).label,
            emoji = emoji.ifBlank { "🛍" },
            rating = 5.0f,
            ratingCount = 0,
            soldCount = 0,
            stock = stock.coerceAtLeast(0),
            options = if (optionName.isNotBlank() && optionValues.isNotEmpty()) {
                listOf(ProductOption(optionName.trim(), optionValues))
            } else {
                emptyList()
            },
        )
        ProductRepository.addCustomProduct(product)
        _lastListedProduct.value = product
        return product
    }

    fun unlistProduct(productId: String) = ProductRepository.removeCustomProduct(productId)

    fun togglePendingTag(productId: String) {
        val current = _pendingVideoTags.value
        _pendingVideoTags.value = if (current.contains(productId)) {
            current - productId
        } else {
            (current + productId).take(MAX_TAGS_PER_VIDEO)
        }
    }

    fun clearPendingTags() {
        _pendingVideoTags.value = emptyList()
    }

    /** Attaches the chosen products to a published video so the feed can sell them. */
    fun publishTags(videoId: String) {
        ProductRepository.tagVideo(videoId, _pendingVideoTags.value)
        _pendingVideoTags.value = emptyList()
    }

    fun tagsFor(videoId: String): List<Product> = ProductRepository.productsForVideo(videoId)

    private fun newProductId(title: String, nowMillis: Long): String {
        val slug = title.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(24)
            .ifBlank { "item" }
        return "p_me_${slug}_${stableHash(nowMillis) % 10_000}"
    }

    private companion object {
        const val COMMISSION_BASIS_POINTS = 500L // 5%
        const val MAX_TAGS_PER_VIDEO = 6
    }
}
