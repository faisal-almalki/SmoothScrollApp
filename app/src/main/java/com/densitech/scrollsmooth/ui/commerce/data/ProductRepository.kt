package com.densitech.scrollsmooth.ui.commerce.data

import com.densitech.scrollsmooth.ui.commerce.model.Product
import com.densitech.scrollsmooth.ui.commerce.model.ProductCategory
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.commerce.model.VideoProductTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The catalogue as the UI sees it: the seeded storefront plus anything the signed in creator has
 * listed, plus the video to product tags that make the feed shoppable.
 */
object ProductRepository {

    private const val KEY_CUSTOM_PRODUCTS = "custom_products"
    private const val KEY_VIDEO_TAGS = "video_tags"

    private val _customProducts = MutableStateFlow(
        CommerceStore.readOrNull<List<Product>>(KEY_CUSTOM_PRODUCTS) ?: emptyList()
    )

    private val _products = MutableStateFlow(CommerceCatalog.seedProducts() + _customProducts.value)
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    /** Explicit tags a creator attached while publishing, keyed by video id. */
    private val _videoTags = MutableStateFlow(
        CommerceStore.readOrNull<Map<String, List<String>>>(KEY_VIDEO_TAGS) ?: emptyMap()
    )
    val videoTags: StateFlow<Map<String, List<String>>> = _videoTags.asStateFlow()

    fun product(id: String?): Product? = id?.let { wanted -> _products.value.firstOrNull { it.id == wanted } }

    fun productsByIds(ids: List<String>): List<Product> = ids.mapNotNull { product(it) }

    fun productsOf(sellerId: String): List<Product> = _products.value.filter { it.sellerId == sellerId }

    fun seller(id: String?): Seller? = CommerceCatalog.seller(id) ?: SellerRepository.customSeller(id)

    fun addCustomProduct(product: Product) {
        val updated = _customProducts.value.filterNot { it.id == product.id } + product
        _customProducts.value = updated
        CommerceStore.writeValue(KEY_CUSTOM_PRODUCTS, updated)
        _products.value = CommerceCatalog.seedProducts() + updated
    }

    fun removeCustomProduct(productId: String) {
        val updated = _customProducts.value.filterNot { it.id == productId }
        _customProducts.value = updated
        CommerceStore.writeValue(KEY_CUSTOM_PRODUCTS, updated)
        _products.value = CommerceCatalog.seedProducts() + updated
    }

    /**
     * Products shown over a feed video. An explicit creator tag wins; otherwise the video is
     * tagged deterministically from its id so the demo feed is fully shoppable.
     */
    fun productsForVideo(videoId: String): List<Product> {
        val explicit = _videoTags.value[videoId]
        if (!explicit.isNullOrEmpty()) {
            val resolved = productsByIds(explicit)
            if (resolved.isNotEmpty()) return resolved
        }
        val tag: VideoProductTag = CommerceCatalog.tagForVideo(videoId, _products.value)
        return productsByIds(tag.productIds)
    }

    fun tagVideo(videoId: String, productIds: List<String>) {
        val updated = _videoTags.value.toMutableMap()
        if (productIds.isEmpty()) updated.remove(videoId) else updated[videoId] = productIds
        _videoTags.value = updated
        CommerceStore.writeValue<Map<String, List<String>>>(KEY_VIDEO_TAGS, updated)
    }

    fun search(query: String, category: ProductCategory): List<Product> {
        val trimmed = query.trim()
        return _products.value.filter { product ->
            val matchesCategory = category == ProductCategory.ALL || product.category == category.label
            val matchesQuery = trimmed.isEmpty() ||
                product.title.contains(trimmed, ignoreCase = true) ||
                product.description.contains(trimmed, ignoreCase = true) ||
                product.category.contains(trimmed, ignoreCase = true) ||
                (seller(product.sellerId)?.displayName?.contains(trimmed, ignoreCase = true) == true) ||
                (seller(product.sellerId)?.handle?.contains(trimmed, ignoreCase = true) == true)
            matchesCategory && matchesQuery
        }
    }

    /** Feed ordering for the shop tab: best discounts and best sellers float up. */
    fun trending(): List<Product> = _products.value.sortedWith(
        compareByDescending<Product> { it.discountPercent ?: 0 }.thenByDescending { it.soldCount }
    )
}
