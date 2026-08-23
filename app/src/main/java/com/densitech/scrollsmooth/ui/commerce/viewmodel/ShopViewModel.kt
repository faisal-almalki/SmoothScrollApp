package com.densitech.scrollsmooth.ui.commerce.viewmodel

import androidx.lifecycle.ViewModel
import com.densitech.scrollsmooth.ui.commerce.data.ProductRepository
import com.densitech.scrollsmooth.ui.commerce.data.SellerRepository
import com.densitech.scrollsmooth.ui.commerce.model.Product
import com.densitech.scrollsmooth.ui.commerce.model.ProductCategory
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Browsing state for the shop tab and for any surface that needs to open a product sheet.
 */
@HiltViewModel
class ShopViewModel @Inject constructor() : ViewModel() {

    val allProducts = ProductRepository.products

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _category = MutableStateFlow(ProductCategory.ALL)
    val category = _category.asStateFlow()

    /** The product whose buy sheet is open, if any. Null closes the sheet. */
    private val _openProduct = MutableStateFlow<Product?>(null)
    val openProduct = _openProduct.asStateFlow()

    private val _results = MutableStateFlow(ProductRepository.trending())
    val results = _results.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
        refresh()
    }

    fun onCategoryChange(value: ProductCategory) {
        _category.value = value
        refresh()
    }

    fun clearQuery() {
        _query.value = ""
        refresh()
    }

    fun refresh() {
        val filtered = ProductRepository.search(_query.value, _category.value)
        _results.value = if (_query.value.isBlank()) {
            filtered.sortedWith(
                compareByDescending<Product> { it.discountPercent ?: 0 }.thenByDescending { it.soldCount }
            )
        } else {
            filtered.sortedByDescending { it.soldCount }
        }
    }

    fun openProduct(product: Product) {
        _openProduct.value = product
    }

    fun openProductById(productId: String?) {
        _openProduct.value = ProductRepository.product(productId)
    }

    fun closeProduct() {
        _openProduct.value = null
    }

    fun product(id: String?): Product? = ProductRepository.product(id)

    fun seller(id: String?): Seller? = ProductRepository.seller(id)

    fun productsOf(sellerId: String): List<Product> = ProductRepository.productsOf(sellerId)

    fun productsForVideo(videoId: String): List<Product> = ProductRepository.productsForVideo(videoId)

    fun isFollowing(sellerId: String): Boolean = SellerRepository.isFollowing(sellerId)

    val following = SellerRepository.following

    fun toggleFollow(sellerId: String) = SellerRepository.toggleFollow(sellerId)

    /** Other listings by the same creator, used as a "more from this shop" rail. */
    fun moreFromSeller(product: Product, limit: Int = 6): List<Product> =
        ProductRepository.productsOf(product.sellerId).filterNot { it.id == product.id }.take(limit)
}
